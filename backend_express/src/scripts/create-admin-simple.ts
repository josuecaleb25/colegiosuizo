import bcrypt from 'bcryptjs';
import supabase from '../config/database';

async function createAdminUser() {
  try {
    console.log('🔧 Creando usuario administrador...');

    // Desactivar RLS para las tablas necesarias
    console.log('🔓 Desactivando RLS...');
    
    await supabase.rpc('exec_sql', {
      sql: 'ALTER TABLE personas DISABLE ROW LEVEL SECURITY;'
    });
    
    await supabase.rpc('exec_sql', {
      sql: 'ALTER TABLE usuarios DISABLE ROW LEVEL SECURITY;'
    });

    // Hash de la contraseña
    const hashedPassword = await bcrypt.hash('admin123', 10);
    console.log('🔑 Password hash generado');

    // Insertar persona directamente
    const { data: persona, error: personaError } = await supabase
      .from('personas')
      .upsert({
        dni: '12345678',
        nombres: 'Admin',
        apellidos: 'Sistema',
        correo: 'admin@colegio.com'
      }, { 
        onConflict: 'correo',
        ignoreDuplicates: false 
      })
      .select()
      .single();

    if (personaError) {
      console.log('⚠️ Error en persona (puede ser normal si ya existe):', personaError.message);
    }

    // Obtener la persona (por si ya existía)
    const { data: personaExistente } = await supabase
      .from('personas')
      .select('id')
      .eq('correo', 'admin@colegio.com')
      .single();

    const personaId = persona?.id || personaExistente?.id;

    if (!personaId) {
      throw new Error('No se pudo obtener el ID de la persona');
    }

    console.log('👤 Persona ID:', personaId);

    // Insertar usuario
    const { data: usuario, error: usuarioError } = await supabase
      .from('usuarios')
      .upsert({
        persona_id: personaId,
        email: 'admin@colegio.com',
        password: hashedPassword,
        rol: 'administrador',
        activo: true
      }, { 
        onConflict: 'email',
        ignoreDuplicates: false 
      })
      .select()
      .single();

    if (usuarioError) {
      console.log('⚠️ Error en usuario:', usuarioError.message);
      throw usuarioError;
    }

    console.log('✅ Usuario administrador creado/actualizado exitosamente');
    console.log('📧 Email: admin@colegio.com');
    console.log('🔑 Password: admin123');
    console.log('🆔 Usuario ID:', usuario.id);

  } catch (error: any) {
    console.error('❌ Error:', error.message);
    console.error('📋 Detalles:', error);
  }
}

createAdminUser();