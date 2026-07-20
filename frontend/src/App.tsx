import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth, homeFor } from "./auth/AuthContext";
import { RequireRole } from "./auth/RequireRole";
import { Layout } from "./components/Layout";

import LoginPage from "./pages/LoginPage";
import HelpPage from "./pages/HelpPage";
import PatientHome from "./pages/patient/PatientHome";
import BookAppointment from "./pages/patient/BookAppointment";
import MyAppointments from "./pages/patient/MyAppointments";
import AiTriage from "./pages/patient/AiTriage";
import ReceptionHome from "./pages/reception/ReceptionHome";
import PublishAvailability from "./pages/reception/PublishAvailability";
import RegisterPatient from "./pages/reception/RegisterPatient";
import Billing from "./pages/reception/Billing";
import DentistSchedule from "./pages/dentist/DentistSchedule";
import AdminReports from "./pages/admin/AdminReports";
import AiSettingsPage from "./pages/admin/AiSettingsPage";
import ManagePage from "./pages/admin/ManagePage";

export default function App() {
  const { user } = useAuth();
  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to={homeFor(user.role)} replace /> : <LoginPage />} />

      <Route element={<Layout />}>
        <Route path="/help" element={<RequireRole roles={["PATIENT", "RECEPTIONIST", "DENTIST", "ADMIN"]}><HelpPage /></RequireRole>} />

        {/* Patient */}
        <Route path="/patient" element={<RequireRole roles={["PATIENT"]}><PatientHome /></RequireRole>} />
        <Route path="/patient/book" element={<RequireRole roles={["PATIENT", "RECEPTIONIST", "ADMIN"]}><BookAppointment /></RequireRole>} />
        <Route path="/patient/appointments" element={<RequireRole roles={["PATIENT"]}><MyAppointments /></RequireRole>} />
        <Route path="/patient/triage" element={<RequireRole roles={["PATIENT"]}><AiTriage /></RequireRole>} />

        {/* Receptionist */}
        <Route path="/reception" element={<RequireRole roles={["RECEPTIONIST", "ADMIN"]}><ReceptionHome /></RequireRole>} />
        <Route path="/reception/availability" element={<RequireRole roles={["RECEPTIONIST", "ADMIN"]}><PublishAvailability /></RequireRole>} />
        <Route path="/reception/register" element={<RequireRole roles={["RECEPTIONIST", "ADMIN"]}><RegisterPatient /></RequireRole>} />
        <Route path="/reception/billing" element={<RequireRole roles={["RECEPTIONIST", "ADMIN"]}><Billing /></RequireRole>} />

        {/* Dentist */}
        <Route path="/dentist" element={<RequireRole roles={["DENTIST"]}><DentistSchedule /></RequireRole>} />

        {/* Admin */}
        <Route path="/admin" element={<RequireRole roles={["ADMIN"]}><AdminReports /></RequireRole>} />
        <Route path="/admin/ai" element={<RequireRole roles={["ADMIN"]}><AiSettingsPage /></RequireRole>} />
        <Route path="/admin/manage" element={<RequireRole roles={["ADMIN"]}><ManagePage /></RequireRole>} />
      </Route>

      <Route path="*" element={<Navigate to={user ? homeFor(user.role) : "/login"} replace />} />
    </Routes>
  );
}
