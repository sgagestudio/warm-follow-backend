# SS Reminders Backend

Backend REST/JSON for SS Reminders (customers, templates, reminders, GDPR, audit) built with Java 21 and Spring Boot 3.

## Requirements
- Java 21+
- PostgreSQL 15+

## Quick start
1. Create database:
   - `createdb ss_reminders`
2. Configure env vars (see below).
3. Run:
   - `./mvnw spring-boot:run`

OpenAPI UI: `http://localhost:8080/swagger-ui.html`

## Environment variables
Required:
- `APP_JWT_SECRET` (>= 32 bytes)
- `DB_URL`, `DB_USER`, `DB_PASSWORD`

Optional:
- `APP_ADMIN_EMAILS` (comma-separated)
- `APP_REMINDER_INTERVAL_MINUTES` (default 5)
- `APP_TIME_ZONE` (default Europe/Madrid)
- `APP_GOOGLE_OAUTH_ENABLED` (default false)
- `APP_GOOGLE_OAUTH_MODE` (default mock)
- `APP_GOOGLE_CLIENT_ID`, `APP_GOOGLE_CLIENT_SECRET`, `APP_GOOGLE_REDIRECT_URI`, `APP_GOOGLE_SCOPES`
- `APP_GOOGLE_MOCK_EMAIL`, `APP_GOOGLE_MOCK_SUB`
- `APP_UNSUBSCRIBE_BASE_URL`, `APP_UNSUBSCRIBE_SECRET`
- `APP_RESET_TOKEN_EXPOSE` (default false)
- `APP_WEBHOOK_EMAIL_SECRET`, `APP_WEBHOOK_SMS_SECRET`

## Auth notes
- Gmail domains require Google OAuth. Password login/register returns `AUTH_GOOGLE_REQUIRED`.
- Refresh tokens are stored hashed and rotated on refresh.
- JWT claims: `sub`, `email`, `provider`, `auth_type`, `iat`, `exp`.

## Headers
- `X-Request-Id`: optional request id; echoed in responses and stored in audit events.
- `Idempotency-Key`: supported for `POST /reminders` to prevent duplicates.

## Pagination
- Uses offset cursor: `cursor` is a numeric offset (`0`, `20`, `40`, ...).

## Webhook signature
For `/webhooks/email/status` and `/webhooks/sms/status`:
- Header: `X-Signature`
- Signature = Base64URL(HMAC-SHA256(secret, `provider_message_id:status:error_code:error_message`))

## Tests
Tests run with Testcontainers PostgreSQL:
- `./mvnw test`

## Notes
- Quartz uses JDBC job store with clustering enabled.
- Google OAuth uses mock mode by default (`APP_GOOGLE_OAUTH_MODE=mock`).
- Customer creation and reminder sends require `legal_terms_accepted=true` in user settings (`PATCH /settings`).
- Reminder scheduling and date filters use `APP_TIME_ZONE` for local time interpretation.
