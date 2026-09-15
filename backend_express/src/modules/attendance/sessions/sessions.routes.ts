import { Router } from 'express';
import { authMiddleware, requireRoles } from '../../../middleware/auth';
import { getActiveSession, createSession, closeSession } from './sessions.controller';

const router = Router();
const attendanceManagers = ['profesor', 'administrador', 'admin'];

router.get('/activa', authMiddleware, requireRoles(...attendanceManagers), getActiveSession);
router.post('/', authMiddleware, requireRoles(...attendanceManagers), createSession);
router.put('/:id/cerrar', authMiddleware, requireRoles(...attendanceManagers), closeSession);

export default router;
