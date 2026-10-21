import { useState, useEffect, ReactNode } from "react";
import { Routes, Route, Link, Navigate } from "react-router-dom";
import { api, User, Catalog } from "./api";
import { Context, useApp } from "./context";
import { Layout } from "./components/Layout";
import { ErrorBox, Loading, Empty } from "./components/ui";
import { Home } from "./pages/Home";
import { Auth } from "./pages/Auth";
import { Browse } from "./pages/Browse";
import { ReportForm } from "./pages/ReportForm";
import { Detail } from "./pages/Detail";
import { Dashboard } from "./pages/Dashboard";
import { Admin } from "./pages/Admin";
import { Info } from "./pages/Info";
export function App() {
  const [user, setUser] = useState<User | null>(null),
    [catalog, setCatalog] = useState<Catalog>({
      campuses: [],
      categories: [],
      locations: [],
    }),
    [ready, setReady] = useState(false),
    [error, setError] = useState("");
  async function reload() {
    try {
      setUser(await api<User>("/users/me"));
    } catch {
      setUser(null);
    }
  }
  useEffect(() => {
    Promise.all([reload(), api<Catalog>("/catalog").then(setCatalog)])
      .catch((e) => setError(e.message))
      .finally(() => setReady(true));
  }, []);
  if (!ready) return <Loading />;
  return (
    <Context.Provider value={{ user, catalog, reload }}>
      <Layout>
        <ErrorBox error={error} />
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Auth />} />
          <Route path="/register" element={<Auth register />} />
          <Route
            path="/browse"
            element={
              <Protected>
                <Browse />
              </Protected>
            }
          />
          <Route
            path="/report/:type"
            element={
              <Protected>
                <ReportForm />
              </Protected>
            }
          />
          <Route
            path="/items/:id"
            element={
              <Protected>
                <Detail />
              </Protected>
            }
          />
          <Route
            path="/items/:id/edit"
            element={
              <Protected>
                <ReportForm edit />
              </Protected>
            }
          />
          <Route
            path="/dashboard"
            element={
              <Protected>
                <Dashboard />
              </Protected>
            }
          />
          <Route
            path="/admin"
            element={
              <Protected>
                <Admin />
              </Protected>
            }
          />
          {["about", "help", "safety", "contact"].map((p) => (
            <Route key={p} path={"/" + p} element={<Info page={p} />} />
          ))}
          <Route
            path="*"
            element={
              <Empty>
                Page not found. <Link to="/">Return home</Link>.
              </Empty>
            }
          />
        </Routes>
      </Layout>
    </Context.Provider>
  );
}
function Protected({ children }: { children: ReactNode }) {
  const { user } = useApp();
  return user ? <>{children}</> : <Navigate to="/login" replace />;
}
