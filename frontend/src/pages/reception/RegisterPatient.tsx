import { useState } from "react";
import { Card, ErrorBanner } from "../../components/ui";
import type { Patient } from "../../api/types";
import * as api from "../../api/api";

export default function RegisterPatient() {
  const [form, setForm] = useState({ name: "", address: "", contactNumber: "", email: "", dob: "" });
  const [created, setCreated] = useState<Patient | null>(null);
  const [error, setError] = useState<unknown>(null);
  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) => setForm({ ...form, [k]: e.target.value });

  async function submit() {
    setError(null); setCreated(null);
    if (!form.name || !form.contactNumber) { setError(new Error("Name and contact number are required.")); return; }
    try {
      const p = await api.registerPatient({
        name: form.name, address: form.address || undefined, contactNumber: form.contactNumber,
        email: form.email || undefined, dob: form.dob || undefined,
      });
      setCreated(p);
      setForm({ name: "", address: "", contactNumber: "", email: "", dob: "" });
    } catch (e) { setError(e); }
  }

  return (
    <div>
      <div className="page-head"><h1>Register a patient</h1></div>
      <Card title="Patient details">
        <div className="row">
          <div><label>Full name *</label><input value={form.name} onChange={set("name")} /></div>
          <div><label>Contact number *</label><input value={form.contactNumber} onChange={set("contactNumber")} placeholder="07X XXX XXXX" /></div>
        </div>
        <div className="row">
          <div><label>Email</label><input type="email" value={form.email} onChange={set("email")} /></div>
          <div><label>Date of birth</label><input type="date" value={form.dob} onChange={set("dob")} /></div>
        </div>
        <label>Address</label><input value={form.address} onChange={set("address")} />
        <button className="btn primary" style={{ marginTop: 14 }} onClick={submit}>Register patient</button>
        {error ? <ErrorBanner error={error} /> : null}
        {created && (
          <div className="banner info" style={{ marginTop: 10 }}>
            Registered <strong>{created.name}</strong>. Patient ID: <code>{created.id}</code>
          </div>
        )}
      </Card>
    </div>
  );
}
