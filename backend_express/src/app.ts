import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import apiRoutes from './routes';

dotenv.config();

const app = express();
const allowedOrigins = (process.env.ALLOWED_ORIGINS || '')
  .split(',')
  .map(origin => origin.trim())
  .filter(Boolean);

app.set('trust proxy', 1);
app.disable('x-powered-by');

app.use(cors({
  origin(origin, callback) {
    if (!origin || allowedOrigins.includes('*') || allowedOrigins.includes(origin)) return callback(null, true);
    return callback(null, false);
  },
  credentials: false
}));
app.use((_req, res, next) => {
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Referrer-Policy', 'no-referrer');
  res.setHeader('Cache-Control', 'no-store');
  next();
});
app.use(express.json({ limit: '128kb' }));
app.use(express.urlencoded({ extended: false, limit: '128kb', parameterLimit: 100 }));

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
      horarios: '/api/horarios',
      comunicados: '/api/comunicados',
      usuarios: '/api/usuarios',
      admin: '/api/admin',
      secciones: '/api/secciones',
      notificaciones: '/api/notificaciones'
    }
  });
});

app.get('/healthz', (_req, res) => {
  res.status(200).json({ status: 'ok' });
});

app.use('/api', apiRoutes);

app.use((err: any, _req: express.Request, res: express.Response, _next: express.NextFunction) => {
  const status = Number.isInteger(err.status) && err.status >= 400 && err.status < 500 ? err.status : 500;
  console.error('Request failed:', { status, name: err.name || 'Error' });
  res.status(status).json({
    success: false,
    message: status === 413 ? 'La solicitud es demasiado grande' : 'Error interno del servidor'
  });
});

export default app;
