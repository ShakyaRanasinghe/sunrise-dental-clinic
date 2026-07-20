import { ReactNode } from "react";

export function Spinner({ label }: { label?: string }) {
  return (
    <div className="center">
      <div style={{ display: "grid", placeItems: "center", gap: 10 }}>
        <div className="spinner" />
        {label && <small>{label}</small>}
      </div>
    </div>
  );
}

export function ErrorBanner({ error }: { error: unknown }) {
  const msg = error instanceof Error ? error.message : String(error);
  return <div className="banner error">⚠ {msg}</div>;
}

export function Card({ title, children, actions }: { title?: string; children: ReactNode; actions?: ReactNode }) {
  return (
    <div className="card">
      {(title || actions) && (
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 10 }}>
          {title && <h3 style={{ margin: 0 }}>{title}</h3>}
          {actions}
        </div>
      )}
      {children}
    </div>
  );
}

export function StatCard({ label, value, delta }: { label: string; value: string | number; delta?: { dir: "up" | "down"; text: string } }) {
  return (
    <div className="card stat">
      <div className="value">{value}</div>
      <div className="label">{label}</div>
      {delta && <div className={`delta ${delta.dir}`}>{delta.dir === "up" ? "▲" : "▼"} {delta.text}</div>}
    </div>
  );
}

export function Badge({ kind, children }: { kind: string; children: ReactNode }) {
  return <span className={`badge ${kind}`}>{children}</span>;
}

/** Reused pattern from Idasara's OnboardingStepIndicator — a compact multi-step progress bar. */
export function Steps({ total, current }: { total: number; current: number }) {
  return (
    <div className="steps" aria-label={`Step ${current + 1} of ${total}`}>
      {Array.from({ length: total }).map((_, i) => (
        <div key={i} className={`step ${i < current ? "done" : i === current ? "active" : ""}`} />
      ))}
    </div>
  );
}

export const rs = (n: number) => "Rs " + n.toLocaleString("en-LK");
