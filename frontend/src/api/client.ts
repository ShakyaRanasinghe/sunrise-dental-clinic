// Thin fetch wrapper. Attaches the dev-auth headers (X-User-Uid / X-User-Role) that the
// backend's DevHeaderAuthFilter reads, so the SPA works locally without Firebase. When
// Firebase is enabled later, swap these for an Authorization: Bearer <idToken> header.

import type { Role } from "./types";

const BASE = import.meta.env.VITE_API_BASE || "/api";
const AUTH_KEY = "clinic.auth";

export interface StoredAuth {
  uid: string;
  role: Role;
  email: string;
  displayName: string;
}

export function readAuth(): StoredAuth | null {
  try {
    const raw = localStorage.getItem(AUTH_KEY);
    return raw ? (JSON.parse(raw) as StoredAuth) : null;
  } catch {
    return null;
  }
}
export function writeAuth(auth: StoredAuth | null) {
  if (auth) localStorage.setItem(AUTH_KEY, JSON.stringify(auth));
  else localStorage.removeItem(AUTH_KEY);
}

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

function authHeaders(): Record<string, string> {
  const a = readAuth();
  if (!a) return {};
  return { "X-User-Uid": a.uid, "X-User-Role": a.role };
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...authHeaders(),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!res.ok) {
    let msg = res.statusText;
    try {
      const data = await res.json();
      msg = (data && (data.message || data.error)) || msg;
    } catch {
      /* non-JSON error body */
    }
    throw new ApiError(res.status, msg);
  }
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

export const http = {
  get: <T>(p: string) => request<T>("GET", p),
  post: <T>(p: string, b?: unknown) => request<T>("POST", p, b),
  put: <T>(p: string, b?: unknown) => request<T>("PUT", p, b),
  del: <T>(p: string) => request<T>("DELETE", p),
};

// Try a live call; if the endpoint isn't built yet (404) or the backend is down,
// fall back to bundled sample data so the UI is fully demoable. Logs the fallback.
export async function liveOrMock<T>(live: () => Promise<T>, mock: () => T, label: string): Promise<T> {
  try {
    return await live();
  } catch (e) {
    const status = e instanceof ApiError ? e.status : 0;
    if (status === 404 || status === 0 || status === 403) {
      console.info(`[mock] ${label} — backend endpoint unavailable (status ${status}); using sample data`);
      return mock();
    }
    throw e;
  }
}
