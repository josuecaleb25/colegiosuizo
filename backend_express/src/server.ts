import app from './app';
import { startAttendanceAutoClose } from './services/attendance-auto-close.service';

const PORT = process.env.PORT || 8000;

app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
  console.log(`Environment: ${process.env.NODE_ENV || 'development'}`);
  startAttendanceAutoClose();
});
