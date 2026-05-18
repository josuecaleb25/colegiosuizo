import supabase from '../config/database';

async function testCursosProfesor() {
  console.log('🔍 Probando endpoint de cursos del profesor...\n');

  // Usar el email de un profesor para obtener su persona_id
  const email = 'anibalmoreno@peruanosuizo.edu.pe';
  
  const { data: persona } = await supabase
    .from('personas')
    .select('id, nombres, apellidos')
    .eq('correo', email)
    .single();

  if (!persona) {
    console.log('❌ No se encontró la persona');
    return;
  }

  console.log(`✅ Persona encontrada: ${persona.nombres} ${persona.apellidos}`);
  console.log(`   ID: ${persona.id}\n`);

  // Buscar el docente
  const { data: docente } = await supabase
    .from('docentes')
    .select('id')
    .eq('persona_id', persona.id)
    .single();

  if (!docente) {
    console.log('❌ No se encontró el docente');
    return;
  }

  console.log(`✅ Docente encontrado, ID: ${docente.id}\n`);

  // Obtener asignaciones
  const { data: asignaciones, error } = await supabase
    .from('asignaciones')
    .select(`
      id,
      curso_id,
      seccion_id,
      cursos!inner (
        id,
        nombre,
        descripcion,
        color,
        icono
      ),
      secciones!inner (
        nombre,
        grados!inner (
          nombre
        )
      )
    `)
    .eq('docente_id', docente.id);

  if (error) {
    console.error('❌ Error:', error);
    return;
  }

  console.log(`✅ Asignaciones encontradas: ${asignaciones?.length || 0}\n`);

  asignaciones?.forEach((a: any, index: number) => {
    const curso = a.cursos;
    const seccion = a.secciones;
    const grado = seccion.grados;
    
    console.log(`${index + 1}. ${curso.nombre}`);
    console.log(`   Sección: ${grado.nombre} ${seccion.nombre}`);
    console.log(`   Color: ${curso.color}`);
    console.log(`   Icono: ${curso.icono}\n`);
  });
}

testCursosProfesor();
