import { Router } from 'express';
import supabase from '../config/database';

const router = Router();

// Función para formatear hora sin ceros iniciales (07:30 -> 7:30)
function formatearHora(hora: string): string {
  const [horas, minutos] = hora.split(':');
  return `${parseInt(horas)}:${minutos}`;
}

// Test endpoint simple
router.get('/test', (req, res) => {
  console.log('✅ Test endpoint llamado');
  res.json({
    success: true,
    message: 'Backend funcionando correctamente',
    timestamp: new Date().toISOString()
  });
});

// GET /api/horarios/alumno/:personaId - Horario de un alumno por persona_id
router.get('/alumno/:personaId', async (req, res) => {
  try {
    const { personaId } = req.params;
    const { fecha } = req.query; // Parámetro de fecha opcional

    console.log(`Petición recibida - Persona: ${personaId}, Fecha: ${fecha}`);

    // Obtener alumno por persona_id
    const { data: alumno, error: alumnoError } = await supabase
      .from('alumnos')
      .select('id')
      .eq('persona_id', personaId)
      .eq('estado', 'activo')
      .single();

    if (alumnoError || !alumno) {
      console.log('No se encontró alumno para persona_id:', personaId);
      return res.status(404).json({
        success: false,
        message: 'Alumno no encontrado'
      });
    }

    console.log(`Alumno encontrado: ${alumno.id}`);

    // Obtener sección del alumno
    const { data: matricula, error: matriculaError } = await supabase
      .from('matriculas')
      .select('seccion_id')
      .eq('alumno_id', alumno.id)
      .eq('estado', 'activo')
      .single();

    if (matriculaError) throw matriculaError;

    console.log(`Sección del alumno: ${matricula.seccion_id}`);

    // Obtener día de la semana de la fecha (si se proporciona)
    let diaSemana = 1; // Por defecto lunes
    if (fecha) {
      const fechaObj = new Date(fecha + 'T00:00:00');
      const jsDay = fechaObj.getDay(); // 0=domingo, 1=lunes, etc.
      
      // Convertir de JavaScript getDay() a nuestra convención escolar
      // JavaScript: 0=Dom, 1=Lun, 2=Mar, 3=Mie, 4=Jue, 5=Vie, 6=Sab
      // Nuestra BD: 1=Lun, 2=Mar, 3=Mie, 4=Jue, 5=Vie
      
      if (jsDay === 0 || jsDay === 6) {
        // Domingo (0) o Sábado (6) - no hay clases
        console.log(`Día ${jsDay} es fin de semana - no hay clases`);
        return res.json({
          success: true,
          data: [],
          message: 'No hay clases los fines de semana'
        });
      }
      
      // Para lunes a viernes, el número coincide (1-5)
      diaSemana = jsDay;
      console.log(`Fecha recibida: ${fecha}, JS Day: ${jsDay}, Día escolar: ${diaSemana}`);
    } else {
      console.log('No se proporcionó fecha, mostrando lunes por defecto');
    }

    // Obtener horarios de la sección para el día específico
    const { data, error } = await supabase
      .from('horarios')
      .select(`
        id,
        dia_semana,
        hora_inicio,
        hora_fin,
        salon,
        asignaciones!inner (
          cursos!inner (
            nombre,
            color,
            icono
          ),
          docentes!inner (
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
        )
      `)
      .eq('asignaciones.seccion_id', matricula.seccion_id)
      .eq('activo', true)
      .eq('dia_semana', diaSemana)
      .order('hora_inicio');

    if (error) {
      console.error('Error en query:', error);
      throw error;
    }

    console.log(`Horarios encontrados para día ${diaSemana}: ${data?.length || 0}`);

    let horariosFormateados = data?.map((horario: any) => ({
      id: horario.id,
      dia_semana: horario.dia_semana,
      hora_inicio: formatearHora(horario.hora_inicio.substring(0, 5)), // 07:30:00 -> 7:30
      hora_fin: formatearHora(horario.hora_fin.substring(0, 5)), // 08:15:00 -> 8:15
      aula: horario.salon || '1A',
      curso: horario.asignaciones.cursos.nombre,
      color: horario.asignaciones.cursos.color,
      icono: horario.asignaciones.cursos.icono,
      profesor: `${horario.asignaciones.docentes.personas.nombres} ${horario.asignaciones.docentes.personas.apellidos}`,
      seccion: `${horario.asignaciones.secciones.grados.nombre}${horario.asignaciones.secciones.nombre}`
    })) || [];

    // Insertar el recreo entre 10:30 y 10:50 (después de la 4ta hora)
    // Buscar el índice donde empieza la 5ta hora (10:50 o después)
    const indexRecreo = horariosFormateados.findIndex(h => {
      const [hora, minuto] = h.hora_inicio.split(':').map(Number);
      return hora > 10 || (hora === 10 && minuto >= 50);
    });
    
    // Si encontramos horarios después de las 10:50, insertar el recreo antes
    if (indexRecreo >= 0) {
      horariosFormateados.splice(indexRecreo, 0, {
        id: 'recreo',
        dia_semana: diaSemana,
        hora_inicio: '10:30',
        hora_fin: '10:50',
        aula: '',
        curso: 'RECREO',
        color: '#4CAF50',
        icono: '☕',
        profesor: '',
        seccion: ''
      });
    }

    res.json({
      success: true,
      data: horariosFormateados
    });
  } catch (error: any) {
    console.error('Error en endpoint:', error);
    res.status(500).json({
      success: false,
      message: 'Error al obtener horario del alumno',
      error: error.message
    });
  }
});

// GET /api/horarios/profesor/:personaId - Horario de un profesor por persona_id
router.get('/profesor/:personaId', async (req, res) => {
  try {
    const { personaId } = req.params;
    const { fecha } = req.query; // Parámetro de fecha opcional

    console.log(`Petición recibida - Persona Profesor: ${personaId}, Fecha: ${fecha}`);

    // Obtener docente por persona_id
    const { data: docente, error: docenteError } = await supabase
      .from('docentes')
      .select('id')
      .eq('persona_id', personaId)
      .single();

    if (docenteError || !docente) {
      console.log('No se encontró docente para persona_id:', personaId);
      return res.status(404).json({
        success: false,
        message: 'Docente no encontrado'
      });
    }

    console.log(`Docente encontrado: ${docente.id}`);

    // Obtener día de la semana de la fecha (si se proporciona)
    let diaSemana = 1; // Por defecto lunes
    if (fecha) {
      const fechaObj = new Date(fecha + 'T00:00:00');
      const jsDay = fechaObj.getDay(); // 0=domingo, 1=lunes, etc.
      
      if (jsDay === 0 || jsDay === 6) {
        // Domingo (0) o Sábado (6) - no hay clases
        console.log(`Día ${jsDay} es fin de semana - no hay clases`);
        return res.json({
          success: true,
          data: [],
          message: 'No hay clases los fines de semana'
        });
      }
      
      // Para lunes a viernes, el número coincide (1-5)
      diaSemana = jsDay;
      console.log(`Fecha recibida: ${fecha}, JS Day: ${jsDay}, Día escolar: ${diaSemana}`);
    } else {
      console.log('No se proporcionó fecha, mostrando lunes por defecto');
    }

    const { data, error } = await supabase
      .from('horarios')
      .select(`
        id,
        dia_semana,
        hora_inicio,
        hora_fin,
        salon,
        asignaciones!inner (
          cursos!inner (
            nombre,
            color,
            icono
          ),
          secciones!inner (
            nombre,
            grados!inner (
              nombre
            )
          )
        )
      `)
      .eq('asignaciones.docente_id', docente.id)
      .eq('activo', true)
      .eq('dia_semana', diaSemana)
      .order('hora_inicio');

    if (error) {
      console.error('Error en query:', error);
      throw error;
    }

    console.log(`Horarios encontrados para día ${diaSemana}: ${data?.length || 0}`);

    let horariosFormateados = data?.map((horario: any) => ({
      id: horario.id,
      dia_semana: horario.dia_semana,
      hora_inicio: formatearHora(horario.hora_inicio.substring(0, 5)), // 07:30:00 -> 7:30
      hora_fin: formatearHora(horario.hora_fin.substring(0, 5)), // 08:15:00 -> 8:15
      aula: horario.salon || '1A',
      curso: horario.asignaciones.cursos.nombre,
      color: horario.asignaciones.cursos.color,
      icono: horario.asignaciones.cursos.icono,
      seccion: `${horario.asignaciones.secciones.grados.nombre}${horario.asignaciones.secciones.nombre}`
    })) || [];

    // Insertar el recreo entre 10:30 y 10:50 (después de la 4ta hora)
    const indexRecreo = horariosFormateados.findIndex(h => {
      const [hora, minuto] = h.hora_inicio.split(':').map(Number);
      return hora > 10 || (hora === 10 && minuto >= 50);
    });
    
    // Si encontramos horarios después de las 10:50, insertar el recreo antes
    if (indexRecreo >= 0) {
      horariosFormateados.splice(indexRecreo, 0, {
        id: 'recreo',
        dia_semana: diaSemana,
        hora_inicio: '10:30',
        hora_fin: '10:50',
        aula: '',
        curso: 'RECREO',
        color: '#4CAF50',
        icono: '☕',
        seccion: ''
      });
    }

    res.json({
      success: true,
      data: horariosFormateados
    });
  } catch (error: any) {
    console.error('Error en endpoint:', error);
    res.status(500).json({
      success: false,
      message: 'Error al obtener horario del profesor',
      error: error.message
    });
  }
});

// GET /api/horarios/seccion/:seccionId - Horario de una sección
router.get('/seccion/:seccionId', async (req, res) => {
  try {
    const { seccionId } = req.params;

    const { data, error } = await supabase
      .from('horarios')
      .select(`
        id,
        dia_semana,
        hora_inicio,
        hora_fin,
        salon,
        asignaciones!inner (
          cursos!inner (
            nombre,
            color,
            icono
          ),
          docentes!inner (
            personas!inner (
              nombres,
              apellidos
            )
          )
        )
      `)
      .eq('asignaciones.seccion_id', seccionId)
      .eq('activo', true)
      .order('dia_semana')
      .order('hora_inicio');

    if (error) throw error;

    const horariosFormateados = data?.map((horario: any) => ({
      id: horario.id,
      dia_semana: horario.dia_semana,
      hora_inicio: horario.hora_inicio,
      hora_fin: horario.hora_fin,
      salon: horario.salon,
      curso: horario.asignaciones.cursos.nombre,
      color: horario.asignaciones.cursos.color,
      icono: horario.asignaciones.cursos.icono,
      profesor: `${horario.asignaciones.docentes.personas.nombres} ${horario.asignaciones.docentes.personas.apellidos}`
    })) || [];

    res.json({
      success: true,
      data: horariosFormateados
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al obtener horario de la sección',
      error: error.message
    });
  }
});

// POST /api/horarios - Crear horario
router.post('/', async (req, res) => {
  try {
    const { asignacion_id, dia_semana, hora_inicio, hora_fin, salon } = req.body;

    if (!asignacion_id || !dia_semana || !hora_inicio || !hora_fin) {
      return res.status(400).json({
        success: false,
        message: 'Faltan datos requeridos'
      });
    }

    const { data, error } = await supabase
      .from('horarios')
      .insert({
        asignacion_id,
        dia_semana,
        hora_inicio,
        hora_fin,
        salon
      })
      .select()
      .single();

    if (error) throw error;

    res.json({
      success: true,
      message: 'Horario creado exitosamente',
      data
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al crear horario',
      error: error.message
    });
  }
});

// PUT /api/horarios/:id - Actualizar horario
router.put('/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const { dia_semana, hora_inicio, hora_fin, salon } = req.body;

    const { data, error } = await supabase
      .from('horarios')
      .update({
        dia_semana,
        hora_inicio,
        hora_fin,
        salon
      })
      .eq('id', id)
      .select()
      .single();

    if (error) throw error;

    res.json({
      success: true,
      message: 'Horario actualizado exitosamente',
      data
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al actualizar horario',
      error: error.message
    });
  }
});

// DELETE /api/horarios/:id - Eliminar horario
router.delete('/:id', async (req, res) => {
  try {
    const { id } = req.params;

    const { error } = await supabase
      .from('horarios')
      .update({ activo: false })
      .eq('id', id);

    if (error) throw error;

    res.json({
      success: true,
      message: 'Horario eliminado exitosamente'
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      message: 'Error al eliminar horario',
      error: error.message
    });
  }
});

export default router;