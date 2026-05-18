import bcrypt from 'bcryptjs';
import supabase from '../config/database';

// Cursos de 1°A
const cursos = [
  { nombre: 'Matemática', color: '#FF5722', icono: '📐' },
  { nombre: 'Comunicación', color: '#2196F3', icono: '📖' },
  { nombre: 'Inglés', color: '#4CAF50', icono: '🌍' },
  { nombre: 'CC.SS.', color: '#FF9800', icono: '🌎' },
  { nombre: 'D.P.C.C.', color: '#9C27B0', icono: '⚖️' },
  { nombre: 'Ed. Religiosa', color: '#795548', icono: '✝️' },
  { nombre: 'C y T', color: '#00BCD4', icono: '🔬' },
  { nombre: 'EPT', color: '#FFC107', icono: '💼' },
  { nombre: 'Arte', color: '#E91E63', icono: '🎨' },
  { nombre: 'Ed. Física', color: '#8BC34A', icono: '⚽' },
  { nombre: 'Tutoría', color: '#607D8B', icono: '👥' }
];

// Docentes de 1°A
const docentes = [
  { 
    nombres: 'Aníbal', 
    apellidos: 'Moreno', 
    correo: 'anibalmoreno@peruanosuizo.edu.pe',
    dni: '10000001',
    curso: 'Matemática'
  },
  { 
    nombres: 'Elcy Filomena', 
    apellidos: 'Hernandez Rodas', 
    correo: 'elcyhernandez@peruanosuizo.edu.pe',
    dni: '10000002',
    curso: 'Comunicación'
  },
  { 
    nombres: 'Nelly', 
    apellidos: 'Mujica Galvez', 
    correo: 'nellymujica@peruanosuizo.edu.pe',
    dni: '10000003',
    curso: 'Inglés'
  },
  { 
    nombres: 'Pablo', 
    apellidos: 'Veramendi Rivera', 
    correo: 'pabloveramendi@peruanosuizo.edu.pe',
    dni: '10000004',
    curso: 'CC.SS.'
  },
  { 
    nombres: 'Aydee Glosbinda', 
    apellidos: 'Arellano Cabada', 
    correo: 'aydeearellano@peruanosuizo.edu.pe',
    dni: '10000005',
    curso: 'D.P.C.C.'
  },
  { 
    nombres: 'Edgar Mauro', 
    apellidos: 'Vega Quiñones', 
    correo: 'edgarvega@peruanosuizo.edu.pe',
    dni: '10000006',
    curso: 'Ed. Religiosa'
  },
  { 
    nombres: 'Rocío', 
    apellidos: 'Sosa', 
    correo: 'rociososa@peruanosuizo.edu.pe',
    dni: '10000007',
    curso: 'C y T'
  },
  { 
    nombres: 'Rosmery Ysabel', 
    apellidos: 'Correa Caytano', 
    correo: 'rosmerycorrea@peruanosuizo.edu.pe',
    dni: '10000008',
    curso: 'EPT'
  },
  { 
    nombres: 'Maricella Johana', 
    apellidos: 'Timoteo Gahona', 
    correo: 'maricellatimoteo@peruanosuizo.edu.pe',
    dni: '10000009',
    curso: 'Arte'
  },
  { 
    nombres: 'Walter Manuel', 
    apellidos: 'Castro Valdivia', 
    correo: 'castrovaldivia@peruanosuizo.edu.pe',
    dni: '10000010',
    curso: 'Ed. Física'
  },
  { 
    nombres: 'Walter Manuel', 
    apellidos: 'Castro Valdivia', 
    correo: 'castrovaldivia@peruanosuizo.edu.pe',
    dni: '10000010',
    curso: 'Tutoría'
  }
];

async function seedDocentesYCursos() {
  try {
    console.log('🚀 Iniciando seed de docentes y cursos de 1°A...\n');

    // 1. Obtener año lectivo 2026
    const { data: anio, error: anioError } = await supabase
      .from('anios_lectivos')
      .select('id')
      .eq('nombre', '2026')
      .single();

    if (anioError || !anio) {
      throw new Error('No se encontró el año lectivo 2026');
    }

    console.log('✅ Año lectivo 2026:', anio.id);

    // 2. Obtener grado 1ro
    const { data: grado, error: gradoError } = await supabase
      .from('grados')
      .select('id')
      .eq('nombre', '1ro')
      .single();

    if (gradoError || !grado) {
      throw new Error('No se encontró el grado 1ro');
    }

    console.log('✅ Grado 1ro:', grado.id);

    // 3. Obtener sección 1°A
    const { data: seccion, error: seccionError } = await supabase
      .from('secciones')
      .select('id')
      .eq('grado_id', grado.id)
      .eq('nombre', 'A')
      .eq('anio_lectivo_id', anio.id)
      .single();

    if (seccionError || !seccion) {
      throw new Error('No se encontró la sección 1°A');
    }

    console.log('✅ Sección 1°A:', seccion.id);

    // 4. Crear cursos
    console.log('\n📚 Creando cursos...');
    const cursosCreados: any = {};

    for (const curso of cursos) {
      // Verificar si ya existe
      const { data: existeCurso } = await supabase
        .from('cursos')
        .select('id')
        .eq('nombre', curso.nombre)
        .single();

      if (existeCurso) {
        console.log(`   ⚠️  Curso ${curso.nombre} ya existe`);
        cursosCreados[curso.nombre] = existeCurso.id;
        continue;
      }

      const { data: cursoCreado, error: cursoError } = await supabase
        .from('cursos')
        .insert({
          nombre: curso.nombre,
          descripcion: `Curso de ${curso.nombre} para 1° de secundaria`,
          color: curso.color,
          icono: curso.icono,
          activo: true
        })
        .select()
        .single();

      if (cursoError) {
        console.error(`   ❌ Error creando curso ${curso.nombre}:`, cursoError);
        continue;
      }

      cursosCreados[curso.nombre] = cursoCreado.id;
      console.log(`   ✅ Curso creado: ${curso.nombre}`);
    }

    // 5. Crear docentes y asignarlos
    console.log('\n👨‍🏫 Creando docentes...');
    
    for (const docente of docentes) {
      // Verificar si ya existe la persona
      let { data: persona } = await supabase
        .from('personas')
        .select('id')
        .eq('correo', docente.correo)
        .single();

      if (!persona) {
        // Crear persona
        const { data: personaCreada, error: personaError } = await supabase
          .from('personas')
          .insert({
            dni: docente.dni,
            nombres: docente.nombres,
            apellidos: docente.apellidos,
            correo: docente.correo,
            fecha_nacimiento: '1980-01-01'
          })
          .select()
          .single();

        if (personaError) {
          console.error(`   ❌ Error creando persona ${docente.nombres}:`, personaError);
          continue;
        }

        persona = personaCreada;
        console.log(`   ✅ Persona creada: ${docente.nombres} ${docente.apellidos}`);
      } else {
        console.log(`   ⚠️  Persona ya existe: ${docente.nombres} ${docente.apellidos}`);
      }

      // Verificar si ya existe el docente
      let { data: docenteExiste } = await supabase
        .from('docentes')
        .select('id')
        .eq('persona_id', persona.id)
        .single();

      if (!docenteExiste) {
        // Crear docente
        const codigoDocente = `DOC${docente.dni.substring(4)}`;
        
        const { data: docenteCreado, error: docenteError } = await supabase
          .from('docentes')
          .insert({
            persona_id: persona.id,
            codigo_docente: codigoDocente,
            especialidad: docente.curso,
            estado: 'activo'
          })
          .select()
          .single();

        if (docenteError) {
          console.error(`   ❌ Error creando docente:`, docenteError);
          continue;
        }

        docenteExiste = docenteCreado;
        console.log(`   ✅ Docente creado: ${codigoDocente}`);
      }

      // Asignar docente al curso en la sección 1°A
      const cursoId = cursosCreados[docente.curso];
      
      if (!cursoId) {
        console.error(`   ❌ No se encontró el curso ${docente.curso}`);
        continue;
      }

      // Verificar si ya existe la asignación
      const { data: asignacionExiste } = await supabase
        .from('asignaciones')
        .select('id')
        .eq('docente_id', docenteExiste.id)
        .eq('curso_id', cursoId)
        .eq('seccion_id', seccion.id)
        .single();

      if (asignacionExiste) {
        console.log(`   ⚠️  Asignación ya existe: ${docente.curso}`);
        continue;
      }

      const { error: asignacionError } = await supabase
        .from('asignaciones')
        .insert({
          docente_id: docenteExiste.id,
          curso_id: cursoId,
          seccion_id: seccion.id,
          anio_lectivo_id: anio.id
        });

      if (asignacionError) {
        console.error(`   ❌ Error asignando curso:`, asignacionError);
        continue;
      }

      console.log(`   ✅ Asignado: ${docente.nombres} → ${docente.curso}`);
    }

    console.log('\n🎉 ¡Seed completado exitosamente!');
    console.log(`📊 Cursos creados: ${Object.keys(cursosCreados).length}`);
    console.log(`👨‍🏫 Docentes procesados: ${docentes.length}`);

  } catch (error) {
    console.error('❌ Error:', error);
  }
}

// Ejecutar
seedDocentesYCursos()
  .then(() => {
    console.log('\n✅ Proceso completado');
    process.exit(0);
  })
  .catch((error) => {
    console.error('💥 Error:', error);
    process.exit(1);
  });
