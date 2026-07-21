import { useState } from "react";
import { Card, Badge, ErrorBanner, rs } from "../../components/ui";
import type { Appointment, Bill } from "../../api/types";
import * as api from "../../api/api";

export default function Billing() {
  const [no, setNo] = useState("");
  const [appt, setAppt] = useState<Appointment | null>(null);
  const [bill, setBill] = useState<Bill | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [loading, setLoading] = useState(false);

  async function find() {
    setError(null); setBill(null); setAppt(null);
    try {
      const a = await api.getAppointment(no.trim());
      setAppt(a);
      try { setBill(await api.getBill(a.appointmentNo)); } catch { /* no bill yet */ }
    } catch (e) { setError(e); }
  }
  async function generate() {
    if (!appt) return;
    setLoading(true); setError(null);
    try { setBill(await api.generateBill(appt.appointmentNo)); setAppt({ ...appt, status: "BILLED" }); }
    catch (e) { setError(e); } finally { setLoading(false); }
  }

  return (
    <div>
      <div className="page-head"><h1>Billing</h1></div>
      <Card title="Find appointment">
        <div className="row">
          <input value={no} onChange={(e) => setNo(e.target.value)} placeholder="APT-20260720-0001" />
          <button className="btn primary" style={{ flex: "0 0 auto" }} onClick={find}>Find</button>
        </div>
        {error ? <ErrorBanner error={error} /> : null}
        {appt && (
          <div style={{ marginTop: 12 }}>
            <table><tbody>
              <tr><th>Appointment</th><td>{appt.appointmentNo} <Badge kind={appt.status}>{appt.status}</Badge></td></tr>
              <tr><th>When</th><td>{appt.date} at {appt.time}</td></tr>
            </tbody></table>
            {!bill && appt.status === "COMPLETED" && (
              <button className="btn primary" style={{ marginTop: 10 }} disabled={loading} onClick={generate}>
                {loading ? "Generating…" : "Generate bill"}
              </button>
            )}
            {!bill && appt.status === "CONFIRMED" &&
              <div className="banner warn" style={{ marginTop: 10 }}>The dentist must mark the treatment complete before a bill can be generated.</div>}
          </div>
        )}
      </Card>

      {bill && (
        <Card title={`Receipt ${bill.id}`}>
          <table><tbody>
            <tr><td>Consultation fee</td><td style={{ textAlign: "right" }}>{rs(bill.consultationFee)}</td></tr>
            <tr><td>Treatment cost</td><td style={{ textAlign: "right" }}>{rs(bill.treatmentCost)}</td></tr>
            <tr><td>Service charge</td><td style={{ textAlign: "right" }}>{rs(bill.serviceCharge)}</td></tr>
            {bill.discount > 0 && <tr><td>Discount</td><td style={{ textAlign: "right" }}>-{rs(bill.discount)}</td></tr>}
            {bill.tax > 0 && <tr><td>Tax</td><td style={{ textAlign: "right" }}>{rs(bill.tax)}</td></tr>}
            <tr><td><strong>Total</strong></td><td style={{ textAlign: "right" }}><strong>{rs(bill.total)}</strong></td></tr>
          </tbody></table>
          <p className="muted" style={{ marginTop: 8, fontSize: ".82rem" }}>
            The 3-way revenue split (dentist / clinic / receptionist) is recorded internally and visible to Admin in Reports.
          </p>
        </Card>
      )}
    </div>
  );
}
