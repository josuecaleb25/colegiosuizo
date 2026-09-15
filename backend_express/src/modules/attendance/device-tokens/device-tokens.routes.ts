import { Router } from 'express';
import { authMiddleware } from '../../../middleware/auth';
import { registerDeviceToken } from './device-tokens.controller';

const router = Router();

router.post('/', authMiddleware, registerDeviceToken);

export default router;
