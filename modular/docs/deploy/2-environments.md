# Environments

The same WAR runs everywhere. Nothing is rebuilt per environment, because a rebuild is a chance
for the thing you tested and the thing you deployed to differ.

## What differs, and only this

Every setting in `clinic.properties` is overridable by an environment variable named after it —
upper-cased, dots to underscores. So `db.url` is overridden by `DB_URL`.

| Setting | Development | A clinic |
|---|---|---|
| `DB_URL` | `jdbc:mysql://sunrise-mysql:3306/sunrise_dental?…` | the practice's MySQL |
| `DB_USER` / `DB_PASSWORD` | `root` / `clinic` | a user with rights on one schema, nothing else |
| `DB_POOL_SIZE` | 8 | 8. Raise only if requests are seen waiting for a connection |
| `CLINIC_TIMEZONE` | `Asia/Colombo` | wherever the practice is |
| `CLINIC_BILLING_SERVICE_CHARGE` | 200 | whatever the practice charges |
| `CLINIC_REVENUE_DENTIST_TREATMENT_SHARE` | 0.60 | the practice's agreement with its dentists |
| `CLINIC_REVENUE_RECEPTIONIST_SERVICE_SHARE` | 0 | 0 unless the practice pays handling commission |

**That is the complete list.** There is no `application-dev.yml`, no profile, no build flag. If
something behaves differently between two environments and it is not in this table, that is a
defect rather than configuration.

## The database credential

`clinic.properties` ships with `db.password=` — empty. That is intentional: the file is in
version control, so the only password it can safely carry is none. A deployment that forgets to
set `DB_PASSWORD` fails at start-up with a connection error, which is the correct failure. It
does not start up and quietly connect to something.

The account the application uses needs rights on one schema and nothing else — it never issues
DDL. The schema is loaded once by a person, not by the application at start-up.

## What the application checks about its own environment

Two things announce themselves in the log at start-up, because both were silent defects before:

```
INFO  database_pool_ready url=jdbc:mysql://…/sunrise_dental?… size=8
INFO  clinic_timezone clinic=Asia/Colombo jvm=Etc/UTC
      (dates are computed in the clinic's zone, not the JVM's)
```

The second line is the important one. The container runs UTC and a clinic in Colombo is +05:30,
so for five and a half hours every night UTC is still on yesterday — and a bill would land under
the previous day's takings while the appointment book said otherwise. Every date is computed in
the clinic's zone regardless of where the JVM thinks it is, and the log says so.

## Time and character set

Two environment properties are not application settings but will break it if they are wrong:

| | Requirement | Why |
|---|---|---|
| **Character set** | The database and every text column must be `utf8mb4` | A patient's name or a note with an accent is otherwise stored double-encoded. It round-trips cleanly through a latin1 client, so the corruption only shows when the application reads it |
| **Timezone** | `CLINIC_TIMEZONE` must be the practice's zone | See above |

When loading SQL by hand, always pass `--default-character-set=utf8mb4`. The MySQL client
defaults to latin1 and will silently double-encode the file.

## The schema, and changing it

The schema is three files, applied in order:

| File | Holds |
|---|---|
| `schema.sql` | 14 tables, 24 foreign keys, 1 check constraint |
| `procedures.sql` | 3 routines, 5 triggers |
| `demo-data.sql` | **development only.** Five accounts, three patients, notes, complaints |

`demo-data.sql` must never be loaded into a clinic's database — it creates accounts with a
published password.

There is no migration tool. At this size the schema is applied once and changed by a reviewed SQL
script; a migration framework would be a sixth dependency to manage two years of changes that
have not happened yet. **If the schema outgrows that, add the tool before the second breaking
change, not after.**
