import { Router } from 'express';
import { authMiddleware, requireRoles } from '../../middleware/auth';
import { requireAnnouncementOwner, requireAnnouncementVisible } from './announcement-access';
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
router.post('/:id/leer', authMiddleware, requireAnnouncementVisible, markAnnouncementAsRead);
router.get('/:id/lecturas', authMiddleware, requireRoles('profesor', 'administrador', 'admin'), requireAnnouncementOwner, listAnnouncementReads);
router.get('/:id', authMiddleware, requireAnnouncementVisible, getAnnouncement);
router.put('/:id', authMiddleware, requireRoles('profesor', 'administrador', 'admin'), requireAnnouncementOwner, updateAnnouncement);
router.delete('/:id', authMiddleware, requireRoles('profesor', 'administrador', 'admin'), requireAnnouncementOwner, deleteAnnouncement);

export default router;
