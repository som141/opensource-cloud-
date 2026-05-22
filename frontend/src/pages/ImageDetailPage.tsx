import { useEffect, useMemo, useState } from 'react';
import { apiClient } from '../shared/api/apiClient';
import { readStoredAccessToken } from '../shared/auth/accessTokenStore';
import { PageSection } from '../shared/components/PageSection';

type PageResponse<T> = {
  content: T[];
};

type ImageResponse = {
  id: number;
  projectId: number;
  uploadSessionId: number;
  uploadSessionFileId: number;
  uploaderId: number;
  originalFileName: string;
  originalObjectKey: string;
  contentType: string;
  sizeBytes: number;
  checksumSha256: string;
  format: string;
  status: string;
  width?: number | null;
  height?: number | null;
  dpiX?: number | null;
  dpiY?: number | null;
  createdAt: string;
};

type ImageDownloadUrlResponse = {
  imageId: number;
  type: string;
  objectKey: string;
  downloadUrl: string;
  expiresAt: string;
  requiredHeaders: Record<string, string>;
};

type JobResponse = {
  id: number;
  projectId: number;
  preset: string;
  status: string;
  totalCount: number;
  succeededCount: number;
  failedCount: number;
  createdAt: string;
};

type JobItemResponse = {
  id: number;
  jobId: number;
  imageId: number;
  status: string;
  attempt: number;
  workerId?: string | null;
  processedObjectKey?: string | null;
  errorCode?: string | null;
  errorMessage?: string | null;
  createdAt: string;
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

type RelatedJobItem = {
  job: JobResponse;
  item: JobItemResponse;
};

export function ImageDetailPage() {
  const imageId = useMemo(() => {
    const segments = window.location.pathname.split('/').filter(Boolean);
    return Number(segments[segments.length - 1]);
  }, []);
  const [image, setImage] = useState<ImageResponse | null>(null);
  const [relatedItems, setRelatedItems] = useState<RelatedJobItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [originalDownloading, setOriginalDownloading] = useState(false);
  const [processedDownloading, setProcessedDownloading] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const processedItems = relatedItems.filter((entry) => entry.item.processedObjectKey);

  useEffect(() => {
    void loadImage(true);
  }, [imageId]);

  async function loadImage(showLoading: boolean) {
    if (!Number.isFinite(imageId)) {
      setError('Invalid image id.');
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
      const imageResponse = await apiClient.get<ImageResponse>(`/v1/images/${imageId}`, readStoredAccessToken());
      setImage(imageResponse.result);
      setRelatedItems(await findRelatedJobItems(imageResponse.result));
    } catch (exception) {
      setError(describeError(exception, 'Failed to load image.'));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }

  async function findRelatedJobItems(nextImage: ImageResponse) {
    const jobResponse = await apiClient.get<PageResponse<JobResponse>>('/v1/jobs?size=100', readStoredAccessToken());
    const projectJobs = jobResponse.result.content.filter((job) => job.projectId === nextImage.projectId);
    const itemPages = await Promise.all(
      projectJobs.map(async (job) => {
        const response = await apiClient.get<PageResponse<JobItemResponse>>(
          `/v1/jobs/${job.id}/items?size=500`,
          readStoredAccessToken()
        );
        return response.result.content
          .filter((item) => item.imageId === nextImage.id)
          .map((item) => ({ job, item }));
      })
    );
    return itemPages.flat().sort((left, right) => right.job.id - left.job.id || right.item.id - left.item.id);
  }

  async function downloadOriginal() {
    setOriginalDownloading(true);
    setError(null);
    try {
      const response = await apiClient.get<ImageDownloadUrlResponse>(
        `/v1/images/${imageId}/download?type=original`,
        readStoredAccessToken()
      );
      openDownload(response.result.downloadUrl, image?.originalFileName ?? `image-${imageId}-original`);
    } catch (exception) {
      setError(describeError(exception, 'Failed to download original image.'));
    } finally {
      setOriginalDownloading(false);
    }
  }

  async function downloadProcessed(entry: RelatedJobItem) {
    setProcessedDownloading(entry.item.id);
    setError(null);
    try {
      const response = await apiClient.get<JobItemDownloadUrlResponse>(
        `/v1/jobs/${entry.job.id}/items/${entry.item.id}/download?type=processed`,
        readStoredAccessToken()
      );
      openDownload(response.result.downloadUrl, `job-${entry.job.id}-image-${imageId}-processed.png`);
    } catch (exception) {
      setError(describeError(exception, 'Failed to download processed image.'));
    } finally {
      setProcessedDownloading(null);
    }
  }

  if (loading) {
    return (
      <PageSection title="Loading image" description="Fetching source image metadata and related processed outputs.">
        <div className="status-card">
          <strong>Loading</strong>
          <span>Reading Image #{Number.isFinite(imageId) ? imageId : 'unknown'}.</span>
        </div>
      </PageSection>
    );
  }

  return (
    <div className="project-console">
      {error && (
        <div className="status-card error">
          <strong>Image detail failed</strong>
          <span>{error}</span>
        </div>
      )}

      <section className="console-hero">
        <div>
          <span className="status-pill accent">Source image</span>
          <h2>{image?.originalFileName ?? 'Image unavailable'}</h2>
          <p>
            Review original upload metadata and find processed outputs created by preprocessing jobs that included this
            image.
          </p>
        </div>
        <div className="session-card">
          <span className={`status-dot ${image ? 'online' : 'offline'}`} />
          <strong>{image?.status ?? 'Unavailable'}</strong>
          <small>{refreshing ? 'Refreshing...' : image ? `Project #${image.projectId}` : 'No image metadata'}</small>
        </div>
      </section>

      <section className="metric-strip">
        <MetricCard label="Size" value={image ? formatBytes(image.sizeBytes) : '-'} detail="original" />
        <MetricCard label="Format" value={image?.format ?? '-'} detail={image?.contentType ?? 'content type'} />
        <MetricCard label="Jobs" value={String(relatedItems.length)} detail="using image" />
        <MetricCard label="Processed" value={String(processedItems.length)} detail="downloadable" />
      </section>

      <div className="job-detail-layout">
        <PageSection title="Original metadata" description="Metadata recorded when the upload session was completed.">
          <div className="metadata-grid">
            <span>Image ID</span>
            <strong>{image?.id ?? '-'}</strong>
            <span>Project</span>
            <strong>{image ? `#${image.projectId}` : '-'}</strong>
            <span>Upload session</span>
            <strong>{image ? `#${image.uploadSessionId}` : '-'}</strong>
            <span>Uploader</span>
            <strong>{image ? `#${image.uploaderId}` : '-'}</strong>
            <span>Object key</span>
            <strong>{image?.originalObjectKey ?? '-'}</strong>
            <span>Checksum</span>
            <strong>{image?.checksumSha256 ?? '-'}</strong>
            <span>Dimensions</span>
            <strong>{formatDimensions(image)}</strong>
            <span>DPI</span>
            <strong>{formatDpi(image)}</strong>
            <span>Created</span>
            <strong>{image?.createdAt ? formatDate(image.createdAt) : '-'}</strong>
          </div>
          <div className="download-actions">
            <button
              className="primary-action"
              type="button"
              onClick={downloadOriginal}
              disabled={!image || originalDownloading}
            >
              {originalDownloading ? 'Opening...' : 'Download original'}
            </button>
            {image && (
              <a className="secondary-action" href={`/projects/${image.projectId}`}>
                Open project
              </a>
            )}
            <button className="secondary-action" type="button" onClick={() => void loadImage(false)} disabled={refreshing}>
              {refreshing ? 'Refreshing...' : 'Refresh'}
            </button>
          </div>
        </PageSection>

        <PageSection title="MVP artifact policy" description="Processed outputs are owned by JobItems.">
          <div className="status-card">
            <strong>{processedItems.length} processed outputs ready</strong>
            <span>
              Use Job detail for batch-level ZIP downloads. This page links only outputs related to this image.
            </span>
          </div>
          {image && (
            <a className="secondary-action" href={`/upload`}>
              Run another batch
            </a>
          )}
        </PageSection>
      </div>

      <section className="artifact-board">
        <div className="artifact-title">
          <div>
            <span className="status-pill accent">Related jobs</span>
            <h2>Processed results for this image</h2>
          </div>
          <small>{relatedItems.length} JobItems</small>
        </div>

        {relatedItems.length === 0 ? (
          <div className="empty-state">
            <strong>No related JobItems</strong>
            <span>Create a preprocessing job that includes this image to produce processed outputs.</span>
          </div>
        ) : (
          <div className="job-item-list">
            {relatedItems.map((entry) => (
              <article className="job-item-row" key={`${entry.job.id}-${entry.item.id}`}>
                <div>
                  <strong>Job #{entry.job.id}</strong>
                  <small>Item #{entry.item.id} - attempt {entry.item.attempt}</small>
                </div>
                <span className={`status-pill ${entry.item.status.toLowerCase()}`}>{entry.item.status}</span>
                <div className="job-item-meta">
                  <small>Preset: {entry.job.preset}</small>
                  <small>Job status: {entry.job.status}</small>
                  {entry.item.workerId && <small>Worker: {entry.item.workerId}</small>}
                  {entry.item.completedAt && <small>Completed: {formatDate(entry.item.completedAt)}</small>}
                  {entry.item.errorMessage && (
                    <small className="error-text">
                      {entry.item.errorCode}: {entry.item.errorMessage}
                    </small>
                  )}
                  {entry.item.processedObjectKey && <code>{entry.item.processedObjectKey}</code>}
                </div>
                <div className="download-actions">
                  <a className="secondary-action" href={`/jobs/${entry.job.id}`}>
                    Open job
                  </a>
                  <button
                    className="secondary-action"
                    type="button"
                    disabled={!entry.item.processedObjectKey || processedDownloading === entry.item.id}
                    onClick={() => downloadProcessed(entry)}
                  >
                    {processedDownloading === entry.item.id ? 'Opening...' : 'Download processed'}
                  </button>
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

function formatBytes(bytes: number) {
  if (bytes === 0) {
    return '0 B';
  }
  const units = ['B', 'KB', 'MB', 'GB'];
  const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  return `${(bytes / 1024 ** index).toFixed(index === 0 ? 0 : 1)} ${units[index]}`;
}

function formatDimensions(image: ImageResponse | null) {
  if (!image?.width || !image.height) {
    return 'not extracted';
  }
  return `${image.width} x ${image.height}`;
}

function formatDpi(image: ImageResponse | null) {
  if (!image?.dpiX || !image.dpiY) {
    return 'not extracted';
  }
  return `${image.dpiX} x ${image.dpiY}`;
}
