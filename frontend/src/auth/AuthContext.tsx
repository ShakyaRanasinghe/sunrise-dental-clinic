import { createContext, useContext, useState, ReactNode } from "react";
import { readAuth, writeAuth, StoredAuth } from "../api/client";
import type { Role } from "../api/types";

interface AuthCtx {
  user: StoredAuth | null;
  login: (u: StoredAuth) => void;
  logout: () => void;
}

const Ctx = createContext<AuthCtx>({ user: null, login: () => {}, logout: () => {} });

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<StoredAuth | null>(() => readAuth());
  const login = (u: StoredAuth) => { writeAuth(u); setUser(u); };
  const logout = () => { writeAuth(null); setUser(null); };
  return <Ctx.Provider value={{ user, login, logout }}>{children}</Ctx.Provider>;
}

export const useAuth = () => useContext(Ctx);

/** Home route for each role. */
export function homeFor(role: Role): string {
  switch (role) {
    case "PATIENT": return "/patient";
    case "RECEPTIONIST": return "/reception";
    case "DENTIST": return "/dentist";
    case "ADMIN": return "/admin";
  }
}
