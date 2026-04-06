function Footer() {
  return (
    <footer className="footer">
      <div className="container footer-inner">
        <div className="footer-left">
          <span className="footer-logo">Link<span style={{ color: 'var(--neon-cyan)' }}>Short</span></span>
          <span className="footer-copy">© {new Date().getFullYear()}</span>
        </div>
        <div className="footer-right">
          <span className="footer-tech">Spring Boot · React · Redis</span>
        </div>
      </div>
    </footer>
  );
}

export default Footer;
