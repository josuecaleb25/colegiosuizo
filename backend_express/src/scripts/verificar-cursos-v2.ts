import supabase from '../config/database';

async function verificarCursos() {
  console.log('=== VERIFICANDO CURSOS V2 ===\n');

  // 1. Verificar cursos SIN filtro de activo
  const { data: cursos, error: cursosError } = await supabase
    .from('cursos')
    .select('*');

  console.log('1. CURSOS (todos):');
  console.log(`Total: ${cursos?.length || 0}`);
  if (cursosError) console.error('Error:', cursosError);
  if (cursos && cursos.length > 0) {
    cursos.forEach(c => console.log(`  - ${c.nombre} (ID: ${c.id}, activo: ${c.activo})`));
  }
  console.log('');

  // 2. Verificar alumnos
  const { data: alumnos, error: alumnosError } = await supabase
    .from('alumnos')
    .select('id, codigo, persona_id');

  console.log('2. ALUMNOS:');
  console.log(`Total: ${alumnos?.length || 0}`);
  if (alumnosError) console.error('Error:', alumnosError);
  if (alumnos && alumnos.length > 0) {
    console.log(`  Primeros 3: ${alumnos.slice(0, 3).map((a: any) => a.codigo).join(', ')}`);
  }
  console.log('');

  // 3. Verificar secciones
  const { data: secciones, error: seccionesError } = await supabase
    .from('secciones')
    .select('id, nombre, grado_id');

  console.log('3. SECCIONES:');
  console.log(`Total: ${secciones?.length || 0}`);
  if (seccionesError) console.error('Error:', seccionesError);
  console.log('');

  // 4. Verificar docentes
  const { data: docentes, error: docentesError } = await supabase
    .from('docentes')
    .select('id, persona_id');

  console.log('4. DOCENTES:');
  console.log(`Total: ${docentes?.length || 0}`);
  if (docentesError) console.error('Error:', docentesError);
  console.log('');

  // 5. Verificar asignaciones
  const { data: asignaciones, error: asignacionesError } = await supabase
    .from('asignaciones')
    .select('*');

  console.log('5. ASIGNACIONES:');
  console.log(`Total: ${asignaciones?.length || 0}`);
  if (asignacionesError) console.error('Error:', asignacionesError);
  if (asignaciones && asignaciones.length > 0) {
    asignaciones.forEach((a: any) => {
      console.log(`  - Curso: ${a.curso_id}, Docente: ${a.docente_id}, Sección: ${a.seccion_id}`);
    });
  }
  console.log('');

  // 6. Verificar matrículas
  const { data: matriculas, error: matriculasError } = await supabase
    .from('matriculas')
    .select('alumno_id, seccion_id, estado')
    .limit(5);

  console.log('6. MATRÍCULAS (primeras 5):');
  console.log(`Total en query: ${matriculas?.length || 0}`);
  if (matriculasError) console.error('Error:', matriculasError);
  if (matriculas && matriculas.length > 0) {
    matriculas.forEach((m: any) => {
      console.log(`  - Alumno: ${m.alumno_id}, Sección: ${m.seccion_id}, Estado: ${m.estado}`);
    });
  }

  process.exit(0);
}

verificarCursos().catch(console.error);
