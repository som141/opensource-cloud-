import { PageSection } from '../shared/components/PageSection';
import { useAccessToken } from '../shared/hooks/useAccessToken';

export function OAuthSuccessPage() {
  const { accessToken } = useAccessToken();

  return (
    <PageSection
      title="OAuth callback"
      description="Placeholder for accepting the short-lived access token and loading the current user profile."
    >
      <div className="status-card">
        <strong>Access token query parameter</strong>
        <span>{accessToken ? 'present' : 'missing'}</span>
      </div>
    </PageSection>
  );
}
