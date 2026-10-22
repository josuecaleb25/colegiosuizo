import { Request, Response, NextFunction } from 'express';

type Entry = { count: number; resetAt: number };

export function createRateLimiter(windowMs: number, maxRequests: number, keyFromRequest: (req: Request) => string = req => req.ip || 'unknown') {
  const entries = new Map<string, Entry>();
  let requests = 0;

  return (req: Request, res: Response, next: NextFunction) => {
    const now = Date.now();
    const key = keyFromRequest(req);
    const entry = entries.get(key);

    if (!entry || entry.resetAt <= now) {
      entries.set(key, { count: 1, resetAt: now + windowMs });
    } else {
      entry.count += 1;
      if (entry.count > maxRequests) {
        res.setHeader('Retry-After', String(Math.max(1, Math.ceil((entry.resetAt - now) / 1000))));
        return res.status(429).json({ success: false, message: 'Demasiadas solicitudes. Intenta más tarde.' });
      }
    }

    requests += 1;
    if (requests % 1000 === 0 || entries.size > 20000) {
      for (const [entryKey, value] of entries) {
        if (value.resetAt <= now) entries.delete(entryKey);
      }
      while (entries.size > 20000) entries.delete(entries.keys().next().value as string);
    }

    next();
  };
}
