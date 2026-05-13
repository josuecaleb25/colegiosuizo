import supabase from '../config/database';
import bcrypt from 'bcryptjs';

async function checkAdmin() {
  try {
    console.log('🔍 Buscando usuarios en la base de datos...\n');

    // Buscar todos los usuarios
    const { data: usuarios, error } = await supabase
      .from('usuarios')
      .select(`
        id,
        email,
        rol,
        activo,
        password,
        personas!inner (
          nombres,
          apellidos
        )
      `)
      .limit(10);

    if (error) {
      console.error('❌ Error al buscar usuarios:', error.message);
      return;
    }

    if (!usuarios || usuarios.length === 0) {
      console.log('⚠️ No se encontraron usuarios en la base de datos');
      return;
    }

    console.log(`✅ Se encontraron ${usuarios.length} usuario(s):\n`);

    for (const user of usuarios) {
      console.log('-----------------------------------');
      console.log('📧 Email:', user.email);
      console.log('👤 Nombre:', `${user.personas.nombres} ${user.personas.apellidos}`);
      console.log('🎭 Rol:', user.rol);
      console.log('✓ Activo:', user.activo);
      console.log('🔑 Password hash:', user.password?.substring(0, 20) + '...');
      
      // Verificar si la contraseña 'admin123' funciona
      if (user.password) {
        const isValid = await bcrypt.compare('admin123', user.password);
        console.log('🔐 Password "admin123" válido:', isValid ? '✅ SÍ' : '❌ NO');
      }
      console.log('');
    }

    // Buscar específicamente admin@colegio.com
    console.log('\n🔍 Buscando admin@colegio.com específicamente...');
    const { data: admin } = await supabase
      .from('usuarios')
      .select('*')
      .eq('email', 'admin@colegio.com')
      .single();

    if (admin) {
      console.log('✅ Usuario admin encontrado:', admin);
    } else {
      console.log('❌ Usuario admin@colegio.com NO encontrado');
    }

  } catch (error: any) {
    console.error('❌ Error:', error.message);
  }
}

checkAdmin()
  .then(() => {
    console.log('\n🎉 Verificación completada');
    process.exit(0);
  })
  .catch((error) => {
    console.error('💥 Error:', error);
    process.exit(1);
  });
