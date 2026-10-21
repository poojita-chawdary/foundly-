import { useState, ReactNode } from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import { Search, Plus, Menu, X, LogOut } from "lucide-react";
import { post, refreshCsrf } from "../api";
import { useApp } from "../context";
function Logo() {
  return (
    <Link className="logo" to="/" aria-label="Foundly home">
      <span className="logo-mark">
        <Search size={22} />
        <span />
      </span>
      foundly<span className="logo-dot">.</span>
    </Link>
  );
}
export function Layout({ children }: { children: ReactNode }) {
  const { user, reload } = useApp();
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();
  async function logout() {
    await post("/auth/logout");
    await refreshCsrf();
    await reload();
    navigate("/");
  }
  return (
    <>
      <header>
        <div className="nav-wrap">
          <Logo />
          <span className="campus-label">YOUR CAMPUS. CONNECTED.</span>
          <button
            className="icon mobile-toggle"
            aria-label="Toggle navigation"
            onClick={() => setOpen(!open)}
          >
            {open ? <X /> : <Menu />}
          </button>
          <nav className={open ? "open" : ""} onClick={() => setOpen(false)}>
            <NavLink to="/browse">Browse items</NavLink>
            <NavLink to="/help">How it works</NavLink>
            {user ? (
              <>
                <NavLink to="/dashboard">My dashboard</NavLink>
                {user.role !== "USER" && (
                  <NavLink to="/admin">Moderation</NavLink>
                )}
                <button
                  className="icon"
                  title="Sign out"
                  onClick={() => logout().catch((e) => alert(e.message))}
                >
                  <LogOut size={18} />
                </button>
              </>
            ) : (
              <Link to="/login">Sign in</Link>
            )}
            <Link className="button small" to="/report/found">
              <Plus size={16} /> Report an item
            </Link>
          </nav>
        </div>
      </header>
      <main>{children}</main>
      <footer>
        <div>
          <Logo />
          <p>A little community. A lot of good.</p>
          <small>An independent campus project.</small>
        </div>
        <div className="footer-links">
          {["about", "help", "safety", "contact"].map((p) => (
            <Link key={p} to={"/" + p}>
              {p[0].toUpperCase() + p.slice(1)}
            </Link>
          ))}
        </div>
        <span className="footer-note">Made for finding your way back.</span>
      </footer>
    </>
  );
}
