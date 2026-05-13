import bcrypt from 'bcryptjs';

async function generateHash() {
  const password = 'admin123';
  const hash = await bcrypt.hash(password, 10);
  
  console.log('Password:', password);
  console.log('Hash:', hash);
  console.log('\nSQL para insertar:');
  console.log(`
-- Paso 1: Crear la persona
INSERT INTO personas (dni, nombres, apellidos, correo, telefono)
VALUES ('12345678', 'Admin', 'Sistema', 'admin@colegio.com', '999999999')
ON CONFLICT (correo) DO UPDATE SET
  nombres = EXCLUDED.nombres,
  apellidos = EXCLUDED.apellidos;

-- Paso 2: Crear el usuario
INSERT INTO usuarios (persona_id, email, password, rol, activo)
SELECT 
  p.id, 
  'admin@colegio.com', 
  '${hash}',
  'administrador', 
  true
FROM personas p 
WHERE p.correo = 'admin@colegio.com'
ON CONFLICT (email) DO UPDATE SET
  password = EXCLUDED.password,
  rol = EXCLUDED.rol,
  activo = EXCLUDED.activo;

-- Paso 3: Verificar
SELECT u.id, u.email, u.rol, p.nombres, p.apellidos
FROM usuarios u
INNER JOIN personas p ON u.persona_id = p.id
WHERE u.email = 'admin@colegio.com';
  `);
}

generateHash();
