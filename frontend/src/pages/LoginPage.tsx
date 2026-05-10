import { GoogleLoginButton } from '../features/auth/GoogleLoginButton';
import { PageSection } from '../shared/components/PageSection';

export function LoginPage() {
  return (
    <PageSection
      title="Google sign-in"
      description="Authenticate through the backend OAuth endpoint. Only Google login is exposed."
    >
      <GoogleLoginButton />
    </PageSection>
  );
}
