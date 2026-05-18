import supabase from '../config/database';

async function verificarUsuarios() {
  try {
    console.log('🔍 Verificando usuarios en la base de datos...\n');

    // Obtener todos los usuarios
    const { data: usuarios, error } = await supabase
      .from('usuarios')
      .select(`
        id,
        email,
        rol,
        activo,
        personas (
          nombres,
          apellidos,
          dni
        )
      `)
      .order('email');

    if (error) {
      console.error('❌ Error al consultar usuarios:', error);
      return;
    }

    if (!usuarios || usuarios.length === 0) {
      console.log('❌ No hay usuarios en la base de datos');
      return;
    }

    console.log(`✅ Total de usuarios encontrados: ${usuarios.length}\n`);

    // Agrupar por rol
    const porRol = usuarios.reduce((acc: any, user: any) => {
      if (!acc[user.rol]) acc[user.rol] = [];
      acc[user.rol].push(user);
      return acc;
    }, {});

    // Mostrar por rol
    Object.keys(porRol).forEach(rol => {
      console.log(`\n📋 ${rol.toUpperCase()} (${porRol[rol].length}):`);
      porRol[rol].forEach((user: any) => {
        const nombre = user.personas 
          ? `${user.personas.nombres} ${user.personas.apellidos}` 
          : 'Sin persona';
        const estado = user.activo ? '✅' : '❌';
        console.log(`  ${estado} ${user.email} - ${nombre}`);
      });
    });

    console.log('\n' + '='.repeat(60));
    console.log('📊 RESUMEN:');
    Object.keys(porRol).forEach(rol => {
      console.log(`  ${rol}: ${porRol[rol].length}`);
    });

  } catch (error) {
    console.error('❌ Error:', error);
  }
}

// Ejecutar
verificarUsuarios()
  .then(() => {
    console.log('\n✅ Verificación completada');
    process.exit(0);
  })
  .catch((error) => {
    console.error('💥 Error:', error);
    process.exit(1);
  });
