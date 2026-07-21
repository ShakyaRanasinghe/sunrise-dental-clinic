import { Link } from "react-router-dom";
import { Card } from "../../components/ui";
import { useAuth } from "../../auth/AuthContext";

export default function PatientHome() {
  const { user } = useAuth();
  return (
    <div>
      <div className="page-head"><h1>Welcome, {user?.displayName?.split(" ")[0]} 👋</h1></div>
      <div className="grid cols-3">
        <Card title="📅 Book an appointment">
          <p className="muted">See which dentists are available and book an open slot in a few taps.</p>
          <Link className="btn primary" to="/patient/book">Book now</Link>
        </Card>
        <Card title="🦷 Not sure what you need?">
          <p className="muted">Describe your symptoms and our AI assistant will suggest what kind of care may help.</p>
          <Link className="btn" to="/patient/triage">Check symptoms</Link>
        </Card>
        <Card title="🗂️ Your appointments">
          <p className="muted">View upcoming visits, cancel, or download your bill.</p>
          <Link className="btn" to="/patient/appointments">View appointments</Link>
        </Card>
      </div>
      <div style={{ marginTop: 16 }}>
        <div className="banner info">
          Your clinical diagnosis is private — only you and your treating dentist can ever see it.
        </div>
      </div>
    </div>
  );
}
