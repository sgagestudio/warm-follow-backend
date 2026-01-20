CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS citext;

CREATE TABLE users (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  email citext UNIQUE NOT NULL,
  name text NOT NULL,
  provider text NOT NULL CHECK (provider IN ('email','google')),
  auth_type text NOT NULL CHECK (auth_type IN ('jwt','oauth')),
  password_hash text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE oauth_identities (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  provider text NOT NULL CHECK (provider IN ('google')),
  provider_subject text NOT NULL,
  email text,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (provider, provider_subject),
  UNIQUE (user_id, provider)
);

CREATE TABLE refresh_tokens (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash text NOT NULL,
  issued_at timestamptz NOT NULL DEFAULT now(),
  expires_at timestamptz NOT NULL,
  revoked_at timestamptz,
  ip inet,
  user_agent text
);

CREATE TABLE customers (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  first_name text,
  last_name text,
  email text,
  phone text,
  consent_status text NOT NULL CHECK (consent_status IN ('granted','pending','revoked')),
  consent_date date,
  consent_source text NOT NULL DEFAULT 'web',
  consent_channels text[] NOT NULL DEFAULT ARRAY['email'],
  consent_proof_ref text,
  is_erased boolean NOT NULL DEFAULT false,
  erased_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CHECK (
    (is_erased = false AND first_name IS NOT NULL AND last_name IS NOT NULL AND email IS NOT NULL)
    OR is_erased = true
  )
);

CREATE UNIQUE INDEX customers_owner_email_uniq
  ON customers(owner_user_id, email) WHERE email IS NOT NULL;

CREATE INDEX customers_owner_idx ON customers(owner_user_id);
CREATE INDEX customers_email_idx ON customers(email);

CREATE TABLE customer_consent_events (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  customer_id uuid NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
  status text NOT NULL CHECK (status IN ('granted','pending','revoked')),
  channels text[] NOT NULL,
  proof_ref text,
  source text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE templates (
  id bigserial PRIMARY KEY,
  owner_user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name text NOT NULL,
  subject text,
  content text NOT NULL,
  channel text NOT NULL CHECK (channel IN ('email','sms','both')),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE reminders (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  template_id bigint NOT NULL REFERENCES templates(id) ON DELETE RESTRICT,
  channel text NOT NULL CHECK (channel IN ('email','sms','both')),
  frequency text NOT NULL CHECK (frequency IN ('once','daily','weekly','monthly')),
  scheduled_time time NOT NULL,
  scheduled_date date,
  next_run timestamptz NOT NULL,
  status text NOT NULL CHECK (status IN ('pending','active','completed','failed')),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE reminder_recipients (
  reminder_id uuid NOT NULL REFERENCES reminders(id) ON DELETE CASCADE,
  customer_id uuid NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
  PRIMARY KEY (reminder_id, customer_id)
);

CREATE TABLE transactions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  reminder_id uuid NOT NULL REFERENCES reminders(id) ON DELETE CASCADE,
  triggered_by uuid REFERENCES users(id) ON DELETE SET NULL,
  status text NOT NULL CHECK (status IN ('queued','running','done','failed')),
  request_id text NOT NULL,
  idempotency_key text,
  started_at timestamptz NOT NULL DEFAULT now(),
  finished_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX transactions_reminder_idx ON transactions(reminder_id);
CREATE INDEX transactions_request_idx ON transactions(request_id);

CREATE TABLE deliveries (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  transaction_id uuid NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
  customer_id uuid REFERENCES customers(id) ON DELETE SET NULL,
  channel text NOT NULL CHECK (channel IN ('email','sms')),
  provider_message_id text,
  status text NOT NULL CHECK (status IN ('sent','delivered','bounced','failed')),
  error_code text,
  error_message text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX deliveries_tx_idx ON deliveries(transaction_id);
CREATE INDEX deliveries_customer_idx ON deliveries(customer_id);

CREATE TABLE user_settings (
  user_id uuid PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  settings jsonb NOT NULL DEFAULT '{}',
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE privacy_settings (
  user_id uuid PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  data_retention_enabled boolean NOT NULL DEFAULT true,
  anonymization_enabled boolean NOT NULL DEFAULT false,
  audit_logs_enabled boolean NOT NULL DEFAULT true,
  encryption_enabled boolean NOT NULL DEFAULT true,
  retention_days int,
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE gdpr_requests (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  customer_id uuid NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
  requested_by uuid REFERENCES users(id) ON DELETE SET NULL,
  type text NOT NULL CHECK (type IN ('export','erase')),
  status text NOT NULL CHECK (status IN ('queued','processing','done','failed')),
  result_location text,
  created_at timestamptz NOT NULL DEFAULT now(),
  completed_at timestamptz
);

CREATE TABLE audit_events (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  actor_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
  entity_type text NOT NULL,
  entity_id text NOT NULL,
  action text NOT NULL,
  before jsonb,
  after jsonb,
  request_id text NOT NULL,
  ip inet,
  user_agent text,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX audit_entity_idx ON audit_events(entity_type, entity_id);
CREATE INDEX audit_request_idx ON audit_events(request_id);

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER users_updated_at_trg
BEFORE UPDATE ON users
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER customers_updated_at_trg
BEFORE UPDATE ON customers
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER templates_updated_at_trg
BEFORE UPDATE ON templates
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER reminders_updated_at_trg
BEFORE UPDATE ON reminders
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER deliveries_updated_at_trg
BEFORE UPDATE ON deliveries
FOR EACH ROW EXECUTE FUNCTION set_updated_at();
