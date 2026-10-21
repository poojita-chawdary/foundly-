import { useState, useEffect, ReactNode } from "react";
import { Package, LoaderCircle } from "lucide-react";
import { api } from "../api";
export function ErrorBox({ error }: { error: string }) {
  return error ? (
    <div className="error" role="alert">
      {error}
    </div>
  ) : null;
}
export function Loading() {
  return (
    <div className="empty" role="status">
      <LoaderCircle className="spin" /> Loading…
    </div>
  );
}
export function Empty({ children }: { children: ReactNode }) {
  return (
    <div className="empty">
      <Package size={28} />
      <p>{children}</p>
    </div>
  );
}
// Keep each result tied to its request so tab changes never render stale data.
export function useLoad<T>(url: string) {
  const [version, setVersion] = useState(0);
  const requestKey = url + "#" + version;
  const [state, setState] = useState<{
    key: string;
    data: T | null;
    error: string;
    loading: boolean;
  }>({ key: "", data: null, error: "", loading: true });
  useEffect(() => {
    let active = true;
    setState({ key: requestKey, data: null, error: "", loading: true });
    api<T>(url)
      .then((data) => {
        if (active)
          setState({ key: requestKey, data, error: "", loading: false });
      })
      .catch((error) => {
        if (active)
          setState({
            key: requestKey,
            data: null,
            error: error.message,
            loading: false,
          });
      });
    return () => {
      active = false;
    };
  }, [url, requestKey]);
  const current =
    state.key === requestKey
      ? state
      : { key: requestKey, data: null, error: "", loading: true };
  return { ...current, refresh: () => setVersion((v) => v + 1) };
}
