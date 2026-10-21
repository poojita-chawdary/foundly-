import { useState, FormEvent } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { ArrowRight } from "lucide-react";
import { api, post, refreshCsrf } from "../api";
import { useApp } from "../context";
import { ErrorBox } from "../components/ui";
export function Auth({ register = false }: { register?: boolean }) {
  const { catalog, reload, user } = useApp();
  const [error, setError] = useState(""),
    [message, setMessage] = useState(""),
    [busy, setBusy] = useState(false);
  const navigate = useNavigate();
  if (user) return <Navigate to="/dashboard" />;
  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setBusy(true);
    setError("");
    setMessage("");
    const f = new FormData(e.currentTarget);
    try {
      if (register) {
        const result = await post<{ message: string }>("/auth/register", {
          name: f.get("name"),
          email: f.get("email"),
          password: f.get("password"),
          campusId: Number(f.get("campusId")),
        });
        setMessage(result.message);
      } else {
        await api("/auth/login", {
          method: "POST",
          body: new URLSearchParams({
            email: String(f.get("email")).trim().toLowerCase(),
            password: String(f.get("password")),
          }),
        });
        await refreshCsrf();
        await reload();
        navigate("/dashboard");
      }
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <section className="narrow section">
      <div className="eyebrow">WELCOME TO YOUR CAMPUS COMMUNITY</div>
      <h1 className="page-title">
        {register ? "Good to have you here." : "Welcome back."}
      </h1>
      <p>
        {register
          ? "A small step toward fewer lost things."
          : "Sign in to find what matters."}
      </p>
      <form className="panel form" onSubmit={submit}>
        <ErrorBox error={error} />
        {message && (
          <div className="success" role="status">
            {message} <Link to="/login">Go to sign in</Link>
          </div>
        )}
        {register && (
          <label>
            Your name
            <input name="name" required maxLength={100} autoComplete="name" />
          </label>
        )}
        <label>
          Email address
          <input
            name="email"
            type="email"
            required
            maxLength={254}
            autoComplete="email"
          />
        </label>
        <label>
          Password
          <input
            name="password"
            type="password"
            required
            minLength={register ? 12 : 1}
            maxLength={72}
            autoComplete={register ? "new-password" : "current-password"}
          />
          {register && (
            <small>Use at least 12 characters (up to 72 UTF-8 bytes).</small>
          )}
        </label>
        {register && (
          <label>
            Your campus
            <select name="campusId" required>
              {catalog.campuses.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </label>
        )}
        <button className="button" disabled={busy}>
          {busy ? "Please wait…" : register ? "Create account" : "Sign in"}
          <ArrowRight size={17} />
        </button>
        <p>
          {register ? "Already part of the community?" : "New around here?"}{" "}
          <Link to={register ? "/login" : "/register"}>
            {register ? "Sign in" : "Create an account"}
          </Link>
        </p>
      </form>
    </section>
  );
}
