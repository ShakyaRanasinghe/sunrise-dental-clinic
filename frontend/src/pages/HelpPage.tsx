import { useState } from "react";
import { Card } from "../components/ui";
import * as api from "../api/api";
import { useAuth } from "../auth/AuthContext";

interface Msg { from: "user" | "bot"; text: string; }

const GUIDES: Record<string, { q: string; a: string }[]> = {
  PATIENT: [
    { q: "How do I book an appointment?", a: "Open Book Appointment → pick a dentist → choose a date → select an open time slot → choose a treatment → Confirm. You'll get an appointment number (APT-…)." },
    { q: "How do I cancel?", a: "Go to My Appointments, find the appointment, and press Cancel. The slot is released for others." },
    { q: "Where is my bill?", a: "After your treatment is completed, view and download the receipt from My Appointments → View bill." },
    { q: "Is my diagnosis private?", a: "Yes — only you and your treating dentist can see your diagnosis. Reception and admin never see it." },
  ],
  RECEPTIONIST: [
    { q: "How do I open a dentist's availability?", a: "Publish Availability → choose the dentist, date, start/end time and slot length → Publish. Patients can then book those slots." },
    { q: "How do I bill a patient?", a: "Billing → enter the appointment number → once the dentist has marked it completed, press Generate bill." },
    { q: "How do I add a walk-in patient?", a: "Register Patient → fill in name and contact → Register. Then book on their behalf from Book Appointment." },
  ],
  DENTIST: [
    { q: "How do I record a diagnosis?", a: "My Schedule → open the appointment → type your diagnosis/notes → Mark treatment completed. Notes are confidential." },
  ],
  ADMIN: [
    { q: "How do I change the AI model?", a: "AI Settings → pick a provider/model → Save. It applies instantly, no redeploy." },
    { q: "How do I read the income split?", a: "Reports shows total income and the 3-way split (dentist/clinic/receptionist), by day/week/month and per dentist." },
    { q: "How do I disable a staff account?", a: "Staff & Catalogue → Disable. It's a soft-disable (never deleted) and is audit-logged." },
  ],
};

export default function HelpPage() {
  const { user } = useAuth();
  const guides = GUIDES[user?.role ?? "PATIENT"] ?? [];
  const [messages, setMessages] = useState<Msg[]>([{ from: "bot", text: "Hi! Ask me how to do anything in the system." }]);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);

  async function send() {
    const q = input.trim();
    if (!q) return;
    setMessages((m) => [...m, { from: "user", text: q }]);
    setInput(""); setBusy(true);
    try { const { answer } = await api.aiHelp(q); setMessages((m) => [...m, { from: "bot", text: answer }]); }
    catch { setMessages((m) => [...m, { from: "bot", text: "Sorry, I couldn't answer that right now." }]); }
    finally { setBusy(false); }
  }

  return (
    <div>
      <div className="page-head"><h1>Help &amp; how-to</h1></div>
      <div className="grid cols-2">
        <Card title="Step-by-step guides">
          {guides.map((g, i) => (
            <details key={i} style={{ marginBottom: 8 }}>
              <summary style={{ cursor: "pointer", fontWeight: 600 }}>{g.q}</summary>
              <p className="muted" style={{ margin: "6px 0 0" }}>{g.a}</p>
            </details>
          ))}
        </Card>
        <Card title="Ask the assistant">
          <div className="chat">
            {messages.map((m, i) => <div key={i} className={`msg ${m.from}`}>{m.text}</div>)}
          </div>
          <div className="row" style={{ marginTop: 10 }}>
            <input value={input} onChange={(e) => setInput(e.target.value)}
                   onKeyDown={(e) => e.key === "Enter" && send()} placeholder="Type your question…" />
            <button className="btn primary" style={{ flex: "0 0 auto" }} disabled={busy} onClick={send}>Send</button>
          </div>
        </Card>
      </div>
    </div>
  );
}
