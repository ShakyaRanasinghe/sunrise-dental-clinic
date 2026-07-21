// Typed API surface. Real endpoints (appointments, slots, bill, patient, auth) hit the
// Spring Boot backend; endpoints not yet built (dentists, treatments, reports, AI, staff)
// transparently fall back to bundled sample data via liveOrMock so the SPA always works.

import { http, liveOrMock } from "./client";
import * as mock from "./mock";
import type {
  AiSettings, AiTriageResult, Appointment, Bill, Dentist, FootfallPoint,
  IncomeReport, LockStatus, Patient, Slot, StaffMember, Treatment,
} from "./types";

// ---- Availability & slots (backend: SlotController) ----
export const getAvailability = (dentistId: string, date: string) =>
  liveOrMock<Slot[]>(
    () => http.get(`/availability?dentistId=${encodeURIComponent(dentistId)}&date=${date}`),
    () => mock.mockSlots(dentistId, date),
    "GET /availability",
  );

export const getWeekAvailability = (from: string, to: string) =>
  liveOrMock<Slot[]>(
    () => http.get(`/availability/week?from=${from}&to=${to}`),
    () => mock.mockWeek(from, to),
    "GET /availability/week",
  );

export const publishSession = (body: {
  dentistId: string; date: string; startTime: string; endTime: string; slotMinutes?: number;
}) =>
  liveOrMock<{ sessionId: string; slots: Slot[] }>(
    () => http.post("/sessions", body),
    () => ({ sessionId: "mock-session", slots: mock.mockSlots(body.dentistId, body.date) }),
    "POST /sessions",
  );

// ---- Appointments (backend: AppointmentController) ----
export const bookAppointment = (body: { slotId: string; treatmentId: string; patientId?: string }) =>
  liveOrMock<Appointment>(
    () => http.post("/appointments", body),
    () => mock.mockBook(body.slotId, body.treatmentId, body.patientId || "P-self"),
    "POST /appointments",
  );

export const getAppointment = (no: string) =>
  liveOrMock<Appointment>(() => http.get(`/appointments/${no}`), () => mock.mockGetAppointment(no), "GET /appointments/{no}");

export const cancelAppointment = (no: string) =>
  liveOrMock<Appointment>(() => http.post(`/appointments/${no}/cancel`), () => mock.mockCancel(no), "POST cancel");

export const completeAppointment = (no: string, diagnosis: string) =>
  liveOrMock<Appointment>(
    () => http.post(`/appointments/${no}/complete`, { diagnosis }),
    () => mock.mockComplete(no, diagnosis),
    "POST complete",
  );

export const listAppointments = () =>
  liveOrMock<Appointment[]>(() => Promise.reject(new Error("no list endpoint")), () => mock.mockListAppointments(), "GET appointments (list)");

// ---- Bills (backend: BillController) ----
export const generateBill = (no: string) =>
  liveOrMock<Bill>(() => http.post(`/appointments/${no}/bill`), () => mock.mockBill(no), "POST bill");

export const getBill = (no: string) =>
  liveOrMock<Bill>(() => http.get(`/appointments/${no}/bill`), () => mock.mockBill(no), "GET bill");

// ---- Patients (backend: PatientController) ----
export const registerPatient = (body: {
  name: string; address?: string; contactNumber: string; email?: string; dob?: string;
}) => http.post<Patient>("/patients", body);

export const getPatient = (id: string) => http.get<Patient>(`/patients/${id}`);

// ---- Auth / lockout (backend: AuthController) ----
export const reportFailedLogin = (email: string) => http.post<LockStatus>("/auth/failed-login", { email });
export const reportSuccessfulLogin = (email: string) => http.post<LockStatus>("/auth/successful-login", { email });
export const getLockStatus = (email: string) => http.get<LockStatus>(`/auth/lock-status?email=${encodeURIComponent(email)}`);
export const unlockAccount = (email: string) => http.post<LockStatus>("/auth/unlock", { email });

// ---- Reference data (backend endpoints pending → mock) ----
export const getDentists = () =>
  liveOrMock<Dentist[]>(() => http.get("/dentists"), () => mock.mockDentists, "GET /dentists");
export const getTreatments = () =>
  liveOrMock<Treatment[]>(() => http.get("/treatments"), () => mock.mockTreatments, "GET /treatments");
export const getStaff = () =>
  liveOrMock<StaffMember[]>(() => http.get("/admin/staff"), () => mock.mockStaff, "GET /admin/staff");

// ---- Reports (backend endpoints pending → mock) ----
export const getIncomeReport = (period: string) =>
  liveOrMock<IncomeReport>(() => http.get(`/admin/reports/income?period=${period}`), () => mock.mockIncome(period), "GET income");
export const getFootfall = () =>
  liveOrMock<FootfallPoint[]>(() => http.get("/admin/reports/footfall"), () => mock.mockFootfall(), "GET footfall");
export const getFootfallByReceptionist = () =>
  liveOrMock<FootfallPoint[]>(() => http.get("/admin/reports/footfall-receptionist"), () => mock.mockFootfallByReceptionist(), "GET footfall-recep");

// ---- AI (backend endpoints pending → mock) ----
export const aiTriage = (symptoms: string) =>
  liveOrMock<AiTriageResult>(() => http.post("/ai/triage", { symptoms }), () => mock.mockTriage(symptoms), "POST /ai/triage");
export const aiHelp = (question: string) =>
  liveOrMock<{ answer: string }>(() => http.post("/ai/help", { question }), () => ({ answer: mock.mockHelpAnswer(question) }), "POST /ai/help");
export const getAiSettings = () =>
  liveOrMock<AiSettings>(() => http.get("/admin/ai-settings"), () => mock.getMockAiSettings(), "GET /admin/ai-settings");
export const updateAiSettings = (s: AiSettings) =>
  liveOrMock<AiSettings>(() => http.put("/admin/ai-settings", s), () => mock.setMockAiSettings(s), "PUT /admin/ai-settings");
