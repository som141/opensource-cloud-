import { useEffect, useMemo, useState } from 'react';
import { apiClient } from '../shared/api/apiClient';
import { readStoredAccessToken } from '../shared/auth/accessTokenStore';
import { PageSection } from '../shared/components/PageSection';

type PageResponse<T> = {
  content: T[];
};

type JobResponse = {
  id: number;
  projectId: number;
  userId: number;
  preset: string;
  presetParameters: Record<string, string>;
  debug: boolean;
  priority: string;
  status: string;
  totalCount: number;
  queuedCount: number;
  processingCount: number;
  succeededCount: number;
  failedCount: number;
  createdAt: string;
  startedAt?: string | null;
  completedAt?: string | null;
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
  jobId: number;
  imageId: number;
  status: string;
  attempt: number;
  workerId?: string | null;
  processedObjectKey?: string | null;
  previewObjectKey?: string | null;
  reportObjectKey?: string | null;
  errorCode?: string | null;
  errorMessage?: string | null;
  createdAt: string;
  startedAt?: string | null;
  completedAt?: string | null;
};

type JobItemDownloadUrlResponse = {
  jobId: number;
  itemId: number;
  type: string;
  objectKey: string;
  downloadUrl: string;
  expiresAt: string;
};

type JobZipDownloadResponse = {
  jobId: number;
  fileCount: number;
  objectKey: string;
  downloadUrl: string;
  expiresAt: string;
};

type JobCancelResponse = {
  jobId: number;
  status: string;
};

type JobRetryResponse = {
  jobId: number;
  status: string;
  queuedItems: number;
};

const terminalStatuses = new Set(['SUCCEEDED', 'FAILED', 'PARTIAL_SUCCEEDED', 'CANCELLED']);
const cancellableStatuses = new Set(['CREATED', 'QUEUED', 'RUNNING', 'RETRYING']);
const retryableItemStatuses = new Set(['FAILED', 'DEAD_LETTERED']);

type JobAction = 'cancel' | 'retry';

export function JobDetailPage() {
  const jobId = useMemo(() => {
    const segments = window.location.pathname.split('/').filter(Boolean);
    return Number(segments[segments.length - 1]);
  }, []);
  const [job, setJob] = useState<JobResponse | null>(null);
  const [summary, setSummary] = useState<JobSummaryResponse | null>(null);
  const [items, setItems] = useState<JobItemResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [zipDownloading, setZipDownloading] = useState(false);
  const [itemDownloading, setItemDownloading] = useState<number | null>(null);
  const [actionPending, setActionPending] = useState<JobAction | null>(null);
  const [actionNotice, setActionNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const processedItems = items.filter((item) => item.processedObjectKey);
  const retryableItems = items.filter((item) => retryableItemStatuses.has(item.status));
  const isTerminal = job ? terminalStatuses.has(job.status) : false;
  const canCancel = Boolean(job && cancellableStatuses.has(job.status) && !actionPending);
  const canRetry = retryableItems.length > 0 && !actionPending;

  useEffect(() => {
    void loadJob(true);
  }, [jobId]);

  useEffect(() => {
    if (!job || isTerminal) {
      return undefined;
    }
    const timer = window.setInterval(() => {
      void loadJob(false);
    }, 2500);
    return () => window.clearInterval(timer);
  }, [job?.id, job?.status, isTerminal]);

  async function loadJob(showLoading: boolean) {
    if (!Number.isFinite(jobId)) {
      setError('Invalid job id.');
      setLoading(false);
      return;
    }

    if (showLoading) {
      setLoading(true);
    } else {
      setRefreshing(true);
    }
    setError(null);

    try {
      const [jobResponse, summaryResponse, itemResponse] = await Promise.all([
        apiClient.get<JobResponse>(`/v1/jobs/${jobId}`, readStoredAccessToken()),
        apiClient.get<JobSummaryResponse>(`/v1/jobs/${jobId}/summary`, readStoredAccessToken()),
        apiClient.get<PageResponse<JobItemResponse>>(`/v1/jobs/${jobId}/items?size=500`, readStoredAccessToken())
      ]);
      setJob(jobResponse.result);
      setSummary(summaryResponse.result);
      setItems(itemResponse.result.content);
    } catch (exception) {
      setError(describeError(exception, 'Failed to load job.'));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }

  async function downloadProcessedImage(item: JobItemResponse) {
    setItemDownloading(item.id);
    setError(null);
    try {
      const response = await apiClient.get<JobItemDownloadUrlResponse>(
        `/v1/jobs/${jobId}/items/${item.id}/download?type=processed`,
        readStoredAccessToken()
      );
      openDownload(response.result.downloadUrl, `job-${jobId}-item-${item.id}-processed.png`);
    } catch (exception) {
      setError(describeError(exception, 'Failed to download processed image.'));
    } finally {
      setItemDownloading(null);
    }
  }

  async function downloadProcessedZip() {
    setZipDownloading(true);
    setError(null);
    try {
      const response = await apiClient.get<JobZipDownloadResponse>(
        `/v1/jobs/${jobId}/download.zip`,
        readStoredAccessToken()
      );
      openDownload(response.result.downloadUrl, `job-${response.result.jobId}-processed-results.zip`);
    } catch (exception) {
      setError(describeError(exception, 'Failed to download processed ZIP.'));
    } finally {
      setZipDownloading(false);
    }
  }

  async function cancelJob() {
    if (!job || !window.confirm(`Cancel Job #${job.id}? Queued items will be marked as cancelled.`)) {
      return;
    }

    setActionPending('cancel');
    setActionNotice(null);
    setError(null);
    try {
      const response = await apiClient.post<JobCancelResponse>(
        `/v1/jobs/${job.id}/cancel`,
        {},
        readStoredAccessToken()
      );
      setActionNotice(`Cancel requested for Job #${response.result.jobId}. Current status: ${response.result.status}.`);
      await loadJob(false);
    } catch (exception) {
      setError(describeError(exception, 'Failed to cancel job.'));
    } finally {
      setActionPending(null);
    }
  }

  async function retryFailedItems() {
    if (!job || retryableItems.length === 0) {
      return;
    }
    if (!window.confirm(`Retry ${retryableItems.length} failed items in Job #${job.id}?`)) {
      return;
    }

    setActionPending('retry');
    setActionNotice(null);
    setError(null);
    try {
      const response = await apiClient.post<JobRetryResponse>(
        `/v1/jobs/${job.id}/retry`,
        {},
        readStoredAccessToken()
      );
      setActionNotice(
        `Retry queued ${response.result.queuedItems} items for Job #${response.result.jobId}. `
        + `Current status: ${response.result.status}.`
      );
      await loadJob(false);
    } catch (exception) {
      setError(describeError(exception, 'Failed to retry failed items.'));
    } finally {
      setActionPending(null);
    }
  }

  if (loading) {
    return (
      <PageSection title="Loading job" description="Fetching job state, item progress, and available processed outputs.">
        <div className="status-card">
          <strong>Loading</strong>
          <span>Reading Job #{Number.isFinite(jobId) ? jobId : 'unknown'}.</span>
        </div>
      </PageSection>
    );
  }

  return (
    <div className="project-console">
      {error && (
        <div className="status-card error">
          <strong>Job detail failed</strong>
          <span>{error}</span>
        </div>
      )}

      {actionNotice && (
        <div className="status-card success">
          <strong>Job action completed</strong>
          <span>{actionNotice}</span>
        </div>
      )}

      <section className="console-hero">
        <div>
          <span className="status-pill accent">Preprocessing job</span>
          <h2>{job ? `Job #${job.id}` : 'Job unavailable'}</h2>
          <p>
            Inspect queue progress, image-level Worker results, and processed-only downloads for this document
            preprocessing batch.
          </p>
        </div>
        <div className="session-card">
          <span className={`status-dot ${isTerminal ? 'online' : 'offline'}`} />
          <strong>{job?.status ?? 'Unavailable'}</strong>
          <small>{refreshing ? 'Refreshing...' : isTerminal ? 'terminal state' : 'polling every 2.5s'}</small>
        </div>
      </section>

      <section className="metric-strip">
        <MetricCard label="Total" value={String(summary?.total ?? job?.totalCount ?? 0)} detail="images" />
        <MetricCard label="Succeeded" value={String(summary?.succeeded ?? job?.succeededCount ?? 0)} detail="processed" />
        <MetricCard label="Failed" value={String(summary?.failed ?? job?.failedCount ?? 0)} detail="items" />
        <MetricCard label="Processed" value={String(processedItems.length)} detail="downloadable" />
      </section>

      <div className="job-detail-layout">
        <PageSection title="Progress" description="Current counters are read from the Job summary endpoint.">
          <div className="progress-card">
            <div className="artifact-title">
              <strong>{(summary?.progressPercent ?? 0).toFixed(1)}%</strong>
              <span>{summary ? `${summary.succeeded + summary.failed}/${summary.total} completed` : 'No summary'}</span>
            </div>
            <div className="progress-track">
              <span style={{ width: `${Math.min(summary?.progressPercent ?? 0, 100)}%` }} />
            </div>
          </div>

          <div className="metadata-grid">
            <span>Preset</span>
            <strong>{job?.preset ?? '-'}</strong>
            <span>Priority</span>
            <strong>{job?.priority ?? '-'}</strong>
            <span>Debug</span>
            <strong>{job?.debug ? 'enabled' : 'disabled'}</strong>
            <span>Created</span>
            <strong>{job?.createdAt ? formatDate(job.createdAt) : '-'}</strong>
            <span>Started</span>
            <strong>{job?.startedAt ? formatDate(job.startedAt) : '-'}</strong>
            <span>Completed</span>
            <strong>{job?.completedAt ? formatDate(job.completedAt) : '-'}</strong>
          </div>

          <div className="download-actions">
            <button className="secondary-action" type="button" onClick={() => void loadJob(false)} disabled={refreshing}>
              {refreshing ? 'Refreshing...' : 'Refresh'}
            </button>
            {job && (
              <a className="secondary-action" href={`/projects/${job.projectId}`}>
                Open project
              </a>
            )}
          </div>
        </PageSection>

        <PageSection title="Controls and downloads" description="Cancel active work or retry only failed Worker items.">
          <div className="job-action-stack">
            <div className="status-card">
              <strong>{retryableItems.length} retryable items</strong>
              <span>
                Retry only targets `FAILED` and `DEAD_LETTERED` items. Full rerun is intentionally hidden from MVP UI.
              </span>
            </div>
            <div className="download-actions">
              <button
                className="secondary-action danger-action"
                type="button"
                onClick={cancelJob}
                disabled={!canCancel}
              >
                {actionPending === 'cancel' ? 'Cancelling...' : 'Cancel job'}
              </button>
              <button
                className="secondary-action"
                type="button"
                onClick={retryFailedItems}
                disabled={!canRetry}
              >
                {actionPending === 'retry' ? 'Retrying...' : `Retry failed (${retryableItems.length})`}
              </button>
            </div>
          </div>
          <div className="status-card">
            <strong>{processedItems.length} processed images ready</strong>
            <span>Preview, report, and debug artifacts are intentionally hidden from the MVP UI.</span>
          </div>
          <button
            className="primary-action"
            type="button"
            onClick={downloadProcessedZip}
            disabled={processedItems.length === 0 || zipDownloading}
          >
            {zipDownloading ? 'Preparing ZIP...' : 'Download processed ZIP'}
          </button>
        </PageSection>
      </div>

      <section className="artifact-board">
        <div className="artifact-title">
          <div>
            <span className="status-pill accent">Job items</span>
            <h2>Image-level Worker results</h2>
          </div>
          <small>{items.length} items</small>
        </div>

        {items.length === 0 ? (
          <div className="empty-state">
            <strong>No JobItems</strong>
            <span>The job has no image-level work items yet.</span>
          </div>
        ) : (
          <div className="job-item-list">
            {items.map((item) => (
              <article className="job-item-row" key={item.id}>
                <div>
                  <strong>Item #{item.id}</strong>
                  <small>Image #{item.imageId} - attempt {item.attempt}</small>
                </div>
                <span className={`status-pill ${item.status.toLowerCase()}`}>{item.status}</span>
                <div className="job-item-meta">
                  {item.workerId && <small>Worker: {item.workerId}</small>}
                  {item.startedAt && <small>Started: {formatDate(item.startedAt)}</small>}
                  {item.completedAt && <small>Completed: {formatDate(item.completedAt)}</small>}
                  {item.errorMessage && (
                    <small className="error-text">
                      {item.errorCode}: {item.errorMessage}
                    </small>
                  )}
                  {item.processedObjectKey && <code>{item.processedObjectKey}</code>}
                </div>
                <button
                  className="secondary-action"
                  type="button"
                  disabled={!item.processedObjectKey || itemDownloading === item.id}
                  onClick={() => downloadProcessedImage(item)}
                >
                  {itemDownloading === item.id ? 'Opening...' : 'Download processed'}
                </button>
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

function openDownload(url: string, fileName: string) {
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.target = '_blank';
  anchor.rel = 'noopener noreferrer';
  anchor.download = fileName;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
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
