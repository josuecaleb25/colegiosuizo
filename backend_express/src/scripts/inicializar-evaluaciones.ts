import supabase from '../config/database';

/**
 * Script para inicializar evaluaciones por defecto en todos los cursos
 * Crea las 6 evaluaciones estándar y genera calificaciones para todos los alumnos
 */

const evaluacionesDefault = [
  { nombre: 'Actitudes', peso: 1, orden: 1, activo: true },
  { nombre: 'Participacion', peso: 1, orden: 2, activo: true },
  { nombre: 'Proyecto', peso: 1, orden: 3, activo: true },
  { nombre: 'Examen I', peso: 1, orden: 4, activo: true },
  { nombre: 'Examen II', peso: 1, orden: 5, activo: true },
  { nombre: 'Examen final', peso: 1, orden: 6, activo: true }
];

async function inicializarEvaluaciones() {
  try {
    console.log('🚀 Iniciando proceso de inicialización de evaluaciones...\n');

    // 1. Obtener todas las asignaciones (profesor-curso-sección)
    const { data: asignaciones, error: errorAsignaciones } = await supabase
      .from('asignaciones')
      .select(`
        id,
        cursos (nombre),
        secciones (nombre)
      `);

    if (errorAsignaciones) {
      throw new Error(`Error obteniendo asignaciones: ${errorAsignaciones.message}`);
    }

    if (!asignaciones || asignaciones.length === 0) {
      console.log('⚠️  No hay asignaciones en la base de datos');
      return;
    }

    console.log(`📚 Se encontraron ${asignaciones.length} asignaciones\n`);

    let asignacionesConEvaluaciones = 0;
    let asignacionesSinEvaluaciones = 0;
    let totalEvaluacionesCreadas = 0;
    let totalCalificacionesCreadas = 0;

    // 2. Procesar cada asignación
    for (const asignacion of asignaciones) {
      const cursoNombre = (asignacion.cursos as any)?.nombre || 'Sin nombre';
      const seccionNombre = (asignacion.secciones as any)?.nombre || 'Sin sección';
      console.log(`\n📖 Procesando: ${cursoNombre} - ${seccionNombre}`);

      // Verificar si ya tiene evaluaciones
      const { data: evaluacionesExistentes, error: errorCheck } = await supabase
        .from('evaluaciones')
        .select('id')
        .eq('asignacion_id', asignacion.id)
        .limit(1);

      if (errorCheck) {
        console.error(`   ❌ Error verificando evaluaciones: ${errorCheck.message}`);
        continue;
      }

      if (evaluacionesExistentes && evaluacionesExistentes.length > 0) {
        console.log(`   ⏭️  Ya tiene evaluaciones, omitiendo...`);
        asignacionesConEvaluaciones++;
        continue;
      }

      // Crear evaluaciones para esta asignación
      const evaluacionesConAsignacion = evaluacionesDefault.map(e => ({
        asignacion_id: asignacion.id,
        nombre: e.nombre,
        peso: e.peso,
        orden: e.orden,
        activo: e.activo
      }));

      const { data: evaluacionesCreadas, error: errorEval } = await supabase
        .from('evaluaciones')
        .insert(evaluacionesConAsignacion)
        .select();

      if (errorEval) {
        console.error(`   ❌ Error creando evaluaciones: ${errorEval.message}`);
        continue;
      }

      console.log(`   ✅ Creadas ${evaluacionesCreadas?.length || 0} evaluaciones`);
      totalEvaluacionesCreadas += evaluacionesCreadas?.length || 0;

      // Obtener alumnos de la asignación (a través de matrículas)
      // Necesitamos obtener el curso_id y seccion_id de la asignación
      const { data: asignacionDetalle, error: errorAsignacionDetalle } = await supabase
        .from('asignaciones')
        .select('curso_id, seccion_id')
        .eq('id', asignacion.id)
        .single();

      if (errorAsignacionDetalle || !asignacionDetalle) {
        console.error(`   ❌ Error obteniendo detalle de asignación`);
        continue;
      }

      const { data: matriculas, error: errorAlumnos } = await supabase
        .from('matriculas')
        .select('alumno_id')
        .eq('seccion_id', asignacionDetalle.seccion_id)
        .eq('estado', 'activo');

      if (errorAlumnos) {
        console.error(`   ❌ Error obteniendo alumnos: ${errorAlumnos.message}`);
        continue;
      }

      const alumnos = matriculas || [];

      if (!alumnos || alumnos.length === 0) {
        console.log(`   ⚠️  No tiene alumnos inscritos`);
        asignacionesSinEvaluaciones++;
        continue;
      }

      // Crear calificaciones para cada alumno y cada evaluación
      const calificaciones: any[] = [];
      
      evaluacionesCreadas?.forEach(evaluacion => {
        alumnos.forEach(alumno => {
          calificaciones.push({
            evaluacion_id: evaluacion.id,
            alumno_id: alumno.alumno_id,
            nota: null // Usar 'nota' en lugar de 'calificacion'
          });
        });
      });

      const { error: errorCalif } = await supabase
        .from('calificaciones')
        .insert(calificaciones);

      if (errorCalif) {
        console.error(`   ❌ Error creando calificaciones: ${errorCalif.message}`);
        continue;
      }

      console.log(`   ✅ Creadas ${calificaciones.length} calificaciones para ${alumnos.length} alumnos`);
      totalCalificacionesCreadas += calificaciones.length;
      asignacionesSinEvaluaciones++;
    }

    // Resumen final
    console.log('\n' + '='.repeat(60));
    console.log('📊 RESUMEN DE INICIALIZACIÓN');
    console.log('='.repeat(60));
    console.log(`Total de asignaciones procesadas: ${asignaciones.length}`);
    console.log(`Asignaciones que ya tenían evaluaciones: ${asignacionesConEvaluaciones}`);
    console.log(`Asignaciones con evaluaciones nuevas: ${asignacionesSinEvaluaciones}`);
    console.log(`Total de evaluaciones creadas: ${totalEvaluacionesCreadas}`);
    console.log(`Total de calificaciones creadas: ${totalCalificacionesCreadas}`);
    console.log('='.repeat(60));
    console.log('\n✨ Proceso completado exitosamente\n');

  } catch (error: any) {
    console.error('\n❌ Error en el proceso:', error.message);
    process.exit(1);
  }
}

// Ejecutar el script
inicializarEvaluaciones()
  .then(() => {
    console.log('👋 Finalizando script...');
    process.exit(0);
  })
  .catch((error) => {
    console.error('💥 Error fatal:', error);
    process.exit(1);
  });
