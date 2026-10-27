import supabase from '../config/database';

async function verificarEsquema() {
  console.log('=== VERIFICANDO ESQUEMA ===\n');

  // Obtener un alumno para ver su estructura
  const { data: alumno, error: alumnoError } = await supabase
    .from('alumnos')
    .select('*')
    .limit(1)
    .single();

  console.log('ESTRUCTURA DE ALUMNOS:');
  if (alumnoError) {
    console.error('Error:', alumnoError);
  } else if (alumno) {
    console.log(JSON.stringify(alumno, null, 2));
  }
  console.log('');

  // Contar registros
  const { count: alumnosCount } = await supabase
    .from('alumnos')
    .select('*', { count: 'exact', head: true });

  const { count: docentesCount } = await supabase
    .from('docentes')
    .select('*', { count: 'exact', head: true });

  console.log('CONTEO DE REGISTROS:');
  console.log(`Alumnos: ${alumnosCount}`);
  console.log(`Docentes: ${docentesCount}`);

  process.exit(0);
}

verificarEsquema().catch(console.error);
