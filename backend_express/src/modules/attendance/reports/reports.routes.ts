import { Router } from 'express';
import { getDay, getHistory, getStatistics, listAttendance } from './reports.controller';
import { authMiddleware, requireRoles } from '../../../middleware/auth';
import { requireStudentAccess } from '../../../middleware/student-access';

const router = Router();

router.get('/estadisticas', authMiddleware, requireRoles('profesor', 'administrador', 'admin'), getStatistics);
router.get('/historial/:alumno_id', authMiddleware, requireStudentAccess(req => req.params.alumno_id), getHistory);
router.get('/del-dia', authMiddleware, requireRoles('profesor', 'administrador', 'admin'), getDay);
router.get('/', authMiddleware, requireRoles('profesor', 'administrador', 'admin'), listAttendance);

export default router;
