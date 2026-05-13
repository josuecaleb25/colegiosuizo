-- Verificar estructura de la tabla codigos_qr
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'codigos_qr'
ORDER BY ordinal_position;

-- Si la tabla no existe o no tiene la columna correcta, ejecuta esto:
-- ALTER TABLE codigos_qr ADD COLUMN IF NOT EXISTS alumno_id UUID REFERENCES alumnos(id);

-- O si la columna se llama diferente, verifica con:
SELECT * FROM codigos_qr LIMIT 1;
