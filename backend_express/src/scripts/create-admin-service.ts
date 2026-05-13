import bcrypt from 'bcryptjs';
import { createClient } from '@supabase/supabase-js';
import dotenv from 'dotenv';

dotenv.config();

async function createAdminWithServiceRole() {
  try {
    console.log('🔧 Creando usuario administrador...');

    // Usar SERVICE_ROLE_KEY para bypass RLS
    const supabaseUrl = process.env.SUPABASE_URL!;
    const serviceRoleKey = process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.SUPABASE_ANON_KEY!;
    
    const supabase = createClient(supabaseUrl, serviceRoleKey, {
      auth: {
        autoRefreshToken: false,
        persistSession: false
      }
    });

    console.log('🔗 Conectado a Supabase');

    // Verificar si ya existe el usuario admin
    const { data: existingUser } = await supabase
      .from('usuarios')
      .select('id, email')
      .eq('email', 'admin@colegio.com')
      .limit(1);

    if (existingUser && existingUser.length > 0) {
      console.log('✅ Usuario administrador ya existe:', existingUser[0]);
      
      // Actualizar la contraseña
      const hashedPassword = await bcrypt.hash('admin123', 10);
      const { error: updateError } = await supabase
        .from('usuarios')
        .update({ password: hashedPassword, activo: true })
        .eq('email', 'admin@colegio.com');
      
      if (updateError) {
        console.log('⚠️ Error al actualizar contraseña:', updateError.message);
      } else {
        console.log('✅ Contraseña actualizada');
      }
      
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
      console.log('👤 Creando persona...');
      const { data: newPersona, error: personaError } = await supabase
        .from('personas')
        .insert({
          dni: '12345678',
          nombres: 'Admin',
          apellidos: 'Sistema',
          correo: 'admin@colegio.com',
          telefono: '999999999'
        })
        .select()
        .single();

      if (personaError) {
        console.error('❌ Error al crear persona:', personaError);
        throw personaError;
      }
      persona = [newPersona];
      console.log('✅ Persona creada:', newPersona);
    }

    // Hash de la contraseña
    const hashedPassword = await bcrypt.hash('admin123', 10);
    console.log('🔐 Password hasheado');

    // Crear usuario
    console.log('👨‍💼 Creando usuario...');
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
      console.error('❌ Error al crear usuario:', userError);
      throw userError;
    }

    console.log('✅ Usuario administrador creado exitosamente');
    console.log('📧 Email: admin@colegio.com');
    console.log('🔑 Password: admin123');
    console.log('👤 Usuario:', newUser);

  } catch (error: any) {
    console.error('❌ Error:', error.message);
    console.error('Detalles:', error);
    process.exit(1);
  }
}

createAdminWithServiceRole()
  .then(() => {
    console.log('🎉 Proceso completado');
    process.exit(0);
  })
  .catch((error) => {
    console.error('💥 Error fatal:', error);
    process.exit(1);
  });
