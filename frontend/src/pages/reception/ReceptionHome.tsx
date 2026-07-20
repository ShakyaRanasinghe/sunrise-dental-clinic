import { Link } from "react-router-dom";
import { Card } from "../../components/ui";

export default function ReceptionHome() {
  return (
    <div>
      <div className="page-head"><h1>Front desk</h1></div>
      <div className="grid cols-2">
        <Card title="🕑 Publish dentist availability">
          <p className="muted">Open a dentist's clinic hours so patients can self-book into slots.</p>
          <Link className="btn primary" to="/reception/availability">Publish slots</Link>
        </Card>
        <Card title="➕ Register a patient">
          <p className="muted">Add a walk-in or phone patient to the system.</p>
          <Link className="btn" to="/reception/register">Register patient</Link>
        </Card>
        <Card title="📅 Book for a patient">
          <p className="muted">Create an appointment on behalf of a patient.</p>
          <Link className="btn" to="/patient/book">New appointment</Link>
        </Card>
        <Card title="🧾 Generate a bill">
          <p className="muted">Produce the receipt for a completed treatment.</p>
          <Link className="btn" to="/reception/billing">Billing</Link>
        </Card>
      </div>
    </div>
  );
}
