import { useEffect, useState } from "react";
import { Card, ErrorBanner, Spinner } from "../../components/ui";
import type { AiSettings } from "../../api/types";
import * as api from "../../api/api";

const MODELS: Record<string, string[]> = {
  gemini: ["gemini-1.5-flash", "gemini-1.5-pro", "gemini-2.0-flash"],
  groq: ["llama-3.1-8b-instant", "llama-3.3-70b-versatile"],
  openai: ["gpt-4o-mini", "gpt-4o"],
};

export default function AiSettingsPage() {
  const [s, setS] = useState<AiSettings | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [saved, setSaved] = useState(false);

  useEffect(() => { api.getAiSettings().then(setS).catch(setError); }, []);
  if (error) return <ErrorBanner error={error} />;
  if (!s) return <Spinner label="Loading AI settings…" />;

  async function save() {
    setSaved(false);
    try { setS(await api.updateAiSettings(s!)); setSaved(true); } catch (e) { setError(e); }
  }
  const models = MODELS[s.provider] ?? [];

  return (
    <div>
      <div className="page-head"><h1>🤖 AI configuration</h1></div>
      <Card title="Model settings (applied live)">
        <p className="muted">
          Change the AI provider/model used by the symptom-triage and help assistants.
          Saving takes effect immediately — no redeploy — and is recorded in the audit log.
        </p>
        <div className="row">
          <div>
            <label>Provider</label>
            <select value={s.provider} onChange={(e) => setS({ ...s, provider: e.target.value, model: MODELS[e.target.value][0] })}>
              {Object.keys(MODELS).map((p) => <option key={p} value={p}>{p}</option>)}
            </select>
          </div>
          <div>
            <label>Model</label>
            <select value={s.model} onChange={(e) => setS({ ...s, model: e.target.value })}>
              {models.map((m) => <option key={m} value={m}>{m}</option>)}
            </select>
          </div>
        </div>
        <label>Temperature: {s.temperature.toFixed(2)}</label>
        <input type="range" min={0} max={1} step={0.05} value={s.temperature}
               onChange={(e) => setS({ ...s, temperature: Number(e.target.value) })} />
        <label style={{ display: "flex", gap: 8, alignItems: "center", marginTop: 12 }}>
          <input type="checkbox" style={{ width: "auto" }} checked={s.enabled} onChange={(e) => setS({ ...s, enabled: e.target.checked })} />
          AI features enabled
        </label>
        <button className="btn primary" style={{ marginTop: 14 }} onClick={save}>Save settings</button>
        {saved && <div className="banner info" style={{ marginTop: 10 }}>Saved — the AI assistants now use <strong>{s.provider} / {s.model}</strong>.</div>}
        <div className="disclaimer" style={{ marginTop: 12 }}>
          The API key stays server-side (secret/env). This screen changes model & parameters only — it never exposes the key.
        </div>
      </Card>
    </div>
  );
}
