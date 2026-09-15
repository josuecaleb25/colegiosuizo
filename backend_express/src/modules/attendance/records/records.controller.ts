import { Request, Response } from 'express';
import recordsService from './records.service';

export async function registerManualAttendance(req: Request, res: Response) {
  try {
    const { sesion_id, alumno_id, estado, observaciones } = req.body;
    if (!sesion_id || !alumno_id || !estado) return res.status(400).json({ success: false, message: 'Faltan campos requeridos' });
    const data = await recordsService.registerManual({ sessionId: sesion_id, studentId: alumno_id, status: estado, observations: observaciones });
    return res.status(201).json({ success: true, message: 'Asistencia registrada exitosamente', data });
  } catch (error: any) {
    return res.status(error.status || 500).json({ success: false, message: error.message || 'Error al registrar asistencia', error: error.message });
  }
}

export async function registerAbsencesBatch(req: Request, res: Response) {
  try {
    const { ausentes, fecha } = req.body;
    if (!Array.isArray(ausentes) || ausentes.length === 0) return res.status(400).json({ success: false, message: 'Se requiere un array de ausentes' });
    if (!fecha) return res.status(400).json({ success: false, message: 'Se requiere fecha' });
    const data = await recordsService.registerAbsencesBatch(ausentes, fecha);
    return res.json({ success: true, message: `Procesados: ${data.guardados} guardados, ${data.errores} errores`, data });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Error al registrar ausentes', error: error.message });
  }
}

export async function registerAbsence(req: Request, res: Response) {
  try {
    const { persona_id, estado, fecha } = req.body;
    if (!persona_id || !fecha) return res.status(400).json({ success: false, message: 'persona_id y fecha son requeridos' });
    const result = await recordsService.registerAbsence(persona_id, estado, fecha);
    if (result.existing) return res.json({ success: true, message: 'Ya existe asistencia para este alumno' });
    return res.json({ success: true, message: 'Ausente registrado', data: result.data });
  } catch (error: any) {
    return res.status(error.status || 500).json({ success: false, message: error.message || 'Error al registrar ausente', error: error.message });
  }
}
