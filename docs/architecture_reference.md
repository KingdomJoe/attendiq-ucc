# AttendIQ UCC Hybrid Architecture — Architecture Reference Skill

## PURPOSE
This skill gives you an accurate, up-to-date map of the entire AttendIQ project architecture so you can make safe, conflict-free changes across both the JavaFX desktop application and the Spring Boot web application.

---

## Project Layout

```
UCC-Attendance-system/
├── backend/          ← Spring Boot 3.x + Thymeleaf + PostgreSQL (REST API + Web UI)
├── desktop/          ← JavaFX 21 fat-jar (non-modular) – Windows-only Lecturer client
├── docs/             ← Architecture documentation
├── scripts/          ← DB seed / deployment helpers
├── docker-compose.yml
└── pom.xml           ← Parent POM (groupId: com.ucc, artifactId: attendance-parent)
```

---

## Key Architectural Facts

### Dual-Mode Backend
The single `backend` module serves **two entirely different clients**:

| Client | Auth Method | Token Storage | How Logout Works |
|--------|------------|---------------|-----------------|
| Web Browser | JWT in **HttpOnly cookie** (`token`) | Browser cookie | GET `/logout` clears cookie + SecurityContext |
| Desktop JavaFX | JWT in **`SessionManager` (in-memory)** | Java static field | `SessionManager.clearSession()` (no server call) |

**Critical rule:** Never conflate the two auth flows. The desktop does NOT use cookies. The web app does NOT use Authorization headers (except the PWA offline logic).

### Spring Security Configuration (backend)
- Spring Security 6, **stateless** (`SessionCreationPolicy.STATELESS`)
- **Default LogoutFilter is DISABLED** via `.logout(AbstractHttpConfigurer::disable)` — the logout is handled by a plain `@GetMapping("/logout")` in `WebController`
- CSRF disabled (safe because the web forms use HttpOnly cookie, not CSRF-vulnerable JS-based auth)
- JWT authentication via `JwtAuthenticationFilter` (added before `UsernamePasswordAuthenticationFilter`)
- Public endpoints: `/auth/**`, `/departments`, `/`, `/login`, `/register/**`, `/logout`, `/lecturer-download`, `/error`, all static assets + PWA files

### Desktop (JavaFX) HTTP Client
- `ApiClient.java` — REST calls to production `https://ucc-attendance-system.onrender.com` (see `api.properties` / `ApiConfig.java`). Local dev: `mvn javafx:run -pl desktop` passes `-Dapi.url=http://localhost:8080`, or set `ATTENDIQ_DEV=true`.
- `SessionManager.java` — static in-memory session state (JWT token, display name, identifier, role)
- `App.java` — JavaFX `Application` subclass, provides static `navigateTo()`, `showLogin()`, `showDashboard()`
- `Launcher.java` — separate entry point that calls `Application.launch(App.class, args)` (required for non-modular JavaFX fat-jar)
- Auth endpoints: `POST /auth/login`, `POST /auth/student/register`, `POST /auth/lecturer/register`

### Database (PostgreSQL + Flyway)
- **Flyway** manages all schema migrations — files at `backend/src/main/resources/db/migration/`
- Current migrations: V1 (core schema), V2, V3, V4 (lecturer_codes seed + lecturer registration)
- **NEVER** edit existing `V*.sql` files after they've been applied — always create a new `V(n+1)__description.sql`
- `spring.jpa.hibernate.ddl-auto=none` (Flyway owns the schema — Hibernate must not auto-update)

---

## Desktop Controller Package Resolution (IDE False Positives)

All controllers in `desktop/src/main/java/com/ucc/attendance/desktop/controller/` import:
```java
import com.ucc.attendance.desktop.App;
import com.ucc.attendance.desktop.SessionManager;
import com.ucc.attendance.desktop.util.FxUtils;
```

The IDE (Cursor/VSCode Java extension) sometimes shows these as unresolved after refactoring or file moves. **This is a false positive.** Maven compiles successfully:
```bash
cd UCC-Attendance-system
mvn compile -pl desktop  # → BUILD SUCCESS
```

**Fix for IDE red underlines:**
1. Open Command Palette → "Java: Clean Java Language Server Workspace"
2. Or: delete `desktop/target/` then run "Java: Force Java Compilation"
3. Or: run `mvn compile -pl desktop` in terminal to confirm it's just an IDE issue

---

## Authentication Flow Diagram

```
DESKTOP LOGIN:
  User → LoginController.handleLogin()
       → ApiClient.login(identifier, password, role) [POST /auth/login]
       → Backend: AuthService.login() → JwtService.generateToken()
       ← AuthResponse(token, role, userId, displayName)
       → SessionManager.setSession(token, displayName, identifier, role)
       → App.showDashboard()

WEB LOGIN:
  User → POST /login (form)
       → WebController.login() → AuthService.login()
       ← JWT token
       → setTokenCookie(response, token)  [HttpOnly cookie "token"]
       → redirect:/lecturer or redirect:/student

WEB LOGOUT:
  User → GET /logout
       → WebController.logout() → clearTokenCookie(response)
       → SecurityContextHolder.clearContext()
       → redirect:/

DESKTOP LOGOUT:
  User → [logout button in any controller]
       → SessionManager.clearSession()
       → App.showLogin()
```

---

## Error Handling Patterns

### Desktop (ApiClient.java)
- HTTP errors: `throw new RuntimeException("API Error (HTTP " + statusCode + "): " + getErrorMessage(body, statusCode))`
- HTTP 409 (conflict): message appended with "Try signing in instead — you may have already registered via the web app."
- Connection refused: "Cannot connect to server at <URL>. Make sure the backend is running."
- Timeout: "Connection to <URL> timed out. Is the server running?"

### Web App
- Business exceptions: `ApiException` (custom) with `HttpStatus` and `code`/`message` fields
- Global handler: `WebExceptionHandler` handles `ApiException`, `AccessDeniedException`, `HttpRequestMethodNotSupportedException`, etc.
- Flash attributes used for user-facing error/success messages in Thymeleaf templates

---

## Module Build Commands

```bash
# Compile desktop only (fast check)
mvn compile -pl desktop

# Run backend (Spring Boot)
cd backend && mvn spring-boot:run

# Build desktop fat-jar
cd desktop && mvn package -DskipTests

# Full project build
mvn clean package -DskipTests
```

---

## Key Files Reference

| File | Purpose |
|------|---------|
| `backend/.../config/SecurityConfig.java` | Spring Security filter chain, CORS, JWT filter wiring |
| `backend/.../controller/WebController.java` | All web UI routes (login, register, student, lecturer pages) |
| `backend/.../controller/AuthController.java` | REST API auth endpoints (`/auth/login`, `/auth/*/register`) |
| `backend/.../security/JwtAuthenticationFilter.java` | Extracts JWT from either cookie or Authorization header |
| `backend/.../service/AuthService.java` | Registration, login, me() logic; BCrypt password hashing |
| `desktop/.../App.java` | JavaFX entry point, scene navigation |
| `desktop/.../SessionManager.java` | In-memory session state (token, role, display name) |
| `desktop/.../ApiClient.java` | All REST calls from desktop to backend |
| `desktop/.../controller/LoginController.java` | Desktop login/register UI logic |
