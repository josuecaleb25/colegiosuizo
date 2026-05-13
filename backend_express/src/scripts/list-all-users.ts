import supabase from '../config/database';

async function listAllUsers() {
  try {
    console.log('🔍 Listando TODOS los usuarios...\n');

    // Buscar todos los usuarios sin filtros
    const { data: users, error } = await supabase
      .from('usuarios')
      .select(`
        id,
        email,
        rol,
        activo,
        password,
        persona_id
      `)
      .limit(50);

    if (error) {
      console.error('❌ Error:', error);
      return;
    }

    if (!users || users.length === 0) {
      console.log('⚠️ No hay usuarios en la base de datos');
      return;
    }

    console.log(`✅ Se encontraron ${users.length} usuario(s):\n`);

    for (const user of users) {
      console.log('-----------------------------------');
      console.log('ID:', user.id);
      console.log('Email:', user.email);
      console.log('Rol:', user.rol);
      console.log('Activo:', user.activo);
      console.log('Persona ID:', user.persona_id);
      console.log('Password:', user.password ? (user.password.substring(0, 30) + '...') : 'NULL');
      console.log('');
    }

  } catch (error: any) {
    console.error('❌ Error:', error.message);
  }
}

listAllUsers()
  .then(() => {
    console.log('🎉 Listado completado');
    process.exit(0);
  })
  .catch((error) => {
    console.error('💥 Error:', error);
    process.exit(1);
  });
