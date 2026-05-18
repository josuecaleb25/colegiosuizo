import supabase from '../config/database';

async function testHorarios() {
  try {
    console.log('🕐 Probando endpoints de horarios...');

    // Obtener docente ID
    const { data: docente } = await supabase
      .from('docentes')
      .select('id, personas(nombres, apellidos)')
      .eq('personas.correo', 'anibalmoreno@peruanosuizo.edu.pe')
      .single();

    if (!docente) {
      console.log('❌ No se encontró el docente');
      return;
    }

    console.log(`✅ Docente encontrado: ${docente.personas.nombres} ${docente.personas.apellidos} (ID: ${docente.id})`);

    // Probar endpoint de horarios del profesor
    const response = await fetch(`http://localhost:3000/api/horarios/profesor/${docente.id}`);
    const data = await response.json();

    console.log(`📅 Horarios del profesor: ${data.data?.length || 0}`);
    
    if (data.data && data.data.length > 0) {
      const dias = ['', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes'];
      console.log('Primeros 5 horarios:');
      data.data.slice(0, 5).forEach((h: any) => {
        console.log(`   ${dias[h.dia_semana]} ${h.hora_inicio}-${h.hora_fin}: ${h.curso} (${h.seccion})`);
      });
    }

  } catch (error: any) {
    console.error('❌ Error:', error.message);
  }
}

testHorarios().then(() => {
  console.log('🏁 Test finalizado');
  process.exit(0);
});