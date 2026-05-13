import bcrypt from 'bcryptjs';
import supabase from '../config/database';

async function createAdmin() {
  try {
    console.log('🔧 Creando usuario administrador...');

    // Verificar si ya existe el usuario admin
    const { data: existingUser } = await supabase
      .from('usuarios')
      .select('id')
      .eq('email', 'admin@colegio.com')
      .limit(1);

    if (existingUser && existingUser.length > 0) {
      console.log('✅ Usuario administrador ya existe');
      return;
    }

    // Verificar si existe la persona
    let { data: persona } = await supabase
      .from('personas')
      .select('id')
      .eq('correo', 'admin@colegio.com')
      .limit(1);

    if (!persona || persona.length === 0) {
      // Crear persona
      const { data: newPersona, error: personaError } = await supabase
        .from('personas')
        .insert({
          dni: '12345678',
          nombres: 'Admin',
          apellidos: 'Sistema',
          correo: 'admin@colegio.com'
        })
        .select()
        .single();

      if (personaError) {
        throw personaError;
      }
      persona = [newPersona];
    }

    // Hash de la contraseña
    const hashedPassword = await bcrypt.hash('admin123', 10);

    // Crear usuario
    const { data: newUser, error: userError } = await supabase
      .from('usuarios')
      .insert({
        persona_id: persona[0].id,
        email: 'admin@colegio.com',
        password: hashedPassword,
        rol: 'administrador',
        activo: true
      })
      .select()
      .single();

    if (userError) {
      throw userError;
    }

    console.log('✅ Usuario administrador creado exitosamente');
    console.log('📧 Email: admin@colegio.com');
    console.log('🔑 Password: admin123');

  } catch (error: any) {
    console.error('❌ Error al crear usuario administrador:', error.message);
    throw error;
  }
}

// Ejecutar si se llama directamente
if (require.main === module) {
  createAdmin()
    .then(() => {
      console.log('🎉 Proceso completado');
      process.exit(0);
    })
    .catch((error) => {
      console.error('💥 Error:', error);
      process.exit(1);
    });
}

export default createAdmin;