# AttendIQ UCC — Safe Refactoring & Change Patterns Skill

## PURPOSE
When making ANY change to the AttendIQ codebase, consult this skill first to avoid breaking the hybrid desktop+web architecture.

---

## The Fundamental Tension

This project has **one backend, two clients** that behave very differently:

```
                    ┌─────────────────────────────────────────┐
                    │   PostgreSQL DB (shared)                │
                    └──────────────┬──────────────────────────┘
                                   │
                    ┌──────────────▼──────────────────────────┐
                    │   Spring Boot Backend                   │
                    │   ├── REST API  (/auth/**, /courses/**) │
                    │   └── Web UI   (/, /student, /lecturer) │
                    └──────┬───────────────────┬──────────────┘
                           │                   │
              ┌────────────▼──────┐   ┌────────▼────────────┐
              │  JavaFX Desktop   │   │  Web Browser        │
              │  (JWT in memory)  │   │  (JWT in cookie)    │
              │  Bearer header    │   │  HttpOnly cookie    │
              └───────────────────┘   └─────────────────────┘
```

---

## Rules for Making Changes

### Rule 1: JWT Token Extraction
`JwtAuthenticationFilter` tries **two sources** in order:
1. `Authorization: Bearer <token>` header → used by desktop
2. `token` cookie → used by web browser

**DO NOT** remove either path. If you modify `JwtAuthenticationFilter`, verify both paths still work.

### Rule 2: Security `permitAll()` Endpoints
When adding new public endpoints, update BOTH:
- `SecurityConfig.java` → add to `.requestMatchers(...)`.permitAll()`
- Ensure the endpoint doesn't accidentally expose sensitive data

Never remove `/logout` from `permitAll()` — the web app's GET logout needs it.

### Rule 3: Desktop Import Resolution
When working in the `desktop/` module and the IDE shows "cannot be resolved" for:
```java
import com.ucc.attendance.desktop.App;
import com.ucc.attendance.desktop.SessionManager;
import com.ucc.attendance.desktop.util.FxUtils;
```
**These are IDE cache errors — the code compiles correctly.** Do NOT rename or move these classes to "fix" the IDE. Run `mvn compile -pl desktop` to verify.

### Rule 4: Flyway Migration Immutability
**NEVER** edit `V1__*.sql`, `V2__*.sql`, `V3__*.sql`, or `V4__*.sql` once they've been applied.
- Need to add a column? Create `V5__add_column_to_table.sql`
- Need to fix a constraint? Create `V5__fix_constraint.sql`
- Editing applied migrations causes `FlywayException: Validate failed` on startup

### Rule 5: SessionManager is Stateful Per JVM
`SessionManager` holds state for the **entire running desktop JVM**. It is a static singleton.
- It is NOT tied to a database session or HTTP session
- Logout (`clearSession()`) wipes it immediately and permanently for that run
- If you add new user properties, add them to `SessionManager` fields AND update `setSession()` and `clearSession()`

### Rule 6: ApiClient Error Handling
All errors from the backend should be surfaced to the user via `FxUtils.showError()` or the label-based error display in `LoginController`. The pattern is:
```java
} catch (Exception e) {
    Platform.runLater(() -> {
        FxUtils.showError("Error Title", e.getMessage() != null ? e.getMessage() : "Unknown error");
    });
}
```
Never swallow exceptions silently in the desktop client.

### Rule 7: Thymeleaf Template Flash Attributes
For web UI success/error messages, use `RedirectAttributes`:
```java
redirectAttributes.addFlashAttribute("error", errorMsg);
return "redirect:/destination";
```
And in the template, check `${error}` and `${success}` from the model.

---

## Common Pitfalls

### Pitfall 1: Using `@PostMapping` for Logout
Spring Security's default `LogoutFilter` was intercepting POST `/logout` before it was disabled. The project now uses:
- `@GetMapping("/logout")` in `WebController` — **always use GET for logout**
- `.logout(AbstractHttpConfigurer::disable)` in `SecurityConfig` — Spring's built-in logout is disabled

### Pitfall 2: Desktop Registering Already-Existing Accounts
HTTP 409 means the email/index/lecturer-code already exists in the database. This happens when:
- The user registered via the web app first
- The user's Flyway migration seeded the account
- Solution: Sign in instead of registering

### Pitfall 3: Database Connection in Tests
The `backend` module uses Flyway — integration tests that spin up the context will try to run migrations. Ensure `application-test.properties` points to a test DB or use `@DataJpaTest` with H2 if needed.

### Pitfall 4: Desktop `@FXML` Field Null
If `@FXML` fields are null at runtime, the most common cause is a mismatch between the FXML file's `fx:id` and the Java field name. The controller class must also match what's declared in the FXML `fx:controller` attribute.

### Pitfall 5: `Platform.runLater()` for UI Updates
**All** JavaFX UI changes from background threads MUST go inside `Platform.runLater(() -> { ... })`. Missing this causes `IllegalStateException: Not on FX application thread`.

### Pitfall 6: Thymeleaf `${session}` Model Attribute Name
Never use `model.addAttribute("session", ...)` in Spring MVC + Thymeleaf. `${session}` is reserved for `HttpSession`. Use `attendanceSession`, `liveSession`, etc. This broke `/lecturer/session/{id}` (null header, failed QR fetch).

---

## Adding New Features — Checklist

### Adding a New REST API Endpoint
- [ ] Add handler in appropriate `@RestController` (e.g., `CourseController`, `SessionController`)
- [ ] Add `ApiClient` method in `desktop/.../ApiClient.java`
- [ ] Update `SecurityConfig` if the endpoint needs different auth (public vs authenticated)
- [ ] If Desktop uses it, add a controller method + FXML button

### Adding a New Web Page
- [ ] Add `@GetMapping` in `WebController.java`
- [ ] Create Thymeleaf template in `backend/src/main/resources/templates/`
- [ ] If the page is public, add to `SecurityConfig` permitAll
- [ ] Add navigation link in relevant existing templates (student.html, lecturer.html)

### Adding a Database Column
- [ ] Create new migration `V(n+1)__description.sql`
- [ ] Update JPA entity class
- [ ] Update DTOs / record classes as needed
- [ ] Update `ApiClient` records in the desktop if the column is returned in API responses

### Modifying Authentication Logic
- [ ] Test with BOTH desktop client (Bearer token) AND web browser (cookie)
- [ ] Check `JwtAuthenticationFilter` handles both token sources
- [ ] Verify public endpoints still work without a token
- [ ] Verify protected endpoints reject unauthenticated requests
