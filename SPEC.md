# Warm Follow SaaS Backend Spec

## Goals
- Convert the existing Warm Follow backend into a multi-tenant SaaS core.
- Preserve current style and architecture (Spring Boot 3, Java 21, PostgreSQL, Quartz JDBC, /webhooks/*).
- Provide billing, usage, limits, onboarding, and delivery tracking for a sellable SaaS.

## Tenant Context and Auth
- JWT access + refresh tokens.
- JWT claims include: user_id (sub), email, provider, auth_type, workspace_id, role.
- Workspace context is derived from JWT and can be overridden by `X-Workspace-Id` header.
- RBAC roles: owner, admin, member.

## Domains (Minimal)
### Workspace
- id (uuid)
- name (text)
- plan (starter | pro | business)
- status (trialing | active | past_due | suspended | canceled)
- stripe_customer_id (text)
- stripe_subscription_id (text)
- stripe_price_id (text, optional)
- trial_ends_at (timestamptz)
- created_at, updated_at

### WorkspaceMember
- workspace_id (uuid)
- user_id (uuid)
- role (owner | admin | member)
- status (active | invited | suspended)
- created_at, updated_at

### User
- id (uuid)
- email (citext, unique)
- name (text)
- provider (email | google)
- auth_type (jwt | oauth)
- status (active | invited | disabled)
- last_login (timestamptz)
- created_at, updated_at

### Client (Customer)
- id (uuid)
- workspace_id (uuid)
- first_name, last_name
- email, phone
- tags (text[])
- locale, timezone
- consent_status (granted | pending | revoked)
- consent_date, consent_source, consent_channels[], consent_proof_ref
- do_not_email, do_not_sms
- email_bounce_reason, email_bounce_at, email_complaint_at
- created_at, updated_at

### Template
- id (bigint)
- workspace_id (uuid)
- name
- channel (email | sms | both)
- subject (email only)
- content (body)
- variables (jsonb)
- version (int)
- created_at, updated_at

### Reminder (Schedule)
- id (uuid)
- workspace_id (uuid)
- template_id (bigint)
- channel (email | sms | both)
- schedule_type (legacy | once | interval | cron)
- schedule_expression (cron)
- schedule_interval_minutes (int)
- scheduled_at (timestamptz)
- scheduled_date, scheduled_time (legacy)
- timezone (text)
- status (pending | active | completed | failed | canceled)
- next_run (timestamptz)
- created_at, updated_at

### Transaction (Dispatch Run)
- id (uuid)
- workspace_id (uuid)
- reminder_id (uuid)
- status (queued | running | done | failed)
- request_id, idempotency_key
- started_at, finished_at
- created_at

### Delivery (Message)
- id (uuid)
- workspace_id (uuid)
- transaction_id (uuid)
- reminder_id (uuid)
- channel (email | sms)
- provider_message_id
- to (email or phone)
- payload_snapshot (jsonb)
- status (queued | sent | delivered | bounced | complained | failed | canceled)
- error_code, error_message
- queued_at, sent_at, delivered_at, bounced_at, complained_at, failed_at, canceled_at
- created_at, updated_at

### SendingDomain
- id (uuid)
- workspace_id (uuid)
- domain (text)
- status (pending | verified | failed)
- verification_records (jsonb)
- last_checked_at, failure_reason
- created_at, updated_at

### Unsubscribe
- id (uuid)
- workspace_id (uuid)
- channel (email | sms)
- email, phone
- token_hash (text)
- status (pending | confirmed)
- created_at, confirmed_at

### UsageLedger
- workspace_id (uuid)
- period (YYYY-MM)
- emails_sent, sms_sent
- sms_credits_balance
- overage_cost_cents
- created_at, updated_at

### SmsCreditPurchase
- workspace_id (uuid)
- checkout_session_id
- payment_intent_id
- credits
- amount_cents
- status (pending | succeeded | failed)
- created_at, updated_at

### StripeEvent
- id (stripe event id)
- type
- payload (jsonb)
- workspace_id (nullable)
- created_at

### AuditLog
- workspace_id (uuid)
- actor_user_id (uuid)
- action
- entity_type, entity_id
- metadata (jsonb)
- ip, user_agent
- created_at

### WorkspaceSettings / PrivacySettings
- WorkspaceSettings: workspace_id, settings json (company_name, timezone, locale, legal_terms_accepted, default_domain_id)
- PrivacySettings: user_id, retention_days override, audit_logs_enabled, etc (existing).

## Flows
### 1) Authentication and Session
- Register (owner): create user + workspace, return JWTs.
- Invite: owner/admin sends invite; user accepts with token.
- Login/logout/refresh.
- Reset password.
- `/auth/me` returns user + current workspace + plan/limits/usage summary.

### 2) Multi-tenant and RBAC
- Workspace context from JWT or `X-Workspace-Id`.
- Workspace membership checked on each request.
- Owner/admin required for billing, member management, domains.

### 3) Onboarding
- Create workspace.
- Add sending domain; return recommended SPF/DKIM/DMARC records.
- Manual verification check endpoint.
- Create initial template, client, and reminder.

### 4) Email Sending (SES)
- EmailService uses AWS SES.
- Idempotency: dedupe key per reminder_id + scheduled_time + recipient.
- Delivery state transitions: queued -> sent -> delivered/bounced/complained/failed.
- Events: SES -> SNS -> `/webhooks/email/status` with SNS signature verification (optional HMAC for internal forwarding).
- Bounce/complaint: set `do_not_email=true` and store reason/date; suppress future sends.

### 5) Unsubscribe and Preferences
- Unsubscribe token stored/validated; public endpoints:
  - GET `/u/{token}` landing
  - POST `/u/{token}` confirm
- Legacy `/unsubscribe/{token}` kept for compatibility.
- SMS STOP keywords can be mapped via Twilio status callback or explicit preference update.

### 6) SMS Sending (Twilio)
- SmsService with retry/backoff.
- Status callback `/webhooks/sms/status`.
- Usage tracking: decrement credits or add overage.

### 7) Scheduler (Quartz) and Dispatch
- Quartz interval from `APP_REMINDER_INTERVAL_MINUTES`.
- Job selects due reminders with DB locking (`FOR UPDATE SKIP LOCKED`).
- Creates deliveries in queued state, then sends and updates status.
- Per-tenant rate limit to avoid floods.

### 8) Billing (Stripe)
- Plans encoded in backend (starter/pro/business).
- Stripe Customer + Subscription.
- Webhooks:
  - subscription.created/updated/deleted
  - invoice.payment_succeeded/failed
- Customer portal session endpoint.
- SMS credits: Checkout Session for packs; credit on payment success.
- Enforcement: payment_failed or canceled -> block sends.

### 9) Limits and Usage
- `/usage/current`: current period usage + credits + overage.
- `/limits`: plan limits.
- Enforcement with stable error codes:
  - LIMIT_USERS_REACHED, LIMIT_CLIENTS_REACHED, SMS_CREDITS_EMPTY, BILLING_REQUIRED.

### 10) GDPR
- Tenant export (async if needed).
- Tenant erase/anonymize.
- Retention by plan: automatic purge of deliveries/history.

### 11) Operations and Security
- `/health`, `/ready`.
- Rate limiting per IP and per tenant.
- HMAC on internal webhooks, SNS/Twilio/Stripe verification.
- Structured logs without sensitive PII.

## Endpoints (Summary + RBAC)
- Auth: `/auth/*` (public for register/login/refresh/forgot/reset; others auth)
- Workspaces: `/workspaces`, `/workspaces/current` (owner/admin)
- Members: `/workspaces/{id}/members/*` (owner/admin)
- Sending domains: `/sending-domains/*` (owner/admin)
- Clients: `/customers/*` (member+)
- Templates: `/templates/*` (member+)
- Reminders: `/reminders/*` (member+)
- Deliveries: `/deliveries/*` (member+)
- Usage/limits: `/usage/current`, `/limits` (member+)
- Billing: `/billing/*` (owner)
- GDPR: `/gdpr/*` (owner/admin)
- Webhooks: `/webhooks/*` (public, signature verified)
- Unsubscribe: `/u/{token}`, `/unsubscribe/{token}` (public)
- Health: `/health`, `/ready` (public)

## States
- Delivery: queued -> sent -> delivered | bounced | complained | failed (optional canceled)
- Reminder: pending -> active -> completed | failed | canceled
- Workspace: trialing | active | past_due | suspended | canceled
- Membership: invited | active | suspended
- Domain: pending | verified | failed
- Billing: trialing | active | past_due | canceled | unpaid

## Error Format
```
{
  "code": "LIMIT_CLIENTS_REACHED",
  "message": "Client limit reached for current plan.",
  "details": { "limit": 200, "current": 200 },
  "traceId": "b3f1..."
}
```

## Plan Limits (Backend Rules)
- Starter (9 EUR/mo): 1 user, 200 clients, email included, SMS add-on only, retention 30 days
- Pro (19 EUR/mo): 3 users, 1,000 clients, optional 10 SMS included, retention 180 days
- Business (39 EUR/mo): 10 users, 5,000 clients, optional 50 SMS included, retention 365-730 days
- SMS add-ons: packs 100/500/1000, or pay-as-you-go (single price for now)

## Events
### External Webhooks
- Stripe: subscription/invoice events
- SES/SNS: delivery, bounce, complaint
- Twilio: message status callbacks

### Internal
- AuditLog entries for CRUD and send actions
- UsageLedger increments on send
