import { Router } from 'express';
import authRoutes from './auth.routes';
import alumnosRoutes from './alumnos.routes';
import asistenciaRoutes from '../modules/attendance/attendance.routes';
import mobileRoutes from './mobile.routes';
import horariosRoutes from './horarios.routes';
import comunicadosRoutes from '../modules/announcements/announcements.routes';
import usuariosRoutes from './usuarios.routes';
import adminRoutes from './admin.routes';
import seccionesRoutes from './secciones.routes';
import notificacionesRoutes from '../modules/notifications/notifications.routes';
import { authMiddleware } from '../middleware/auth';

const router = Router();

router.use('/auth', authRoutes);
router.use(authMiddleware);
router.use('/alumnos', alumnosRoutes);
router.use('/asistencia', asistenciaRoutes);
router.use('/mobile', mobileRoutes);
router.use('/horarios', horariosRoutes);
router.use('/comunicados', comunicadosRoutes);
router.use('/usuarios', usuariosRoutes);
router.use('/admin', adminRoutes);
router.use('/secciones', seccionesRoutes);
router.use('/notificaciones', notificacionesRoutes);

export default router;
