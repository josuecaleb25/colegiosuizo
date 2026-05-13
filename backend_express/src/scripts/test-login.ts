import bcrypt from 'bcryptjs';
import supabase from '../config/database';

async function testLogin() {
  try {
    const email = 'admin@colegio.com';
    const password = 'admin123';

    console.log('🔍 Probando login con:', email);
    console.log('🔑 Password:', password);
    console.log('');

    // Buscar usuario (sin filtro de activo primero)
    const { data: users, error } = await supabase
      .from('usuarios')
      .select(`
        *,
        personas!inner (
          nombres,
          apellidos
        )
      `)
      .eq('email', email)
      .limit(1);

    if (error) {
      console.error('❌ Error al buscar usuario:', error);
      return;
    }

    if (!users || users.length === 0) {
      console.log('❌ Usuario no encontrado');
      return;
    }

    const user = users[0];
    console.log('✅ Usuario encontrado:');
    console.log('  - ID:', user.id);
    console.log('  - Email:', user.email);
    console.log('  - Rol:', user.rol);
    console.log('  - Activo:', user.activo);
    console.log('  - Nombre:', `${user.personas.nombres} ${user.personas.apellidos}`);
    console.log('  - Password hash:', user.password);
    console.log('');

    // Verificar contraseña
    console.log('🔐 Verificando contraseña...');
    const isValidPassword = await bcrypt.compare(password, user.password);
    
    if (isValidPassword) {
      console.log('✅ ¡Contraseña correcta!');
      console.log('');
      console.log('🎉 Login exitoso');
    } else {
      console.log('❌ Contraseña incorrecta');
      console.log('');
      console.log('💡 Solución: Actualizar el password hash en la base de datos');
      
      // Generar nuevo hash
      const newHash = await bcrypt.hash(password, 10);
      console.log('');
      console.log('📝 Ejecuta este SQL en Supabase:');
      console.log('');
      console.log(`UPDATE usuarios SET password = '${newHash}' WHERE email = '${email}';`);
    }

  } catch (error: any) {
    console.error('❌ Error:', error.message);
  }
}

testLogin()
  .then(() => {
    console.log('');
    console.log('🎉 Test completado');
    process.exit(0);
  })
  .catch((error) => {
    console.error('💥 Error:', error);
    process.exit(1);
  });
