import { useState } from "react";
import { Link } from "react-router-dom";
import { Card, Badge, ErrorBanner } from "../../components/ui";
import type { AiTriageResult } from "../../api/types";
import * as api from "../../api/api";

export default function AiTriage() {
  const [text, setText] = useState("");
  const [result, setResult] = useState<AiTriageResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<unknown>(null);

  async function ask() {
    if (!text.trim()) return;
    setLoading(true); setError(null);
    try { setResult(await api.aiTriage(text.trim())); }
    catch (e) { setError(e); } finally { setLoading(false); }
  }

  return (
    <div>
      <div className="page-head"><h1>🦷 AI symptom check</h1></div>
      <Card title="Describe what you're feeling">
        <p className="muted">Tell us about your teeth or gums — e.g. “sharp pain when I drink something cold”. The assistant will suggest what kind of care might help.</p>
        <textarea rows={3} value={text} onChange={(e) => setText(e.target.value)} placeholder="Describe your symptoms…" />
        <button className="btn primary" style={{ marginTop: 10 }} disabled={loading} onClick={ask}>
          {loading ? "Thinking…" : "Get guidance"}
        </button>
        {error ? <ErrorBanner error={error} /> : null}
      </Card>

      {result && (
        <Card title="Guidance">
          <p>
            <strong>{result.category}</strong>{" "}
            <Badge kind={result.urgency}>{result.urgency === "urgent" ? "See a dentist urgently" : result.urgency === "soon" ? "See a dentist soon" : "Routine"}</Badge>
          </p>
          <p>{result.advice}</p>
          {result.suggestedTreatment && (
            <p>Suggested next step: <strong>{result.suggestedTreatment}</strong>{" "}
              <Link className="btn sm primary" to={`/patient/book?treatmentId=${result.suggestedTreatmentId}`}>Book this</Link>
            </p>
          )}
          <div className="disclaimer">{result.disclaimer}</div>
          {!result.aiGenerated && <p><small className="muted">Offline guidance (rules-based). With a Gemini API key configured, this uses the live model.</small></p>}
        </Card>
      )}
    </div>
  );
}
