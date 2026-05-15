import { useEffect, useState } from 'react';
import { apiClient } from '../shared/api/apiClient';
import { storeAccessToken } from '../shared/auth/accessTokenStore';
import { PageSection } from '../shared/components/PageSection';
import { useAccessToken } from '../shared/hooks/useAccessToken';

type TokenRefreshResponse = {
  accessToken: string;
  accessTokenExpiresAt: string;
};

export function OAuthSuccessPage() {
  const { accessToken, setAccessToken } = useAccessToken();
  const [status, setStatus] = useState(accessToken ? 'Session secured.' : 'Securing session...');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (accessToken) {
      return;
    }
    apiClient.post<TokenRefreshResponse>('/v1/auth/refresh', {})
      .then((response) => {
        storeAccessToken(response.result.accessToken);
        setAccessToken(response.result.accessToken);
        setStatus('Session secured.');
      })
      .catch((exception) => {
        setError(exception instanceof Error ? exception.message : 'Failed to secure the OAuth session.');
        setStatus('Session setup failed.');
      });
  }, [accessToken, setAccessToken]);

  return (
    <PageSection
      title="Session handoff"
      description="The refresh cookie is used to mint a short-lived access token without exposing it in the address bar."
    >
      <div className={`status-card ${error ? 'error' : ''}`}>
        <strong>{status}</strong>
        <span>
          {error
            ? error
            : 'The token value is hidden from the UI and removed from the URL immediately if an older redirect includes it.'}
        </span>
      </div>
      <a className="primary-action" href="/upload">
        Open batch upload
      </a>
    </PageSection>
  );
}
