import supabase from '../config/database';

async function checkUsers() {
  try {
    console.log('🔍 Verificando usuarios existentes...');

    // Listar todos los usuarios
    const { data: usuarios, error } = await supabase
      .from('usuarios')
      .select(`
        id,
        email,
        rol,
        activo,
        personas!inner (
          nombres,
          apellidos,
          correo
        )
      `);

    if (error) {
      console.error('❌ Error al consultar usuarios:', error.message);
      return;
    }

    console.log('📊 Usuarios encontrados:', usuarios?.length || 0);
    
    if (usuarios && usuarios.length > 0) {
      usuarios.forEach((user: any, index: number) => {
        console.log(`\n👤 Usuario ${index + 1}:`);
        console.log(`   📧 Email: ${user.email}`);
        console.log(`   👨‍💼 Rol: ${user.rol}`);
        console.log(`   ✅ Activo: ${user.activo}`);
        console.log(`   📛 Nombre: ${user.personas.nombres} ${user.personas.apellidos}`);
      });
    } else {
      console.log('⚠️ No se encontraron usuarios');
    }

    // También verificar personas
    const { data: personas } = await supabase
      .from('personas')
      .select('id, nombres, apellidos, correo')
      .limit(5);

    console.log('\n👥 Primeras 5 personas:');
    personas?.forEach((persona: any, index: number) => {
      console.log(`   ${index + 1}. ${persona.nombres} ${persona.apellidos} (${persona.correo})`);
    });

  } catch (error: any) {
    console.error('❌ Error:', error.message);
  }
}

checkUsers();