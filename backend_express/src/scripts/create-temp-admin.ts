import bcrypt from 'bcryptjs';
import supabase from '../config/database';

async function createTempAdmin() {
  try {
    console.log('🔧 Creando usuario temporal con profesor existente...');

    // Buscar la persona del profesor
    const { data: profesor } = await supabase
      .from('personas')
      .select('id')
      .eq('correo', 'profesor@colegio.com')
      .single();

    if (!profesor) {
      console.log('❌ No se encontró el profesor');
      return;
    }

    console.log('👨‍🏫 Profesor encontrado, ID:', profesor.id);

    // Hash de la contraseña
    const hashedPassword = await bcrypt.hash('admin123', 10);

    // Intentar insertar usuario directamente (sin RLS)
    try {
      const { data: usuario, error } = await supabase
        .from('usuarios')
        .insert({
          persona_id: profesor.id,
          email: 'profesor@colegio.com',
          password: hashedPassword,
          rol: 'administrador',
          activo: true
        })
        .select()
        .single();

      if (error) {
        console.log('⚠️ Error RLS esperado:', error.message);
        console.log('💡 Necesitas desactivar RLS manualmente en Supabase');
        console.log('🔗 Ve a: https://dhirwwytreumhebccuht.supabase.co/project/default/editor');
        console.log('📝 Ejecuta: ALTER TABLE usuarios DISABLE ROW LEVEL SECURITY;');
        console.log('📝 Luego ejecuta este script de nuevo');
      } else {
        console.log('✅ Usuario creado exitosamente:', usuario);
      }
    } catch (err: any) {
      console.log('⚠️ Error:', err.message);
    }

    // Información para login manual
    console.log('\n📋 Información para crear usuario manualmente:');
    console.log('🆔 persona_id:', profesor.id);
    console.log('📧 email: profesor@colegio.com');
    console.log('🔑 password_hash:', hashedPassword);
    console.log('👨‍💼 rol: administrador');
    console.log('✅ activo: true');

  } catch (error: any) {
    console.error('❌ Error:', error.message);
  }
}

createTempAdmin();