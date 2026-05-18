import supabase from '../config/database';

async function debugHorarios() {
  try {
    console.log('🔍 Debuggeando horarios...');

    // Obtener todos los horarios para ver qué días tenemos
    const { data: todosHorarios } = await supabase
      .from('horarios')
      .select('dia_semana')
      .eq('activo', true);

    console.log('Días de la semana en la BD:', [...new Set(todosHorarios?.map(h => h.dia_semana))]);

    // Obtener horarios del lunes (día 1)
    const { data: horariosLunes } = await supabase
      .from('horarios')
      .select('dia_semana, hora_inicio, hora_fin, asignaciones(cursos(nombre))')
      .eq('dia_semana', 1)
      .eq('activo', true)
      .limit(5);

    console.log(`Horarios del lunes (día 1): ${horariosLunes?.length || 0}`);
    horariosLunes?.forEach(h => {
      console.log(`  ${h.hora_inicio}-${h.hora_fin}: ${h.asignaciones?.cursos?.nombre}`);
    });

    // Probar la lógica de fecha
    const fecha = '2026-05-18';
    const fechaObj = new Date(fecha + 'T00:00:00');
    let diaSemana = fechaObj.getDay();
    if (diaSemana === 0) diaSemana = 7;
    
    console.log(`\nFecha: ${fecha} -> Día: ${diaSemana}`);

    // Obtener horarios con el filtro
    const { data: horariosFiltrados } = await supabase
      .from('horarios')
      .select('dia_semana, hora_inicio, hora_fin, asignaciones(cursos(nombre))')
      .eq('dia_semana', diaSemana)
      .eq('activo', true);

    console.log(`Horarios filtrados para día ${diaSemana}: ${horariosFiltrados?.length || 0}`);

  } catch (error: any) {
    console.error('❌ Error:', error.message);
  }
}

debugHorarios().then(() => {
  console.log('🏁 Debug finalizado');
  process.exit(0);
});