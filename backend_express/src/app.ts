import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import apiRoutes from './routes';

dotenv.config();

const app = express();

app.use(cors({
  origin: process.env.ALLOWED_ORIGINS?.split(',') || '*',
  credentials: true
}));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

app.get('/', (_req, res) => {
  res.json({
    success: true,
    message: 'API Sistema de Asistencia IE Peruano Suizo',
    version: '2.0.0',
    backend: 'Express.js + TypeScript',
    database: 'Supabase (PostgreSQL)',
    endpoints: {
      auth: '/api/auth',
      alumnos: '/api/alumnos',
      asistencia: '/api/asistencia',
      mobile: '/api/mobile',
      cursos: '/api/cursos',
      calificaciones: '/api/calificaciones',
      evaluaciones: '/api/evaluaciones',
      horarios: '/api/horarios',
      comunicados: '/api/comunicados',
      usuarios: '/api/usuarios',
      admin: '/api/admin',
      secciones: '/api/secciones',
      notificaciones: '/api/notificaciones'
    }
  });
});

app.use('/api', apiRoutes);

app.post('/api/test-fcm', (req, res) => {
  res.json({ success: true, message: 'Test FCM OK', received: req.body });
});

app.use((err: any, _req: express.Request, res: express.Response, _next: express.NextFunction) => {
  console.error('Error:', err);
  res.status(err.status || 500).json({
    success: false,
    message: err.message || 'Error interno del servidor',
    error: process.env.NODE_ENV === 'development' ? err : {}
  });
});

export default app;
