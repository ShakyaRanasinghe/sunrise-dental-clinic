# Running the application locally on Windows 11

The same application as [local-setup.md](local-setup.md) describes, on a Windows
machine. The supported path is **WSL2 + Docker Desktop**: the shell scripts are bash,
Docker provides MySQL and Tomcat, and nothing else is installed natively on Windows.

> Everything in [local-setup.md](local-setup.md) about ports, accounts, configuration
> and troubleshooting still applies. This page covers only what is different on Windows.

---

## 0. Prerequisites on the Windows side

| Need | Check | Where from |
|---|---|---|
| WSL2 with an Ubuntu distro | `wsl -l -v` in PowerShell | `wsl --install -d Ubuntu` |
| Docker Desktop, engine running | whale icon in the system tray | [docker.com](https://www.docker.com/products/docker-desktop/) |
| WSL integration enabled for Ubuntu | Docker Desktop → Settings → Resources → **WSL Integration** → toggle *Ubuntu* on | — |

After enabling integration, a new WSL terminal must have a working `docker`:

```bash
docker version          # client=… server=…  — if "command not found", restart the terminal
```

**Docker Desktop must be running before any command below.** Start it from the Start
menu and wait for the whale icon; the first start after boot takes ~30 s.

---

## 1. One-time setup inside WSL

Open an Ubuntu terminal (Start menu → Ubuntu). You are now on a Linux filesystem —
do not work under `/mnt/c/...` for anything heavy except the repository itself.

### 1a. JDK 17 + Maven, without sudo

Installed into `~/tools` so no administrator rights are needed:

```bash
mkdir -p ~/tools && cd ~/tools

# JDK 17 (Temurin, via Adoptium's latest-ga redirect)
curl -sSL -o jdk17.tar.gz \
  "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse"
tar xzf jdk17.tar.gz && rm jdk17.tar.gz

# Maven 3.9.x (Maven Central is faster than Apache's own mirrors)
curl -sSL -o maven.tar.gz \
  "https://repo1.maven.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.tar.gz"
tar xzf maven.tar.gz && rm maven.tar.gz
```

Wire them into every future shell:

```bash
cat >> ~/.bashrc <<'EOF'
# Java 17 + Maven for sunrise-dental-clinic
export JAVA_HOME="$HOME/tools/jdk-17.0.20.1+1"
export PATH="$HOME/tools/apache-maven-3.9.9/bin:$JAVA_HOME/bin:$PATH"
EOF
source ~/.bashrc

java -version    # openjdk 17.x
mvn -v           # Apache Maven 3.9.9
```

*(Alternative: `sudo apt install openjdk-17-jdk maven` does the same system-wide if
you have the sudo password.)*

### 1b. The CRLF problem — read before running any script

A repository cloned by Windows git carries **CRLF line endings**, which break bash:

```
/usr/bin/env: 'bash\r': No such file or directory
```

Fix it once, per clone, and prevent recurrence:

```bash
cd <repo>
sed -i 's/\r$//' scripts/*.sh      # repair the three scripts that ship as CRLF
git config core.autocrlf input     # keep new checkouts/commits LF-only
```

If `git status` then shows hundreds of modified files, that is the same disease in
reverse — normalise and clear it:

```bash
git add -A && git diff --cached --stat   # should list nothing but scripts/*.sh
git reset                                # index clean again, working tree stays LF
```

### 1c. Git identity

Commits need a name and email; a fresh WSL has neither:

```bash
git config --global user.name  "Your Name"
git config --global user.email "you@example.com"
```

---

## 2. The daily loop

```powershell
# PowerShell / Start menu: launch Docker Desktop, wait for the whale
```

```bash
cd ~/.../sunrise-dental-clinic-private    # or /mnt/c/Users/<you>/Documents/...
./scripts/dev-up.sh                       # DB + schema + demo data + build + deploy
./scripts/smoke.sh                        # 57 end-to-end checks, must be 0 failed
```

Then open **<http://localhost:8080>** — accounts and portals are in
[local-setup.md §2](local-setup.md#2-signing-in); every password is `Password123`.

Useful extras (identical to Linux):

```bash
docker logs -f sunrise-tomcat             # application log
docker exec -it sunrise-mysql mysql --default-character-set=utf8mb4 -uroot -pclinic sunrise_dental
./scripts/dev-up.sh                       # re-run after any code change; idempotent
```

---

## 3. What differs from the Linux setup, and why

| Thing | Linux host | This Windows setup |
|---|---|---|
| Shell | bash natively | bash inside WSL2 |
| MySQL/Tomcat | Docker Desktop (same) | Docker Desktop, reached through WSL integration |
| Java/Maven | system packages | user-local `~/tools`, no admin rights |
| Line endings | LF everywhere | must be forced (§1b) or scripts break |
| Repo location | anywhere | works on `/mnt/c` (Windows drive); slower I/O, acceptable here |

**Port 3308 is still correct.** On this machine it avoids any collision with a
Windows-native MySQL service holding 3306 — the reasoning in
[local-setup.md](local-setup.md#why-port-3308-and-not-3306) applies twice over.
Do not change it.

---

## 4. Windows-specific failures seen in practice

| Symptom | Cause | Fix |
|---|---|---|
| `docker: command not found` in WSL while Docker Desktop runs | WSL integration off for this distro | Settings → Resources → WSL Integration → toggle Ubuntu, restart terminal |
| `failed to connect to the docker API … dockerDesktopLinuxEngine` | Docker Desktop not started | Launch it, wait for the whale, retry |
| `/usr/bin/env: 'bash\r'` when running `dev-up.sh` | CRLF line endings (§1b) | `sed -i 's/\r$//' scripts/*.sh` |
| Hundreds of phantom modified files in `git status` | checked out with Windows autocrlf, used under WSL git | `git config core.autocrlf input`, then `git add -A && git reset` |
| `java: command not found` in a *new* terminal | `.bashrc` exports missing (§1a) | re-run the `cat >> ~/.bashrc` block |
| First `dev-up.sh` very slow | pulling `mysql:8.0` + `tomcat:10.1-jdk17-temurin`, plus Maven downloading the world | one-off |
| Build oddly slow overall | repo sits on `/mnt/c` (9P file system bridge) | normal for this layout; cloning into `~/` inside WSL would be faster if it ever bothers you |

Everything else — JSP 500s, 404 views, charset corruption, timezone drift — behaves
the same as on Linux and is covered by
[local-setup.md §7](local-setup.md#7-when-it-does-not-start).
