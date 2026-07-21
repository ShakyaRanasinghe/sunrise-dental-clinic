import { useEffect, useState } from "react";
import { Card, Badge, ErrorBanner, rs } from "../../components/ui";
import type { Appointment, Bill } from "../../api/types";
import * as api from "../../api/api";

export default function MyAppointments() {
  const [list, setList] = useState<Appointment[]>([]);
  const [lookup, setLookup] = useState("");
  const [error, setError] = useState<unknown>(null);
  const [bill, setBill] = useState<Bill | null>(null);
  const [selected, setSelected] = useState<Appointment | null>(null);

  const refresh = () => api.listAppointments().then(setList).catch(setError);
  useEffect(() => { refresh(); }, []);

  async function find() {
    setError(null); setBill(null);
    try { setSelected(await api.getAppointment(lookup.trim())); }
    catch (e) { setError(e); }
  }
  async function cancel(no: string) {
    try { await api.cancelAppointment(no); await refresh(); if (selected?.appointmentNo === no) setSelected(await api.getAppointment(no)); }
    catch (e) { setError(e); }
  }
  async function viewBill(no: string) {
    setError(null);
    try { setBill(await api.getBill(no)); }
    catch (e) { setError(e); }
  }
  function downloadBill(b: Bill) {
    const lines = [
      "SUNRISE DENTAL CLINIC — RECEIPT", "================================",
      `Receipt:      ${b.id}`, `Appointment:  ${b.appointmentNo}`, "",
      `Consultation: ${rs(b.consultationFee)}`, `Treatment:    ${rs(b.treatmentCost)}`,
      `Service:      ${rs(b.serviceCharge)}`, `Discount:     ${rs(b.discount)}`, `Tax:          ${rs(b.tax)}`,
      "--------------------------------", `TOTAL:        ${rs(b.total)}`,
    ].join("\n");
    const url = URL.createObjectURL(new Blob([lines], { type: "text/plain" }));
    const a = document.createElement("a"); a.href = url; a.download = `${b.id}.txt`; a.click(); URL.revokeObjectURL(url);
  }

  return (
    <div>
      <div className="page-head"><h1>My appointments</h1></div>
      {error ? <ErrorBanner error={error} /> : null}

      <Card title="Find an appointment by number">
        <div className="row">
          <input value={lookup} onChange={(e) => setLookup(e.target.value)} placeholder="APT-20260720-0001" />
          <button className="btn primary" style={{ flex: "0 0 auto" }} onClick={find}>Look up</button>
        </div>
        {selected && (
          <div style={{ marginTop: 12 }}>
            <table><tbody>
              <tr><th>Appointment</th><td>{selected.appointmentNo} <Badge kind={selected.status}>{selected.status}</Badge></td></tr>
              <tr><th>When</th><td>{selected.date} at {selected.time}</td></tr>
              <tr><th>Treatment</th><td>{selected.treatmentId}</td></tr>
              {selected.diagnosis && <tr><th>Diagnosis (private)</th><td>{selected.diagnosis}</td></tr>}
            </tbody></table>
            <div className="row" style={{ marginTop: 10 }}>
              {selected.status === "CONFIRMED" && <button className="btn" onClick={() => cancel(selected.appointmentNo)}>Cancel</button>}
              <button className="btn" onClick={() => viewBill(selected.appointmentNo)}>View bill</button>
            </div>
          </div>
        )}
      </Card>

      {bill && (
        <Card title={`Receipt ${bill.id}`} actions={<button className="btn sm" onClick={() => downloadBill(bill)}>⬇ Download</button>}>
          <table><tbody>
            <tr><td>Consultation</td><td style={{ textAlign: "right" }}>{rs(bill.consultationFee)}</td></tr>
            <tr><td>Treatment</td><td style={{ textAlign: "right" }}>{rs(bill.treatmentCost)}</td></tr>
            <tr><td>Service charge</td><td style={{ textAlign: "right" }}>{rs(bill.serviceCharge)}</td></tr>
            <tr><td><strong>Total</strong></td><td style={{ textAlign: "right" }}><strong>{rs(bill.total)}</strong></td></tr>
          </tbody></table>
        </Card>
      )}

      <Card title="Recent appointments (this session)">
        {list.length === 0 ? <p className="muted">No appointments yet. Book one to see it here.</p> : (
          <div className="table-wrap"><table>
            <thead><tr><th>No.</th><th>Date</th><th>Time</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {list.map((a) => (
                <tr key={a.appointmentNo}>
                  <td>{a.appointmentNo}</td><td>{a.date}</td><td>{a.time}</td>
                  <td><Badge kind={a.status}>{a.status}</Badge></td>
                  <td>{a.status === "CONFIRMED" && <button className="btn sm" onClick={() => cancel(a.appointmentNo)}>Cancel</button>}</td>
                </tr>
              ))}
            </tbody>
          </table></div>
        )}
      </Card>
    </div>
  );
}
