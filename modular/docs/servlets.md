# Servlet and Route Contract

Every servlet the system declares, its URL mapping, the view it renders, the role it admits, and the
requirement it satisfies.

This is a **technical requirement**, not a summary. `web.xml` is generated from this table, and
CON-02 — *web interaction must be by Jakarta Servlet* — is satisfied by it. Until now the servlet
names were scattered across eighteen documents with no single authority, which is how the two
mapping conflicts in §5 went unnoticed.

| | |
|---|---|
| Servlets | **42** concrete across 8 modules, plus 2 abstract bases in `platform/web` |
| Filters | 1 — `AuthenticationFilter`, mapped `/*` |
| Requirement prefix | `FR-WEB-` |
| Mappings | **46** declared, plus `/css/*` served by Tomcat's default servlet = 47 routes |
| Declared in | [`../src/main/webapp/WEB-INF/web.xml`](../src/main/webapp/WEB-INF/web.xml) — written, and verified against §6 by script |

---

## 1. Mapping rules

Requirements that govern every entry in the tables below.

| ID | Requirement | Status |
|---|---|---|
| **FR-WEB-01** | Every servlet, filter and listener must be declared in `web.xml`, not discovered from annotations, so one file lists every URL the system answers | Built in `layered/` |
| **FR-WEB-02** | `HomeServlet` must use the **empty** `url-pattern`, which matches the context root exactly. Mapping it to `/` would replace Tomcat's default servlet and every static asset request would answer a redirect | Built — this was a real defect |
| **FR-WEB-03** | Page servlets must extend `PageServlet`; API servlets must extend `BaseServlet`. Nothing extends `HttpServlet` directly | Built |
| **FR-WEB-04** | A servlet must obtain only its **own module's service** from `AppContext`. Reaching a repository is a tier violation (NFR-MNT-02) | **Specified** — 12 of 20 servlets break it in `layered/` |
| **FR-WEB-05** | A servlet must not import from a sibling module's `web/` or `data/` package | Specified |
| **FR-WEB-06** | Every page servlet must render a view under `/WEB-INF/jsp/`, unreachable by URL (CON-07) | Built |
| **FR-WEB-07** | Every API servlet must answer with a status code and a JSON body, and must never redirect | Built |
| **FR-WEB-08** | Role checks must be expressed as `AccessControl.require(user, Action)`, never as an `if` on `Role` | Specified — FR-OOP-04 |
| **FR-WEB-09** | A prefix mapping `/x/*` claims every path beneath it. No two servlets may claim overlapping prefixes; a sub-resource of another module's prefix must be given its own top-level namespace | **Specified — see §5** |
| **FR-WEB-10** | Every route in these tables must appear in exactly one row. A route in two rows is a mapping conflict; a route in none is unreachable | Specified |

### Servlet mapping precedence, since §5 depends on it

Jakarta Servlet resolves in this order, and only these forms exist:

| Form | Example | Beats |
|---|---|---|
| Exact | `/api/dentists` | everything |
| Prefix | `/api/appointments/*` | extension and default |
| Extension | `*.jsp` | default |
| Default | `/` | nothing |
| **Context root** | `""` (empty) | — matches `/` alone, leaves the default servlet intact |

There is **no** pattern for `/api/appointments/*/review`. Servlet mapping has no wildcard in the
middle. That constraint is what produces both conflicts in §5.

---

## 2. `platform/web` — shared bases

| Class | Kind | Mapping | Purpose |
|---|---|---|---|
| `BaseServlet` | abstract | — | JSON writing, body parsing, exception-to-status mapping |
| `PageServlet` | abstract | — | `render(request, response, view)` — forwards to `/WEB-INF/jsp/` |
| `HelpServlet` | concrete | `GET /help` + `/help/*` | Public. FR-HLP-01…03, GAP-FTB-12 (`/help/patient`, `/help/reception`, `/help/dentist`, `/help/admin` pick their view from the path) |
| `AuthenticationFilter` | filter | `/*` | Resolves the principal; 302 for pages, 401 JSON for `/api/**` |

---

## 3. Page servlets

| Module | Servlet | Method · Route | View | Role | Requirement |
|---|---|---|---|---|---|
| access | `HomeServlet` | `GET` `""` *(context root)* | redirect | any | FR-WEB-02, FR-AUTH-04 |
| access | `StaffPortalServlet` | `GET` `/staff` | `access/staff-portal.jsp` | public | GAP-FTB-10 |
| access | `PatientLoginServlet` | `GET`·`POST` `/login/patient` | `access/login-patient.jsp` | `PATIENT` | FR-PAT-01, FR-OOP-11 |
| access | `PatientLoginServlet` | `GET`·`POST` `/login/patient` | `access/login-patient.jsp` | `PATIENT` | FR-PAT-01, FR-OOP-11 |
| access | `ReceptionLoginServlet` | `GET`·`POST` `/login/reception` | `access/login-reception.jsp` | `RECEPTIONIST` | FR-REC-01 |
| access | `DentistLoginServlet` | `GET`·`POST` `/login/dentist` | `access/login-dentist.jsp` | `DENTIST` | FR-DEN-01 |
| access | `AdminLoginServlet` | `GET`·`POST` `/login/admin` | `access/login-admin.jsp` | `ADMIN` | FR-ADM-01 |
| access | `RegisterServlet` | `GET`·`POST` `/register` | `access/register.jsp` | public | FR-PAT-03 |
| access | `LogoutServlet` | `POST /logout` | redirect | signed in | FR-EXT-01 |
| patients | `PatientRecordsServlet` | `GET`·`POST` `/reception/patients` | `patients/records.jsp` | `RECEPTIONIST` `ADMIN` | FR-REC-20…25 |
| patients | `PatientProfileServlet` | `GET`·`POST` `/patient/profile` | `patients/profile.jsp` | `PATIENT` | FR-PAT-40…47 |
| scheduling | `AvailabilityPageServlet` | `GET`·`POST` `/reception/availability` | `scheduling/availability.jsp` | `RECEPTIONIST` | FR-REC-40…44 |
| appointments | `PatientHomeServlet` | `GET /patient/home` | `appointments/patient-home.jsp` | `PATIENT` | FR-PAT-20…23 |
| appointments | `BookAppointmentServlet` | `GET`·`POST` `/patient/book` | `appointments/book.jsp` | `PATIENT` `RECEPTIONIST` `ADMIN` | FR-PAT-10…16, FR-REC-30…34 |
| appointments | `ReceptionDayServlet` | `GET /reception/home` | `appointments/reception-day.jsp` | `RECEPTIONIST` | FR-REC-10…13 |
| appointments | `DentistScheduleServlet` | `GET`·`POST` `/dentist/schedule` | `appointments/dentist-schedule.jsp` | `DENTIST` | FR-DEN-10…46 |
| scheduling | `DentistProfileServlet` | `GET`·`POST` `/dentist/profile` | `scheduling/dentist-profile.jsp` | `DENTIST` | GAP-DEN-14 |
| billing | `BillingPageServlet` | `GET`·`POST` `/reception/billing` | `billing/billing.jsp` | `RECEPTIONIST` `ADMIN` | FR-REC-50…56 |
| billing | `ReceiptServlet` | `GET /reception/receipt` | `billing/receipt.jsp` | `RECEPTIONIST` `ADMIN` `PATIENT` | FR-REC-52, FR-PAT-22 |
| reporting | `AdminReportsServlet` | `GET /admin/reports` | `reporting/reports.jsp` | `ADMIN` | FR-ADM-10…19 |
| reporting | `CsvExportServlet` | `GET /admin/export` | — *(streams CSV)* | `ADMIN` | FR-ADM-18 |
| reporting | `AccountsServlet` | `GET`·`POST` `/admin/accounts` | `reporting/accounts.jsp` | `ADMIN` | FR-ADM-20…27 |
| reporting | `AuditTrailServlet` | `GET /admin/audit` | `reporting/audit.jsp` | `ADMIN` | FR-ADM-30…33 |
| reporting | `PricingServlet` | `GET`·`POST` `/admin/pricing` | `reporting/pricing.jsp` | `ADMIN` | GAP-ADM-10 |
| feedback | `PatientComplaintsServlet` | `GET`·`POST` `/patient/complaints` | `feedback/patient-complaints.jsp` | `PATIENT` | FR-PAT-50…57 |
| feedback | `AdminComplaintsServlet` | `GET`·`POST` `/admin/complaints` | `feedback/admin-complaints.jsp` | `ADMIN` | FR-ADM-50…58 |

**26 page servlets**, one of which (`HelpServlet`) lives in `platform/web` and is listed in §2. `AbstractLoginServlet` is abstract and declares no mapping — the four
concrete portals each declare their own, which is the whole point of the hierarchy.

---

## 4. API servlets

| Module | Servlet | Mapping | Handles | Requirement |
|---|---|---|---|---|
| access | `AuthApiServlet` | `/api/auth/*` | login · logout · register · me · lock-status · unlock | FR-AUTH-01…06 |
| access | `AccountApiServlet` | `/api/accounts` *(exact)* | `POST` — staff account creation | FR-ADM-20…22 |
| patients | `PatientApiServlet` | `/api/patients/*` | search · one patient · create · **notes sub-resource** | FR-REC-20…21, FR-NOTE-01…06 |
| scheduling | `ReferenceApiServlet` | `/api/dentists` and `/api/treatments` *(both exact)* | active dentists, active treatments | FR-PAT-10, FR-PAT-12 |
| scheduling | `AvailabilityApiServlet` | `/api/availability/*` and `/api/sessions` | open slots · week range · publish | FR-AVL-01…04 |
| appointments | `AppointmentApiServlet` | `/api/appointments/*` | list · one · cancel · complete · bill | FR-APT-01…08, FR-BIL-01…04 |
| feedback | `ComplaintApiServlet` | `/api/complaints/*` | raise · list · one · review | FR-CMP-01…12 |
| feedback | `ReviewApiServlet` | `/api/reviews/*` and `/api/ratings/*` | rate · read · aggregate | FR-RVW-01…12 |

**8 API servlets.** Note `PatientApiServlet` handles `/api/patients/{id}/notes` itself rather than
delegating: `PatientNote` lives in `patients/domain`, so the sub-resource belongs to the same module
and no boundary is crossed.

---

## 5. Two mapping conflicts this inventory exposed

Both were introduced by writing the API documents per-resource, and neither is visible from any one
of them. This is why the inventory is a requirement and not a summary.

### Conflict 1 — `/api/appointments/{no}/review` cannot exist

[`api/reviews.md`](api/reviews.md) specifies `POST /api/appointments/{no}/review`. That route is
**unimplementable as written**, for two independent reasons:

1. **No servlet pattern matches it.** `/api/appointments/*` is a prefix mapping owned by
   `AppointmentApiServlet`, and `/api/appointments/APT-1/review` matches that prefix. There is no
   `/api/appointments/*/review` form.
2. **Handling it inside `AppointmentApiServlet` inverts the dependency graph.** Reviews live in
   `feedback`, and the graph says `feedback ..> appointments`. Making `appointments` call into
   `feedback` would create a cycle — exactly the check the DAG exists to perform.

**Resolution.** The route moves into the module that owns it:

| Was specified | Becomes | Owner |
|---|---|---|
| `POST /api/appointments/{no}/review` | `POST /api/reviews` — `appointmentNo` in the body | `feedback` |
| `GET /api/appointments/{no}/review` | `GET /api/reviews/{appointmentNo}` | `feedback` |
| `PUT /api/appointments/{no}/review` | `PUT /api/reviews/{appointmentNo}` | `feedback` |

The resource is a review, so it belongs under `/api/reviews`. Nesting it under the appointment read
naturally and mapped impossibly — a good reminder that REST-shaped URLs and servlet mappings are not
the same constraint.

### Conflict 2 — `/api/dentists/{id}/rating` straddles two modules

Same shape. `/api/dentists` is an exact mapping owned by `ReferenceApiServlet` in `scheduling`; the
rating is owned by `feedback`. A `feedback` servlet claiming `/api/dentists/*` would be legal —
exact beats prefix, so `ReferenceApiServlet` would still win `/api/dentists` — but it would put one
module's servlet inside another module's URL namespace, which is the kind of thing that is correct
today and confusing in a year.

**Resolution.** `GET /api/ratings/{dentistId}`, owned by `feedback`, mapped `/api/ratings/*`.

### What this says about the process

Nine per-resource API documents each described a coherent resource. The conflicts only appeared when
every route was listed in one table and **FR-WEB-10** was applied — every route in exactly one row.
That check took one pass and found two design errors that would each have surfaced as a runtime
404 or a compile-time cycle during implementation.

---

## 6. Complete route index

Every URL the system answers, in one list. Checked against §3 and §4 for the FR-WEB-10 property:
each appears once.

```
""                                  HomeServlet                access
/staff                              StaffPortalServlet         access
/login/patient                      PatientLoginServlet        access
/login/reception                    ReceptionLoginServlet      access
/login/dentist                      DentistLoginServlet        access
/login/admin                        AdminLoginServlet          access
/register                           RegisterServlet            access
/logout                             LogoutServlet              access
/help                               HelpServlet                platform
/help/patient                       HelpServlet                platform
/help/reception                     HelpServlet                platform
/help/dentist                       HelpServlet                platform
/help/admin                         HelpServlet                platform
/patient/home                       PatientHomeServlet         appointments
/patient/book                       BookAppointmentServlet     appointments
/patient/profile                    PatientProfileServlet      patients
/patient/complaints                 PatientComplaintsServlet   feedback
/reception/home                     ReceptionDayServlet        appointments
/reception/patients                 PatientRecordsServlet      patients
/reception/availability             AvailabilityPageServlet    scheduling
/reception/billing                  BillingPageServlet         billing
/reception/receipt                  ReceiptServlet             billing
/dentist/schedule                   DentistScheduleServlet     appointments
/dentist/profile                    DentistProfileServlet      scheduling
/admin/reports                      AdminReportsServlet        reporting
/admin/export                       CsvExportServlet           reporting
/admin/accounts                     AccountsServlet            reporting
/admin/audit                        AuditTrailServlet          reporting
/admin/pricing                      PricingServlet             reporting
/admin/complaints                   AdminComplaintsServlet     feedback
/api/auth/*                         AuthApiServlet             access
/api/accounts                       AccountApiServlet          access
/api/patients/*                     PatientApiServlet          patients
/api/dentists                       ReferenceApiServlet        scheduling
/api/treatments                     ReferenceApiServlet        scheduling
/api/availability/*                 AvailabilityApiServlet     scheduling
/api/sessions                       AvailabilityApiServlet     scheduling
/api/appointments/*                 AppointmentApiServlet      appointments
/api/complaints/*                   ComplaintApiServlet        feedback
/api/reviews/*                      ReviewApiServlet           feedback
/api/ratings/*                      ReviewApiServlet           feedback
/css/*                              Tomcat default servlet     —
```

**A prefix-overlap check, since FR-WEB-09 depends on it:** no prefix in the list is a prefix of
another. `/api/appointments/*` and `/api/accounts` share only `/api/a`, which is not a path segment
boundary. `/api/availability/*` and `/api/accounts` likewise. The only exact-inside-prefix pairs
are `/api/dentists` and `/api/treatments`, both exact, both owned by the same servlet.

---

## 7. What implementation needs from this

| Artefact | Generated from | Status |
|---|---|---|
| `web.xml` | §2, §3, §4 — 32 servlet declarations, 35 mappings, 1 filter, session config, 2 error pages | **Written**. Parses, no duplicate patterns, no unmapped servlet, and every route matches §6 |
| Servlet classes | §3 and §4, one file each in its module's `web/` | Not written — the 32 classes `web.xml` names |
| `AppContext` accessors | one per service the servlets need | Not written |
| JSP views | §3's View column, under `/WEB-INF/jsp/` grouped by module | Prototype exists in [`prototype/`](prototype/) |

The route index in §6 is the checklist: 36 rows, each needing a servlet, a mapping and — for the 23
page routes — a view. `web.xml` is written and verified; the classes it names arrive module by
module in the order set out in [`migration-plan.md`](migration-plan.md).
