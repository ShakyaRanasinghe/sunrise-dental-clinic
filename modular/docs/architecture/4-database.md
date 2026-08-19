# The Database

Where MySQL sits, how it is reached, and the discipline that keeps the data tier honest.

Schema detail, cardinality and the confidentiality rules per table are in
[`../er-diagram.md`](../er-diagram.md).

---

## MySQL

The database is a separate process reached by URL. Nothing is stored in the WAR, nothing on
disk.

| | Local | Deployed |
|---|---|---|
| Host | Docker `sunrise-mysql`, port 3308 | Managed MySQL 8 |
| Database | `sunrise_dental` | `sunrise_dental` |
| Credentials | `DB_URL` · `DB_USER` · `DB_PASSWORD` | same three names, different values |
| Pool | 8 connections, hand-written | sized per environment |

Every key in `clinic.properties` is overridable by an environment variable named after it —
`db.url` by `DB_URL`. One WAR ships to every environment with no rebuild and no credential in
version control.

### Three SQL files

```bash
mysql -u root -p < schema.sql        # 14 tables, keys, constraints
mysql -u root -p sunrise_dental < procedures.sql
mysql -u root -p sunrise_dental < demo-data.sql
```

| File | Contents |
|---|---|
| `schema.sql` | The 14 tables in [`er-diagram.md`](../er-diagram.md), plus the **7 missing foreign keys** on staff references |
| `procedures.sql` | `sp_register_appointment` · `fn_calculate_bill` · `trg_prevent_double_booking` · `trg_audit_appointment` · `trg_check_receptionist_role` |
| `demo-data.sql` | One account per role, two dentists, six treatments, three patients |

`procedures.sql` is separate because it is what the assessment's "advanced database features"
criterion asks for, and because it is the file most likely to change while the tables stay put.

### Data discipline

| Rule | Enforced by |
|---|---|
| Every statement parameterised — no concatenated SQL | `JdbcDao` exposes only prepared-statement helpers |
| Connections always returned, including on failure | try-with-resources over `PooledConnection` |
| Booking is one transaction | `TransactionRunner.inTransaction(…)` wraps the slot lock, the insert and the counter |
| No double booking | `SELECT … FOR UPDATE` on the slot row + `UNIQUE (slot.appointment_no)` |

---
