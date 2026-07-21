import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth, homeFor } from "../auth/AuthContext";
import type { Role } from "../api/types";
import * as api from "../api/api";

const ROLES: { role: Role; label: string; uid: string; name: string; email: string }[] = [
  { role: "PATIENT", label: "🧑 Patient", uid: "P-self", name: "Chamari (Patient)", email: "chamari@example.com" },
  { role: "RECEPTIONIST", label: "💼 Receptionist", uid: "u-recep1", name: "Dilani (Reception)", email: "dilani@sunrise.lk" },
  { role: "DENTIST", label: "🦷 Dentist", uid: "D1", name: "Dr. Nimal Silva", email: "silva@sunrise.lk" },
  { role: "ADMIN", label: "📊 Administrator", uid: "u-admin", name: "Samanthi (Manager)", email: "admin@sunrise.lk" },
];

export default function LoginPage() {
  const { login } = useAuth();
  const nav = useNavigate();
  const [sel, setSel] = useState<Role>("PATIENT");
  const [email, setEmail] = useState(ROLES[0].email);
  const [error, setError] = useState<string | null>(null);
  const chosen = ROLES.find((r) => r.role === sel)!;

  async function signIn() {
    setError(null);
    // Demonstrates the account-lockout use case: check status, then report success.
    try {
      const status = await api.getLockStatus(email);
      if (status.locked) {
        const mins = Math.ceil(status.retryAfterSeconds / 60);
        setError(`This account is locked after too many failed attempts. Try again in ~${mins} min or ask an admin to unlock it.`);
        return;
      }
      await api.reportSuccessfulLogin(email).catch(() => {});
    } catch {
      /* auth service optional in dev — proceed */
    }
    login({ uid: chosen.uid, role: sel, email, displayName: chosen.name });
    nav(homeFor(sel));
  }

  return (
    <div className="login-wrap">
      <div className="card login-card">
        <div className="brand" style={{ paddingLeft: 0 }}><span className="logo">🦷</span> Sunrise Dental Clinic</div>
        <p className="muted">Sign in to continue. Choose the role you want to explore.</p>
        <label>Role</label>
        <div className="tiles">
          {ROLES.map((r) => (
            <div key={r.role} className={`tile ${sel === r.role ? "active" : ""}`}
                 onClick={() => { setSel(r.role); setEmail(r.email); }}>
              {r.label}
            </div>
          ))}
        </div>
        <label>Email</label>
        <input value={email} onChange={(e) => setEmail(e.target.value)} placeholder="you@example.com" />
        {error && <div className="banner error">{error}</div>}
        <button className="btn primary" style={{ width: "100%", marginTop: 16, justifyContent: "center" }} onClick={signIn}>
          Sign in as {chosen.name}
        </button>
        <p className="muted" style={{ fontSize: ".8rem", marginTop: 14 }}>
          Dev sign-in uses <code>X-User-Uid</code> / <code>X-User-Role</code> headers (backend <code>DevHeaderAuthFilter</code>).
          In production this is replaced by Firebase Authentication.
        </p>
      </div>
    </div>
  );
}
