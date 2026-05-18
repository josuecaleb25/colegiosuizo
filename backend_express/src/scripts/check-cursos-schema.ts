import supabase from '../config/database';

async function checkSchema() {
  // Intentar insertar un curso de prueba para ver qué columnas acepta
  const { data, error } = await supabase
    .from('cursos')
    .insert({
      nombre: 'Test'
    })
    .select()
    .single();

  if (error) {
    console.log('Error:', error);
  } else {
    console.log('Curso creado:', data);
    console.log('Columnas:', Object.keys(data));
    
    // Eliminar el curso de prueba
    await supabase.from('cursos').delete().eq('id', data.id);
  }
}

checkSchema();
