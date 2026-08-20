# Pointing at an external database

Aiven, PlanetScale, RDS, a MySQL on another machine in the practice — the answer is the same
for all of them.

## The short answer

**Nothing is rebuilt and no code changes. Three environment variables and a URL.**

```bash
export DB_URL='jdbc:mysql://mysql-abc123-sunrise.a.aivencloud.com:24601/defaultdb?sslMode=REQUIRED&serverTimezone=UTC'
export DB_USER='avnadmin'
export DB_PASSWORD='<from the provider>'
```

Restart Tomcat and it is talking to the new database. The host, the **port**, the database name,
the credentials and the SSL settings all live in that one string, and the application reads it at
start-up.

Verified rather than assumed: the same WAR was redeployed pointing at a different host address
and a different pool size, with nothing else changed, and it came up on the new one —

```
INFO database_pool_ready url=jdbc:mysql://172.21.0.2:3306/sunrise_dental?… size=4
```

## Where the credentials live right now

Three places, and they are checked in this order. **The first one that answers wins.**

| Order | Where | Holds today | Use it for |
|---|---|---|---|
| 1 | **Environment variables** — `DB_URL`, `DB_USER`, `DB_PASSWORD`, `DB_POOL_SIZE` | nothing (unset) | every real deployment |
| 2 | `modular/src/main/resources/clinic.properties` | `localhost:3306`, `root`, **empty password** | the committed default only |
| 3 | The hard-coded fallback in `Database` | the same `localhost:3306` | never reached in practice |

The mechanism is one method, in `platform/config/AppConfig`:

```java
public String get(String key, String defaultValue) {
    String fromEnv = System.getenv(envName(key));      // DB_URL
    if (fromEnv != null && !fromEnv.isBlank()) {
        return fromEnv.trim();
    }
    return properties.getProperty(key, defaultValue);  // db.url
}
```

**Every key works this way**, not just the database ones: upper-case it and turn dots into
underscores. `db.pool.size` becomes `DB_POOL_SIZE`, `clinic.timezone` becomes `CLINIC_TIMEZONE`.
There is no separate list of "overridable" settings to keep in step.

### Why `clinic.properties` ships with an empty password

It is in version control, so the only password it can safely carry is none. A deployment that
forgets to set `DB_PASSWORD` fails at start-up with a connection error — which is the correct
failure. It does not start and quietly connect to something else.

**Never put a real credential in `clinic.properties`.** If you do, it is in the git history from
then on, and rotating the password does not remove it.

## A worked example: Aiven for MySQL

Aiven is a useful worked example because it differs from a local MySQL in every way that matters:
a non-standard port, a database that is not called what you expect, a user that is not `root`,
and TLS that is not optional.

### 1. Take the details from the console

Aiven gives you a host, a port, a user, a password and a database name. They look like:

| | Local | Aiven |
|---|---|---|
| Host | `localhost` | `mysql-abc123-sunrise.a.aivencloud.com` |
| **Port** | 3306 | **24601** — allocated per service, never 3306 |
| User | `root` | `avnadmin` |
| Database | `sunrise_dental` | `defaultdb` |
| TLS | off | **required** |

### 2. Load the schema once, by hand

The application never issues DDL, so the schema goes in first. `--default-character-set=utf8mb4`
is not optional — the client defaults to latin1 and will double-encode every non-ASCII character
in the file.

```bash
MYSQL="mysql --default-character-set=utf8mb4 \
  --host=mysql-abc123-sunrise.a.aivencloud.com --port=24601 \
  --user=avnadmin --password=<password> --ssl-mode=REQUIRED"

$MYSQL defaultdb < modular/src/main/resources/schema.sql
$MYSQL defaultdb < modular/src/main/resources/procedures.sql
# Do NOT load demo-data.sql — it creates accounts with a published password.
```

`schema.sql` opens with `CREATE DATABASE IF NOT EXISTS sunrise_dental`. On a hosted service where
you cannot create databases, either drop those two lines and load into the database the provider
gave you, or — on Aiven, where `avnadmin` can — let it create `sunrise_dental` alongside
`defaultdb` and name that in the URL instead.

### 3. Point the application at it

```bash
export DB_URL='jdbc:mysql://mysql-abc123-sunrise.a.aivencloud.com:24601/defaultdb?sslMode=REQUIRED&serverTimezone=UTC'
export DB_USER='avnadmin'
export DB_PASSWORD='<password>'
export DB_POOL_SIZE=8
export CLINIC_TIMEZONE='Asia/Colombo'

"$CATALINA_HOME/bin/shutdown.sh" && "$CATALINA_HOME/bin/startup.sh"
```

Or as a container, which is the same thing:

```bash
docker run -d --name sunrise-tomcat -p 8080:8080 \
  -e DB_URL='jdbc:mysql://mysql-abc123-sunrise.a.aivencloud.com:24601/defaultdb?sslMode=REQUIRED&serverTimezone=UTC' \
  -e DB_USER=avnadmin \
  -e DB_PASSWORD='<password>' \
  -e CLINIC_TIMEZONE=Asia/Colombo \
  -v "$PWD/modular/target/clinic.war:/usr/local/tomcat/webapps/ROOT.war:ro" \
  tomcat:10.1-jdk17-temurin
```

### 4. Check it landed

```bash
docker logs sunrise-tomcat 2>&1 | grep -E 'database_pool_ready|clinic_timezone'
```

The URL in that line is the one it is actually using. If it still says `localhost:3306`, the
variable did not reach the process — which is the commonest mistake, and the log tells you
immediately.

## The URL, parameter by parameter

The local default and a hosted one differ in exactly four places:

```
local   jdbc:mysql://localhost:3306/sunrise_dental?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
hosted  jdbc:mysql://the-host:24601/defaultdb?sslMode=REQUIRED&serverTimezone=UTC
                     ^^^^^^^^ ^^^^^ ^^^^^^^^^  ^^^^^^^^^^^^^^^^
```

| Parameter | Local | Hosted | Why |
|---|---|---|---|
| host, **port** | `localhost:3306` | the provider's, a high port | Allocated per service. Nothing in the code assumes 3306 |
| database | `sunrise_dental` | often `defaultdb` | Whatever you loaded the schema into |
| `useSSL` / `sslMode` | `useSSL=false` | `sslMode=REQUIRED` | The credential and every patient's name cross the public internet. `useSSL` is the deprecated spelling; use `sslMode` |
| `allowPublicKeyRetrieval` | `true` | **drop it** | Only needed for `caching_sha2_password` over an *unencrypted* link. With TLS it is unnecessary, and leaving it on weakens a man-in-the-middle protection you now have |
| `serverTimezone` | `UTC` | `UTC` | Keep it UTC and let `CLINIC_TIMEZONE` do the work — see below |

### Verifying the provider's certificate

`sslMode=REQUIRED` encrypts but does not check who is on the other end. To verify, take the
provider's CA (Aiven offers `ca.pem` in the console), put it in a truststore, and point at it:

```bash
keytool -importcert -alias aiven -file ca.pem \
        -keystore /opt/clinic/truststore.jks -storepass changeit -noprompt

export DB_URL='jdbc:mysql://the-host:24601/defaultdb?sslMode=VERIFY_CA&serverTimezone=UTC&trustCertificateKeyStoreUrl=file:/opt/clinic/truststore.jks&trustCertificateKeyStorePassword=changeit'
```

Still only the URL. Still no rebuild.

## What genuinely changes with a remote database

Four things behave differently, and only one of them needs a decision.

### The pool is opened eagerly, and validated on loan

`Database` opens all `db.pool.size` connections at start-up and holds them. On a remote service
that means eight TLS handshakes before the first request, which is a second or two of start-up
rather than a problem.

It also **checks a connection is alive before lending it** and replaces it if not:

```java
if (!connection.isValid(2)) {
    closeQuietly(connection);            // the server dropped it — idle timeout, restart
    connection = openConnection();
}
```

So a provider that idles connections out — and they all do — is already handled. That check
costs nothing locally and is the difference between working and not working across a network.

### Connection limits

**This is the one to check.** A hosted plan caps connections, and a small Aiven plan allows
around 20–25 in total, shared with anything else connecting.

| Instances × `DB_POOL_SIZE` | Against a 20-connection plan |
|---|---|
| 1 × 8 | comfortable |
| 2 × 8 | fine |
| 2 × 16 | at the limit, and a `mysql` prompt will be refused |

Eight is right for one practice. Raise it only if you see requests waiting — the symptom is
`Timed out waiting for a free database connection` after ten seconds, and the fix is more
connections *or* a slow query holding one, which is worth finding first.

### Latency

Every statement now crosses a network. A page issuing five queries at 1 ms costs 5 ms locally
and 150 ms against a database in another region. Two consequences:

- **Put the database in the same region as the application.** This matters more than the plan
  size.
- The reports are already `SUM` and `GROUP BY` in the database rather than loops in Java, which
  is what makes them viable remotely. Keep it that way: the version that read every bill into
  memory would have been a year of rows over the wire to produce twenty numbers.

### Time

Leave `serverTimezone=UTC` in the URL and set `CLINIC_TIMEZONE` to the practice's zone. The
application computes every date in the clinic's zone regardless of where the server or the
database thinks it is — a hosted database is almost certainly on UTC, and that is fine.

The mismatch is logged so it cannot be silent:

```
INFO clinic_timezone clinic=Asia/Colombo jvm=Etc/UTC
     (dates are computed in the clinic's zone, not the JVM's)
```

Getting this wrong is not cosmetic. Before it was configurable, a bill for an appointment booked
today landed under **yesterday's** takings for five and a half hours every night, and the report
disagreed with the appointment book.

## Where to keep the credential

Not in `clinic.properties`, and not in a `docker run` line that lands in your shell history.

| Where you are running | Use |
|---|---|
| A single server | An environment file readable only by the Tomcat user — `chmod 600` — sourced by the start-up script |
| Docker | `--env-file`, not `-e`, and keep the file out of the image and out of git |
| GitHub Actions | Repository or environment secrets. Nothing in the workflow file |
| A managed platform | Its own secret store |

Rotating it is a restart: change the value, restart Tomcat. Nothing is cached anywhere else and
no other file needs editing.

## What must not change

If any of the following seems necessary, something has gone wrong:

- **No code change.** No class knows a host, a port or a credential. `Database` asks `AppConfig`,
  and `AppConfig` asks the environment.
- **No rebuild.** The WAR that CI produced is the WAR that runs. A rebuild per environment is a
  chance for the thing you tested and the thing you deployed to differ.
- **No second properties file.** There is no `clinic-prod.properties`, no profile, no build flag.
  Something behaving differently between two environments that is not in the table above is a
  defect, not configuration.
- **No schema change.** The same `schema.sql` and `procedures.sql` load into a hosted MySQL 8
  unchanged. The only wrinkle is `CREATE DATABASE`, covered above.

## When it does not connect

| Symptom | Almost always |
|---|---|
| The log still says `localhost:3306` | `DB_URL` did not reach the process. Check the start-up script, or `docker exec … env \| grep DB_` |
| `Cannot connect to the database at …` | Wrong host or port, or the provider's IP allow-list does not include this server |
| `Access denied for user` | Wrong user or password — or the user exists but has no rights on *that* database |
| `Public Key Retrieval is not allowed` | `caching_sha2_password` over an unencrypted link. Use `sslMode=REQUIRED`, which is what you want anyway |
| `SSLHandshakeException … PKIX path building failed` | `sslMode=VERIFY_CA` without the provider's CA in a truststore. Either add it or drop to `REQUIRED` |
| `Unknown database 'sunrise_dental'` | The schema was loaded into `defaultdb`. Name that in the URL, or load it into `sunrise_dental` |
| Timed out waiting for a free connection | The pool is exhausted. Either `DB_POOL_SIZE` is too small for the traffic, or one slow statement is holding a connection |
| Non-ASCII arrives as `â€”` | The schema or seed data was loaded without `--default-character-set=utf8mb4` |
