import supabase from '../config/database';

async function fixDuplicados() {
  try {
    console.log('🔧 Arreglando alumnos con emails duplicados...\n');

    // 1. Obtener año lectivo y secciones
    const { data: anio } = await supabase
      .from('anios_lectivos')
      .select('id')
      .eq('nombre', '2026')
      .single();

    const { data: secciones } = await supabase
      .from('secciones')
      .select(`
        id,
        nombre,
        grados!inner (nombre)
      `)
      .eq('anio_lectivo_id', anio.id)
      .eq('grados.nombre', '1ro');

    const seccionB = secciones?.find(s => s.nombre === 'B');
    const seccionE = secciones?.find(s => s.nombre === 'E');

    // 2. Arreglar MAYTA CAPCHA JOSUE en 1ro B (el segundo)
    console.log('📚 Arreglando MAYTA CAPCHA JOSUE en 1ro B...');
    
    const { data: persona1, error: error1 } = await supabase
      .from('personas')
      .insert({
        dni: '00000048', // DNI que falló
        nombres: 'JOSUE',
        apellidos: 'MAYTA CAPCHA',
        correo: 'maytacapcha2@peruanosuizo.edu.pe', // Email único con "2"
        fecha_nacimiento: '2010-01-01'
      })
      .select()
      .single();

    if (error1) {
      console.error('❌ Error creando JOSUE MAYTA CAPCHA:', error1);
    } else {
      // Crear alumno
      const { data: alumno1 } = await supabase
        .from('alumnos')
        .insert({
          persona_id: persona1.id,
          codigo_alumno: '1B016',
          fecha_ingreso: '2026-03-01',
          estado: 'activo'
        })
        .select()
        .single();

      // Crear matrícula
      await supabase
        .from('matriculas')
        .insert({
          alumno_id: alumno1.id,
          seccion_id: seccionB.id,
          anio_lectivo_id: anio.id,
          fecha_matricula: '2026-03-01',
          estado: 'activo'
        });

      // Crear código QR
      await supabase
        .from('codigos_qr')
        .insert({
          persona_id: persona1.id,
          codigo: 'QR1B016',
          activo: true
        });

      console.log('✅ JOSUE MAYTA CAPCHA creado exitosamente');
    }

    // 3. Arreglar CALDERON URBINA JESUS ENNODIO en 1ro E
    console.log('📚 Arreglando CALDERON URBINA JESUS ENNODIO en 1ro E...');
    
    const { data: persona2, error: error2 } = await supabase
      .from('personas')
      .insert({
        dni: '00000131', // DNI que falló
        nombres: 'JESUS ENNODIO',
        apellidos: 'CALDERON URBINA',
        correo: 'calderonurbina2@peruanosuizo.edu.pe', // Email único con "2"
        fecha_nacimiento: '2010-01-01'
      })
      .select()
      .single();

    if (error2) {
      console.error('❌ Error creando JESUS ENNODIO CALDERON URBINA:', error2);
    } else {
      // Crear alumno
      const { data: alumno2 } = await supabase
        .from('alumnos')
        .insert({
          persona_id: persona2.id,
          codigo_alumno: '1E004',
          fecha_ingreso: '2026-03-01',
          estado: 'activo'
        })
        .select()
        .single();

      // Crear matrícula
      await supabase
        .from('matriculas')
        .insert({
          alumno_id: alumno2.id,
          seccion_id: seccionE.id,
          anio_lectivo_id: anio.id,
          fecha_matricula: '2026-03-01',
          estado: 'activo'
        });

      // Crear código QR
      await supabase
        .from('codigos_qr')
        .insert({
          persona_id: persona2.id,
          codigo: 'QR1E004',
          activo: true
        });

      console.log('✅ JESUS ENNODIO CALDERON URBINA creado exitosamente');
    }

    console.log('\n🎉 ¡Duplicados arreglados!');
    console.log('📊 Ahora deberías tener los 158 alumnos completos');

  } catch (error) {
    console.error('❌ Error:', error);
  }
}

fixDuplicados();