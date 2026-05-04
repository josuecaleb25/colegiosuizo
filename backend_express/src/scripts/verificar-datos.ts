import supabase from '../config/database';

async function verificarDatos() {
  try {
    console.log('🔍 Verificando datos en Supabase...\n');

    // Contar personas
    const { count: personasCount, error: personasError } = await supabase
      .from('personas')
      .select('*', { count: 'exact', head: true });

    if (personasError) {
      console.error('❌ Error al contar personas:', personasError);
    } else {
      console.log(`👥 Personas: ${personasCount}`);
    }

    // Contar alumnos
    const { count: alumnosCount, error: alumnosError } = await supabase
      .from('alumnos')
      .select('*', { count: 'exact', head: true });

    if (alumnosError) {
      console.error('❌ Error al contar alumnos:', alumnosError);
    } else {
      console.log(`🎓 Alumnos: ${alumnosCount}`);
    }

    // Contar matrículas
    const { count: matriculasCount, error: matriculasError } = await supabase
      .from('matriculas')
      .select('*', { count: 'exact', head: true });

    if (matriculasError) {
      console.error('❌ Error al contar matrículas:', matriculasError);
    } else {
      console.log(`📋 Matrículas: ${matriculasCount}`);
    }

    // Contar códigos QR
    const { count: qrCount, error: qrError } = await supabase
      .from('codigos_qr')
      .select('*', { count: 'exact', head: true });

    if (qrError) {
      console.error('❌ Error al contar códigos QR:', qrError);
    } else {
      console.log(`🔲 Códigos QR: ${qrCount}`);
    }

    // Contar por sección
    console.log('\n📊 Alumnos por sección:');
    
    const { data: secciones } = await supabase
      .from('secciones')
      .select(`
        id,
        nombre,
        grados!inner (nombre)
      `)
      .eq('grados.nombre', '1ro');

    if (secciones) {
      for (const seccion of secciones) {
        const { count } = await supabase
          .from('matriculas')
          .select('*', { count: 'exact', head: true })
          .eq('seccion_id', seccion.id);
        
        console.log(`   1ro ${seccion.nombre}: ${count || 0} alumnos`);
      }
    }

    console.log('\n✅ Verificación completada');

  } catch (error) {
    console.error('❌ Error:', error);
  }
}

verificarDatos();
