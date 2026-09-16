-- Notification identity migration
-- Safe, additive migration. It does not delete students, QR records,
-- enrollments, tokens, or notification history.

BEGIN;

ALTER TABLE device_tokens
  ADD COLUMN IF NOT EXISTS persona_id uuid;

ALTER TABLE notificaciones_historial
  ADD COLUMN IF NOT EXISTS persona_id uuid;

-- Existing mobile accounts are currently represented by the student's
-- account. Preserve that behavior while making the recipient explicit.
UPDATE device_tokens AS dt
SET persona_id = a.persona_id
FROM alumnos AS a
WHERE dt.estudiante_id = a.id
  AND dt.persona_id IS NULL;

UPDATE notificaciones_historial AS nh
SET persona_id = a.persona_id
FROM alumnos AS a
WHERE nh.estudiante_id = a.id
  AND nh.persona_id IS NULL;

-- Stop before creating indexes if any existing row cannot be mapped.
DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM device_tokens
    WHERE estudiante_id IS NOT NULL
      AND persona_id IS NULL
  ) THEN
    RAISE EXCEPTION 'Migration stopped: device_tokens contains unmapped students';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM notificaciones_historial
    WHERE estudiante_id IS NOT NULL
      AND persona_id IS NULL
  ) THEN
    RAISE EXCEPTION 'Migration stopped: notification history contains unmapped students';
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_device_tokens_persona_id
  ON device_tokens (persona_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_device_tokens_persona_token
  ON device_tokens (persona_id, token)
  WHERE persona_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_notificaciones_historial_persona_id
  ON notificaciones_historial (persona_id);

COMMIT;
