import { FormEvent, useEffect, useMemo, useState } from 'react';
import { apiClient } from '../shared/api/apiClient';
import { readStoredAccessToken } from '../shared/auth/accessTokenStore';
import { PageSection } from '../shared/components/PageSection';

type PageResponse<T> = {
  content: T[];
};

type ProjectResponse = {
  id: number;
  name: string;
  description?: string;
  defaultPreset?: string;
};

type ImageListResponse = {
  id: number;
  originalFileName: string;
  status: string;
};

type UploadSessionResponse = {
  id: number;
};

type PresignedUploadUrlResponse = {
  sessionId: number;
  uploadTargets: Array<{
    uploadFileId: number;
    uploadUrl: string;
    requiredHeaders: Record<string, string>;
  }>;
};

type JobCreateResponse = {
  jobId: number;
  status: string;
  totalImages: number;
};

type JobSummaryResponse = {
  jobId: number;
  total: number;
  queued: number;
  processing: number;
  succeeded: number;
  failed: number;
  progressPercent: number;
};

type JobItemResponse = {
  id: number;
  imageId: number;
  status: string;
  processedObjectKey?: string;
  previewObjectKey?: string;
  reportObjectKey?: string;
  errorCode?: string;
  errorMessage?: string;
};

type JobItemDownloadUrlResponse = {
  jobId: number;
  itemId: number;
  type: 'processed' | 'preview' | 'report';
  objectKey: string;
  downloadUrl: string;
  expiresAt: string;
};

type SelectedUploadFile = {
  key: string;
  file: File;
};

type FilePhase = 'selected' | 'hashing' | 'ready' | 'uploading' | 'uploaded' | 'completed' | 'failed';

type FileUploadRow = {
  key: string;
  name: string;
  size: number;
  phase: FilePhase;
  detail?: string;
};

type SmokeResult = {
  project?: ProjectResponse;
  images?: ImageListResponse[];
  job?: JobCreateResponse;
  summary?: JobSummaryResponse;
  items?: JobItemResponse[];
};

export function UploadPage() {
  const [accessToken] = useState(() => readStoredAccessToken() ?? '');
  const [projects, setProjects] = useState<ProjectResponse[]>([]);
  const [projectId, setProjectId] = useState('');
  const [projectName, setProjectName] = useState(() => `Batch ingest ${new Date().toLocaleString()}`);
  const [selectedFiles, setSelectedFiles] = useState<SelectedUploadFile[]>([]);
  const [fileRows, setFileRows] = useState<FileUploadRow[]>([]);
  const [debug, setDebug] = useState(true);
  const [busy, setBusy] = useState(false);
  const [logs, setLogs] = useState<string[]>([]);
  const [result, setResult] = useState<SmokeResult>({});
  const [error, setError] = useState<string | null>(null);

  const totalSize = useMemo(
    () => selectedFiles.reduce((sum, entry) => sum + entry.file.size, 0),
    [selectedFiles]
  );
  const completedItems = result.summary ? result.summary.succeeded + result.summary.failed : 0;

  useEffect(() => {
    if (!accessToken) {
      return;
    }
    loadProjects(accessToken).catch((exception) => setError(exception.message));
  }, [accessToken]);

  async function loadProjects(token: string) {
    const response = await apiClient.get<PageResponse<ProjectResponse>>('/v1/projects?size=50', token);
    setProjects(response.result.content);
    if (!projectId && response.result.content.length > 0) {
      setProjectId(String(response.result.content[0].id));
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!accessToken) {
      setError('Google login is required before running a batch preprocess job.');
      return;
    }
    if (selectedFiles.length === 0) {
      setError('Select at least one document image.');
      return;
    }

    setBusy(true);
    setError(null);
    setLogs([]);
    setResult({});

    try {
      const project = await ensureProject();
      appendLog(`Project ready: ${project.name} (#${project.id})`);

      const existingImageIds = await runStep('Load existing project images', () => loadExistingImageIds(project.id));
      const hashedFiles = await runStep('Calculate SHA-256 checksums', () => hashSelectedFiles());
      appendLog(`SHA-256 calculated for ${hashedFiles.length} files.`);

      const session = await runStep('Create upload session', () => createUploadSession(project, selectedFiles));
      appendLog(`Upload session created: #${session.id}`);

      const uploadTargets = await runStep('Issue presigned upload URLs', () => createPresignedTargets(session, hashedFiles));
      appendLog(`Presigned upload URLs issued: ${uploadTargets.length}`);

      await runStep('Upload originals to object storage', () => uploadFilesToObjectStorage(uploadTargets));
      appendLog('Original images uploaded to MinIO.');

      await runStep('Complete upload session', () => completeUpload(session, uploadTargets.map((target) => target.uploadFileId)));
      appendLog('Upload session completed.');

      const images = await runStep(
        'Resolve uploaded image metadata',
        () => findCreatedImages(project.id, selectedFiles, existingImageIds)
      );
      appendLog(`Image metadata created: ${images.length}`);

      const job = await runStep('Create preprocessing job', () => createJob(project, images));
      appendLog(`Preprocess job queued: #${job.jobId} (${job.totalImages} images)`);

      const smokeResult: SmokeResult = { project, images, job };
      setResult(smokeResult);
      await runStep('Poll preprocessing job', () => pollJob(job.jobId, smokeResult));
    } catch (exception) {
      setError(describeError(exception, 'Batch preprocess flow failed.'));
    } finally {
      setBusy(false);
    }
  }

  async function ensureProject() {
    if (projectId) {
      const selected = projects.find((project) => project.id === Number(projectId));
      if (selected) {
        return selected;
      }
    }
    const response = await apiClient.post<ProjectResponse>(
      '/v1/projects',
      {
        name: projectName,
        description: 'Created by the local batch upload flow.',
        defaultPreset: 'A4_SCAN_300DPI'
      },
      accessToken
    );
    await loadProjects(accessToken);
    setProjectId(String(response.result.id));
    return response.result;
  }

  async function loadExistingImageIds(projectIdValue: number) {
    const response = await apiClient.get<PageResponse<ImageListResponse>>(
      `/v1/projects/${projectIdValue}/images?size=500`,
      accessToken
    );
    return new Set(response.result.content.map((image) => image.id));
  }

  async function hashSelectedFiles() {
    return Promise.all(
      selectedFiles.map(async (entry) => {
        updateFileRow(entry.key, { phase: 'hashing', detail: 'Calculating checksum' });
        const checksumSha256 = await sha256(entry.file);
        updateFileRow(entry.key, { phase: 'ready', detail: checksumSha256.slice(0, 12) });
        return { ...entry, checksumSha256 };
      })
    );
  }

  async function createUploadSession(project: ProjectResponse, entries: SelectedUploadFile[]) {
    const response = await apiClient.post<UploadSessionResponse>(
      `/v1/projects/${project.id}/upload-sessions`,
      {
        expectedFileCount: entries.length,
        expectedTotalSizeBytes: entries.reduce((sum, entry) => sum + entry.file.size, 0)
      },
      accessToken
    );
    return response.result;
  }

  async function createPresignedTargets(
    session: UploadSessionResponse,
    entries: Array<SelectedUploadFile & { checksumSha256: string }>
  ) {
    const response = await apiClient.post<PresignedUploadUrlResponse>(
      `/v1/upload-sessions/${session.id}/files/presigned-url`,
      {
        files: entries.map((entry) => ({
          fileName: entry.file.name,
          contentType: entry.file.type || 'image/png',
          sizeBytes: entry.file.size,
          checksumSha256: entry.checksumSha256
        }))
      },
      accessToken
    );
    if (response.result.uploadTargets.length !== entries.length) {
      throw new Error('Presigned upload target count does not match selected file count.');
    }
    return response.result.uploadTargets.map((target, index) => ({
      ...target,
      entry: entries[index]
    }));
  }

  async function uploadFilesToObjectStorage(
    targets: Array<PresignedUploadUrlResponse['uploadTargets'][number] & { entry: SelectedUploadFile }>
  ) {
    await Promise.all(
      targets.map(async (target) => {
        updateFileRow(target.entry.key, { phase: 'uploading', detail: 'Uploading original' });
        const response = await uploadFile(target);
        if (!response.ok) {
          updateFileRow(target.entry.key, { phase: 'failed', detail: `HTTP ${response.status}` });
          throw new Error(`MinIO upload failed for ${target.entry.file.name} with HTTP ${response.status}.`);
        }
        updateFileRow(target.entry.key, { phase: 'uploaded', detail: 'Stored in MinIO' });
      })
    );
  }

  async function completeUpload(session: UploadSessionResponse, uploadFileIds: number[]) {
    await apiClient.post(
      `/v1/upload-sessions/${session.id}/complete`,
      { uploadFileIds },
      accessToken
    );
    setFileRows((previous) => previous.map((row) => ({ ...row, phase: 'completed', detail: 'Metadata ready' })));
  }

  async function findCreatedImages(
    projectIdValue: number,
    entries: SelectedUploadFile[],
    existingImageIds: Set<number>
  ) {
    const response = await apiClient.get<PageResponse<ImageListResponse>>(
      `/v1/projects/${projectIdValue}/images?size=500`,
      accessToken
    );
    const selectedNames = new Set(entries.map((entry) => entry.file.name));
    const images = response.result.content
      .filter((candidate) => !existingImageIds.has(candidate.id) && selectedNames.has(candidate.originalFileName))
      .sort((left, right) => left.id - right.id);
    if (images.length < entries.length) {
      throw new Error(`Only ${images.length}/${entries.length} uploaded image metadata rows were found.`);
    }
    return images.slice(-entries.length);
  }

  async function createJob(project: ProjectResponse, images: ImageListResponse[]) {
    const response = await apiClient.post<JobCreateResponse>(
      '/v1/jobs',
      {
        projectId: project.id,
        imageIds: images.map((image) => image.id),
        preset: project.defaultPreset || 'A4_SCAN_300DPI',
        presetParameters: { targetDpi: '300' },
        debug,
        priority: 'NORMAL',
        outputOptions: {
          saveProcessedImage: true,
          savePreview: true,
          saveReportJson: true,
          saveDebugArtifacts: debug
        }
      },
      accessToken
    );
    return response.result;
  }

  async function pollJob(jobId: number, smokeResult: SmokeResult) {
    for (let attempt = 0; attempt < 60; attempt++) {
      await delay(1500);
      const [summaryResponse, itemsResponse] = await Promise.all([
        apiClient.get<JobSummaryResponse>(`/v1/jobs/${jobId}/summary`, accessToken),
        apiClient.get<PageResponse<JobItemResponse>>(`/v1/jobs/${jobId}/items?size=200`, accessToken)
      ]);
      const next = {
        ...smokeResult,
        summary: summaryResponse.result,
        items: itemsResponse.result.content
      };
      setResult(next);
      appendLog(
        `Job progress: ${summaryResponse.result.progressPercent.toFixed(1)}% `
        + `(${summaryResponse.result.succeeded} succeeded / ${summaryResponse.result.failed} failed)`
      );
      if (summaryResponse.result.succeeded + summaryResponse.result.failed >= summaryResponse.result.total) {
        return;
      }
    }
    appendLog('Polling timed out. Refresh the job summary after the Worker finishes.');
  }

  async function downloadArtifact(item: JobItemResponse, type: JobItemDownloadUrlResponse['type']) {
    if (!result.job) {
      setError('Job result is not ready yet.');
      return;
    }
    try {
      const response = await apiClient.get<JobItemDownloadUrlResponse>(
        `/v1/jobs/${result.job.jobId}/items/${item.id}/download?type=${type}`,
        accessToken
      );
      const anchor = document.createElement('a');
      anchor.href = response.result.downloadUrl;
      anchor.target = '_blank';
      anchor.rel = 'noopener noreferrer';
      anchor.download = downloadFileName(response.result);
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      appendLog(`Download URL opened: ${type} for item #${item.id}`);
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : `Failed to download ${type}.`);
    }
  }

  function handleFileSelection(files: FileList | null) {
    const nextFiles = Array.from(files ?? []).map((file, index) => ({
      key: `${file.name}-${file.size}-${file.lastModified}-${index}`,
      file
    }));
    setSelectedFiles(nextFiles);
    setFileRows(nextFiles.map((entry) => ({
      key: entry.key,
      name: entry.file.name,
      size: entry.file.size,
      phase: 'selected',
      detail: 'Waiting'
    })));
    setResult({});
    setError(null);
  }

  function updateFileRow(key: string, patch: Partial<FileUploadRow>) {
    setFileRows((previous) => previous.map((row) => (row.key === key ? { ...row, ...patch } : row)));
  }

  function appendLog(message: string) {
    setLogs((previous) => [...previous, `${new Date().toLocaleTimeString()} ${message}`]);
  }

  async function uploadFile(
    target: PresignedUploadUrlResponse['uploadTargets'][number] & { entry: SelectedUploadFile }
  ) {
    try {
      return await fetch(target.uploadUrl, {
        method: 'PUT',
        headers: target.requiredHeaders,
        body: target.entry.file
      });
    } catch (exception) {
      updateFileRow(target.entry.key, { phase: 'failed', detail: 'Network/CORS failure' });
      const targetOrigin = safeOrigin(target.uploadUrl);
      throw new Error(
        `Object storage upload request failed for ${target.entry.file.name}. `
        + `pageOrigin=${window.location.origin}, targetOrigin=${targetOrigin}. `
        + describeError(exception, 'Browser fetch failed.')
      );
    }
  }

  return (
    <div className="upload-console">
      <section className="console-hero">
        <div>
          <span className="status-pill accent">Batch pipeline</span>
          <h2>Prepare scanned documents for OCR at queue scale.</h2>
          <p>
            Upload multiple scans, let the API create one JobItem per image, and watch the Worker produce processed
            images, previews, reports, and optional debug artifacts.
          </p>
        </div>
        <div className="session-card">
          <span className={`status-dot ${accessToken ? 'online' : 'offline'}`} />
          <strong>{accessToken ? 'Authenticated' : 'Login required'}</strong>
          <small>Access token value is hidden. Refresh token stays in an HttpOnly cookie.</small>
        </div>
      </section>

      {!accessToken && (
        <div className="status-card warning">
          <strong>Google session required</strong>
          <span>Sign in first. The OAuth callback now refreshes the access token without exposing it in the URL.</span>
          <a className="primary-action" href="/login">Go to login</a>
        </div>
      )}

      <section className="metric-strip">
        <MetricCard label="Selected" value={String(selectedFiles.length)} detail="images" />
        <MetricCard label="Batch size" value={formatBytes(totalSize)} detail="original input" />
        <MetricCard
          label="Job progress"
          value={`${result.summary?.progressPercent.toFixed(0) ?? '0'}%`}
          detail={result.summary ? `${completedItems}/${result.summary.total} done` : 'not started'}
        />
        <MetricCard label="Artifacts" value={String(result.items?.filter((item) => item.processedObjectKey).length ?? 0)} detail="ready" />
      </section>

      <form className="upload-layout" onSubmit={handleSubmit}>
        <div className="control-panel">
          <PageSection
            title="1. Scope"
            description="Choose an existing project or create a fresh batch workspace."
          >
            <label className="field">
              Existing project
              <select value={projectId} onChange={(event) => setProjectId(event.target.value)}>
                <option value="">Create a new project</option>
                {projects.map((project) => (
                  <option key={project.id} value={project.id}>
                    #{project.id} {project.name}
                  </option>
                ))}
              </select>
            </label>

            {!projectId && (
              <label className="field">
                New project name
                <input value={projectName} onChange={(event) => setProjectName(event.target.value)} />
              </label>
            )}

            <label className="check-row">
              <input type="checkbox" checked={debug} onChange={(event) => setDebug(event.target.checked)} />
              Save per-step debug artifacts
            </label>
          </PageSection>

          <PageSection
            title="2. Files"
            description="Select multiple document images. Each image becomes a separate queue message."
          >
            <label className="file-dropzone">
              <span>Drop zone</span>
              <strong>{selectedFiles.length > 0 ? `${selectedFiles.length} files ready` : 'Choose document images'}</strong>
              <small>PNG, JPEG, WEBP, BMP, or TIFF. Duplicate checksums are rejected per project.</small>
              <input
                type="file"
                multiple
                accept="image/png,image/jpeg,image/jpg,image/webp,image/bmp,image/tiff"
                onChange={(event) => handleFileSelection(event.target.files)}
              />
            </label>

            <button className="primary-action" type="submit" disabled={busy || !accessToken}>
              {busy ? 'Running batch...' : 'Upload batch and preprocess'}
            </button>
          </PageSection>
        </div>

        <div className="batch-panel">
          <PageSection
            title="3. Batch monitor"
            description="Track upload preparation, Worker progress, and downloadable artifacts."
          >
            {error && (
              <div className="status-card error">
                <strong>Batch flow failed</strong>
                <span>{error}</span>
              </div>
            )}

            {fileRows.length > 0 ? (
              <div className="file-list">
                {fileRows.map((row) => (
                  <div className="file-row" key={row.key}>
                    <div>
                      <strong>{row.name}</strong>
                      <small>{formatBytes(row.size)}</small>
                    </div>
                    <span className={`status-pill ${row.phase}`}>{row.phase}</span>
                    <small>{row.detail}</small>
                  </div>
                ))}
              </div>
            ) : (
              <div className="empty-state">
                <strong>No files selected</strong>
                <span>Select a small batch first, then scale up after the flow is verified.</span>
              </div>
            )}

            {result.summary && (
              <div className="progress-card">
                <div>
                  <strong>Job #{result.summary.jobId}</strong>
                  <span>{result.summary.succeeded} succeeded, {result.summary.failed} failed, {result.summary.processing} processing</span>
                </div>
                <div className="progress-track">
                  <span style={{ width: `${Math.min(result.summary.progressPercent, 100)}%` }} />
                </div>
              </div>
            )}

            {logs.length > 0 && (
              <div className="log-panel">
                {logs.map((log) => (
                  <span key={log}>{log}</span>
                ))}
              </div>
            )}
          </PageSection>
        </div>
      </form>

      {result.items && result.items.length > 0 && (
        <section className="artifact-board">
          <div>
            <span className="status-pill accent">Results</span>
            <h2>Worker artifacts</h2>
          </div>
          <div className="artifact-grid">
            {result.items.map((item) => (
              <div className="artifact-card" key={item.id}>
                <div className="artifact-title">
                  <strong>Item #{item.id}</strong>
                  <span className={`status-pill ${item.status.toLowerCase()}`}>{item.status}</span>
                </div>
                <small>Image #{item.imageId}</small>
                {item.errorMessage && <span className="error-text">{item.errorCode}: {item.errorMessage}</span>}
                {item.processedObjectKey && <code>{item.processedObjectKey}</code>}
                {item.previewObjectKey && <code>{item.previewObjectKey}</code>}
                {item.reportObjectKey && <code>{item.reportObjectKey}</code>}
                <div className="download-actions">
                  <button
                    type="button"
                    className="secondary-action"
                    disabled={!item.processedObjectKey}
                    onClick={() => downloadArtifact(item, 'processed')}
                  >
                    Processed
                  </button>
                  <button
                    type="button"
                    className="secondary-action"
                    disabled={!item.previewObjectKey}
                    onClick={() => downloadArtifact(item, 'preview')}
                  >
                    Preview
                  </button>
                  <button
                    type="button"
                    className="secondary-action"
                    disabled={!item.reportObjectKey}
                    onClick={() => downloadArtifact(item, 'report')}
                  >
                    Report
                  </button>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}

async function runStep<T>(stepName: string, action: () => Promise<T>): Promise<T> {
  try {
    return await action();
  } catch (exception) {
    throw new Error(`${stepName} failed. ${describeError(exception, 'Unknown error.')}`);
  }
}

function describeError(exception: unknown, fallback: string) {
  if (exception instanceof Error && exception.message) {
    return exception.message;
  }
  return fallback;
}

function safeOrigin(url: string) {
  try {
    return new URL(url).origin;
  } catch {
    return 'invalid-url';
  }
}

function MetricCard({ label, value, detail }: { label: string; value: string; detail: string }) {
  return (
    <div className="metric-card">
      <span>{label}</span>
      <strong>{value}</strong>
      <small>{detail}</small>
    </div>
  );
}

async function sha256(file: File) {
  const digest = await window.crypto.subtle.digest('SHA-256', await file.arrayBuffer());
  return Array.from(new Uint8Array(digest))
    .map((byte) => byte.toString(16).padStart(2, '0'))
    .join('');
}

function delay(milliseconds: number) {
  return new Promise((resolve) => window.setTimeout(resolve, milliseconds));
}

function downloadFileName(target: JobItemDownloadUrlResponse) {
  const extension = target.type === 'report' ? 'json' : 'png';
  return `job-${target.jobId}-item-${target.itemId}-${target.type}.${extension}`;
}

function formatBytes(bytes: number) {
  if (bytes === 0) {
    return '0 B';
  }
  const units = ['B', 'KB', 'MB', 'GB'];
  const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  return `${(bytes / 1024 ** index).toFixed(index === 0 ? 0 : 1)} ${units[index]}`;
}
