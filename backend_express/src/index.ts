import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import supabase from './config/database';

// Importar rutas
import authRoutes from './routes/auth.routes';
import alumnosRoutes from './routes/alumnos.routes';
import asistenciaRoutes from './routes/asistencia.routes';
import mobileRoutes from './routes/mobile.routes';

dotenv.config();

const app = express();
const PORT = process.env.PORT || 8000;

// Middlewares
app.use(cors({
  origin: process.env.ALLOWED_ORIGINS?.split(',') || '*',
  credentials: true
}));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Rutas
app.get('/', (req, res) => {
  res.json({ 
    success: true, 
    message: 'API Sistema de Asistencia IE Peruano Suizo',
    version: '1.0.0',
    backend: 'Express.js + TypeScript',
    database: 'Supabase (PostgreSQL)'
  });
});

app.use('/api/auth', authRoutes);
app.use('/api/alumnos', alumnosRoutes);
app.use('/api/asistencia', asistenciaRoutes);
app.use('/api/mobile', mobileRoutes);

// Manejo de errores
app.use((err: any, req: express.Request, res: express.Response, next: express.NextFunction) => {
  console.error('Error:', err);
  res.status(err.status || 500).json({
    success: false,
    message: err.message || 'Error interno del servidor',
    error: process.env.NODE_ENV === 'development' ? err : {}
  });
});

// Iniciar servidor
app.listen(PORT, () => {
  console.log(`🚀 Servidor corriendo en puerto ${PORT}`);
  console.log(`📍 Entorno: ${process.env.NODE_ENV || 'development'}`);
  console.log(`🔗 Supabase URL: ${process.env.SUPABASE_URL}`);
});

export default app;
