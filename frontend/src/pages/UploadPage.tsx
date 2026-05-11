import { FormEvent, useEffect, useState } from 'react';
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

type SmokeResult = {
  project?: ProjectResponse;
  image?: ImageListResponse;
  job?: JobCreateResponse;
  summary?: JobSummaryResponse;
  items?: JobItemResponse[];
};

export function UploadPage() {
  const [accessToken, setAccessToken] = useState(() => readStoredAccessToken() ?? '');
  const [projects, setProjects] = useState<ProjectResponse[]>([]);
  const [projectId, setProjectId] = useState('');
  const [projectName, setProjectName] = useState(() => `Local smoke ${new Date().toLocaleString()}`);
  const [file, setFile] = useState<File | null>(null);
  const [debug, setDebug] = useState(true);
  const [busy, setBusy] = useState(false);
  const [logs, setLogs] = useState<string[]>([]);
  const [result, setResult] = useState<SmokeResult>({});
  const [error, setError] = useState<string | null>(null);

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
      setError('Google login is required before running the smoke test.');
      return;
    }
    if (!file) {
      setError('Select one image file first.');
      return;
    }

    setBusy(true);
    setError(null);
    setLogs([]);
    setResult({});

    try {
      const project = await ensureProject();
      appendLog(`Project ready: ${project.name} (#${project.id})`);

      const checksumSha256 = await sha256(file);
      appendLog(`SHA-256 calculated: ${checksumSha256.slice(0, 12)}...`);

      const session = await createUploadSession(project, file);
      appendLog(`Upload session created: #${session.id}`);

      const uploadTarget = await createPresignedTarget(session, file, checksumSha256);
      appendLog('Presigned upload URL issued.');

      await uploadToObjectStorage(uploadTarget, file);
      appendLog('Original image uploaded to MinIO.');

      await completeUpload(session, uploadTarget.uploadFileId);
      appendLog('Upload session completed.');

      const image = await findLatestImage(project.id, file.name);
      appendLog(`Image metadata created: #${image.id}`);

      const job = await createJob(project, image);
      appendLog(`Preprocess job queued: #${job.jobId}`);

      const smokeResult: SmokeResult = { project, image, job };
      setResult(smokeResult);
      await pollJob(job.jobId, smokeResult);
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Smoke test failed.');
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
        description: 'Created by the local frontend smoke flow.',
        defaultPreset: 'A4_SCAN_300DPI'
      },
      accessToken
    );
    await loadProjects(accessToken);
    setProjectId(String(response.result.id));
    return response.result;
  }

  async function createUploadSession(project: ProjectResponse, selectedFile: File) {
    const response = await apiClient.post<UploadSessionResponse>(
      `/v1/projects/${project.id}/upload-sessions`,
      {
        expectedFileCount: 1,
        expectedTotalSizeBytes: selectedFile.size
      },
      accessToken
    );
    return response.result;
  }

  async function createPresignedTarget(
    session: UploadSessionResponse,
    selectedFile: File,
    checksumSha256: string
  ) {
    const response = await apiClient.post<PresignedUploadUrlResponse>(
      `/v1/upload-sessions/${session.id}/files/presigned-url`,
      {
        files: [
          {
            fileName: selectedFile.name,
            contentType: selectedFile.type || 'image/png',
            sizeBytes: selectedFile.size,
            checksumSha256
          }
        ]
      },
      accessToken
    );
    return response.result.uploadTargets[0];
  }

  async function uploadToObjectStorage(
    target: PresignedUploadUrlResponse['uploadTargets'][number],
    selectedFile: File
  ) {
    const response = await fetch(target.uploadUrl, {
      method: 'PUT',
      headers: target.requiredHeaders,
      body: selectedFile
    });
    if (!response.ok) {
      throw new Error(`MinIO upload failed with HTTP ${response.status}.`);
    }
  }

  async function completeUpload(session: UploadSessionResponse, uploadFileId: number) {
    await apiClient.post(
      `/v1/upload-sessions/${session.id}/complete`,
      { uploadFileIds: [uploadFileId] },
      accessToken
    );
  }

  async function findLatestImage(projectIdValue: number, fileName: string) {
    const response = await apiClient.get<PageResponse<ImageListResponse>>(
      `/v1/projects/${projectIdValue}/images?size=100`,
      accessToken
    );
    const image = response.result.content
      .filter((candidate) => candidate.originalFileName === fileName)
      .sort((left, right) => right.id - left.id)[0];
    if (!image) {
      throw new Error('Uploaded image metadata was not found.');
    }
    return image;
  }

  async function createJob(project: ProjectResponse, image: ImageListResponse) {
    const response = await apiClient.post<JobCreateResponse>(
      '/v1/jobs',
      {
        projectId: project.id,
        imageIds: [image.id],
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
    for (let attempt = 0; attempt < 30; attempt++) {
      await delay(1500);
      const [summaryResponse, itemsResponse] = await Promise.all([
        apiClient.get<JobSummaryResponse>(`/v1/jobs/${jobId}/summary`, accessToken),
        apiClient.get<PageResponse<JobItemResponse>>(`/v1/jobs/${jobId}/items?size=20`, accessToken)
      ]);
      const next = {
        ...smokeResult,
        summary: summaryResponse.result,
        items: itemsResponse.result.content
      };
      setResult(next);
      appendLog(`Job progress: ${summaryResponse.result.progressPercent.toFixed(1)}%`);
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
      appendLog(`Download URL opened: ${type}`);
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : `Failed to download ${type}.`);
    }
  }

  function appendLog(message: string) {
    setLogs((previous) => [...previous, `${new Date().toLocaleTimeString()} ${message}`]);
  }

  return (
    <PageSection
      title="Local image preprocess smoke"
      description="Upload one image through the frontend, create a preprocessing job, and poll the Worker result."
    >
      {!accessToken && (
        <div className="status-card warning">
          <strong>Login required</strong>
          <span>Use Google login first. The OAuth success page stores the access token in this browser.</span>
          <a className="primary-action" href="/login">Go to login</a>
        </div>
      )}

      <form className="smoke-form" onSubmit={handleSubmit}>
        <label>
          Access token
          <input value={accessToken} onChange={(event) => setAccessToken(event.target.value)} />
        </label>

        <label>
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
          <label>
            New project name
            <input value={projectName} onChange={(event) => setProjectName(event.target.value)} />
          </label>
        )}

        <label>
          Document image
          <input
            type="file"
            accept="image/png,image/jpeg,image/jpg,image/webp,image/bmp,image/tiff"
            onChange={(event) => setFile(event.target.files?.[0] ?? null)}
          />
        </label>

        <label className="check-row">
          <input type="checkbox" checked={debug} onChange={(event) => setDebug(event.target.checked)} />
          Save per-step debug artifacts
        </label>

        <button className="primary-action" type="submit" disabled={busy}>
          {busy ? 'Running smoke flow...' : 'Upload and preprocess'}
        </button>
      </form>

      {error && (
        <div className="status-card error">
          <strong>Smoke flow failed</strong>
          <span>{error}</span>
        </div>
      )}

      {logs.length > 0 && (
        <div className="log-panel">
          {logs.map((log) => (
            <span key={log}>{log}</span>
          ))}
        </div>
      )}

      {result.job && (
        <div className="status-grid">
          <div className="status-card">
            <strong>Job #{result.job.jobId}</strong>
            <span>{result.summary?.progressPercent.toFixed(1) ?? '0.0'}% complete</span>
          </div>
          <div className="status-card">
            <strong>Image</strong>
            <span>#{result.image?.id} {result.image?.originalFileName}</span>
          </div>
        </div>
      )}

      {result.items?.map((item) => (
        <div className="artifact-card" key={item.id}>
          <strong>Item #{item.id}: {item.status}</strong>
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
              Download processed
            </button>
            <button
              type="button"
              className="secondary-action"
              disabled={!item.previewObjectKey}
              onClick={() => downloadArtifact(item, 'preview')}
            >
              Download preview
            </button>
            <button
              type="button"
              className="secondary-action"
              disabled={!item.reportObjectKey}
              onClick={() => downloadArtifact(item, 'report')}
            >
              Download report
            </button>
          </div>
        </div>
      ))}
    </PageSection>
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
