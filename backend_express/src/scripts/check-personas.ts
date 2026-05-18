import supabase from '../config/database';

async function checkPersonas() {
  const { data, error } = await supabase
    .from('personas')
    .select('correo, nombres, apellidos')
    .eq('correo', 'abadvillanera@peruanosuizo.edu.pe')
    .limit(1);

  if (error) {
    console.log('Error:', error);
    return;
  }

  console.log('Persona encontrada:', data);
}

checkPersonas();
