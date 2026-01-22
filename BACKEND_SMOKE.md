Build (no tests)
1) `mvn -DskipTests package`
   - Result: `BUILD SUCCESS` (2026-01-22)
   - If `target/` permission fails, use:
     `mvn -Dproject.build.directory=C:\Temp\wf-target -DskipTests package`

Run (apply Flyway migrations)
2) Ensure env vars are set:
   - `WARM_FOLLOW_DB_URL`
   - `WARM_FOLLOW_DB_USER`
   - `WARM_FOLLOW_DB_PASSWORD`
   - `WARM_FOLLOW_JWT_SECRET`
3) Start the app (JDK 21 from IntelliJ):
```powershell
$env:JAVA_HOME="C:\Users\Marco\.jdks\ms-21.0.9"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
mvn -DskipTests spring-boot:run
```
   - Result log:
     - `Migrating schema "public" to version "4 - onboarding state"`
     - `Successfully applied 2 migrations to schema "public", now at version v4`

Smoke (curl examples)
4) Register a user to get tokens:
```bash
curl -s -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"owner@example.com","password":"StrongPass123","name":"Owner User","workspace_name":"Acme Workspace"}'
```
Expected: JSON with `access_token`, `refresh_token`, `user`, `workspace`, `role`.

5) Login and capture token (if already registered):
```bash
curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"owner@example.com","password":"StrongPass123"}'
```
Expected: JSON with `access_token`.

6) /auth/me includes counts:
```bash
curl -s http://localhost:8080/auth/me \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```
Expected: `counts: { clients: <number>, users: <number> }` present.

7) Accept legal terms (required before creating customers/reminders):
```bash
curl -s -X PATCH http://localhost:8080/settings \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"settings":{"legal_terms_accepted":true}}'
```

8) GET /onboarding returns steps + current_step:
```bash
curl -s http://localhost:8080/onboarding \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```
Expected: `steps` object and `current_step` string or null.

9) PATCH /onboarding marks a step complete and advances:
```bash
curl -s -X PATCH http://localhost:8080/onboarding \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"step":"domain_configured","completed":true}'
```
Expected: `steps.domain_configured=true` and `current_step` moves to next false step.

10) Create sending domain (requires real DNS to verify):
```bash
curl -s -X POST http://localhost:8080/sending-domains \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"domain":"example.com"}'
```
Expected: domain response with `status: pending` and `verification_records`.
Example response:
```json
{"id":"b6073941-94a2-465c-8ce4-982efe98aa2a","domain":"example.com","status":"pending","verification_records":[{"type":"TXT","name":"@","value":"v=spf1 include:amazonses.com ~all","ttl":null,"priority":null,"purpose":"spf"},{"type":"CNAME","name":"dkim._domainkey","value":"dkim.amazonses.com","ttl":null,"priority":null,"purpose":"dkim"},{"type":"TXT","name":"_dmarc","value":"v=DMARC1; p=none","ttl":null,"priority":null,"purpose":"dmarc"}],"created_at":"2026-01-22T13:52:30.292076200Z","updated_at":"2026-01-22T13:52:30.292076200Z"}
```

11) Verify sending domain (repeat until status is verified):
```bash
curl -s -X POST http://localhost:8080/sending-domains/$DOMAIN_ID/verify \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```
Expected: `status: verified`.
Example response:
```json
{"id":"b6073941-94a2-465c-8ce4-982efe98aa2a","domain":"example.com","status":"verified","verification_records":[{"type":"TXT","name":"@","value":"v=spf1 include:amazonses.com ~all","ttl":null,"priority":null,"purpose":"spf"},{"type":"CNAME","name":"dkim._domainkey","value":"dkim.amazonses.com","ttl":null,"priority":null,"purpose":"dkim"},{"type":"TXT","name":"_dmarc","value":"v=DMARC1; p=none","ttl":null,"priority":null,"purpose":"dmarc"}],"created_at":"2026-01-22T13:52:30.292076Z","updated_at":"2026-01-22T13:52:30.399874900Z"}
```

12) Create a customer:
```bash
curl -s -X POST http://localhost:8080/customers \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"first_name":"Ada","last_name":"Lovelace","email":"ada@example.com","consent_status":"granted","consent_source":"form","consent_channels":["email"]}'
```
Expected: customer response with `id`.

13) Create a template:
```bash
curl -s -X POST http://localhost:8080/templates \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Welcome","subject":"Hello","content":"Hi {{name}}","channel":"email"}'
```
Expected: template response with `id`.

14) Create a reminder:
```bash
curl -s -X POST http://localhost:8080/reminders \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"template_id":1,"customer_ids":["<CUSTOMER_ID>"],"channel":"email","frequency":"once","scheduled_time":"09:00:00","scheduled_date":"2026-01-22"}'
```
Expected: reminder response with `id` and `status`.

15) GET /onboarding reflects computed truth:
```bash
curl -s http://localhost:8080/onboarding \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```
Expected:
`template_created=true` after step 13,
`first_reminder_scheduled=true` after step 14,
`domain_configured=true` once a verified sending domain exists.

Executed run (2026-01-22)
- Register email: owner+20260122145229@example.com
- Create sending domain: id=b6073941-94a2-465c-8ce4-982efe98aa2a status=pending
- Verify sending domain: status=verified
- /onboarding domain_configured=true

Root cause (fixed)
- Request body: `{"domain":"example.com"}`
- Response: `500 INTERNAL_ERROR` with `{"error":{"code":"INTERNAL_ERROR","message":"Unexpected error","details":{}}}`
- Stack trace:
```
org.springframework.web.servlet.resource.NoResourceFoundException: No static resource sending-domains.
	at org.springframework.web.servlet.resource.ResourceHttpRequestHandler.handleRequest(ResourceHttpRequestHandler.java:585)
	at org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter.handle(HttpRequestHandlerAdapter.java:52)
	at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1089)
```
