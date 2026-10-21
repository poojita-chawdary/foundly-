export type User = {
  id: number;
  name: string;
  role: string;
  campusId: number;
  campusName: string;
};
export type Item = {
  id: number;
  ownerId: number;
  type: string;
  title: string;
  categoryId: number;
  category: string;
  colour: string;
  description: string;
  locationId: number;
  location: string;
  eventDate: string;
  status: string;
  imageUrl: string | null;
  createdAt: string;
};
export type Option = { id: number; name: string; campusId?: number };
export type Catalog = {
  campuses: Option[];
  categories: Option[];
  locations: Option[];
};
export type Page = {
  content: Item[];
  total: number;
  page: number;
  size: number;
};
export type Match = {
  id: number;
  score: number;
  explanation: string;
  lost: Item;
  found: Item;
};
export type Claim = {
  id: number;
  reportId: number;
  title: string;
  claimantId: number;
  claimantName: string;
  status: string;
  handoverMessage: string | null;
  canReview: boolean;
  canReturn: boolean;
  createdAt: string;
};
let csrf: { token: string; headerName: string } | null = null;
export async function refreshCsrf() {
  const response = await fetch("/api/auth/csrf", {
    credentials: "same-origin",
  });
  if (!response.ok) throw new Error("Could not establish a secure connection.");
  csrf = await response.json();
}
export async function api<T = unknown>(
  url: string,
  options: RequestInit = {},
): Promise<T> {
  const method = options.method ?? "GET";
  const headers = new Headers(options.headers);
  if (method !== "GET") {
    if (!csrf) await refreshCsrf();
    headers.set(csrf!.headerName, csrf!.token);
    if (
      options.body &&
      !(options.body instanceof FormData) &&
      !(options.body instanceof URLSearchParams)
    )
      headers.set("Content-Type", "application/json");
  }
  const response = await fetch("/api" + url, {
    ...options,
    headers,
    credentials: "same-origin",
  });
  if (!response.ok) {
    const error = await response
      .json()
      .catch(() => ({ message: "The request failed. Please try again." }));
    throw new Error(error.message ?? "The request failed.");
  }
  if (response.status === 204 || response.headers.get("content-length") === "0")
    return undefined as T;
  const text = await response.text();
  return text ? JSON.parse(text) : (undefined as T);
}
export const post = <T = unknown>(url: string, data?: unknown) =>
  api<T>(url, {
    method: "POST",
    body: data === undefined ? undefined : JSON.stringify(data),
  });
