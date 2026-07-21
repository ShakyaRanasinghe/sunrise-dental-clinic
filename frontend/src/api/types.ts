// TypeScript mirror of the backend DTOs (com.sunrise.clinic.dto) + reference data.

export type Role = "PATIENT" | "RECEPTIONIST" | "DENTIST" | "ADMIN";
export type SlotStatus = "OPEN" | "BOOKED";
export type AppointmentStatus = "CONFIRMED" | "COMPLETED" | "BILLED" | "CANCELLED";
export type Urgency = "routine" | "soon" | "urgent";

export interface Slot {
  id: string;
  dentistId: string;
  date: string;        // ISO yyyy-MM-dd
  startTime: string;   // HH:mm[:ss]
  durationMinutes: number;
  status: SlotStatus;
}

export interface Appointment {
  appointmentNo: string;
  patientId: string;
  dentistId: string;
  slotId: string;
  treatmentId: string;
  date: string;
  time: string;
  status: AppointmentStatus;
  diagnosis?: string;  // present only on the clinical view (dentist/patient)
}

export interface Bill {
  id: string;
  appointmentNo: string;
  consultationFee: number;
  treatmentCost: number;
  serviceCharge: number;
  discount: number;
  tax: number;
  total: number;
}

export interface Patient {
  id: string;
  userUid?: string;
  name: string;
  address?: string;
  contactNumber: string;
  email?: string;
  dob?: string;
}

export interface LockStatus {
  locked: boolean;
  attemptsRemaining: number;
  retryAfterSeconds: number;
}

// ---- Reference data (backend endpoints pending — served from mock fallback) ----
export interface Dentist {
  id: string;
  name: string;
  specialization: string;
  consultationFee: number;
}

export interface Treatment {
  id: string;
  name: string;
  description: string;
  baseCost: number;
}

export interface StaffMember {
  uid: string;
  name: string;
  email: string;
  role: Role;
  active: boolean;
}

// ---- Reports (backend endpoints pending — served from mock fallback) ----
export interface FootfallPoint { label: string; dentist: number; receptionist?: number; total: number; }
export interface IncomeReport {
  period: string;
  total: number;
  dentistEarning: number;
  clinicEarning: number;
  receptionistEarning: number;
  series: { label: string; total: number }[];
  byDentist: { name: string; earning: number }[];
  byReceptionist: { name: string; earning: number }[];
}

// ---- AI ----
export interface AiTriageResult {
  category: string;
  suggestedTreatmentId?: string;
  suggestedTreatment?: string;
  urgency: Urgency;
  advice: string;
  disclaimer: string;
  aiGenerated: boolean;
}

export interface AiSettings {
  provider: string;
  model: string;
  temperature: number;
  enabled: boolean;
}
