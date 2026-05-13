import supabase from '../config/database';

async function verificarCursos() {
  console.log('=== VERIFICANDO CURSOS ===\n');

  // 1. Verificar cursos
  const { data: cursos, error: cursosError } = await supabase
    .from('cursos')
    .select('*')
    .eq('activo', true);

  console.log('1. CURSOS ACTIVOS:');
  console.log(`Total: ${cursos?.length || 0}`);
  if (cursos && cursos.length > 0) {
    cursos.forEach(c => console.log(`  - ${c.nombre} (ID: ${c.id})`));
  }
  console.log('');

  // 2. Verificar alumnos
  const { data: alumnos, error: alumnosError } = await supabase
    .from('alumnos')
    .select('id, codigo, personas(nombres, apellidos)');

  console.log('2. ALUMNOS:');
  console.log(`Total: ${alumnos?.length || 0}`);
  if (alumnos && alumnos.length > 0) {
    alumnos.slice(0, 3).forEach((a: any) => {
      console.log(`  - ${a.personas.nombres} ${a.personas.apellidos} (ID: ${a.id}, Código: ${a.codigo})`);
    });
  }
  console.log('');

  // 3. Verificar matrículas
  const { data: matriculas, error: matriculasError } = await supabase
    .from('matriculas')
    .select('alumno_id, seccion_id, estado')
    .eq('estado', 'activo');

  console.log('3. MATRÍCULAS ACTIVAS:');
  console.log(`Total: ${matriculas?.length || 0}`);
  console.log('');

  // 4. Verificar asignaciones
  const { data: asignaciones, error: asignacionesError } = await supabase
    .from('asignaciones')
    .select(`
      id,
      curso_id,
      docente_id,
      seccion_id,
      cursos(nombre),
      secciones(nombre, grados(nombre))
    `);

  console.log('4. ASIGNACIONES:');
  console.log(`Total: ${asignaciones?.length || 0}`);
  if (asignaciones && asignaciones.length > 0) {
    asignaciones.forEach((a: any) => {
      console.log(`  - ${a.cursos.nombre} en ${a.secciones.grados.nombre}${a.secciones.nombre}`);
    });
  }
  console.log('');

  // 5. Probar endpoint de cursos para un alumno
  if (alumnos && alumnos.length > 0) {
    const alumnoId = alumnos[0].id;
    console.log(`5. PROBANDO ENDPOINT PARA ALUMNO ${alumnoId}:`);

    const { data: matricula } = await supabase
      .from('matriculas')
      .select('seccion_id')
      .eq('alumno_id', alumnoId)
      .eq('estado', 'activo')
      .single();

    if (matricula) {
      console.log(`  Sección del alumno: ${matricula.seccion_id}`);

      const { data: cursosAlumno } = await supabase
        .from('asignaciones')
        .select(`
          id,
          cursos (
            id,
            nombre
          ),
          docentes!inner (
            id,
            personas!inner (
              nombres,
              apellidos
            )
          ),
          secciones!inner (
            nombre,
            grados!inner (
              nombre
            )
          )
        `)
        .eq('seccion_id', matricula.seccion_id);

      console.log(`  Cursos encontrados: ${cursosAlumno?.length || 0}`);
      if (cursosAlumno && cursosAlumno.length > 0) {
        cursosAlumno.forEach((c: any) => {
          console.log(`    - ${c.cursos.nombre} con ${c.docentes.personas.nombres} ${c.docentes.personas.apellidos}`);
        });
      }
    } else {
      console.log('  El alumno no tiene matrícula activa');
    }
  }

  process.exit(0);
}

verificarCursos().catch(console.error);
