import { useEffect, useState } from "react";
import { Card, Badge, ErrorBanner, rs } from "../../components/ui";
import type { StaffMember, Treatment } from "../../api/types";
import * as api from "../../api/api";

export default function ManagePage() {
  const [staff, setStaff] = useState<StaffMember[]>([]);
  const [treatments, setTreatments] = useState<Treatment[]>([]);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    api.getStaff().then(setStaff).catch(setError);
    api.getTreatments().then(setTreatments).catch(setError);
  }, []);

  function toggle(uid: string) {
    // Optimistic soft-disable (mirrors the backend soft-delete pattern: active=false, never hard-delete).
    setStaff((prev) => prev.map((s) => (s.uid === uid ? { ...s, active: !s.active } : s)));
  }

  return (
    <div>
      <div className="page-head"><h1>Staff &amp; catalogue</h1></div>
      {error ? <ErrorBanner error={error} /> : null}

      <Card title="Staff accounts (RBAC)">
        <p className="muted" style={{ fontSize: ".88rem" }}>Disabling an account is a soft-disable (never a hard delete) and is audit-logged.</p>
        <div className="table-wrap"><table>
          <thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Status</th><th></th></tr></thead>
          <tbody>
            {staff.map((s) => (
              <tr key={s.uid}>
                <td>{s.name}</td><td>{s.email}</td>
                <td><Badge kind="OPEN">{s.role}</Badge></td>
                <td>{s.active ? <Badge kind="COMPLETED">active</Badge> : <Badge kind="CANCELLED">disabled</Badge>}</td>
                <td><button className="btn sm" onClick={() => toggle(s.uid)}>{s.active ? "Disable" : "Enable"}</button></td>
              </tr>
            ))}
          </tbody>
        </table></div>
      </Card>

      <Card title="Treatment catalogue &amp; pricing">
        <div className="table-wrap"><table>
          <thead><tr><th>Treatment</th><th>Description</th><th style={{ textAlign: "right" }}>Base cost</th></tr></thead>
          <tbody>
            {treatments.map((t) => (
              <tr key={t.id}><td>{t.name}</td><td className="muted">{t.description}</td><td style={{ textAlign: "right" }}>{rs(t.baseCost)}</td></tr>
            ))}
          </tbody>
        </table></div>
      </Card>
    </div>
  );
}
