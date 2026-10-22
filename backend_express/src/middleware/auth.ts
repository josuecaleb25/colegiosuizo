import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';

export interface AuthRequest extends Request {
  user?: {
    id: string;
    email: string;
    rol: string;
  };
}

const adminRoles = new Set(['administrador', 'admin']);

export function getJwtSecret(): string {
  const secret = process.env.JWT_SECRET;
  if (!secret || Buffer.byteLength(secret, 'utf8') < 32) {
    throw new Error('JWT_SECRET must be configured with at least 32 bytes');
  }
  return secret;
}

export function assertJwtSecretConfigured(): void {
  getJwtSecret();
}

export function signAccessToken(payload: { id: string; email: string; rol: string }): string {
  return jwt.sign(payload, getJwtSecret(), {
    algorithm: 'HS256',
    expiresIn: (process.env.JWT_EXPIRES_IN || '8h') as jwt.SignOptions['expiresIn']
  });
}

export const requireRoles = (...allowedRoles: string[]) => {
  return (req: AuthRequest, res: Response, next: NextFunction) => {
    if (!req.user) {
      return res.status(401).json({ success: false, message: 'Autenticación requerida' });
    }
    if (!allowedRoles.includes(req.user.rol)) {
      return res.status(403).json({ success: false, message: 'No tienes permisos para realizar esta operación' });
    }
    next();
  };
};

export const requireSelfOrAdmin = (getTargetId: (req: AuthRequest) => string) => {
  return (req: AuthRequest, res: Response, next: NextFunction) => {
    if (!req.user) {
      return res.status(401).json({ success: false, message: 'Autenticación requerida' });
    }
    if (!adminRoles.has(req.user.rol) && req.user.id !== getTargetId(req)) {
      return res.status(403).json({ success: false, message: 'No tienes permisos para consultar estos datos' });
    }
    next();
  };
};

export const authMiddleware = (req: AuthRequest, res: Response, next: NextFunction) => {
  if (req.user) return next();
  const authorization = req.headers.authorization || '';
  const match = authorization.match(/^Bearer\s+(.+)$/i);
  if (!match) {
    return res.status(401).json({ success: false, message: 'Token no proporcionado' });
  }

  try {
    const decoded = jwt.verify(match[1], getJwtSecret(), { algorithms: ['HS256'] }) as jwt.JwtPayload;
    if (typeof decoded.id !== 'string' || typeof decoded.email !== 'string' || typeof decoded.rol !== 'string') {
      return res.status(401).json({ success: false, message: 'Token inválido' });
    }
    req.user = { id: decoded.id, email: decoded.email, rol: decoded.rol };
    next();
  } catch {
    return res.status(401).json({ success: false, message: 'Token inválido' });
  }
};

export const optionalAuthMiddleware = (req: AuthRequest, res: Response, next: NextFunction) => {
  if (!req.headers.authorization) return next();
  return authMiddleware(req, res, next);
};
