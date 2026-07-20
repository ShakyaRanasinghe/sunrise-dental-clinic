import { useEffect, useState } from "react";
import {
  Bar, BarChart, CartesianGrid, Cell, Legend, Line, LineChart, Pie, PieChart,
  ResponsiveContainer, Tooltip, XAxis, YAxis,
} from "recharts";
import { Card, StatCard, Spinner, ErrorBanner, rs } from "../../components/ui";
import type { FootfallPoint, IncomeReport } from "../../api/types";
import * as api from "../../api/api";

const COLORS = ["#0e7c86", "#2c6ebb", "#c9820a", "#1f9d55", "#9b5de5"];
type Period = "day" | "week" | "month";

export default function AdminReports() {
  const [period, setPeriod] = useState<Period>("week");
  const [income, setIncome] = useState<IncomeReport | null>(null);
  const [footfall, setFootfall] = useState<FootfallPoint[]>([]);
  const [error, setError] = useState<unknown>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    Promise.all([api.getIncomeReport(period), api.getFootfall()])
      .then(([inc, ff]) => { setIncome(inc); setFootfall(ff); })
      .catch(setError).finally(() => setLoading(false));
  }, [period]);

  function exportCsv() {
    if (!income) return;
    const rows = [["Category", "Amount (Rs)"],
      ["Total", income.total], ["Dentist earnings", income.dentistEarning],
      ["Clinic earnings", income.clinicEarning], ["Receptionist earnings", income.receptionistEarning],
      ...income.byDentist.map((d) => [`Dentist: ${d.name}`, d.earning])];
    const csv = rows.map((r) => r.join(",")).join("\n");
    const url = URL.createObjectURL(new Blob([csv], { type: "text/csv" }));
    const a = document.createElement("a"); a.href = url; a.download = `income-${period}.csv`; a.click(); URL.revokeObjectURL(url);
  }

  if (loading) return <Spinner label="Loading reports…" />;
  if (error) return <ErrorBanner error={error} />;
  if (!income) return null;

  const split = [
    { name: "Dentist", value: income.dentistEarning },
    { name: "Clinic", value: income.clinicEarning },
    { name: "Reception", value: income.receptionistEarning },
  ];

  return (
    <div>
      <div className="page-head">
        <h1>Clinic reports</h1>
        <div className="row" style={{ flex: "0 0 auto" }}>
          <select value={period} onChange={(e) => setPeriod(e.target.value as Period)}>
            <option value="day">Today</option><option value="week">This week</option><option value="month">This month</option>
          </select>
          <button className="btn" onClick={exportCsv}>⬇ Export CSV</button>
        </div>
      </div>

      <div className="grid cols-4">
        <StatCard label={`Total income (${period})`} value={rs(income.total)} delta={{ dir: "up", text: "vs last period" }} />
        <StatCard label="Dentist earnings" value={rs(income.dentistEarning)} />
        <StatCard label="Clinic earnings" value={rs(income.clinicEarning)} />
        <StatCard label="Receptionist earnings" value={rs(income.receptionistEarning)} />
      </div>

      <div className="grid cols-2" style={{ marginTop: 16 }}>
        <Card title="Income trend">
          <ResponsiveContainer width="100%" height={240}>
            <LineChart data={income.series}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e3e9f0" />
              <XAxis dataKey="label" /><YAxis /><Tooltip formatter={(v: number) => rs(v)} />
              <Line type="monotone" dataKey="total" stroke="#0e7c86" strokeWidth={2} />
            </LineChart>
          </ResponsiveContainer>
        </Card>
        <Card title="Revenue split (3-way)">
          <ResponsiveContainer width="100%" height={240}>
            <PieChart>
              <Pie data={split} dataKey="value" nameKey="name" outerRadius={90} label>
                {split.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
              </Pie>
              <Tooltip formatter={(v: number) => rs(v)} /><Legend />
            </PieChart>
          </ResponsiveContainer>
        </Card>
      </div>

      <div className="grid cols-2" style={{ marginTop: 16 }}>
        <Card title="Daily footfall by dentist">
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={footfall}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e3e9f0" />
              <XAxis dataKey="label" /><YAxis /><Tooltip />
              <Bar dataKey="total" fill="#2c6ebb" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </Card>
        <Card title="Income by dentist">
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={income.byDentist} layout="vertical">
              <CartesianGrid strokeDasharray="3 3" stroke="#e3e9f0" />
              <XAxis type="number" /><YAxis type="category" dataKey="name" width={90} /><Tooltip formatter={(v: number) => rs(v)} />
              <Bar dataKey="earning" fill="#0e7c86" radius={[0, 6, 6, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </Card>
      </div>

      <div className="grid cols-2" style={{ marginTop: 16 }}>
        <Card title="Earnings by receptionist">
          <div className="table-wrap"><table>
            <thead><tr><th>Receptionist</th><th style={{ textAlign: "right" }}>Service-charge earnings</th></tr></thead>
            <tbody>{income.byReceptionist.map((r) => (
              <tr key={r.name}><td>{r.name}</td><td style={{ textAlign: "right" }}>{rs(r.earning)}</td></tr>
            ))}</tbody>
          </table></div>
        </Card>
        <Card title="How the split works">
          <p className="muted" style={{ fontSize: ".9rem" }}>
            Each bill divides three ways: the <strong>consultation fee</strong> goes to the dentist,
            the <strong>treatment cost</strong> splits 60% dentist / 40% clinic, and the
            <strong> service charge</strong> goes to the receptionist who handled it.
          </p>
        </Card>
      </div>
    </div>
  );
}
