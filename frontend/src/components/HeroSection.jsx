function HeroSection() {
  return (
    <section className="hero anim-fade-up">
      <div className="hero-tag">
        <span className="hero-tag-dot" />
        NEXT-GEN URL SHORTENER
      </div>
      <h1 className="hero-title">
        Shorten. Track.
        <br />
        <span className="hero-glow">Dominate.</span>
      </h1>
      <p className="hero-desc">
        Lightning-fast short links with real-time analytics, QR&nbsp;codes,
        password protection, and zero&nbsp;compromises.
      </p>
      <div className="hero-stats stagger">
        <div className="hero-stat anim-fade-up">
          <span className="hero-stat-val">3.5T+</span>
          <span className="hero-stat-label">Possible codes</span>
        </div>
        <div className="hero-stat anim-fade-up">
          <span className="hero-stat-val">&lt;5ms</span>
          <span className="hero-stat-label">Redirect latency</span>
        </div>
        <div className="hero-stat anim-fade-up">
          <span className="hero-stat-val">256-bit</span>
          <span className="hero-stat-label">Encryption ready</span>
        </div>
      </div>
    </section>
  );
}

export default HeroSection;
