import { useEffect, useState } from "react";
import { Card, ErrorBanner, Badge } from "../../components/ui";
import type { Dentist, Slot } from "../../api/types";
import * as api from "../../api/api";

function today() { return new Date().toISOString().slice(0, 10); }

export default function PublishAvailability() {
  const [dentists, setDentists] = useState<Dentist[]>([]);
  const [dentistId, setDentistId] = useState("");
  const [date, setDate] = useState(today());
  const [startTime, setStart] = useState("16:00");
  const [endTime, setEnd] = useState("18:00");
  const [slotMinutes, setSlotMinutes] = useState(30);
  const [slots, setSlots] = useState<Slot[]>([]);
  const [error, setError] = useState<unknown>(null);
  const [msg, setMsg] = useState<string | null>(null);

  useEffect(() => { api.getDentists().then((d) => { setDentists(d); setDentistId(d[0]?.id ?? ""); }).catch(setError); }, []);

  async function publish() {
    setError(null); setMsg(null);
    try {
      const res = await api.publishSession({ dentistId, date, startTime, endTime, slotMinutes });
      setSlots(res.slots);
      setMsg(`Published ${res.slots.length} slots for ${dentists.find((d) => d.id === dentistId)?.name} on ${date}.`);
    } catch (e) { setError(e); }
  }

  return (
    <div>
      <div className="page-head"><h1>Publish dentist availability</h1></div>
      <Card title="New availability window">
        <p className="muted">e.g. “Dr. Silva is in the clinic on {date} from {startTime} to {endTime}”. The system splits it into bookable slots.</p>
        <div className="row">
          <div>
            <label>Dentist</label>
            <select value={dentistId} onChange={(e) => setDentistId(e.target.value)}>
              {dentists.map((d) => <option key={d.id} value={d.id}>{d.name} — {d.specialization}</option>)}
            </select>
          </div>
          <div><label>Date</label><input type="date" min={today()} value={date} onChange={(e) => setDate(e.target.value)} /></div>
        </div>
        <div className="row">
          <div><label>Start</label><input type="time" value={startTime} onChange={(e) => setStart(e.target.value)} /></div>
          <div><label>End</label><input type="time" value={endTime} onChange={(e) => setEnd(e.target.value)} /></div>
          <div><label>Slot length (min)</label>
            <select value={slotMinutes} onChange={(e) => setSlotMinutes(Number(e.target.value))}>
              <option value={15}>15</option><option value={30}>30</option><option value={45}>45</option><option value={60}>60</option>
            </select>
          </div>
        </div>
        <button className="btn primary" style={{ marginTop: 14 }} onClick={publish}>Publish slots</button>
        {msg && <div className="banner info" style={{ marginTop: 10 }}>{msg}</div>}
        {error ? <ErrorBanner error={error} /> : null}
      </Card>

      {slots.length > 0 && (
        <Card title="Generated slots">
          <div className="row" style={{ flexWrap: "wrap" }}>
            {slots.map((s) => (
              <span key={s.id} className="btn sm" style={{ flex: "0 0 auto" }}>
                {s.startTime} <Badge kind={s.status}>{s.status}</Badge>
              </span>
            ))}
          </div>
        </Card>
      )}
    </div>
  );
}
