-- Make the token conflict target compatible with PostgREST upsert.
-- The previous index was partial, so ON CONFLICT (persona_id, token)
-- could not infer it even though all existing rows have persona_id.

BEGIN;

DROP INDEX IF EXISTS uq_device_tokens_persona_token;

CREATE UNIQUE INDEX IF NOT EXISTS uq_device_tokens_persona_token
  ON device_tokens (persona_id, token);

COMMIT;
