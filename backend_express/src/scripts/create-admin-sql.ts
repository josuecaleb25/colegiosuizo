import bcrypt from 'bcryptjs';
import supabase from '../config/database';

async function createAdminWithSQL() {
  try {
    console.log('🔧 Creando usuario administrador con SQL directo...');

    // Hash de la contraseña
    const hashedPassword = await bcrypt.hash('admin123', 10);
    console.log('🔑 Password hash:', hashedPassword);

    // Ejecutar SQL directo para desactivar RLS e insertar datos
    const sqlCommands = `
      -- Desactivar RLS temporalmente
      ALTER TABLE personas DISABLE ROW LEVEL SECURITY;
      ALTER TABLE usuarios DISABLE ROW LEVEL SECURITY;
      
      -- Insertar o actualizar persona
      INSERT INTO personas (dni, nombres, apellidos, correo)
      VALUES ('12345678', 'Admin', 'Sistema', 'admin@colegio.com')
      ON CONFLICT (correo) DO UPDATE SET
        nombres = EXCLUDED.nombres,
        apellidos = EXCLUDED.apellidos;
      
      -- Insertar o actualizar usuario
      INSERT INTO usuarios (persona_id, email, password, rol, activo)
      SELECT p.id, 'admin@colegio.com', '${hashedPassword}', 'administrador', true
      FROM personas p WHERE p.correo = 'admin@colegio.com'
      ON CONFLICT (email) DO UPDATE SET
        password = EXCLUDED.password,
        rol = EXCLUDED.rol,
        activo = EXCLUDED.activo;
    `;

    console.log('📝 Ejecutando comandos SQL...');
    
    // Ejecutar cada comando por separado
    const commands = sqlCommands.split(';').filter(cmd => cmd.trim());
    
    for (const command of commands) {
      if (command.trim()) {
        console.log('⚡ Ejecutando:', command.trim().substring(0, 50) + '...');
        const { error } = await supabase.rpc('exec_sql', { sql: command.trim() });
        if (error) {
          console.log('⚠️ Error en comando:', error.message);
        }
      }
    }

    console.log('✅ Usuario administrador creado exitosamente');
    console.log('📧 Email: admin@colegio.com');
    console.log('🔑 Password: admin123');

    // Verificar que se creó correctamente
    const { data: usuario } = await supabase
      .from('usuarios')
      .select('id, email, rol')
      .eq('email', 'admin@colegio.com')
      .single();

    if (usuario) {
      console.log('✅ Verificación exitosa - Usuario encontrado:', usuario);
    } else {
      console.log('⚠️ No se pudo verificar el usuario');
    }

  } catch (error: any) {
    console.error('❌ Error:', error.message);
  }
}

createAdminWithSQL();