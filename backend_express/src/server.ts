import app from './app';
import { startAttendanceAutoClose } from './services/attendance-auto-close.service';
import { assertJwtSecretConfigured } from './middleware/auth';

const PORT = process.env.PORT || 8000;

assertJwtSecretConfigured();

const server = app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
  console.log(`Environment: ${process.env.NODE_ENV || 'development'}`);
  startAttendanceAutoClose();
});

const shutdown = () => {
  const timeout = setTimeout(() => server.closeAllConnections(), 10000);
  timeout.unref();
  server.close(() => {
    clearTimeout(timeout);
    process.exit(0);
  });
};

process.on('SIGTERM', shutdown);
process.on('SIGINT', shutdown);
