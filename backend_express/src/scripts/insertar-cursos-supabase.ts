import supabase from '../config/database';

async function insertarCursos() {
  console.log('=== INSERTANDO CURSOS EN SUPABASE ===\n');

  const cursos = [
    {
      nombre: 'Matemática',
      descripcion: 'Curso de matemáticas',
      color: '#BA1924',
      icono: '📐',
      activo: true
    },
    {
      nombre: 'Comunicación',
      descripcion: 'Lengua y literatura',
      color: '#2E7D32',
      icono: '📚',
      activo: true
    },
    {
      nombre: 'Inglés',
      descripcion: 'Idioma inglés',
      color: '#1976D2',
      icono: '🌍',
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
      nombre: 'Ciencias Sociales',
      descripcion: 'Historia y geografía',
      color: '#F57C00',
      icono: '🌎',
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
      nombre: 'Arte y Cultura',
      descripcion: 'Artes plásticas y música',
      color: '#C2185B',
      icono: '🎨',
      activo: true
    },
    {
      nombre: 'Educación para el Trabajo',
      descripcion: 'Computación y tecnología',
      color: '#455A64',
      icono: '💻',
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
      nombre: 'Tutoría',
      descripcion: 'Orientación y tutoría',
      color: '#616161',
      icono: '👥',
      activo: true
    }
  ];

  console.log(`Insertando ${cursos.length} cursos...\n`);

  const { data, error } = await supabase
    .from('cursos')
    .insert(cursos)
    .select();

  if (error) {
    console.error('❌ Error al insertar cursos:', error);
    process.exit(1);
  }

  console.log(`✅ ${data.length} cursos insertados exitosamente\n`);
  
  data.forEach((c: any) => {
    console.log(`  - ${c.nombre} (${c.color})`);
  });

  // Verificar
  const { count } = await supabase
    .from('cursos')
    .select('*', { count: 'exact', head: true });

  console.log(`\nTotal de cursos en la base de datos: ${count}`);

  process.exit(0);
}

insertarCursos().catch(console.error);
