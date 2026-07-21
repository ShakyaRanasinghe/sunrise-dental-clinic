import { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { useAuth, homeFor } from "./AuthContext";
import type { Role } from "../api/types";

/** Route guard: redirect to login if signed out, or to the user's home if the role is wrong. */
export function RequireRole({ roles, children }: { roles: Role[]; children: ReactNode }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (!roles.includes(user.role)) return <Navigate to={homeFor(user.role)} replace />;
  return <>{children}</>;
}
