import { FormEvent, useEffect, useState } from 'react';
import { apiClient } from '../shared/api/apiClient';
import { readStoredAccessToken } from '../shared/auth/accessTokenStore';
import { PageSection } from '../shared/components/PageSection';

type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
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

const presets = ['A4_SCAN_300DPI', 'LOW_CONTRAST_SCAN', 'RECEIPT', 'NOISY_SCAN', 'AUTO'];

export function ProjectListPage() {
  const [projects, setProjects] = useState<ProjectResponse[]>([]);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [defaultPreset, setDefaultPreset] = useState('A4_SCAN_300DPI');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadProjects();
  }, []);

  async function loadProjects() {
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.get<PageResponse<ProjectResponse>>('/v1/projects?size=50', readStoredAccessToken());
      setProjects(response.result.content);
    } catch (exception) {
      setError(describeError(exception, 'Failed to load projects.'));
    } finally {
      setLoading(false);
    }
  }

  async function createProject(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!name.trim()) {
      setError('Project name is required.');
      return;
    }

    setSaving(true);
    setError(null);
    try {
      await apiClient.post<ProjectResponse>(
        '/v1/projects',
        {
          name: name.trim(),
          description: description.trim() || null,
          defaultPreset
        },
        readStoredAccessToken()
      );
      setName('');
      setDescription('');
      setDefaultPreset('A4_SCAN_300DPI');
      await loadProjects();
    } catch (exception) {
      setError(describeError(exception, 'Failed to create project.'));
    } finally {
      setSaving(false);
    }
  }

  async function deleteProject(project: ProjectResponse) {
    if (!window.confirm(`Delete project "${project.name}"?`)) {
      return;
    }
    setError(null);
    try {
      await apiClient.delete(`/v1/projects/${project.id}`, readStoredAccessToken());
      setProjects((previous) => previous.filter((candidate) => candidate.id !== project.id));
    } catch (exception) {
      setError(describeError(exception, 'Failed to delete project.'));
    }
  }

  return (
    <div className="project-console">
      <section className="console-hero">
        <div>
          <span className="status-pill accent">Project workspace</span>
          <h2>Group scan batches before queueing preprocessing work.</h2>
          <p>
            Projects own uploaded images, default presets, access roles, and the preprocessing jobs created from those
            image sets.
          </p>
        </div>
        <div className="session-card">
          <span className={`status-dot ${error ? 'offline' : 'online'}`} />
          <strong>{projects.length} projects</strong>
          <small>{loading ? 'Loading workspace data' : 'Ready for upload batches'}</small>
        </div>
      </section>

      {error && (
        <div className="status-card error">
          <strong>Project flow failed</strong>
          <span>{error}</span>
        </div>
      )}

      <div className="project-layout">
        <PageSection title="Create project" description="Create a workspace for a document image batch.">
          <form className="stack-form" onSubmit={createProject}>
            <label className="field">
              Name
              <input value={name} onChange={(event) => setName(event.target.value)} placeholder="Library scan batch" />
            </label>
            <label className="field">
              Description
              <textarea
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                placeholder="Optional notes for this preprocessing batch"
              />
            </label>
            <label className="field">
              Default preset
              <select value={defaultPreset} onChange={(event) => setDefaultPreset(event.target.value)}>
                {presets.map((preset) => (
                  <option key={preset} value={preset}>
                    {preset}
                  </option>
                ))}
              </select>
            </label>
            <button className="primary-action" type="submit" disabled={saving}>
              {saving ? 'Creating...' : 'Create project'}
            </button>
          </form>
        </PageSection>

        <PageSection title="Project list" description="Open a project to inspect images and recent preprocessing jobs.">
          {loading ? (
            <div className="empty-state">
              <strong>Loading projects</strong>
              <span>Reading your project memberships.</span>
            </div>
          ) : projects.length === 0 ? (
            <div className="empty-state">
              <strong>No projects yet</strong>
              <span>Create the first project before uploading scanned document images.</span>
            </div>
          ) : (
            <div className="project-grid">
              {projects.map((project) => (
                <article className="project-card" key={project.id}>
                  <div className="artifact-title">
                    <strong>{project.name}</strong>
                    <span className="status-pill accent">{project.myRole}</span>
                  </div>
                  <p>{project.description || 'No description provided.'}</p>
                  <div className="metadata-grid">
                    <span>Preset</span>
                    <strong>{project.defaultPreset || 'A4_SCAN_300DPI'}</strong>
                    <span>Owner</span>
                    <strong>{project.ownerEmail}</strong>
                    <span>Updated</span>
                    <strong>{formatDate(project.updatedAt)}</strong>
                  </div>
                  <div className="download-actions">
                    <a className="secondary-action" href={`/projects/${project.id}`}>
                      Open
                    </a>
                    <a className="secondary-action" href="/upload">
                      Upload images
                    </a>
                    {project.myRole === 'OWNER' && (
                      <button className="text-button danger" type="button" onClick={() => deleteProject(project)}>
                        Delete
                      </button>
                    )}
                  </div>
                </article>
              ))}
            </div>
          )}
        </PageSection>
      </div>
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
