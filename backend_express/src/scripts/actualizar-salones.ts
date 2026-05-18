import supabase from '../config/database';

async function actualizarSalones() {
  try {
    console.log('🏫 Actualizando salones en horarios...');
    
    const { data, error } = await supabase
      .from('horarios')
      .update({ salon: '1A' })
      .eq('activo', true);
    
    if (error) {
      console.log('❌ Error:', error.message);
    } else {
      console.log('✅ Salones actualizados correctamente');
    }
    
    // Verificar actualización
    const { data: horarios } = await supabase
      .from('horarios')
      .select('salon')
      .eq('activo', true)
      .limit(3);
    
    console.log('Primeros 3 horarios:', horarios);
    
  } catch (error: any) {
    console.error('❌ Error:', error.message);
  }
}

actualizarSalones().then(() => {
  console.log('🏁 Script finalizado');
  process.exit(0);
});