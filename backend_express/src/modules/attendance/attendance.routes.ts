import { Router } from 'express';
import sessionsRoutes from './sessions/sessions.routes';
import deviceTokensRoutes from './device-tokens/device-tokens.routes';
import recordsRoutes from './records/records.routes';
import mutationsRoutes from './records/records.mutations.routes';
import reportsRoutes from './reports/reports.routes';
import webAttendanceRoutes from './reports/web-attendance.routes';

const router = Router();

router.use('/sesiones', sessionsRoutes);
router.use('/device-token', deviceTokensRoutes);
router.use('/web', webAttendanceRoutes);
router.use(recordsRoutes);
router.use(mutationsRoutes);
router.use(reportsRoutes);

export default router;
