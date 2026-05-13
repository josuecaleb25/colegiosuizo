import supabase from '../config/database';

async function seedCursos() {
  console.log('=== POBLANDO CURSOS ===\n');

  const cursos = [
    {
      nombre: 'Matemática',
      descripcion: 'Curso de matemáticas',
      color: '#BA1924',
      icono: '📐',
      activo: true
    },
    {
      nombre: 'Educación para el Trabajo',
      descripcion: 'Computación y tecnología',
      color: '#4564A4',
      icono: '💼',
      activo: true
    },
    {
      nombre: 'Inglés',
      descripcion: 'Idioma inglés',
      color: '#976D2',
      icono: '🌐',
      activo: true
    },
    {
      nombre: 'Orientación y tutoría',
      descripcion: 'Orientación y tutoría',
      color: '#616161',
      icono: '📚',
      activo: true
    },
    {
      nombre: 'Ciencias Sociales',
      descripcion: 'Historia y geografía',
      color: '#F67C00',
      icono: '🌍',
      activo: true
    },
    {
      nombre: 'Arte y Cultura',
      descripcion: 'Artes plásticas y música',
      color: '#C21858',
      icono: '🎨',
      activo: true
    },
    {
      nombre: 'Ciencia y Tecnología',
      descripcion: 'Ciencias naturales',
      color: '#7B1FA2',
      icono: '🔬',
      activo: true
    },
    {
      nombre: 'Religión',
      descripcion: 'Educación religiosa',
      color: '#5D4037',
      icono: '✝️',
      activo: true
    },
    {
      nombre: 'Educación Física',
      descripcion: 'Deportes y actividad física',
      color: '#00796B',
      icono: '⚽',
      activo: true
    },
    {
      nombre: 'Comunicación',
      descripcion: 'Lengua y literatura',
      color: '#2E7D32',
      icono: '📖',
      activo: true
    }
  ];

  console.log(`Insertando ${cursos.length} cursos...\n`);

  for (const curso of cursos) {
    const { data, error } = await supabase
      .from('cursos')
      .insert(curso)
      .select()
      .single();

    if (error) {
      console.error(`❌ Error al insertar ${curso.nombre}:`, error.message);
    } else {
      console.log(`✅ ${curso.nombre} insertado (ID: ${data.id})`);
    }
  }

  console.log('\n=== VERIFICANDO CURSOS INSERTADOS ===\n');

  const { data: cursosInsertados, count } = await supabase
    .from('cursos')
    .select('*', { count: 'exact' });

  console.log(`Total de cursos en la base de datos: ${count}`);
  
  if (cursosInsertados && cursosInsertados.length > 0) {
    cursosInsertados.forEach(c => {
      console.log(`  - ${c.nombre} (${c.color})`);
    });
  }

  process.exit(0);
}

seedCursos().catch(console.error);
