import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import type { Role } from "../api/types";

interface NavItem { to: string; label: string; icon: string; }

const NAV: Record<Role, NavItem[]> = {
  PATIENT: [
    { to: "/patient", label: "Home", icon: "🏠" },
    { to: "/patient/book", label: "Book Appointment", icon: "📅" },
    { to: "/patient/appointments", label: "My Appointments", icon: "🗂️" },
    { to: "/patient/triage", label: "AI Symptom Check", icon: "🦷" },
    { to: "/help", label: "Help", icon: "❓" },
  ],
  RECEPTIONIST: [
    { to: "/reception", label: "Home", icon: "🏠" },
    { to: "/reception/availability", label: "Publish Availability", icon: "🕑" },
    { to: "/reception/register", label: "Register Patient", icon: "➕" },
    { to: "/reception/billing", label: "Billing", icon: "🧾" },
    { to: "/help", label: "Help", icon: "❓" },
  ],
  DENTIST: [
    { to: "/dentist", label: "My Schedule", icon: "🗓️" },
    { to: "/help", label: "Help", icon: "❓" },
  ],
  ADMIN: [
    { to: "/admin", label: "Reports", icon: "📊" },
    { to: "/admin/ai", label: "AI Settings", icon: "🤖" },
    { to: "/admin/manage", label: "Staff & Catalogue", icon: "👥" },
    { to: "/help", label: "Help", icon: "❓" },
  ],
};

export function Layout() {
  const { user, logout } = useAuth();
  const nav = useNavigate();
  if (!user) return null;
  const items = NAV[user.role];
  return (
    <div className="app">
      <aside className="sidebar">
        <div className="brand"><span className="logo">🦷</span> Sunrise Dental</div>
        {items.map((it) => (
          <NavLink key={it.to} to={it.to} end={it.to === `/${user.role.toLowerCase()}` || it.to === "/patient"}
                   className={({ isActive }) => `nav-link ${isActive ? "active" : ""}`}>
            <span>{it.icon}</span> {it.label}
          </NavLink>
        ))}
        <div className="nav-spacer" />
        <button className="btn ghost" onClick={() => { logout(); nav("/login"); }}>↪ Sign out</button>
      </aside>
      <div className="main">
        <header className="topbar">
          <strong>Sunrise Dental Clinic</strong>
          <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
            <span className="muted">{user.displayName}</span>
            <span className="role-pill">{user.role.toLowerCase()}</span>
          </div>
        </header>
        <div className="content"><Outlet /></div>
      </div>
    </div>
  );
}
