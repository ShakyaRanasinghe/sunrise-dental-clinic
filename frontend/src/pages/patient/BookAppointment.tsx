import { useEffect, useState } from "react";
import { Card, Steps, ErrorBanner, Spinner, Badge, rs } from "../../components/ui";
import type { Appointment, Dentist, Slot, Treatment } from "../../api/types";
import * as api from "../../api/api";
import { useAuth } from "../../auth/AuthContext";
import { useSearchParams } from "react-router-dom";

function today() { return new Date().toISOString().slice(0, 10); }

export default function BookAppointment() {
  const { user } = useAuth();
  const [params] = useSearchParams();
  const [step, setStep] = useState(0);
  const [dentists, setDentists] = useState<Dentist[]>([]);
  const [treatments, setTreatments] = useState<Treatment[]>([]);
  const [dentist, setDentist] = useState<Dentist | null>(null);
  const [date, setDate] = useState(today());
  const [slots, setSlots] = useState<Slot[]>([]);
  const [slot, setSlot] = useState<Slot | null>(null);
  const [treatment, setTreatment] = useState<Treatment | null>(null);
  const [patientId, setPatientId] = useState(user?.role === "PATIENT" ? "" : "");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [booked, setBooked] = useState<Appointment | null>(null);

  useEffect(() => {
    Promise.all([api.getDentists(), api.getTreatments()])
      .then(([d, t]) => {
        setDentists(d); setTreatments(t);
        const pre = params.get("treatmentId");
        if (pre) setTreatment(t.find((x) => x.id === pre) ?? null);
      })
      .catch(setError);
  }, [params]);

  async function loadSlots(d: Dentist, dt: string) {
    setLoading(true); setError(null); setSlot(null);
    try { setSlots(await api.getAvailability(d.id, dt)); }
    catch (e) { setError(e); } finally { setLoading(false); }
  }

  async function confirm() {
    if (!slot || !treatment) return;
    setLoading(true); setError(null);
    try {
      const appt = await api.bookAppointment({
        slotId: slot.id, treatmentId: treatment.id,
        patientId: user?.role === "PATIENT" ? undefined : patientId || undefined,
      });
      setBooked(appt);
    } catch (e) { setError(e); } finally { setLoading(false); }
  }

  if (booked) {
    return (
      <Card title="✅ Appointment confirmed">
        <p>Your appointment is booked. A confirmation has been sent to you.</p>
        <table><tbody>
          <tr><th>Appointment No.</th><td><strong>{booked.appointmentNo}</strong></td></tr>
          <tr><th>Date &amp; time</th><td>{booked.date} at {booked.time}</td></tr>
          <tr><th>Dentist</th><td>{dentist?.name}</td></tr>
          <tr><th>Treatment</th><td>{treatment?.name}</td></tr>
          <tr><th>Status</th><td><Badge kind={booked.status}>{booked.status}</Badge></td></tr>
        </tbody></table>
        <button className="btn primary" style={{ marginTop: 14 }} onClick={() => { setBooked(null); setStep(0); setSlot(null); setTreatment(null); }}>
          Book another
        </button>
      </Card>
    );
  }

  return (
    <div>
      <div className="page-head"><h1>Book an appointment</h1></div>
      <Steps total={4} current={step} />
      {error ? <ErrorBanner error={error} /> : null}

      {step === 0 && (
        <Card title="1. Choose a dentist">
          <div className="grid cols-3">
            {dentists.map((d) => (
              <div key={d.id} className={`tile ${dentist?.id === d.id ? "active" : ""}`}
                   onClick={() => { setDentist(d); loadSlots(d, date); setStep(1); }}>
                <div>{d.name}</div>
                <small>{d.specialization}</small>
                <div><small>Consultation {rs(d.consultationFee)}</small></div>
              </div>
            ))}
          </div>
        </Card>
      )}

      {step === 1 && dentist && (
        <Card title={`2. Pick a date — ${dentist.name}`}
              actions={<button className="btn sm" onClick={() => setStep(0)}>← Change dentist</button>}>
          <label>Date</label>
          <input type="date" value={date} min={today()} onChange={(e) => { setDate(e.target.value); loadSlots(dentist, e.target.value); }} />
          <div style={{ marginTop: 14 }}>
            {loading ? <Spinner label="Loading availability…" /> : (
              slots.length === 0
                ? <div className="banner warn">No open slots for this dentist on {date}. Try another date, or ask reception to publish availability.</div>
                : <>
                    <h3>Open slots</h3>
                    <div className="row" style={{ flexWrap: "wrap" }}>
                      {slots.map((s) => (
                        <button key={s.id} className={`btn ${slot?.id === s.id ? "primary" : ""}`}
                                style={{ flex: "0 0 auto" }} onClick={() => { setSlot(s); setStep(2); }}>
                          {s.startTime}
                        </button>
                      ))}
                    </div>
                  </>
            )}
          </div>
        </Card>
      )}

      {step === 2 && slot && (
        <Card title="3. Choose a treatment"
              actions={<button className="btn sm" onClick={() => setStep(1)}>← Change slot</button>}>
          <p className="muted">Slot: <strong>{slot.date} at {slot.startTime}</strong> with {dentist?.name}</p>
          <div className="grid cols-3">
            {treatments.map((t) => (
              <div key={t.id} className={`tile ${treatment?.id === t.id ? "active" : ""}`} onClick={() => { setTreatment(t); setStep(3); }}>
                <div>{t.name}</div>
                <small>{t.description}</small>
                <div><small>{rs(t.baseCost)}</small></div>
              </div>
            ))}
          </div>
        </Card>
      )}

      {step === 3 && slot && treatment && (
        <Card title="4. Confirm" actions={<button className="btn sm" onClick={() => setStep(2)}>← Change treatment</button>}>
          {user?.role !== "PATIENT" && (
            <>
              <label>Patient ID (booking on behalf of a patient)</label>
              <input value={patientId} onChange={(e) => setPatientId(e.target.value)} placeholder="Patient ID" />
            </>
          )}
          <table style={{ marginTop: 10 }}><tbody>
            <tr><th>Dentist</th><td>{dentist?.name} — {dentist?.specialization}</td></tr>
            <tr><th>When</th><td>{slot.date} at {slot.startTime}</td></tr>
            <tr><th>Treatment</th><td>{treatment.name}</td></tr>
            <tr><th>Est. consultation</th><td>{rs(dentist?.consultationFee ?? 0)}</td></tr>
            <tr><th>Est. treatment</th><td>{rs(treatment.baseCost)}</td></tr>
          </tbody></table>
          <button className="btn primary" style={{ marginTop: 14 }} disabled={loading} onClick={confirm}>
            {loading ? "Booking…" : "Confirm booking"}
          </button>
        </Card>
      )}
    </div>
  );
}
