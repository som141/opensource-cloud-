import { useEffect, useMemo, useState } from 'react';
import { apiClient } from '../shared/api/apiClient';
import { readStoredAccessToken } from '../shared/auth/accessTokenStore';
import { PageSection } from '../shared/components/PageSection';

type PageResponse<T> = {
  content: T[];
  totalElements?: number;
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

type JobResponse = {
  id: number;
  projectId: number;
  preset: string;
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

type DashboardData = {
  projects: ProjectResponse[];
  summaries: ProjectSummaryResponse[];
  jobs: JobResponse[];
  projectTotal: number;
  jobTotal: number;
};

const activeJobStatuses = new Set(['CREATED', 'QUEUED', 'RUNNING', 'RETRYING', 'CANCEL_REQUESTED']);

export function DashboardPage() {
  const [data, setData] = useState<DashboardData>({
    projects: [],
    summaries: [],
    jobs: [],
    projectTotal: 0,
    jobTotal: 0
  });
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const stats = useMemo(() => buildStats(data), [data]);
  const recentJobs = useMemo(() => sortByCreatedAt(data.jobs).slice(0, 6), [data.jobs]);
  const recentProjects = useMemo(
    () => [...data.projects].sort((left, right) => Date.parse(right.updatedAt) - Date.parse(left.updatedAt)).slice(0, 4),
    [data.projects]
  );

  useEffect(() => {
    void loadDashboard(true);
  }, []);

  async function loadDashboard(showLoading: boolean) {
    if (showLoading) {
      setLoading(true);
    } else {
      setRefreshing(true);
    }
    setError(null);

    try {
      const [projectResponse, jobResponse] = await Promise.all([
        apiClient.get<PageResponse<ProjectResponse>>('/v1/projects?size=100', readStoredAccessToken()),
        apiClient.get<PageResponse<JobResponse>>('/v1/jobs?size=100', readStoredAccessToken())
      ]);
      const projects = projectResponse.result.content;
      const summaries = await Promise.all(
        projects.map(async (project) => {
          const response = await apiClient.get<ProjectSummaryResponse>(
            `/v1/projects/${project.id}/summary`,
            readStoredAccessToken()
          );
          return response.result;
        })
      );

      setData({
        projects,
        summaries,
        jobs: jobResponse.result.content,
        projectTotal: projectResponse.result.totalElements ?? projects.length,
        jobTotal: jobResponse.result.totalElements ?? jobResponse.result.content.length
      });
    } catch (exception) {
      setError(describeError(exception, 'Failed to load dashboard.'));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }

  return (
    <div className="project-console">
      {error && (
        <div className="status-card error">
          <strong>Dashboard failed</strong>
          <span>{error}</span>
          <div className="download-actions">
            <a className="primary-action" href="/oauth2/authorization/google">
              Continue with Google
            </a>
            <button className="secondary-action" type="button" onClick={() => void loadDashboard(false)}>
              Retry
            </button>
          </div>
        </div>
      )}

      <section className="console-hero">
        <div>
          <span className="status-pill accent">Operations overview</span>
          <h2>Track the preprocessing workspace before the next batch.</h2>
          <p>
            Dashboard data is loaded from the same MVP APIs used by Projects, Upload, and Job detail. It summarizes
            project scope, uploaded images, queue-backed jobs, and processed outputs without reintroducing admin-only
            features.
          </p>
        </div>
        <div className="session-card">
          <span className={`status-dot ${error ? 'offline' : 'online'}`} />
          <strong>{loading ? 'Loading' : error ? 'Attention required' : 'Workspace ready'}</strong>
          <small>{refreshing ? 'Refreshing...' : `Last read ${new Date().toLocaleTimeString()}`}</small>
        </div>
      </section>

      <section className="metric-strip">
        <MetricCard label="Projects" value={String(stats.projectCount)} detail="workspaces" />
        <MetricCard label="Images" value={String(stats.imageCount)} detail="uploaded originals" />
        <MetricCard label="Jobs" value={String(stats.jobCount)} detail={`${stats.activeJobs} active`} />
        <MetricCard label="Success rate" value={stats.successRate} detail={`${stats.processedItems} processed`} />
      </section>

      <div className="dashboard-layout">
        <PageSection title="Next action" description="Use the operational snapshot to decide what to run next.">
          <div className="dashboard-action-grid">
            <a className="dashboard-action-card primary" href="/upload">
              <span>Run a batch</span>
              <strong>Upload images or ZIP</strong>
              <small>Create one JobItem per image and download processed-only results.</small>
            </a>
            <a className="dashboard-action-card" href="/projects">
              <span>Manage scope</span>
              <strong>Open projects</strong>
              <small>Review images, default presets, and project-level job history.</small>
            </a>
          </div>
        </PageSection>

        <PageSection title="Pipeline health" description="Derived from recent Job counters, not from admin APIs.">
          <div className="metadata-grid">
            <span>Queued</span>
            <strong>{stats.queuedItems}</strong>
            <span>Processing</span>
            <strong>{stats.processingItems}</strong>
            <span>Failed items</span>
            <strong>{stats.failedItems}</strong>
            <span>Latest job</span>
            <strong>{recentJobs[0] ? `#${recentJobs[0].id} ${recentJobs[0].status}` : 'none'}</strong>
          </div>
          <button
            className="secondary-action"
            type="button"
            disabled={refreshing}
            onClick={() => void loadDashboard(false)}
          >
            {refreshing ? 'Refreshing...' : 'Refresh dashboard'}
          </button>
        </PageSection>
      </div>

      <section className="artifact-board">
        <div className="artifact-title">
          <div>
            <span className="status-pill accent">Recent jobs</span>
            <h2>Preprocessing activity</h2>
          </div>
          <small>{loading ? 'loading' : `${recentJobs.length}/${data.jobTotal} shown`}</small>
        </div>

        {loading ? (
          <div className="empty-state">
            <strong>Loading jobs</strong>
            <span>Reading recent preprocessing jobs.</span>
          </div>
        ) : recentJobs.length === 0 ? (
          <div className="empty-state">
            <strong>No jobs yet</strong>
            <span>Upload images first to create preprocessing jobs.</span>
          </div>
        ) : (
          <div className="job-item-list">
            {recentJobs.map((job) => (
              <article className="job-item-row dashboard-job-row" key={job.id}>
                <div>
                  <strong>Job #{job.id}</strong>
                  <small>Project #{job.projectId} - {job.preset}</small>
                </div>
                <span className={`status-pill ${job.status.toLowerCase()}`}>{job.status}</span>
                <div className="job-item-meta">
                  <small>{job.succeededCount}/{job.totalCount} processed</small>
                  <small>{job.failedCount} failed - {job.processingCount} processing - {job.queuedCount} queued</small>
                  <small>Created: {formatDate(job.createdAt)}</small>
                </div>
                <a className="secondary-action" href={`/jobs/${job.id}`}>
                  Open job
                </a>
              </article>
            ))}
          </div>
        )}
      </section>

      <section className="artifact-board">
        <div className="artifact-title">
          <div>
            <span className="status-pill accent">Recent projects</span>
            <h2>Workspace scope</h2>
          </div>
          <small>{loading ? 'loading' : `${recentProjects.length}/${data.projectTotal} shown`}</small>
        </div>

        {loading ? (
          <div className="empty-state">
            <strong>Loading projects</strong>
            <span>Reading project memberships and summaries.</span>
          </div>
        ) : recentProjects.length === 0 ? (
          <div className="empty-state">
            <strong>No projects yet</strong>
            <span>Create a project from the upload flow or the project workspace.</span>
          </div>
        ) : (
          <div className="project-grid">
            {recentProjects.map((project) => {
              const summary = data.summaries.find((candidate) => candidate.projectId === project.id);
              return (
                <article className="project-card" key={project.id}>
                  <div className="artifact-title">
                    <strong>{project.name}</strong>
                    <span className="status-pill accent">{project.myRole}</span>
                  </div>
                  <p>{project.description || 'No description provided.'}</p>
                  <div className="metadata-grid">
                    <span>Images</span>
                    <strong>{summary?.imageCount ?? 0}</strong>
                    <span>Jobs</span>
                    <strong>{summary?.jobCount ?? 0}</strong>
                    <span>Preset</span>
                    <strong>{project.defaultPreset || 'A4_SCAN_300DPI'}</strong>
                    <span>Updated</span>
                    <strong>{formatDate(project.updatedAt)}</strong>
                  </div>
                  <div className="download-actions">
                    <a className="secondary-action" href={`/projects/${project.id}`}>
                      Open project
                    </a>
                    <a className="secondary-action" href="/upload">
                      Upload more
                    </a>
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}

function buildStats(data: DashboardData) {
  const imageCount = data.summaries.reduce((sum, summary) => sum + summary.imageCount, 0);
  const totalItems = data.jobs.reduce((sum, job) => sum + job.totalCount, 0);
  const processedItems = data.jobs.reduce((sum, job) => sum + job.succeededCount, 0);
  const failedItems = data.jobs.reduce((sum, job) => sum + job.failedCount, 0);
  const queuedItems = data.jobs.reduce((sum, job) => sum + job.queuedCount, 0);
  const processingItems = data.jobs.reduce((sum, job) => sum + job.processingCount, 0);
  const activeJobs = data.jobs.filter((job) => activeJobStatuses.has(job.status)).length;
  const successRate = totalItems === 0 ? '-' : `${Math.round((processedItems / totalItems) * 100)}%`;

  return {
    projectCount: data.projectTotal || data.projects.length,
    imageCount,
    jobCount: data.jobTotal || data.jobs.length,
    activeJobs,
    processedItems,
    failedItems,
    queuedItems,
    processingItems,
    successRate
  };
}

function sortByCreatedAt(jobs: JobResponse[]) {
  return [...jobs].sort((left, right) => Date.parse(right.createdAt) - Date.parse(left.createdAt));
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
