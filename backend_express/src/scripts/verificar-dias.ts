import supabase from '../config/database';

async function verificarDias() {
  try {
    const { data } = await supabase
      .from('horarios')
      .select('dia_semana')
      .eq('activo', true);
    
    const dias = [...new Set(data?.map(h => h.dia_semana))].sort();
    console.log('Días en la BD:', dias);
    
    // Verificar qué días corresponden a qué
    console.log('Convención en la BD:');
    dias.forEach(dia => {
      const nombres = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];
      if (dia >= 1 && dia <= 7) {
        // Si usamos 1-7 donde 1=Lunes
        const nombresEscolares = ['', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];
        console.log(`  ${dia} = ${nombresEscolares[dia]}`);
      } else {
        console.log(`  ${dia} = ${nombres[dia] || 'Desconocido'}`);
      }
    });
    
  } catch (error: any) {
    console.error('Error:', error.message);
  }
}

verificarDias().then(() => {
  console.log('Verificación completada');
  process.exit(0);
});