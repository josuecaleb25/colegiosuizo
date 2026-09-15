import { Router } from 'express';
import authRoutes from './auth.routes';
import alumnosRoutes from './alumnos.routes';
import asistenciaRoutes from './asistencia.routes';
import mobileRoutes from './mobile.routes';
import cursosRoutes from './cursos.routes';
import calificacionesRoutes from './calificaciones.routes';
import evaluacionesRoutes from './evaluaciones';
import horariosRoutes from './horarios.routes';
import comunicadosRoutes from './comunicados.routes';
import usuariosRoutes from './usuarios.routes';
import adminRoutes from './admin.routes';
import seccionesRoutes from './secciones.routes';
import notificacionesRoutes from '../modules/notifications/notifications.routes';

const router = Router();

router.use('/auth', authRoutes);
router.use('/alumnos', alumnosRoutes);
router.use('/asistencia', asistenciaRoutes);
router.use('/mobile', mobileRoutes);
router.use('/cursos', cursosRoutes);
router.use('/calificaciones', calificacionesRoutes);
router.use('/evaluaciones', evaluacionesRoutes);
router.use('/horarios', horariosRoutes);
router.use('/comunicados', comunicadosRoutes);
router.use('/usuarios', usuariosRoutes);
router.use('/admin', adminRoutes);
router.use('/secciones', seccionesRoutes);
router.use('/notificaciones', notificacionesRoutes);

export default router;
