import { Router } from 'express';
import { authMiddleware, requireRoles } from '../../middleware/auth';
import {
  listAnnouncements,
  getAnnouncement,
  createAnnouncement,
  markAnnouncementAsRead,
  listSentAnnouncements,
  listAnnouncementReads,
  updateAnnouncement,
  deleteAnnouncement
} from './announcements.controller';

const router = Router();

router.get('/', listAnnouncements);
router.get('/historial/enviados', listSentAnnouncements);
router.post('/', authMiddleware, requireRoles('profesor', 'administrador', 'admin'), createAnnouncement);
router.post('/:id/leer', authMiddleware, markAnnouncementAsRead);
router.get('/:id/lecturas', listAnnouncementReads);
router.get('/:id', getAnnouncement);
router.put('/:id', authMiddleware, requireRoles('profesor', 'administrador', 'admin'), updateAnnouncement);
router.delete('/:id', authMiddleware, requireRoles('profesor', 'administrador', 'admin'), deleteAnnouncement);

export default router;
