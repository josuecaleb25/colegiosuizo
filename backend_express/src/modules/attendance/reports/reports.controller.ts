import { Request, Response } from 'express';
import reportsService from './reports.service';

function sendError(res: Response, error: any, fallback: string) {
  const status = error?.status || 500;
  res.status(status).json({ success: false, code: error?.code || 'ATTENDANCE_REPORT_ERROR', message: error?.message || fallback });
}

export async function listAttendance(req: Request, res: Response) {
  try {
    const { fecha, seccion_id, alumno_id } = req.query;
    const data = await reportsService.list(String(fecha || ''), seccion_id ? String(seccion_id) : undefined, alumno_id ? String(alumno_id) : undefined);
    res.json({ success: true, data, total: data.length });
  } catch (error) {
    sendError(res, error, 'No se pudo obtener la asistencia');
  }
}

export async function getDay(req: Request, res: Response) {
  try {
    const { fecha, sesion_id } = req.query;
    if (!fecha || !sesion_id) return res.status(400).json({ success: false, code: 'SESSION_ID_REQUIRED', message: 'Se requieren fecha y sesion_id' });
    const data = await reportsService.day(String(sesion_id), String(fecha));
    res.json({ success: true, data, total: data.length });
  } catch (error) {
    sendError(res, error, 'No se pudo obtener la asistencia del día');
  }
}

export async function getLeaderboard(req: Request, res: Response) {
  try {
    const { seccion_id, tipo, mes } = req.query;
    if (!tipo || !mes) return res.status(400).json({ success: false, message: 'Se requiere tipo y mes' });
    const data = await reportsService.leaderboard(seccion_id ? String(seccion_id) : undefined, String(tipo), String(mes));
    res.json({ success: true, data });
  } catch (error) {
    sendError(res, error, 'No se pudo obtener el ranking de asistencia');
  }
}

export async function getStatistics(req: Request, res: Response) {
  try {
    const { fecha_inicio, fecha_fin, seccion_id } = req.query;
    const data = await reportsService.statistics(
      fecha_inicio ? String(fecha_inicio) : undefined,
      fecha_fin ? String(fecha_fin) : undefined,
      seccion_id ? String(seccion_id) : undefined
    );
    res.json({ success: true, data });
  } catch (error) {
    sendError(res, error, 'No se pudieron obtener las estadísticas');
  }
}

export async function getHistory(req: Request, res: Response) {
  try {
    const { fecha_inicio, fecha_fin, limit } = req.query;
    const parsedLimit = Math.min(Math.max(Number(limit || 100), 1), 500);
    const data = await reportsService.history(
      req.params.alumno_id,
      fecha_inicio ? String(fecha_inicio) : undefined,
      fecha_fin ? String(fecha_fin) : undefined,
      parsedLimit
    );
    res.json({ success: true, data, total: data.length });
  } catch (error) {
    sendError(res, error, 'No se pudo obtener el historial de asistencia');
  }
}
