import supabase from '../config/database';

async function verificarActitudes() {
  try {
    console.log('🔍 Verificando evaluaciones "Actitudes"...\n');

    // 1. Buscar todas las evaluaciones con nombre "Actitudes"
    const { data: actitudes, error: errorActitudes } = await supabase
      .from('evaluaciones')
      .select('*')
      .eq('nombre', 'Actitudes')
      .order('created_at', { ascending: false });

    if (errorActitudes) {
      console.error('❌ Error:', errorActitudes);
      return;
    }

    console.log(`📊 Total de evaluaciones "Actitudes": ${actitudes?.length || 0}\n`);

    if (actitudes && actitudes.length > 0) {
      actitudes.forEach((act, index) => {
        console.log(`${index + 1}. ID: ${act.id}`);
        console.log(`   Asignación: ${act.asignacion_id}`);
        console.log(`   Orden: ${act.orden}`);
        console.log(`   Peso: ${act.peso}`);
        console.log(`   Activo: ${act.activo}`);
        console.log(`   Creado: ${act.created_at}`);
        console.log('');
      });
    }

    // 2. Verificar específicamente para la asignación de Matemática 1roA
    const asignacionMatematica = '62522628-5ce3-41bd-8f68-3d5f89d90a3a';
    
    console.log(`\n🔍 Verificando evaluaciones para asignación ${asignacionMatematica}...\n`);

    const { data: evalMatematica, error: errorMat } = await supabase
      .from('evaluaciones')
      .select('*')
      .eq('asignacion_id', asignacionMatematica)
      .order('orden', { ascending: true });

    if (errorMat) {
      console.error('❌ Error:', errorMat);
      return;
    }

    console.log(`📊 Total de evaluaciones: ${evalMatematica?.length || 0}\n`);

    if (evalMatematica && evalMatematica.length > 0) {
      evalMatematica.forEach((ev, index) => {
        console.log(`${index + 1}. ${ev.nombre} (orden: ${ev.orden}, activo: ${ev.activo})`);
      });
    }

    // 3. Verificar si existe "Actitudes" para esta asignación
    const tieneActitudes = evalMatematica?.some(ev => ev.nombre === 'Actitudes');
    
    if (tieneActitudes) {
      console.log('\n✅ La asignación SÍ tiene "Actitudes"');
    } else {
      console.log('\n❌ La asignación NO tiene "Actitudes"');
      console.log('\n💡 Puedo agregar "Actitudes" a esta asignación si quieres.');
    }

  } catch (error) {
    console.error('❌ Error:', error);
  }
}

verificarActitudes()
  .then(() => {
    console.log('\n✅ Verificación completada');
    process.exit(0);
  })
  .catch((error) => {
    console.error('❌ Error:', error);
    process.exit(1);
  });
