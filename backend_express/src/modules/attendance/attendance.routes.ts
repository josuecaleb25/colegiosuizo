import { Router } from 'express';
import sessionsRoutes from './sessions/sessions.routes';
import deviceTokensRoutes from './device-tokens/device-tokens.routes';
import recordsRoutes from './records/records.routes';
import mutationsRoutes from './records/records.mutations.routes';
import reportsRoutes from './reports/reports.routes';

const router = Router();

router.use('/sesiones', sessionsRoutes);
router.use('/device-token', deviceTokensRoutes);
router.use(recordsRoutes);
router.use(mutationsRoutes);
router.use(reportsRoutes);

export default router;
