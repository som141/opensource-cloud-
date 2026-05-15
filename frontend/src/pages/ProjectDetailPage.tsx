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
  description?: string | null;
  defaultPreset?: string | null;
  status: string;
  ownerEmail: string;
  myRole: string;
  createdAt: string;
  updatedAt: string;
};

type ProjectSummaryResponse = {
  projectId: number;
  name: string;
  memberCount: number;
  imageCount: number;
  jobCount: number;
};

type ImageListResponse = {
  id: number;
  originalFileName: string;
  contentType: string;
  sizeBytes: number;
  status: string;
  createdAt: string;
};

type JobResponse = {
  id: number;
  projectId: number;
  preset: string;
  debug: boolean;
  priority: string;
  status: string;
  totalCount: number;
  succeededCount: number;
  failedCount: number;
  createdAt: string;
};

const presets = ['A4_SCAN_300DPI', 'LOW_CONTRAST_SCAN', 'RECEIPT', 'NOISY_SCAN', 'AUTO'];

export function ProjectDetailPage() {
  const projectId = useMemo(() => {
    const segments = window.location.pathname.split('/').filter(Boolean);
    return Number(segments[segments.length - 1]);
  }, []);
  const [project, setProject] = useState<ProjectResponse | null>(null);
  const [summary, setSummary] = useState<ProjectSummaryResponse | null>(null);
  const [images, setImages] = useState<ImageListResponse[]>([]);
  const [jobs, setJobs] = useState<JobResponse[]>([]);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [defaultPreset, setDefaultPreset] = useState('A4_SCAN_300DPI');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadProject();
  }, [projectId]);

  async function loadProject() {
    if (!Number.isFinite(projectId)) {
      setError('Invalid project id.');
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const [projectResponse, summaryResponse, imageResponse, jobResponse] = await Promise.all([
        apiClient.get<ProjectResponse>(`/v1/projects/${projectId}`, readStoredAccessToken()),
        apiClient.get<ProjectSummaryResponse>(`/v1/projects/${projectId}/summary`, readStoredAccessToken()),
        apiClient.get<PageResponse<ImageListResponse>>(`/v1/projects/${projectId}/images?size=100`, readStoredAccessToken()),
        apiClient.get<PageResponse<JobResponse>>('/v1/jobs?size=100', readStoredAccessToken())
      ]);
      const nextProject = projectResponse.result;
      setProject(nextProject);
      setSummary(summaryResponse.result);
      setImages(imageResponse.result.content);
      setJobs(jobResponse.result.content.filter((job) => job.projectId === projectId));
      setName(nextProject.name);
      setDescription(nextProject.description ?? '');
      setDefaultPreset(nextProject.defaultPreset || 'A4_SCAN_300DPI');
    } catch (exception) {
      setError(describeError(exception, 'Failed to load project.'));
    } finally {
      setLoading(false);
    }
  }

  async function updateProject(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!project) {
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const response = await apiClient.patch<ProjectResponse>(
        `/v1/projects/${project.id}`,
        {
          name: name.trim(),
          description: description.trim() || null,
          defaultPreset
        },
        readStoredAccessToken()
      );
      setProject(response.result);
      setName(response.result.name);
      setDescription(response.result.description ?? '');
      setDefaultPreset(response.result.defaultPreset || 'A4_SCAN_300DPI');
      await loadProject();
    } catch (exception) {
      setError(describeError(exception, 'Failed to update project.'));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="project-console">
      {error && (
        <div className="status-card error">
          <strong>Project detail failed</strong>
          <span>{error}</span>
        </div>
      )}

      <section className="console-hero">
        <div>
          <span className="status-pill accent">Project detail</span>
          <h2>{project?.name ?? 'Loading project workspace'}</h2>
          <p>
            Review images, preprocessing jobs, and the default preset before sending more scanned documents through the
            worker queue.
          </p>
        </div>
        <div className="session-card">
          <span className={`status-dot ${project ? 'online' : 'offline'}`} />
          <strong>{project?.myRole ?? (loading ? 'Loading' : 'Unavailable')}</strong>
          <small>{project?.ownerEmail ?? 'Project membership required'}</small>
        </div>
      </section>

      <section className="metric-strip">
        <MetricCard label="Images" value={String(summary?.imageCount ?? 0)} detail="uploaded" />
        <MetricCard label="Jobs" value={String(summary?.jobCount ?? jobs.length)} detail="created" />
        <MetricCard label="Members" value={String(summary?.memberCount ?? 0)} detail="with access" />
        <MetricCard label="Preset" value={project?.defaultPreset || 'A4'} detail="default" />
      </section>

      <div className="project-layout">
        <PageSection title="Project settings" description="Owners and editors can adjust the metadata used by new jobs.">
          <form className="stack-form" onSubmit={updateProject}>
            <label className="field">
              Name
              <input value={name} onChange={(event) => setName(event.target.value)} disabled={!project} />
            </label>
            <label className="field">
              Description
              <textarea value={description} onChange={(event) => setDescription(event.target.value)} disabled={!project} />
            </label>
            <label className="field">
              Default preset
              <select value={defaultPreset} onChange={(event) => setDefaultPreset(event.target.value)} disabled={!project}>
                {presets.map((preset) => (
                  <option key={preset} value={preset}>
                    {preset}
                  </option>
                ))}
              </select>
            </label>
            <div className="download-actions">
              <button className="primary-action" type="submit" disabled={!project || saving}>
                {saving ? 'Saving...' : 'Save changes'}
              </button>
              <a className="secondary-action" href="/upload">
                Upload more images
              </a>
            </div>
          </form>
        </PageSection>

        <PageSection title="Recent images" description="Images currently attached to this project.">
          {images.length === 0 ? (
            <div className="empty-state">
              <strong>No images yet</strong>
              <span>Upload document images from the batch upload page.</span>
            </div>
          ) : (
            <div className="file-list">
              {images.map((image) => (
                <a className="file-row link-row" href={`/images/${image.id}`} key={image.id}>
                  <div>
                    <strong>{image.originalFileName}</strong>
                    <small>{image.contentType} · {formatBytes(image.sizeBytes)}</small>
                  </div>
                  <span className={`status-pill ${image.status.toLowerCase()}`}>{image.status}</span>
                  <small>{formatDate(image.createdAt)}</small>
                </a>
              ))}
            </div>
          )}
        </PageSection>
      </div>

      <section className="artifact-board">
        <div>
          <span className="status-pill accent">Jobs</span>
          <h2>Recent preprocessing work</h2>
        </div>
        {jobs.length === 0 ? (
          <div className="empty-state">
            <strong>No jobs yet</strong>
            <span>Create a preprocessing job from the upload page after images are added.</span>
          </div>
        ) : (
          <div className="artifact-grid">
            {jobs.map((job) => (
              <article className="artifact-card" key={job.id}>
                <div className="artifact-title">
                  <strong>Job #{job.id}</strong>
                  <span className={`status-pill ${job.status.toLowerCase()}`}>{job.status}</span>
                </div>
                <div className="metadata-grid">
                  <span>Preset</span>
                  <strong>{job.preset}</strong>
                  <span>Images</span>
                  <strong>{job.totalCount}</strong>
                  <span>Success / Failed</span>
                  <strong>{job.succeededCount} / {job.failedCount}</strong>
                </div>
                <div className="download-actions">
                  <a className="secondary-action" href={`/jobs/${job.id}`}>
                    Open job
                  </a>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
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

function describeError(exception: unknown, fallback: string) {
  return exception instanceof Error && exception.message ? exception.message : fallback;
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(new Date(value));
}

function formatBytes(bytes: number) {
  if (bytes === 0) {
    return '0 B';
  }
  const units = ['B', 'KB', 'MB', 'GB'];
  const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  return `${(bytes / 1024 ** index).toFixed(index === 0 ? 0 : 1)} ${units[index]}`;
}
