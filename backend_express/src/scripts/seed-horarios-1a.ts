import supabase from '../config/database';

async function seedHorarios1A() {
  try {
    console.log('🕐 Iniciando creación de horarios para 1°A...');

    // Obtener la sección 1°A
    const { data: seccion, error: seccionError } = await supabase
      .from('secciones')
      .select(`
        id,
        nombre,
        grados!inner (
          nombre
        )
      `)
      .eq('nombre', 'A')
      .eq('grados.nombre', '1ro')
      .single();

    if (seccionError || !seccion) {
      console.log('❌ Error buscando sección:', seccionError?.message);
      
      // Intentar buscar de otra forma
      const { data: secciones } = await supabase
        .from('secciones')
        .select('id, nombre, grados(nombre)');
      
      console.log('Secciones disponibles:', secciones);
      throw new Error('No se encontró la sección 1°A');
    }

    console.log(`✅ Sección 1°A encontrada: ${seccion.id}`);

    // Obtener todas las asignaciones de 1°A con sus cursos
    const { data: asignaciones, error: asignacionesError } = await supabase
      .from('asignaciones')
      .select(`
        id,
        cursos!inner (
          nombre
        )
      `)
      .eq('seccion_id', seccion.id);

    if (asignacionesError || !asignaciones) {
      throw new Error('No se encontraron asignaciones para 1°A');
    }

    console.log(`✅ ${asignaciones.length} asignaciones encontradas`);

    // Crear un mapa de curso -> asignacion_id
    const asignacionMap: { [key: string]: string } = {};
    asignaciones.forEach((asig: any) => {
      asignacionMap[asig.cursos.nombre] = asig.id;
    });

    // Definir el horario de 1°A
    const horarios = [
      // LUNES
      { dia: 1, hora_inicio: '07:30', hora_fin: '08:15', curso: 'Matemática' },
      { dia: 1, hora_inicio: '08:15', hora_fin: '09:00', curso: 'Matemática' },
      { dia: 1, hora_inicio: '09:00', hora_fin: '09:45', curso: 'Ed. Religiosa' },
      { dia: 1, hora_inicio: '09:45', hora_fin: '10:30', curso: 'Ed. Religiosa' },
      { dia: 1, hora_inicio: '10:50', hora_fin: '11:35', curso: 'C y T' },
      { dia: 1, hora_inicio: '11:35', hora_fin: '12:20', curso: 'Comunicación' },
      { dia: 1, hora_inicio: '12:20', hora_fin: '13:05', curso: 'Comunicación' },

      // MARTES
      { dia: 2, hora_inicio: '07:30', hora_fin: '08:15', curso: 'Comunicación' },
      { dia: 2, hora_inicio: '08:15', hora_fin: '09:00', curso: 'Comunicación' },
      { dia: 2, hora_inicio: '09:00', hora_fin: '09:45', curso: 'Matemática' },
      { dia: 2, hora_inicio: '09:45', hora_fin: '10:30', curso: 'Matemática' },
      { dia: 2, hora_inicio: '10:50', hora_fin: '11:35', curso: 'Inglés' },
      { dia: 2, hora_inicio: '11:35', hora_fin: '12:20', curso: 'C y T' },
      { dia: 2, hora_inicio: '12:20', hora_fin: '13:05', curso: 'Inglés' },

      // MIÉRCOLES
      { dia: 3, hora_inicio: '07:30', hora_fin: '08:15', curso: 'D.P.C.C.' },
      { dia: 3, hora_inicio: '08:15', hora_fin: '09:00', curso: 'D.P.C.C.' },
      { dia: 3, hora_inicio: '09:00', hora_fin: '09:45', curso: 'Arte' },
      { dia: 3, hora_inicio: '09:45', hora_fin: '10:30', curso: 'Arte' },
      { dia: 3, hora_inicio: '10:50', hora_fin: '11:35', curso: 'Matemática' },
      { dia: 3, hora_inicio: '11:35', hora_fin: '12:20', curso: 'Ed. Física' },
      { dia: 3, hora_inicio: '12:20', hora_fin: '13:05', curso: 'Ed. Física' },

      // JUEVES
      { dia: 4, hora_inicio: '07:30', hora_fin: '08:15', curso: 'CC.SS.' },
      { dia: 4, hora_inicio: '08:15', hora_fin: '09:00', curso: 'CC.SS.' },
      { dia: 4, hora_inicio: '09:00', hora_fin: '09:45', curso: 'Tutoría' }, // Cambio: Ed. Física por Tutoría
      { dia: 4, hora_inicio: '09:45', hora_fin: '10:30', curso: 'Tutoría' }, // Cambio: Ed. Física por Tutoría
      { dia: 4, hora_inicio: '10:50', hora_fin: '11:35', curso: 'D.P.C.C.' },
      { dia: 4, hora_inicio: '11:35', hora_fin: '12:20', curso: 'D.P.C.C.' },

      // VIERNES
      { dia: 5, hora_inicio: '07:30', hora_fin: '08:15', curso: 'CC.SS.' },
      { dia: 5, hora_inicio: '08:15', hora_fin: '09:00', curso: 'Comunicación' },
      { dia: 5, hora_inicio: '09:00', hora_fin: '09:45', curso: 'C y T' },
      { dia: 5, hora_inicio: '09:45', hora_fin: '10:30', curso: 'C y T' },
      { dia: 5, hora_inicio: '10:50', hora_fin: '11:35', curso: 'C y T' },
      { dia: 5, hora_inicio: '11:35', hora_fin: '12:20', curso: 'EPT' },
      { dia: 5, hora_inicio: '12:20', hora_fin: '13:05', curso: 'EPT' },
    ];

    console.log('📚 Insertando horarios...');

    let insertados = 0;
    let errores = 0;

    for (const horario of horarios) {
      const asignacionId = asignacionMap[horario.curso];
      
      if (!asignacionId) {
        console.log(`❌ No se encontró asignación para el curso: ${horario.curso}`);
        errores++;
        continue;
      }

      try {
        const { error } = await supabase
          .from('horarios')
          .insert({
            asignacion_id: asignacionId,
            dia_semana: horario.dia,
            hora_inicio: horario.hora_inicio,
            hora_fin: horario.hora_fin,
            salon: '1A', // Salón por defecto
            activo: true
          });

        if (error) {
          console.log(`❌ Error al insertar horario ${horario.curso} ${horario.hora_inicio}: ${error.message}`);
          errores++;
        } else {
          insertados++;
        }
      } catch (err: any) {
        console.log(`❌ Error al insertar horario ${horario.curso}: ${err.message}`);
        errores++;
      }
    }

    console.log(`\n✅ Proceso completado:`);
    console.log(`   - Horarios insertados: ${insertados}`);
    console.log(`   - Errores: ${errores}`);
    console.log(`   - Total procesados: ${horarios.length}`);

    // Verificar horarios creados
    const { data: horariosCreados } = await supabase
      .from('horarios')
      .select(`
        dia_semana,
        hora_inicio,
        hora_fin,
        asignaciones!inner (
          cursos!inner (
            nombre
          )
        )
      `)
      .eq('asignaciones.seccion_id', seccion.id)
      .eq('activo', true)
      .order('dia_semana')
      .order('hora_inicio');

    console.log(`\n📋 Horarios creados para 1°A: ${horariosCreados?.length || 0}`);
    
    if (horariosCreados && horariosCreados.length > 0) {
      const diasSemana = ['', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes'];
      horariosCreados.forEach((h: any) => {
        console.log(`   ${diasSemana[h.dia_semana]} ${h.hora_inicio}-${h.hora_fin}: ${h.asignaciones.cursos.nombre}`);
      });
    }

  } catch (error: any) {
    console.error('❌ Error general:', error.message);
  }
}

// Ejecutar si se llama directamente
if (require.main === module) {
  seedHorarios1A().then(() => {
    console.log('🏁 Script finalizado');
    process.exit(0);
  });
}

export default seedHorarios1A;