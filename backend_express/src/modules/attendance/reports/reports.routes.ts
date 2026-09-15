import { Router } from 'express';
import { getDay, getHistory, getLeaderboard, getStatistics, listAttendance } from './reports.controller';
import { authMiddleware, requireRoles } from '../../../middleware/auth';

const router = Router();

router.get('/estadisticas', getStatistics);
router.get('/historial/:alumno_id', getHistory);
router.get('/leaderboard', getLeaderboard);
router.get('/del-dia', authMiddleware, requireRoles('profesor', 'administrador', 'admin'), getDay);
router.get('/', listAttendance);

export default router;
