import { GoogleLoginButton } from '../features/auth/GoogleLoginButton';
import { PageSection } from '../shared/components/PageSection';

export function LoginPage() {
  return (
    <PageSection
      title="Secure workspace access"
      description="Sign in through Google. The refresh token is kept in an HttpOnly cookie and the access token is no longer shown in the URL or UI."
    >
      <GoogleLoginButton />
    </PageSection>
  );
}
