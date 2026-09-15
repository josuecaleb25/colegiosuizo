import { Router } from 'express';
import { authMiddleware, requireRoles } from '../../../middleware/auth';
import { registerManualAttendance, registerAbsencesBatch, registerAbsence } from './records.controller';

const router = Router();
const managers = ['profesor', 'administrador', 'admin'];

router.post('/', authMiddleware, requireRoles(...managers), registerManualAttendance);
router.post('/registrar-ausentes-batch', authMiddleware, requireRoles(...managers), registerAbsencesBatch);
router.post('/registrar-ausente', authMiddleware, requireRoles(...managers), registerAbsence);

export default router;
