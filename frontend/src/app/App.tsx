import { AppProviders } from './providers';
import { resolveRoute, routes } from './routes';

export function App() {
  const route = resolveRoute(window.location.pathname);

  return (
    <AppProviders>
      <div className="shell">
        <aside className="sidebar" aria-label="Primary">
          <a className="brand" href="/">
            <span className="brand-mark">DP</span>
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
        </aside>
        <main className="content">
          <div className="topbar">
            <span>Local MVP</span>
            <strong>Queue-backed preprocessing workspace</strong>
          </div>
          <header className="page-header">
            <p className="eyebrow">Large-scale document image preprocessing</p>
            <h1>{route.label}</h1>
          </header>
          {route.element}
        </main>
      </div>
    </AppProviders>
  );
}
