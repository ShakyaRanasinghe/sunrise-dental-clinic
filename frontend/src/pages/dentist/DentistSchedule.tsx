import { useEffect, useState } from "react";
import { Card, Badge, ErrorBanner } from "../../components/ui";
import type { Appointment } from "../../api/types";
import * as api from "../../api/api";

export default function DentistSchedule() {
  const [list, setList] = useState<Appointment[]>([]);
  const [no, setNo] = useState("");
  const [selected, setSelected] = useState<Appointment | null>(null);
  const [diagnosis, setDiagnosis] = useState("");
  const [error, setError] = useState<unknown>(null);
  const [saved, setSaved] = useState(false);

  const refresh = () => api.listAppointments().then(setList).catch(setError);
  useEffect(() => { refresh(); }, []);

  async function open(n: string) {
    setError(null); setSaved(false); setDiagnosis("");
    try { const a = await api.getAppointment(n.trim()); setSelected(a); setDiagnosis(a.diagnosis ?? ""); }
    catch (e) { setError(e); }
  }
  async function complete() {
    if (!selected) return;
    setError(null);
    try {
      const a = await api.completeAppointment(selected.appointmentNo, diagnosis);
      setSelected(a); setSaved(true); await refresh();
    } catch (e) { setError(e); }
  }

  return (
    <div>
      <div className="page-head"><h1>My schedule</h1></div>
      {error ? <ErrorBanner error={error} /> : null}
      <div className="banner info">🔒 Diagnosis notes you record here are confidential — visible only to you and the patient, never to reception or admin.</div>

      <Card title="Open an appointment">
        <div className="row">
          <input value={no} onChange={(e) => setNo(e.target.value)} placeholder="APT-20260720-0001" />
          <button className="btn primary" style={{ flex: "0 0 auto" }} onClick={() => open(no)}>Open</button>
        </div>
      </Card>

      {selected && (
        <Card title={`Appointment ${selected.appointmentNo}`}>
          <table><tbody>
            <tr><th>Patient</th><td>{selected.patientId}</td></tr>
            <tr><th>When</th><td>{selected.date} at {selected.time}</td></tr>
            <tr><th>Status</th><td><Badge kind={selected.status}>{selected.status}</Badge></td></tr>
          </tbody></table>
          <label>Diagnosis / clinical notes (confidential)</label>
          <textarea rows={3} value={diagnosis} onChange={(e) => setDiagnosis(e.target.value)} placeholder="e.g. Distal caries on tooth 36; recommended composite filling." />
          <button className="btn primary" style={{ marginTop: 10 }} disabled={selected.status === "CANCELLED"} onClick={complete}>
            Mark treatment completed
          </button>
          {saved && <div className="banner info" style={{ marginTop: 10 }}>Saved. Reception can now generate the bill.</div>}
        </Card>
      )}

      <Card title="Today's appointments">
        {list.length === 0 ? <p className="muted">No appointments loaded. Open one by number above.</p> : (
          <div className="table-wrap"><table>
            <thead><tr><th>No.</th><th>Patient</th><th>Time</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {list.map((a) => (
                <tr key={a.appointmentNo}>
                  <td>{a.appointmentNo}</td><td>{a.patientId}</td><td>{a.time}</td>
                  <td><Badge kind={a.status}>{a.status}</Badge></td>
                  <td><button className="btn sm" onClick={() => open(a.appointmentNo)}>Open</button></td>
                </tr>
              ))}
            </tbody>
          </table></div>
        )}
      </Card>
    </div>
  );
}
