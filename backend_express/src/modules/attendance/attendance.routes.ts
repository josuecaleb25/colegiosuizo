import { Router } from 'express';
import legacyAttendanceRoutes from '../../routes/asistencia.routes';
import sessionsRoutes from './sessions/sessions.routes';
import deviceTokensRoutes from './device-tokens/device-tokens.routes';

const router = Router();

// Session routes are extracted first; the remaining attendance endpoints stay
// behind the legacy router until each one is migrated and verified.
router.use('/sesiones', sessionsRoutes);
router.use('/device-token', deviceTokensRoutes);
router.use(legacyAttendanceRoutes);

export default router;
