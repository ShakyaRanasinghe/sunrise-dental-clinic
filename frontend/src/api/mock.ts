// Bundled sample data + an in-memory store so the SPA is fully demoable offline (or before
// a given backend endpoint is built). When the real backend is reachable, api.ts prefers it
// and only falls back here on 404/network error (see liveOrMock).

import type {
  AiSettings, AiTriageResult, Appointment, Bill, Dentist, FootfallPoint,
  IncomeReport, Slot, StaffMember, Treatment, Urgency,
} from "./types";

export const mockDentists: Dentist[] = [
  { id: "D1", name: "Dr. Nimal Silva", specialization: "General Dentistry", consultationFee: 1500 },
  { id: "D2", name: "Dr. Anoma Perera", specialization: "Orthodontics", consultationFee: 2500 },
  { id: "D3", name: "Dr. Ruwan Fernando", specialization: "Oral Surgery", consultationFee: 2500 },
];

export const mockTreatments: Treatment[] = [
  { id: "T1", name: "Checkup", description: "Routine dental examination", baseCost: 1000 },
  { id: "T2", name: "Scaling & Cleaning", description: "Plaque and tartar removal", baseCost: 3000 },
  { id: "T3", name: "Filling", description: "Composite cavity filling", baseCost: 5000 },
  { id: "T4", name: "Extraction", description: "Tooth extraction", baseCost: 4500 },
  { id: "T5", name: "Root Canal", description: "Endodontic root-canal treatment", baseCost: 15000 },
  { id: "T6", name: "Whitening", description: "Professional teeth whitening", baseCost: 8000 },
];

export const mockStaff: StaffMember[] = [
  { uid: "u-admin", name: "Samanthi (Manager)", email: "admin@sunrise.lk", role: "ADMIN", active: true },
  { uid: "u-recep1", name: "Dilani Rathnayake", email: "dilani@sunrise.lk", role: "RECEPTIONIST", active: true },
  { uid: "u-recep2", name: "Kasun Jayawardena", email: "kasun@sunrise.lk", role: "RECEPTIONIST", active: true },
  { uid: "D1", name: "Dr. Nimal Silva", email: "silva@sunrise.lk", role: "DENTIST", active: true },
  { uid: "D2", name: "Dr. Anoma Perera", email: "perera@sunrise.lk", role: "DENTIST", active: true },
  { uid: "D3", name: "Dr. Ruwan Fernando", email: "fernando@sunrise.lk", role: "DENTIST", active: false },
];

let aiSettings: AiSettings = { provider: "gemini", model: "gemini-1.5-flash", temperature: 0.4, enabled: true };
export const getMockAiSettings = () => ({ ...aiSettings });
export const setMockAiSettings = (s: AiSettings) => { aiSettings = { ...s }; return getMockAiSettings(); };

// ---------- in-memory clinical store ----------
const slots = new Map<string, Slot>();
const appointments = new Map<string, Appointment>();
const bills = new Map<string, Bill>();
let apptSeq = 0;

function two(n: number) { return String(n).padStart(2, "0"); }

/** Deterministically synthesise open slots for a dentist on a date (09:00–12:00, 16:00–18:00). */
export function mockSlots(dentistId: string, date: string): Slot[] {
  const key = `${dentistId}:${date}`;
  const existing = [...slots.values()].filter((s) => s.dentistId === dentistId && s.date === date);
  if (existing.length) return existing.filter((s) => s.status === "OPEN");
  const windows: [number, number][] = [[9, 12], [16, 18]];
  const made: Slot[] = [];
  for (const [start, end] of windows) {
    for (let h = start; h < end; h++) {
      for (const m of [0, 30]) {
        const id = `S-${key}-${two(h)}${two(m)}`;
        const slot: Slot = { id, dentistId, date, startTime: `${two(h)}:${two(m)}`, durationMinutes: 30, status: "OPEN" };
        slots.set(id, slot);
        made.push(slot);
      }
    }
  }
  return made;
}

export function mockWeek(from: string, to: string): Slot[] {
  // ensure a few dentists have slots across the range start day
  mockDentists.forEach((d) => mockSlots(d.id, from));
  return [...slots.values()].filter((s) => s.date >= from && s.date <= to && s.status === "OPEN");
}

export function mockBook(slotId: string, treatmentId: string, patientId: string): Appointment {
  const slot = slots.get(slotId);
  if (!slot) throw new Error("Slot not found (mock)");
  if (slot.status === "BOOKED") throw new Error("That slot was just taken. Please pick another.");
  const d = (slot.date || "").replace(/-/g, "");
  apptSeq += 1;
  const appointmentNo = `APT-${d}-${String(apptSeq).padStart(4, "0")}`;
  const appt: Appointment = {
    appointmentNo, patientId, dentistId: slot.dentistId, slotId, treatmentId,
    date: slot.date, time: slot.startTime, status: "CONFIRMED",
  };
  slot.status = "BOOKED";
  appointments.set(appointmentNo, appt);
  return { ...appt };
}

export function mockGetAppointment(no: string): Appointment {
  const a = appointments.get(no);
  if (!a) throw new Error(`Appointment ${no} not found (mock)`);
  return { ...a };
}
export function mockListAppointments(): Appointment[] { return [...appointments.values()]; }
export function mockCancel(no: string): Appointment {
  const a = mockGetAppointment(no); a.status = "CANCELLED"; appointments.set(no, a);
  const s = slots.get(a.slotId); if (s) s.status = "OPEN";
  return { ...a };
}
export function mockComplete(no: string, diagnosis: string): Appointment {
  const a = mockGetAppointment(no); a.status = "COMPLETED"; a.diagnosis = diagnosis; appointments.set(no, a);
  return { ...a };
}

const SERVICE_CHARGE = 200;
export function mockBill(no: string): Bill {
  const existing = [...bills.values()].find((b) => b.appointmentNo === no);
  if (existing) return { ...existing };
  const a = mockGetAppointment(no);
  const dentist = mockDentists.find((d) => d.id === a.dentistId);
  const treatment = mockTreatments.find((t) => t.id === a.treatmentId);
  const consultationFee = dentist?.consultationFee ?? 1500;
  const treatmentCost = treatment?.baseCost ?? 0;
  const total = consultationFee + treatmentCost + SERVICE_CHARGE;
  const bill: Bill = {
    id: `RCP-${no.slice(4)}`, appointmentNo: no, consultationFee, treatmentCost,
    serviceCharge: SERVICE_CHARGE, discount: 0, tax: 0, total,
  };
  bills.set(bill.id, bill);
  a.status = "BILLED"; appointments.set(no, a);
  return { ...bill };
}

// ---------- reports ----------
export function mockIncome(period: string): IncomeReport {
  const series = [
    { label: "Mon", total: 42000 }, { label: "Tue", total: 55500 }, { label: "Wed", total: 38000 },
    { label: "Thu", total: 61000 }, { label: "Fri", total: 72500 }, { label: "Sat", total: 89000 },
  ];
  const total = series.reduce((s, p) => s + p.total, 0);
  const treatmentPortion = total * 0.62;
  return {
    period,
    total,
    dentistEarning: Math.round(total * 0.18 + treatmentPortion * 0.6),
    clinicEarning: Math.round(treatmentPortion * 0.4),
    receptionistEarning: 200 * 173,
    series,
    byDentist: [
      { name: "Dr. Silva", earning: 128500 }, { name: "Dr. Perera", earning: 96000 }, { name: "Dr. Fernando", earning: 74000 },
    ],
    byReceptionist: [
      { name: "Dilani R.", earning: 20600 }, { name: "Kasun J.", earning: 14000 },
    ],
  };
}

export function mockFootfall(): FootfallPoint[] {
  return [
    { label: "Dr. Silva", dentist: 12, total: 12 },
    { label: "Dr. Perera", dentist: 9, total: 9 },
    { label: "Dr. Fernando", dentist: 7, total: 7 },
  ];
}
export function mockFootfallByReceptionist(): FootfallPoint[] {
  return [
    { label: "Dilani R.", dentist: 0, receptionist: 17, total: 17 },
    { label: "Kasun J.", dentist: 0, receptionist: 11, total: 11 },
  ];
}

// ---------- AI dental triage (deterministic rules — stands in for Gemini when offline) ----------
const DISCLAIMER =
  "This is general guidance only, not a diagnosis. Please book an appointment with a licensed dentist for anything beyond routine information.";

export function mockTriage(symptoms: string): AiTriageResult {
  const s = symptoms.toLowerCase();
  let category = "General check-up";
  let urgency: Urgency = "routine";
  let treatmentId = "T1";
  let advice = "Your symptoms sound mild. A routine check-up will let a dentist assess things properly.";
  if (/(severe|unbearable|swelling|swollen|abscess|pus|fever|bleeding a lot|knocked out)/.test(s)) {
    category = "Possible acute infection / emergency"; urgency = "urgent"; treatmentId = "T4";
    advice = "These symptoms can indicate an infection or acute problem. Please seek dental care as soon as possible — today if you can.";
  } else if (/(cavity|hole|sensitive|sensitivity|ache|toothache|pain when|cold|hot|sweet)/.test(s)) {
    category = "Possible cavity / sensitivity"; urgency = "soon"; treatmentId = "T3";
    advice = "Sensitivity or aching often points to a cavity. A filling may be needed — see a dentist within a few days.";
  } else if (/(stain|yellow|white(n|r)|colour|color|brighter)/.test(s)) {
    category = "Cosmetic / whitening"; urgency = "routine"; treatmentId = "T6";
    advice = "This sounds cosmetic. A cleaning or whitening treatment can help; no urgency.";
  } else if (/(crooked|braces|align|gap|straighten)/.test(s)) {
    category = "Orthodontic"; urgency = "routine"; treatmentId = "T1";
    advice = "For alignment concerns, an orthodontic consultation is the right first step.";
  } else if (/(plaque|tartar|clean|gum|bleeding gums|breath)/.test(s)) {
    category = "Gum care / cleaning"; urgency = "soon"; treatmentId = "T2";
    advice = "Gum symptoms usually respond well to a professional scaling & cleaning.";
  }
  const treatment = mockTreatments.find((t) => t.id === treatmentId);
  return {
    category, urgency, suggestedTreatmentId: treatmentId, suggestedTreatment: treatment?.name,
    advice, disclaimer: DISCLAIMER, aiGenerated: false,
  };
}

export function mockHelpAnswer(q: string): string {
  const s = q.toLowerCase();
  if (/book|appointment|slot/.test(s))
    return "To book: open **Book Appointment**, choose a dentist, pick a date, select an open time slot, choose the treatment, and confirm. You'll get a confirmation with your appointment number (APT-…).";
  if (/bill|receipt|pay/.test(s))
    return "After a dentist marks your treatment complete, reception generates your bill. You can view and download it under **My Bill**.";
  if (/cancel/.test(s))
    return "Open **My Appointments**, find the appointment, and press **Cancel**. The slot is released for other patients.";
  if (/diagnosis|record/.test(s))
    return "Clinical diagnosis is confidential — only your treating dentist and you can see it. Reception and admin never see diagnosis notes.";
  return "I can help with booking, viewing availability, bills, and cancellations. Try asking 'How do I book an appointment?'";
}
