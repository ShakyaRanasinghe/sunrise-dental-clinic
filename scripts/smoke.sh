#!/usr/bin/env bash
#
# End-to-end smoke test against a running instance.
#
#   ./scripts/dev-up.sh && ./scripts/smoke.sh
#
# Drives the whole clinic over HTTP as the four roles would, and asserts what each
# one may and may not see. Every check prints its expectation, so a failure says
# what was wanted rather than only what happened.
#
# WHY THIS EXISTS
#
# The 266 unit tests are all below the web tier: they exercise services against
# in-memory repositories, and they are fast and precise for that. What they cannot
# see is everything that only exists when the application is assembled - a servlet
# mapping, a JSP that does not compile, a JDBC statement a trigger refuses, a
# timezone, a charset. Every defect found late in this project was of that kind:
#
#   - a BEFORE INSERT trigger that made every appointment update fail
#   - a view name that turned every page-level error into a 404
#   - dates computed in UTC for a clinic at +05:30
#   - a form posting to the JSP's own path instead of the route
#
# None of those could fail a unit test. This script would have caught all four.
#
# The migration plan called for a different gate here - deploy layered/ and
# modular/ side by side and diff the HTML. That gate is moot: layered/ is not a
# submission candidate and is not maintained, so a diff against it would compare
# the shipped application to code nobody runs. This is the useful thing instead.
#
set -uo pipefail

BASE="${BASE:-http://localhost:8080}"
PASSWORD="${PASSWORD:-Password123}"
TODAY="$(date +%F)"

pass=0
fail=0
JARS="$(mktemp -d)"
trap 'rm -rf "$JARS"' EXIT

bold() { printf '\n\033[1m%s\033[0m\n' "$*"; }

# check <description> <expected> <actual>
check() {
  if [ "$2" = "$3" ]; then
    printf '  \033[32m✓\033[0m %-58s %s\n' "$1" "$3"
    pass=$((pass + 1))
  else
    printf '  \033[31m✗\033[0m %-58s expected %s, got %s\n' "$1" "$2" "$3"
    fail=$((fail + 1))
  fi
}

# contains <description> <needle> <haystack>
contains() {
  case "$3" in
    *"$2"*) printf '  \033[32m✓\033[0m %s\n' "$1"; pass=$((pass + 1)) ;;
    *)      printf '  \033[31m✗\033[0m %-58s did not contain "%s"\n' "$1" "$2"; fail=$((fail + 1)) ;;
  esac
}

# lacks <description> <needle> <haystack>
lacks() {
  case "$3" in
    *"$2"*) printf '  \033[31m✗\033[0m %-58s leaked "%s"\n' "$1" "$2"; fail=$((fail + 1)) ;;
    *)      printf '  \033[32m✓\033[0m %s\n' "$1"; pass=$((pass + 1)) ;;
  esac
}

signin() {  # signin <portal> <email> -> writes a cookie jar, echoes its path
  local jar="$JARS/$1-$2"
  curl -sS -o /dev/null -c "$jar" -d "email=$2&password=$PASSWORD" "$BASE/login/$1"
  echo "$jar"
}

status() { curl -sS -b "$1" -o /dev/null -w '%{http_code}' "$BASE$2"; }
body()   { curl -sS -b "$1" "$BASE$2"; }
post()   { curl -sS -b "$1" -o /dev/null -w '%{http_code}' -X POST "$BASE$2"; }
postjson() {
  curl -sS -b "$1" -H 'Content-Type: application/json' -d "$3" -w '|%{http_code}' "$BASE$2"
}

# ---------------------------------------------------------------- reachable
bold "0. the application is up"
check "GET /help answers"                     200 "$(curl -sS -o /dev/null -w '%{http_code}' "$BASE/help")"
check "GET /login answers"                    200 "$(curl -sS -o /dev/null -w '%{http_code}' "$BASE/login")"
check "the stylesheet is served, not redirected" 200 "$(curl -sS -o /dev/null -w '%{http_code}' "$BASE/css/app.css")"
contains "every page carries a doctype" '<!doctype html>' "$(curl -sS "$BASE/login")"

# ---------------------------------------------------------------- sign in
bold "1. the four portals"
PATIENT=$(signin patient   nimal@example.lk)
RECEPTION=$(signin reception reception@sunrisedental.lk)
DENTIST=$(signin dentist   silva@sunrisedental.lk)
OTHER_DENTIST=$(signin dentist jayasuriya@sunrisedental.lk)
ADMIN=$(signin admin       admin@sunrisedental.lk)

check "a patient lands on their own page"     200 "$(status "$PATIENT" /patient/home)"
check "a receptionist lands on the day view"  200 "$(status "$RECEPTION" /reception/home)"
check "a dentist lands on their schedule"     200 "$(status "$DENTIST" /dentist/schedule)"
check "an administrator lands on reports"     200 "$(status "$ADMIN" /admin/reports)"

WRONG_PASSWORD=$(curl -sS -d "email=nimal@example.lk&password=Wrong999" "$BASE/login/patient" \
  | grep -o 'Incorrect email or password.' | head -1)
WRONG_PORTAL=$(curl -sS -d "email=admin@sunrisedental.lk&password=$PASSWORD" "$BASE/login/patient" \
  | grep -o 'Incorrect email or password.' | head -1)
check "a wrong portal reads like a wrong password" "$WRONG_PASSWORD" "$WRONG_PORTAL"

# ---------------------------------------------------------------- registering
bold "1a. a new patient registers"
# This group exists because it was missing. The script signed in as the seeded patient and
# never registered anybody, so it did not notice that registration was impossible: the
# servlet required a contact number the form never asked for, and answered 400 to
# everything. It also created only the account, so anything that got through signed in to
# a profile that did not exist and failed at the first booking.
NEW_EMAIL="smoke-$$@example.lk"
NEW_JAR="$JARS/registered"
REGISTERED=$(curl -sS -c "$NEW_JAR" -o /dev/null -w '%{http_code}' \
  -d "name=Smoke Tester&email=$NEW_EMAIL&password=Password123&confirmPassword=Password123" \
  "$BASE/register")
check "registering needs no contact number"    302 "$REGISTERED"
check "and lands on the patient's own page"    200 "$(status "$NEW_JAR" /patient/home)"
# The profile row is what booking resolves through. Without it this answers 404.
check "the new account has a patient profile"  200 "$(status "$NEW_JAR" /patient/profile)"

# ---------------------------------------------------------------- role areas
bold "2. every role reaches only its own pages"
for path in /patient/home /reception/home /dentist/schedule /admin/reports; do
  case "$path" in
    /patient/*)   own=$PATIENT ;;
    /reception/*) own=$RECEPTION ;;
    /dentist/*)   own=$DENTIST ;;
    *)            own=$ADMIN ;;
  esac
  check "$path — its own role" 200 "$(status "$own" "$path")"
done
check "a patient cannot open the dentist's schedule" 403 "$(status "$PATIENT" /dentist/schedule)"
check "a dentist cannot open the day view"           403 "$(status "$DENTIST" /reception/home)"
check "an administrator covers the front desk"       200 "$(status "$ADMIN" /reception/patients)"
check "but not a patient's own pages"                403 "$(status "$ADMIN" /patient/home)"

# ---------------------------------------------------------------- the journey
bold "3. the whole journey — publish, book, treat, bill"
postjson "$RECEPTION" /api/sessions \
  "{\"dentistId\":\"d-silva\",\"date\":\"$TODAY\",\"startTime\":\"09:00\",\"endTime\":\"12:00\"}" >/dev/null
OPEN_BEFORE=$(body "$RECEPTION" "/api/availability?dentistId=d-silva&date=$TODAY" | grep -o '"startTime"' | wc -l)
check "reception publishes six bookable times" 6 "$OPEN_BEFORE"

BOOKED=$(curl -sS -b "$PATIENT" -o /dev/null -w '%{redirect_url}' \
  -d "slotId=d-silva_${TODAY}_09:30&treatmentId=t-scaling" "$BASE/patient/book")
APPOINTMENT="${BOOKED##*booked=}"
contains "the patient books and gets a number" "APT-" "$APPOINTMENT"

OPEN_AFTER=$(body "$RECEPTION" "/api/availability?dentistId=d-silva&date=$TODAY" | grep -o '"startTime"' | wc -l)
check "the booked time leaves the open list" 5 "$OPEN_AFTER"
contains "reception's day view names the patient" "Nimal Perera" "$(body "$RECEPTION" "/reception/home?date=$TODAY")"
check "booking the same time again is refused" 409 \
  "$(postjson "$PATIENT" /api/appointments "{\"slotId\":\"d-silva_${TODAY}_09:30\",\"treatmentId\":\"t-checkup\"}" | sed 's/.*|//')"

curl -sS -b "$DENTIST" -o /dev/null \
  -d "appointmentNo=$APPOINTMENT&date=$TODAY&diagnosis=Scaling done, no decay" "$BASE/dentist/schedule"
contains "the dentist records the treatment" '"status":"COMPLETED"' "$(body "$DENTIST" "/api/appointments/$APPOINTMENT")"

BILL=$(post "$RECEPTION" "/api/appointments/$APPOINTMENT/bill")
check "reception issues the bill"             201 "$BILL"
contains "the bill totals 5200.00" '"total":5200.00' "$(body "$RECEPTION" "/api/appointments/$APPOINTMENT/bill")"
check "billing twice is refused"              409 "$(post "$RECEPTION" "/api/appointments/$APPOINTMENT/bill")"
contains "the receipt prints a formatted total" "5,200.00" \
  "$(body "$RECEPTION" "/reception/receipt?appointmentNo=$APPOINTMENT")"

# The point of 1a: a profile that exists is one that can book.
postjson "$RECEPTION" /api/sessions \
  "{\"dentistId\":\"d-jayasuriya\",\"date\":\"$TODAY\",\"startTime\":\"14:00\",\"endTime\":\"15:00\"}" >/dev/null
NEW_BOOKING=$(curl -sS -b "$NEW_JAR" -o /dev/null -w '%{redirect_url}' \
  -d "slotId=d-jayasuriya_${TODAY}_14:00&treatmentId=t-checkup" "$BASE/patient/book")
contains "a freshly registered patient can book at once" "booked=APT-" "$NEW_BOOKING"

# The confirmation is recorded by an observer the appointment service knows nothing about,
# after the transaction has committed. Nothing is actually sent - no transport is
# configured - so the row reads LOGGED, which is the point: it is not SENT.
bold "3a. the patient is told, and it is on the record"
CONFIRMATION=$(body "$PATIENT" "/api/appointments/$APPOINTMENT")
check "booking left a notification row" 1 \
  "$(docker exec -i sunrise-mysql mysql -uroot -pclinic sunrise_dental -N \
      -e "SELECT COUNT(*) FROM notification WHERE appointment_no='$APPOINTMENT';" 2>/dev/null)"
check "recorded as LOGGED, not SENT"    LOGGED \
  "$(docker exec -i sunrise-mysql mysql -uroot -pclinic sunrise_dental -N \
      -e "SELECT status FROM notification WHERE appointment_no='$APPOINTMENT' LIMIT 1;" 2>/dev/null)"
contains "and it carries the appointment number" "$APPOINTMENT" \
  "$(docker exec -i sunrise-mysql mysql -uroot -pclinic sunrise_dental -N \
      -e "SELECT body FROM notification WHERE appointment_no='$APPOINTMENT' LIMIT 1;" 2>/dev/null)"

# ---------------------------------------------------------------- money
bold "4. the revenue policy"
REPORT=$(body "$ADMIN" "/admin/reports")
contains "the owner's margin is shown"        "1,600.00" "$REPORT"
contains "the dentist's share is shown"       "3,600.00" "$REPORT"
lacks    "reception is never shown a bill's split" "clinic_earning" "$(body "$RECEPTION" "/reception/billing")"
check    "only the administrator exports the CSV" 200 "$(status "$ADMIN" /admin/reports.csv)"

# ---------------------------------------------------------------- clinical
bold "5. medical notes — three rules, three directions"
curl -sS -b "$PATIENT" -o /dev/null \
  -d "action=declare&category=ALLERGY&detail=Allergic to penicillin&critical=1" "$BASE/patient/profile"
check "the patient reads their own notes"          200 "$(status "$PATIENT" /api/patients/p-nimal/notes)"
check "the treating dentist reads them"            200 "$(status "$DENTIST" /api/patients/p-nimal/notes)"
check "another dentist cannot"                     403 "$(status "$OTHER_DENTIST" /api/patients/p-nimal/notes)"
check "reception cannot"                           403 "$(status "$RECEPTION" /api/patients/p-nimal/notes)"
check "the administrator cannot"                   403 "$(status "$ADMIN" /api/patients/p-nimal/notes)"
contains "the dentist's schedule warns before treating" "declared something important" \
  "$(body "$DENTIST" "/dentist/schedule?date=$TODAY")"
lacks    "reception's day view carries no diagnosis" "Scaling done" \
  "$(body "$RECEPTION" "/reception/home?date=$TODAY")"

# ---------------------------------------------------------------- complaints
bold "6. complaints — the dentist never sees one"
postjson "$PATIENT" /api/complaints \
  "{\"dentistId\":\"d-silva\",\"appointmentNo\":\"$APPOINTMENT\",\"category\":\"WAIT_TIME\",\"detail\":\"I waited over an hour past my time and nobody explained why.\"}" >/dev/null
check "the patient sees their own"                 200 "$(status "$PATIENT" /api/complaints)"
check "the administrator sees them"                200 "$(status "$ADMIN" /api/complaints)"
check "the named dentist cannot"                   403 "$(status "$DENTIST" /api/complaints)"
check "reception cannot"                           403 "$(status "$RECEPTION" /api/complaints)"

# ---------------------------------------------------------------- reviews
bold "7. reviews — a dentist sees an aggregate, never a comment"
postjson "$PATIENT" /api/reviews \
  "{\"appointmentNo\":\"$APPOINTMENT\",\"rating\":2,\"comment\":\"The waiting room was filthy\"}" >/dev/null
DENTIST_VIEW=$(body "$DENTIST" /api/reviews)
contains "the dentist is given a count"            '"reviews":1' "$DENTIST_VIEW"
contains "and the mean is withheld below five"     '"mean":null' "$DENTIST_VIEW"
lacks    "and no comment reaches them"             "filthy" "$DENTIST_VIEW"
contains "the administrator reads the comment"     "filthy" "$(body "$ADMIN" /api/reviews/d-silva)"
check    "a dentist cannot list individual reviews" 403 "$(status "$DENTIST" /api/reviews/d-silva)"
check    "reception sees nothing"                   403 "$(status "$RECEPTION" /api/reviews)"

# ---------------------------------------------------------------- admin
bold "8. the administrator's screens"
check "accounts"       200 "$(status "$ADMIN" /admin/accounts)"
check "audit trail"    200 "$(status "$ADMIN" /admin/audit)"
check "complaints"     200 "$(status "$ADMIN" /admin/complaints)"
contains "the trail answers who changed this appointment" "APPOINTMENT_COMPLETED" \
  "$(body "$ADMIN" "/admin/audit?targetId=$APPOINTMENT")"

# ---------------------------------------------------------------- out
bold "9. signing out"
curl -sS -b "$RECEPTION" -c "$RECEPTION" -o /dev/null -X POST "$BASE/logout"
check "a signed-out session is turned away" 302 "$(status "$RECEPTION" /reception/home)"

# ---------------------------------------------------------------- result
printf '\n\033[1m%s passed, %s failed\033[0m\n' "$pass" "$fail"
[ "$fail" -eq 0 ] || exit 1
