export type JobStatus =
  | 'CREATED'
  | 'QUEUED'
  | 'RUNNING'
  | 'PARTIAL_SUCCEEDED'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'CANCEL_REQUESTED'
  | 'CANCELLED'
  | 'RETRYING';

export type JobSummary = {
  jobId: number;
  total: number;
  queued: number;
  processing: number;
  succeeded: number;
  failed: number;
  progressPercent: number;
};
