import bcrypt from 'bcryptjs';
import supabase from '../config/database';

async function crearAdminJoshue() {
  try {
    const email = 'ochoareyesjosue@gmail.com';
    const password = 'ochoa123';

    console.log('🔧 Creando usuario administrador...');

    // Verificar si ya existe la persona
    let { data: persona } = await supabase
      .from('personas')
      .select('id')
      .eq('correo', email)
      .maybeSingle();

    if (persona) {
      console.log('👤 Persona ya existe (ID:', persona.id, ')');
    } else {
      const { data: newPersona, error: personaError } = await supabase
        .from('personas')
        .insert({
          dni: null,
          nombres: 'Joshue',
          apellidos: 'Ochoa Reyes',
          correo: email
        })
        .select()
        .single();

      if (personaError) throw personaError;
      persona = newPersona;
      console.log('✅ Persona creada (ID:', persona.id, ')');
    }

    // Verificar si ya existe el usuario
    const { data: existing } = await supabase
      .from('usuarios')
      .select('id')
      .eq('email', email)
      .maybeSingle();

    if (existing) {
      console.log('ℹ️ El usuario ya existe (ID:', existing.id, ')');
    }

    // Hash de la contraseña
    const hashedPassword = await bcrypt.hash(password, 10);

    // Insertar o actualizar el usuario
    const { data: usuario, error: usuarioError } = await supabase
      .from('usuarios')
      .upsert({
        persona_id: persona.id,
        email,
        password: hashedPassword,
        rol: 'administrador',
        activo: true
      }, { onConflict: 'email' })
      .select()
      .single();

    if (usuarioError) throw usuarioError;

    console.log('✅ Usuario administrador creado/actualizado exitosamente');
    console.log('📧 Email: ' + email);
    console.log('🔑 Password: ' + password);
    console.log('🆔 Usuario ID:', usuario.id);

  } catch (error: any) {
    console.error('❌ Error:', error.message);
  }
}

crearAdminJoshue();
