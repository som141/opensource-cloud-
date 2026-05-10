import { createJobProgressEventSource } from '../../shared/api/jobEvents';

type JobProgressPanelProps = {
  jobId: string;
};

export function JobProgressPanel({ jobId }: JobProgressPanelProps) {
  const eventPath = createJobProgressEventSource.path(jobId);

  return (
    <div className="status-card">
      <strong>SSE progress stream</strong>
      <span>{eventPath}</span>
    </div>
  );
}
