import { PageSection } from '../shared/components/PageSection';
import { JobProgressPanel } from '../features/job/JobProgressPanel';

export function JobDetailPage() {
  return (
    <PageSection
      title="Job detail"
      description="Placeholder for job progress, image-level item states, retry actions, and SSE updates."
    >
      <JobProgressPanel jobId="placeholder" />
    </PageSection>
  );
}
