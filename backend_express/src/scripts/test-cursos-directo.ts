import { createClient } from '@supabase/supabase-js';

const SUPABASE_URL = 'https://dhirwwytreumhebccuht.supabase.co';
const SUPABASE_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRoaXJ3d3l0cmV1bWhlYmNjdWh0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzc4NjAxNjgsImV4cCI6MjA5MzQzNjE2OH0.gdZyHp4XaEioelEblxH2qRoOQHC2vHlPHwWFjGEDiMI';

async function testCursos() {
  console.log('=== TEST DIRECTO DE CURSOS ===\n');
  console.log('URL:', SUPABASE_URL);
  console.log('');

  const supabase = createClient(SUPABASE_URL, SUPABASE_KEY);

  // Test 1: Todos los cursos sin filtro
  console.log('1. Consultando TODOS los cursos (sin filtro):');
  const { data: todos, error: error1, count: count1 } = await supabase
    .from('cursos')
    .select('*', { count: 'exact' });

  console.log('   Count:', count1);
  console.log('   Data length:', todos?.length);
  console.log('   Error:', error1);
  if (todos && todos.length > 0) {
    console.log('   Primeros 3 cursos:');
    todos.slice(0, 3).forEach((c: any) => {
      console.log(`     - ${c.nombre} (activo: ${c.activo})`);
    });
  }
  console.log('');

  // Test 2: Solo cursos activos
  console.log('2. Consultando cursos ACTIVOS:');
  const { data: activos, error: error2, count: count2 } = await supabase
    .from('cursos')
    .select('*', { count: 'exact' })
    .eq('activo', true);

  console.log('   Count:', count2);
  console.log('   Data length:', activos?.length);
  console.log('   Error:', error2);
  if (activos && activos.length > 0) {
    console.log('   Cursos activos:');
    activos.forEach((c: any) => {
      console.log(`     - ${c.nombre}`);
    });
  }
  console.log('');

  // Test 3: Verificar estructura de tabla
  console.log('3. Estructura de un curso:');
  const { data: unCurso } = await supabase
    .from('cursos')
    .select('*')
    .limit(1)
    .single();

  if (unCurso) {
    console.log(JSON.stringify(unCurso, null, 2));
  } else {
    console.log('   No hay cursos en la tabla');
  }

  process.exit(0);
}

testCursos().catch(console.error);
