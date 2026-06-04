const formats = ['PNG', 'JPEG', 'TIFF', 'BMP', 'WEBP', 'ZIP'];

export function LandingPage() {
  return (
    <div className="landing">
      <header className="landing-header">
        <nav className="landing-nav">
          <a href="/dashboard">Dashboard</a>
          <a href="/projects">Projects</a>
          <a href="/upload">Upload</a>
          <a className="primary-action sm" href="/oauth2/authorization/google">Sign in</a>
        </nav>
      </header>

      <section className="landing-hero">
        <p className="eyebrow">Document Intelligence Ops</p>
        <h1>Preprocess scanned<br />documents at scale.</h1>
        <p className="landing-hero-desc">
          Upload scanned documents, run preprocessing jobs, and download results —
          all in one workspace. Built for teams that need reliable, queue-backed document processing.
        </p>
        <div className="landing-actions">
          <a className="primary-action" href="/oauth2/authorization/google">Continue with Google</a>
          <a className="secondary-action" href="/dashboard">Go to Dashboard</a>
        </div>
        <div className="landing-formats">
          <span>Supported formats</span>
          {formats.map((fmt) => (
            <span key={fmt} className="status-pill">{fmt}</span>
          ))}
        </div>
      </section>

      <section className="landing-stats-bar">
        <div className="landing-stat">
          <strong>500+</strong>
          <span>images per batch</span>
        </div>
        <div className="landing-stat-divider" />
        <div className="landing-stat">
          <strong>512 MB</strong>
          <span>max ZIP size</span>
        </div>
        <div className="landing-stat-divider" />
        <div className="landing-stat">
          <strong>Queue-backed</strong>
          <span>job processing</span>
        </div>
        <div className="landing-stat-divider" />
        <div className="landing-stat">
          <strong>Per-image</strong>
          <span>result tracking</span>
        </div>
      </section>

      <section className="landing-how">
        <p className="eyebrow">How it works</p>
        <h2>Three steps to processed documents.</h2>
        <div className="landing-steps">
          <div className="landing-step">
            <span className="landing-step-num">01</span>
            <h3>Upload</h3>
            <p>Select images or a ZIP file. Each image is queued as a separate preprocessing job item automatically.</p>
          </div>
          <div className="landing-step-arrow">→</div>
          <div className="landing-step">
            <span className="landing-step-num">02</span>
            <h3>Process</h3>
            <p>The Worker picks up each item, applies your preset, and produces a processed image with a full report.</p>
          </div>
          <div className="landing-step-arrow">→</div>
          <div className="landing-step">
            <span className="landing-step-num">03</span>
            <h3>Download</h3>
            <p>Download processed images one by one or grab everything as a single ZIP when the job is complete.</p>
          </div>
        </div>
      </section>

      <section className="landing-cta">
        <h2>Ready to start preprocessing?</h2>
        <p>Sign in with your Google account and run your first batch in minutes.</p>
        <a className="primary-action" href="/oauth2/authorization/google">Continue with Google</a>
      </section>

      <footer className="landing-footer">
        <span>© 2025 DocPrep Cloud</span>
        <span>document intelligence ops</span>
      </footer>
    </div>
  );
}
