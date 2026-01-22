ALTER TABLE users
  ADD COLUMN status text NOT NULL DEFAULT 'active'
    CHECK (status IN ('active','invited','disabled')),
  ADD COLUMN last_login timestamptz;

CREATE TABLE workspaces (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name text NOT NULL,
  plan text NOT NULL CHECK (plan IN ('starter','pro','business')),
  status text NOT NULL CHECK (status IN ('trialing','active','past_due','suspended','canceled')),
  stripe_customer_id text,
  stripe_subscription_id text,
  stripe_price_id text,
  trial_ends_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE workspace_memberships (
  workspace_id uuid NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role text NOT NULL CHECK (role IN ('owner','admin','member')),
  status text NOT NULL DEFAULT 'active' CHECK (status IN ('active','invited','suspended')),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (workspace_id, user_id)
);

CREATE TABLE workspace_invitations (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id uuid NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
  email citext NOT NULL,
  role text NOT NULL CHECK (role IN ('owner','admin','member')),
  token_hash text NOT NULL,
  status text NOT NULL CHECK (status IN ('pending','accepted','revoked','expired')),
  invited_by uuid REFERENCES users(id) ON DELETE SET NULL,
  expires_at timestamptz NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX workspace_invitations_workspace_email_uniq
  ON workspace_invitations(workspace_id, email);

CREATE TABLE workspace_settings (
  workspace_id uuid PRIMARY KEY REFERENCES workspaces(id) ON DELETE CASCADE,
  settings jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE sending_domains (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id uuid NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
  domain text NOT NULL,
  status text NOT NULL CHECK (status IN ('pending','verified','failed')),
  verification_records jsonb NOT NULL DEFAULT '{}'::jsonb,
  last_checked_at timestamptz,
  failure_reason text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (workspace_id, domain)
);

CREATE TABLE unsubscribes (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id uuid NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
  channel text NOT NULL CHECK (channel IN ('email','sms')),
  email text,
  phone text,
  token_hash text NOT NULL UNIQUE,
  status text NOT NULL CHECK (status IN ('pending','confirmed')),
  created_at timestamptz NOT NULL DEFAULT now(),
  confirmed_at timestamptz
);

CREATE TABLE usage_ledger (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id uuid NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
  period text NOT NULL,
  emails_sent int NOT NULL DEFAULT 0,
  sms_sent int NOT NULL DEFAULT 0,
  sms_credits_balance int NOT NULL DEFAULT 0,
  overage_cost_cents int NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (workspace_id, period)
);

CREATE TABLE stripe_events (
  id text PRIMARY KEY,
  workspace_id uuid REFERENCES workspaces(id) ON DELETE SET NULL,
  type text NOT NULL,
  payload jsonb NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE sms_credit_purchases (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id uuid NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
  checkout_session_id text NOT NULL,
  payment_intent_id text,
  credits int NOT NULL,
  amount_cents int NOT NULL,
  status text NOT NULL CHECK (status IN ('pending','succeeded','failed')),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (checkout_session_id)
);

CREATE TABLE user_workspace_map (
  user_id uuid PRIMARY KEY,
  workspace_id uuid NOT NULL
);

INSERT INTO user_workspace_map (user_id, workspace_id)
SELECT u.id, gen_random_uuid()
FROM users u;

INSERT INTO workspaces (id, name, plan, status)
SELECT m.workspace_id, COALESCE(u.name, 'Workspace') || ' Workspace', 'starter', 'active'
FROM users u
JOIN user_workspace_map m ON m.user_id = u.id
ON CONFLICT (id) DO NOTHING;

INSERT INTO workspace_memberships (workspace_id, user_id, role, status)
SELECT m.workspace_id, m.user_id, 'owner', 'active'
FROM user_workspace_map m
ON CONFLICT (workspace_id, user_id) DO NOTHING;

INSERT INTO workspace_settings (workspace_id)
SELECT m.workspace_id
FROM user_workspace_map m
ON CONFLICT (workspace_id) DO NOTHING;

ALTER TABLE customers
  ADD COLUMN workspace_id uuid,
  ADD COLUMN tags text[] NOT NULL DEFAULT ARRAY[]::text[],
  ADD COLUMN locale text,
  ADD COLUMN timezone text,
  ADD COLUMN do_not_email boolean NOT NULL DEFAULT false,
  ADD COLUMN do_not_sms boolean NOT NULL DEFAULT false,
  ADD COLUMN email_bounce_reason text,
  ADD COLUMN email_bounce_at timestamptz,
  ADD COLUMN email_complaint_at timestamptz;

UPDATE customers c
SET workspace_id = m.workspace_id
FROM user_workspace_map m
WHERE c.owner_user_id = m.user_id;
ALTER TABLE customers ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE customers
  ADD CONSTRAINT customers_workspace_fk FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;

DROP INDEX IF EXISTS customers_owner_email_uniq;
CREATE UNIQUE INDEX customers_workspace_email_uniq
  ON customers(workspace_id, email) WHERE email IS NOT NULL;
CREATE INDEX customers_workspace_idx ON customers(workspace_id);

ALTER TABLE customer_consent_events ADD COLUMN workspace_id uuid;
UPDATE customer_consent_events e
SET workspace_id = c.workspace_id
FROM customers c
WHERE e.customer_id = c.id;
ALTER TABLE customer_consent_events ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE customer_consent_events
  ADD CONSTRAINT consent_events_workspace_fk FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;
CREATE INDEX consent_events_workspace_idx ON customer_consent_events(workspace_id);

ALTER TABLE templates
  ADD COLUMN workspace_id uuid,
  ADD COLUMN variables jsonb,
  ADD COLUMN version int NOT NULL DEFAULT 1;
UPDATE templates t
SET workspace_id = m.workspace_id
FROM user_workspace_map m
WHERE t.owner_user_id = m.user_id;
ALTER TABLE templates ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE templates
  ADD CONSTRAINT templates_workspace_fk FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;
CREATE INDEX templates_workspace_idx ON templates(workspace_id);

ALTER TABLE reminders
  ADD COLUMN workspace_id uuid,
  ADD COLUMN schedule_type text NOT NULL DEFAULT 'legacy'
    CHECK (schedule_type IN ('legacy','once','interval','cron')),
  ADD COLUMN schedule_expression text,
  ADD COLUMN schedule_interval_minutes int,
  ADD COLUMN scheduled_at timestamptz,
  ADD COLUMN timezone text;
UPDATE reminders r
SET workspace_id = m.workspace_id
FROM user_workspace_map m
WHERE r.owner_user_id = m.user_id;
ALTER TABLE reminders ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE reminders
  ADD CONSTRAINT reminders_workspace_fk FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;
CREATE INDEX reminders_workspace_idx ON reminders(workspace_id);

ALTER TABLE reminders DROP CONSTRAINT IF EXISTS reminders_status_check;
ALTER TABLE reminders
  ADD CONSTRAINT reminders_status_check
  CHECK (status IN ('pending','active','completed','failed','canceled'));

ALTER TABLE transactions ADD COLUMN workspace_id uuid;
UPDATE transactions t
SET workspace_id = r.workspace_id
FROM reminders r
WHERE t.reminder_id = r.id;
ALTER TABLE transactions ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE transactions
  ADD CONSTRAINT transactions_workspace_fk FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;
CREATE INDEX transactions_workspace_idx ON transactions(workspace_id);

ALTER TABLE deliveries
  ADD COLUMN workspace_id uuid,
  ADD COLUMN reminder_id uuid,
  ADD COLUMN recipient text,
  ADD COLUMN payload_snapshot jsonb,
  ADD COLUMN queued_at timestamptz,
  ADD COLUMN sent_at timestamptz,
  ADD COLUMN delivered_at timestamptz,
  ADD COLUMN bounced_at timestamptz,
  ADD COLUMN complained_at timestamptz,
  ADD COLUMN failed_at timestamptz,
  ADD COLUMN canceled_at timestamptz,
  ADD COLUMN dedupe_key text;

UPDATE deliveries d
SET workspace_id = r.workspace_id,
    reminder_id = r.id
FROM transactions t
JOIN reminders r ON r.id = t.reminder_id
WHERE d.transaction_id = t.id;

ALTER TABLE deliveries ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE deliveries
  ADD CONSTRAINT deliveries_workspace_fk FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;
ALTER TABLE deliveries
  ADD CONSTRAINT deliveries_reminder_fk FOREIGN KEY (reminder_id) REFERENCES reminders(id) ON DELETE SET NULL;

ALTER TABLE deliveries DROP CONSTRAINT IF EXISTS deliveries_status_check;
ALTER TABLE deliveries
  ADD CONSTRAINT deliveries_status_check
  CHECK (status IN ('queued','sent','delivered','bounced','complained','failed','canceled'));

CREATE INDEX deliveries_workspace_idx ON deliveries(workspace_id);
CREATE INDEX deliveries_reminder_idx ON deliveries(reminder_id);
CREATE UNIQUE INDEX deliveries_workspace_dedupe_uniq
  ON deliveries(workspace_id, dedupe_key) WHERE dedupe_key IS NOT NULL;

ALTER TABLE gdpr_requests ADD COLUMN workspace_id uuid;
UPDATE gdpr_requests g
SET workspace_id = c.workspace_id
FROM customers c
WHERE g.customer_id = c.id;
ALTER TABLE gdpr_requests ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE gdpr_requests
  ADD CONSTRAINT gdpr_requests_workspace_fk FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;

ALTER TABLE compliance_assessments ADD COLUMN workspace_id uuid;
UPDATE compliance_assessments ca
SET workspace_id = m.workspace_id
FROM user_workspace_map m
WHERE ca.owner_user_id = m.user_id;
ALTER TABLE compliance_assessments ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE compliance_assessments
  ADD CONSTRAINT compliance_assessments_workspace_fk FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;

ALTER TABLE audit_events ADD COLUMN workspace_id uuid;
UPDATE audit_events ae
SET workspace_id = m.workspace_id
FROM user_workspace_map m
WHERE ae.actor_user_id = m.user_id;
UPDATE audit_events ae
SET workspace_id = m.workspace_id
FROM user_workspace_map m
WHERE ae.workspace_id IS NULL
  AND ae.entity_type IN ('user','user_settings','privacy_settings')
  AND ae.entity_id = m.user_id::text;
UPDATE audit_events ae
SET workspace_id = c.workspace_id
FROM customers c
WHERE ae.workspace_id IS NULL
  AND ae.entity_type = 'customer'
  AND ae.entity_id = c.id::text;
UPDATE audit_events ae
SET workspace_id = t.workspace_id
FROM templates t
WHERE ae.workspace_id IS NULL
  AND ae.entity_type = 'template'
  AND ae.entity_id = t.id::text;
UPDATE audit_events ae
SET workspace_id = r.workspace_id
FROM reminders r
WHERE ae.workspace_id IS NULL
  AND ae.entity_type = 'reminder'
  AND ae.entity_id = r.id::text;
UPDATE audit_events ae
SET workspace_id = tr.workspace_id
FROM transactions tr
WHERE ae.workspace_id IS NULL
  AND ae.entity_type = 'transaction'
  AND ae.entity_id = tr.id::text;
UPDATE audit_events ae
SET workspace_id = d.workspace_id
FROM deliveries d
WHERE ae.workspace_id IS NULL
  AND ae.entity_type = 'delivery'
  AND ae.entity_id = d.id::text;
UPDATE audit_events ae
SET workspace_id = g.workspace_id
FROM gdpr_requests g
WHERE ae.workspace_id IS NULL
  AND ae.entity_type = 'gdpr_request'
  AND ae.entity_id = g.id::text;
UPDATE audit_events ae
SET workspace_id = ca.workspace_id
FROM compliance_assessments ca
WHERE ae.workspace_id IS NULL
  AND ae.entity_type = 'compliance_assessment'
  AND ae.entity_id = ca.id::text;
UPDATE audit_events
SET workspace_id = (SELECT id FROM workspaces ORDER BY created_at ASC LIMIT 1)
WHERE workspace_id IS NULL;
ALTER TABLE audit_events ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE audit_events
  ADD CONSTRAINT audit_events_workspace_fk FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;
CREATE INDEX audit_events_workspace_idx ON audit_events(workspace_id);

CREATE TRIGGER workspaces_updated_at_trg
BEFORE UPDATE ON workspaces
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER workspace_memberships_updated_at_trg
BEFORE UPDATE ON workspace_memberships
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER workspace_invitations_updated_at_trg
BEFORE UPDATE ON workspace_invitations
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER workspace_settings_updated_at_trg
BEFORE UPDATE ON workspace_settings
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER sending_domains_updated_at_trg
BEFORE UPDATE ON sending_domains
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER usage_ledger_updated_at_trg
BEFORE UPDATE ON usage_ledger
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER sms_credit_purchases_updated_at_trg
BEFORE UPDATE ON sms_credit_purchases
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TABLE user_workspace_map;
