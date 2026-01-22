CREATE TABLE onboarding_state (
  workspace_id uuid PRIMARY KEY REFERENCES workspaces(id) ON DELETE CASCADE,
  steps jsonb NOT NULL DEFAULT '{}'::jsonb,
  current_step text,
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TRIGGER onboarding_state_updated_at_trg
BEFORE UPDATE ON onboarding_state
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
