import { useEffect, useState } from 'react';
import { AppProviders } from './providers';
import { resolveRoute, routes } from './routes';
import { apiClient } from '../shared/api/apiClient';
import { clearAccessToken, readStoredAccessToken } from '../shared/auth/accessTokenStore';

type CurrentUser = {
  id: number;
  email: string;
  name: string;
  profileImageUrl?: string | null;
  role: string;
  providers: string[];
};

export function App() {
  const pathname = window.location.pathname;
  const route = resolveRoute(pathname);

  if (pathname === '/') {
    return (
      <AppProviders>
        {route.element}
      </AppProviders>
    );
  }

  return (
    <AppProviders>
      <div className="shell">
        <aside className="sidebar" aria-label="Primary">
          <a className="brand" href="/dashboard">
            <img className="brand-mark" src="/logo.png" alt="DocPrep Cloud" />
            <span>
              <strong>DocPrep Cloud</strong>
              <small>document intelligence ops</small>
            </span>
          </a>
          <nav className="nav-list">
            {routes
              .filter((item) => !item.path.includes(':') && item.showInNav !== false)
              .map((item) => (
                <a key={item.path} href={item.path} className={item.path === route.path ? 'active' : undefined}>
                  {item.label}
                </a>
              ))}
          </nav>
          <SidebarAccount />
        </aside>
        <main className="content">
          <header className="page-header">
            {route.eyebrow && <p className="eyebrow">{route.eyebrow}</p>}
            <h1>{route.label}</h1>
          </header>
          {route.element}
        </main>
      </div>
    </AppProviders>
  );
}

function SidebarAccount() {
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [logoutPending, setLogoutPending] = useState(false);

  useEffect(() => {
    let mounted = true;
    apiClient
      .get<CurrentUser>('/v1/auth/me', readStoredAccessToken())
      .then((response) => {
        if (mounted) {
          setCurrentUser(response.result);
        }
      })
      .catch(() => {
        if (mounted) {
          setCurrentUser(null);
        }
      })
      .finally(() => {
        if (mounted) {
          setLoading(false);
        }
      });

    return () => {
      mounted = false;
    };
  }, []);

  const logout = async () => {
    setLogoutPending(true);
    try {
      await apiClient.post('/v1/auth/logout', {}, readStoredAccessToken());
    } catch {
      // The local token must still be cleared even if the refresh cookie already expired.
    } finally {
      clearAccessToken();
      window.location.href = '/';
    }
  };

  if (loading) {
    return (
      <section className="sidebar-account" aria-label="Account">
        <span className="account-state">Checking session</span>
      </section>
    );
  }

  if (!currentUser) {
    return (
      <section className="sidebar-account" aria-label="Account">
        <span className="account-state">Not signed in</span>
        <a className="account-login" href="/oauth2/authorization/google">
          Continue with Google
        </a>
      </section>
    );
  }

  const displayName = currentUser.name || currentUser.email;
  const initials = displayName.slice(0, 2).toUpperCase();

  return (
    <section className="sidebar-account" aria-label="Account">
      <div className="account-card">
        {currentUser.profileImageUrl ? (
          <img className="account-avatar" src={currentUser.profileImageUrl} alt="" referrerPolicy="no-referrer" />
        ) : (
          <span className="account-avatar">{initials}</span>
        )}
        <span className="account-copy">
          <strong>{displayName}</strong>
          <small>{currentUser.email}</small>
        </span>
      </div>
      <button className="text-action" type="button" onClick={logout} disabled={logoutPending}>
        {logoutPending ? 'Signing out...' : 'Sign out'}
      </button>
    </section>
  );
}
