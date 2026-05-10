export type JobProgressEvent = {
  eventType: 'JOB_PROGRESS' | 'JOB_COMPLETED' | 'JOB_FAILED' | 'HEARTBEAT';
  jobId: number;
  total?: number;
  queued?: number;
  processing?: number;
  succeeded?: number;
  failed?: number;
  progressPercent?: number;
};

export const createJobProgressEventSource = {
  path(jobId: string | number) {
    return `/api/v1/jobs/${jobId}/events`;
  },

  connect(jobId: string | number, onMessage: (event: MessageEvent<string>) => void): EventSource {
    const eventSource = new EventSource(this.path(jobId), { withCredentials: true });
    eventSource.onmessage = onMessage;
    return eventSource;
  }
};
