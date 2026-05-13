import supabase from '../config/database';

async function checkTables() {
  try {
    console.log('🔍 Verificando tablas...\n');

    // Verificar tabla personas
    console.log('📋 Tabla PERSONAS:');
    const { data: personas, error: errorPersonas, count: countPersonas } = await supabase
      .from('personas')
      .select('*', { count: 'exact' })
      .limit(5);

    if (errorPersonas) {
      console.log('❌ Error:', errorPersonas.message);
    } else {
      console.log(`✅ ${countPersonas || 0} registros encontrados`);
      if (personas && personas.length > 0) {
        console.log('Primeros registros:', personas);
      }
    }
    console.log('');

    // Verificar tabla usuarios
    console.log('📋 Tabla USUARIOS:');
    const { data: usuarios, error: errorUsuarios, count: countUsuarios } = await supabase
      .from('usuarios')
      .select('*', { count: 'exact' })
      .limit(5);

    if (errorUsuarios) {
      console.log('❌ Error:', errorUsuarios.message);
    } else {
      console.log(`✅ ${countUsuarios || 0} registros encontrados`);
      if (usuarios && usuarios.length > 0) {
        console.log('Primeros registros:', usuarios);
      }
    }
    console.log('');

    // Verificar tabla alumnos
    console.log('📋 Tabla ALUMNOS:');
    const { data: alumnos, error: errorAlumnos, count: countAlumnos } = await supabase
      .from('alumnos')
      .select('*', { count: 'exact' })
      .limit(5);

    if (errorAlumnos) {
      console.log('❌ Error:', errorAlumnos.message);
    } else {
      console.log(`✅ ${countAlumnos || 0} registros encontrados`);
      if (alumnos && alumnos.length > 0) {
        console.log('Primeros registros:', alumnos.slice(0, 2));
      }
    }

  } catch (error: any) {
    console.error('❌ Error:', error.message);
  }
}

checkTables()
  .then(() => {
    console.log('🎉 Verificación completada');
    process.exit(0);
  })
  .catch((error) => {
    console.error('💥 Error:', error);
    process.exit(1);
  });
