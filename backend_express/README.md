# Backend Express - Sistema de Asistencia IE Peruano Suizo

Backend en Express.js + TypeScript + PostgreSQL (Supabase) para el sistema de asistencia escolar.

## Requisitos

- Node.js 18+
- PostgreSQL (Supabase)
- npm o yarn

## Instalación

```bash
# Instalar dependencias
npm install

# Configurar variables de entorno
cp .env.example .env
# Editar .env con tus credenciales
```

## Variables de Entorno

```env
# Supabase Configuration
SUPABASE_URL=https://dhirwwytreumhebccuht.supabase.co
SUPABASE_ANON_KEY=tu_anon_key_aqui

# Server
PORT=8000
NODE_ENV=development

# JWT
JWT_SECRET=tu_secret_key
JWT_EXPIRES_IN=7d

# CORS
ALLOWED_ORIGINS=*
```

### Obtener las credenciales de Supabase

1. Ve a tu proyecto en [Supabase](https://supabase.com/dashboard)
2. Ve a **Settings** > **API**
3. Copia:
   - **Project URL** → `SUPABASE_URL`
   - **anon public** key → `SUPABASE_ANON_KEY`

## Scripts

```bash
# Desarrollo (con hot reload)
npm run dev

# Compilar TypeScript
npm run build

# Producción
npm start

# Cargar datos de prueba
npm run seed
```

## Endpoints API

### Autenticación
- `POST /api/auth/login` - Login de usuario
- `POST /api/auth/register` - Registro de usuario

### Mobile API
- `GET /api/mobile/test` - Test de conexión
- `GET /api/mobile/test-alumnos` - Test con 10 alumnos
- `GET /api/mobile/usuarios` - Todos los usuarios con QR
- `GET /api/mobile/asistencia/alumnos` - Alumnos para asistencia
- `POST /api/mobile/asistencia/escanear-qr` - Escanear QR y registrar

### Alumnos
- `GET /api/alumnos` - Listar alumnos
- `GET /api/alumnos/:id` - Obtener alumno
- `POST /api/alumnos` - Crear alumno
- `PUT /api/alumnos/:id` - Actualizar alumno
- `DELETE /api/alumnos/:id` - Eliminar alumno

### Asistencia
- `GET /api/asistencia` - Listar asistencias
- `POST /api/asistencia` - Registrar asistencia
- `PUT /api/asistencia/:id` - Actualizar asistencia
- `DELETE /api/asistencia/:id` - Eliminar asistencia
- `GET /api/asistencia/estadisticas` - Estadísticas

## Estructura del Proyecto

```
backend_express/
├── src/
│   ├── config/
│   │   └── database.ts       # Configuración PostgreSQL
│   ├── routes/
│   │   ├── auth.routes.ts    # Rutas de autenticación
│   │   ├── mobile.routes.ts  # Rutas para app móvil
│   │   ├── alumnos.routes.ts # Rutas de alumnos
│   │   └── asistencia.routes.ts # Rutas de asistencia
│   ├── scripts/
│   │   └── seed.ts           # Script de datos iniciales
│   └── index.ts              # Servidor principal
├── package.json
├── tsconfig.json
└── .env
```

## Deploy a Railway

1. Crear proyecto en Railway
2. Conectar repositorio GitHub
3. Configurar variables de entorno
4. Railway detectará automáticamente Node.js
5. Build y deploy automático

## Credenciales de Prueba

**Admin:**
- Email: `admin@colegio.com`
- Password: `admin123`

**Profesor:**
- Email: `profesor@colegio.com`
- Password: `profesor123`

**Alumnos/Padres:**
- Password: `Suizo2026*`
