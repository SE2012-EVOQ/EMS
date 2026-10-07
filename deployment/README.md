# Runtime deployment and first Manager/Admin

Use Java 21, Node 22 (build only), and MySQL 8.4/InnoDB. Runtime schema validation is always enabled; Hibernate does not create/update tables. The finalized assignment artifacts and `database/01_schema.sql` are frozen.

## Fresh database only

Create an external, private MySQL client option file (mode 600), containing `[client]`, host, port, setup username and password. Do not put it in this repository or pass a password on a command line. Run from the repository:

```sh
MYSQL_CNF=/secure/path/mysql-setup.cnf EMS_DATABASE_NAME=evoq_ems sh deployment/init-fresh-db.sh
```

The script creates a database without `IF NOT EXISTS`; it refuses an existing name. It reads only the table-DDL section of the finalized schema, omitting the destructive preamble, and applies the non-destructive runtime role seed. It creates no sample/demo business data. If installation fails part-way, inspect and resolve the new empty/partial installation yourself; there is no automatic drop/recreate recovery.

**Retained database:** never run `database/01_schema.sql` or the fresh-install script against retained data. Back up first. Existing schema validation must pass. Missing runtime roles may be seeded with `database/06_runtime_roles.sql`; this inserts names without deleting or changing existing accounts. Future schema changes need a reviewed migration; none are introduced here. Demo sample/negative-test scripts are not production initialization.

## First real Manager/Admin

The frozen schema requires every account to reference an Employee. Before the first account exists, the operator must provision the first real administrator's department and active Employee identity using the approved schema (or select an existing active identity). Use real name, email, hire date and job title, and obtain that `employee_id`. This is a one-time installation prerequisite, not a new Employee creation API or policy. Bootstrap never creates/rewrites Employee business records or leave balances.

For a completely empty installation, run an operator-reviewed identity insert with the real person's values (use an existing department instead if appropriate):

```sql
START TRANSACTION;
INSERT INTO department (name, description) VALUES ('Administration', 'Initial administrator department');
SET @bootstrap_department_id = LAST_INSERT_ID();
INSERT INTO employee (department_id, first_name, last_name, email, hire_date, job_title, status)
VALUES (@bootstrap_department_id, 'YOUR_FIRST_NAME', 'YOUR_LAST_NAME', 'YOUR_REAL_EMAIL',
        'YOUR_ACTUAL_HIRE_DATE', 'YOUR_JOB_TITLE', 'ACTIVE');
SELECT LAST_INSERT_ID() AS bootstrap_employee_id;
COMMIT;
```

Replace every placeholder before executing; do not run it against an existing identity. The operator chooses actual organization/profile values; the bootstrap adds no business-policy defaults.

Create an external password file containing a randomly generated secret: at least 16 characters, at most 72 UTF-8 bytes. Leading/trailing whitespace is stripped. Restrict file access to the operator and backend runtime identity. Set these properties via an external Spring properties file or environment:

```properties
ems.bootstrap.enabled=true
ems.bootstrap.username=your.admin.username
ems.bootstrap.employee-id=THE_EXISTING_ACTIVE_EMPLOYEE_ID
ems.bootstrap.password-file=/secure/path/first-admin-password
```

Equivalent environment variables: `EMS_BOOTSTRAP_ENABLED`, `EMS_BOOTSTRAP_USERNAME`, `EMS_BOOTSTRAP_EMPLOYEE_ID`, `EMS_BOOTSTRAP_PASSWORD_FILE`. Bootstrap is disabled unless explicitly enabled and is unavailable in `dev`. It locks the existing Manager/Admin role row to serialize competing instances, verifies there are **no accounts at all**, locks/checks the active Employee, and inserts only a BCrypt password hash in one transaction. Invalid input, missing roles/inactive employee or any existing account refuses startup and rolls back. It does not reset an existing administrator or recover lost passwords.

Run once with the normal production configuration, confirm sign-in, then disable/remove bootstrap settings and securely remove the password file. Leaving bootstrap enabled deliberately refuses the next restart; it never overwrites an account. Do not enable `dev` in production. Forced first-login password changes, recovery and audit trails remain outside the frozen schema/current scope.

## Native runtime

Build and verify:

```sh
(cd backend && sh mvnw -q test && sh mvnw -q -DskipTests package)
(cd frontend && npm ci && node --test test/api.test.mjs && npm run build)
```

Backend tests require an isolated/local MySQL schema, the `dev` profile and an external `DEV_DEMO_PASSWORD`; do not point tests at production. Maven config resolves the Mockito agent from its local repository/version, with no developer-specific absolute path.

Provide configuration outside source, for example a mode-600 `application.properties` mounted/readable by the runtime account:

```properties
spring.profiles.active=prod
DB_URL=jdbc:mysql://database-host:3306/evoq_ems?sslMode=VERIFY_IDENTITY&serverTimezone=UTC
DB_USERNAME=ems_runtime
DB_PASSWORD=SET_EXTERNALLY
FRONTEND_ORIGIN=https://ems.example.org
ems.business-timezone=Asia/Colombo
```

Run `java -jar backend/target/ems-backend-0.0.1-SNAPSHOT.jar --spring.config.additional-location=file:/secure/config/`. A config tree is also supported with `SPRING_CONFIG_IMPORT=configtree:/run/secrets/`; a file named `DB_PASSWORD` supplies that property. `EMS_BUSINESS_TIMEZONE` overrides the default Asia/Colombo. Never change the JVM-global timezone to implement attendance policy.

The `prod` profile requires explicit DB configuration, disables SQL logging, enables Secure/HttpOnly/SameSite=Lax session cookies, and sets a 30-minute session timeout. Its startup guard requires an HTTPS frontend origin without a path, a nonempty external DB password and secure cookies, and rejects `prod` combined with `dev` before demo initializers run. Terminate TLS at a trusted reverse proxy, overwrite forwarded headers there, and restrict direct backend access. Use a same-origin `/api` reverse proxy (frontend build `VITE_API_BASE_URL=/api`); unrelated cross-site origins are not the supported default topology. No wildcard credentialed CORS or CSRF bypass is needed.

Give the runtime MySQL user only needed SELECT/INSERT/UPDATE/DELETE grants on this schema, no DROP/CREATE/ALTER privileges. Use a separate setup account for fresh DDL. Require authenticated TLS for remote MySQL. Configure backups and verify restore separately; no deployment action deletes retained data.

## Containers

`backend/Dockerfile` packages with Java 21 and runs as UID/GID 10001. `frontend/Dockerfile` builds with Node 22 and serves an SPA and `/api` proxy using unprivileged nginx. `deployment/compose.yml` connects to an **existing** database; it contains no MySQL init volume or destructive database lifecycle.

Put an external file named `DB_PASSWORD` in `EMS_SECRET_DIR`. The directory/file must be readable by backend UID/GID 10001 (for example operator-owned directory mode 750 and file mode 640 with group 10001). Do not mount unrelated secrets. Set `DB_URL`, `DB_USERNAME`, `FRONTEND_ORIGIN=https://your-host`, and absolute `EMS_SECRET_DIR` in the operator's environment; optionally `EMS_BUSINESS_TIMEZONE`.

```sh
docker compose -f deployment/compose.yml config
docker compose -f deployment/compose.yml up --build -d
```

The frontend binds only `127.0.0.1:8081` on the host; place the trusted HTTPS proxy in front of it. Backend has no host port. For a first bootstrap, place its password file in the external secret directory and use a controlled one-off backend run with the four `EMS_BOOTSTRAP_*` variables before starting the normal backend. Disable/remove these variables and delete its password file afterward. The proxy's API target is the compose service `backend:8080`; a different topology must adjust that nginx config.

Container builds/compose require Docker. These files are portable templates, not proof of a deployed TLS/database environment. CI in `.github/workflows/verify.yml` provisions an ephemeral MySQL 8.4 database with fresh DDL, generates development credentials in the runner, runs backend tests/package and frontend tests/build. Its isolated no-password MySQL service is never a production configuration.

## Session/access limitations to coordinate

Each authenticated request revalidates the session account's active flag, identity, role and password hash before authorization. A changed/deleted account invalidates that session and returns 401; reauthentication gets current privileges. Changes committed after a request's revalidation cannot cancel an already-running request. Account deactivation does not invent a new attendance-closing policy; existing reconciliation remains unchanged.

Frontend 401 handling centrally clears authenticated state and CSRF state and returns to login. Authenticated permission/CSRF failures remain 403 and do not sign out the user; failed login is separate. Restarting a single backend loses in-memory sessions. Multi-instance session persistence/stickiness and operational monitoring are deployment follow-ups.

Owner access-control audit (no endpoint permissions changed): Asset controllers currently permit any signed-in role to register/update/status/assign/return and read all/history/other-employee assignments; the Asset owner must define/enforce scope. Employee directory and profile-by-ID reads are broadly authenticated, and supervisor direct-report reads accept another supervisor's ID without ownership validation; the Employee owner must resolve directory/direct-report scope. Leave decision endpoints already check the assigned supervisor; Leave report calculations remain owner work. These gaps block treating the entire application as production-approved, even though shared session handling is hardened.
