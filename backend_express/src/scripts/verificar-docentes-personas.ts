import supabase from '../config/database';

async function verificarDocentes() {
  console.log('🔍 Verificando docentes en tabla personas...\n');

  const { data: personas, error } = await supabase
    .from('personas')
    .select('dni, nombres, apellidos, correo')
    .like('correo', '%@peruanosuizo.edu.pe')
    .not('correo', 'like', '%alumno%')
    .order('correo');

  if (error) {
    console.error('Error:', error);
    return;
  }

  console.log(`Total de docentes encontrados: ${personas?.length || 0}\n`);

  personas?.forEach((p: any, index: number) => {
    console.log(`${index + 1}. ${p.nombres} ${p.apellidos}`);
    console.log(`   Email: ${p.correo}`);
    console.log(`   DNI: ${p.dni}\n`);
  });
}

verificarDocentes();
