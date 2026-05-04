# 🚀 Instrucciones de Inicio - Backend Express

## ✅ Estado Actual

El backend Express.js está **completamente configurado** y listo para usar.

---

## 📋 Pasos para Iniciar

### 1. Instalar Node.js
Si no tienes Node.js instalado:
- Descargar desde: https://nodejs.org/ (versión 18 o superior)
- Verificar instalación:
  ```bash
  node --version
  npm --version
  ```

### 2. Instalar Dependencias
```bash
cd backend_express
npm install
```

Esto instalará:
- express (servidor web)
- pg (PostgreSQL client)
- typescript (compilador)
- cors, dotenv, bcryptjs, jsonwebtoken, qrcode, uuid

### 3. Configurar Variables de Entorno
Crear archivo `.env` en la raíz de `backend_express/`:

```env
# Supabase Configuration
SUPABASE_URL=https://dhirwwytreumhebccuht.supabase.co
SUPABASE_ANON_KEY=tu_anon_key_aqui

# Server
PORT=8000
NODE_ENV=development

# JWT
JWT_SECRET=4j3n+6=2^fk_cp6q4)f8-+8zgekyz2zojx5uyamn(^cao6b(zw
JWT_EXPIRES_IN=7d

# CORS
ALLOWED_ORIGINS=*
```

**¿Dónde obtener las credenciales de Supabase?**

1. Ve a tu proyecto en [Supabase Dashboard](https://supabase.com/dashboard)
2. Selecciona tu proyecto: `dhirwwytreumhebccuht`
3. Ve a **Settings** (⚙️) > **API**
4. Copia:
   - **Project URL** → Pégalo en `SUPABASE_URL`
   - **anon public** key → Pégalo en `SUPABASE_ANON_KEY`

La `anon key` es segura para usar en el cliente porque Supabase maneja los permisos con Row Level Security (RLS).

### 4. Iniciar el Servidor
```bash
npm run dev
```

Deberías ver:
```
🚀 Servidor corriendo en puerto 8000
📍 Entorno: development
✅ Base de datos conectada: 2026-05-04 ...
```

### 5. Probar la Conexión
Abrir en el navegador:
- http://localhost:8000/ - Debe mostrar mensaje de bienvenida
- http://localhost:8000/api/mobile/test - Test de conexión
- http://localhost:8000/api/mobile/test-alumnos - Test con 10 alumnos

---

## 🔧 Comandos Disponibles

```bash
# Desarrollo (con hot reload)
npm run dev

# Compilar TypeScript a JavaScript
npm run build

# Ejecutar en producción (después de build)
npm start

# Cargar datos de prueba (cuando esté implementado)
npm run seed
```

---

## 📱 Conectar con la App Android

### Opción 1: Emulador Android Studio
1. Iniciar el backend: `npm run dev`
2. En `AppConfig.java` usar:
   ```java
   public static final String BASE_URL = "http://10.0.2.2:8000/";
   ```

### Opción 2: Dispositivo Físico
1. Conectar PC y dispositivo a la misma red WiFi
2. Obtener IP de tu PC:
   - Windows: `ipconfig` (buscar IPv4)
   - Mac/Linux: `ifconfig` o `ip addr`
3. Iniciar el backend: `npm run dev`
4. En `AppConfig.java` usar tu IP:
   ```java
   public static final String BASE_URL = "http://TU_IP:8000/";
   ```
   Ejemplo: `http://192.168.101.9:8000/`

---

## 🧪 Probar los Endpoints

### Con curl (Terminal)
```bash
# Test de conexión
curl http://localhost:8000/api/mobile/test

# Login
curl -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@colegio.com","password":"admin123"}'

# Obtener alumnos para asistencia
curl http://localhost:8000/api/mobile/asistencia/alumnos
```

### Con Postman o Thunder Client
1. Importar colección de endpoints
2. Probar cada endpoint individualmente

---

## 📊 Endpoints Disponibles

### Autenticación
- `POST /api/auth/login` - Login
- `POST /api/auth/register` - Registro

### Mobile API
- `GET /api/mobile/test` - Test
- `GET /api/mobile/test-alumnos` - Test con datos
- `GET /api/mobile/usuarios` - Usuarios con QR
- `GET /api/mobile/asistencia/alumnos` - Alumnos para asistencia
- `POST /api/mobile/asistencia/escanear-qr` - Escanear QR

### Alumnos
- `GET /api/alumnos` - Listar
- `GET /api/alumnos/:id` - Obtener uno
- `POST /api/alumnos` - Crear
- `PUT /api/alumnos/:id` - Actualizar
- `DELETE /api/alumnos/:id` - Eliminar

### Asistencia
- `GET /api/asistencia` - Listar
- `POST /api/asistencia` - Registrar
- `PUT /api/asistencia/:id` - Actualizar
- `DELETE /api/asistencia/:id` - Eliminar
- `GET /api/asistencia/estadisticas` - Estadísticas

---

## 🔐 Credenciales de Prueba

### Admin
```
Email: admin@colegio.com
Password: admin123
```

### Profesor
```
Email: profesor@colegio.com
Password: profesor123
```

### Alumnos/Padres
```
Email: {apellido}{nombre}@peruanosuizo.edu.pe
Password: Suizo2026*
```

---

## ❌ Solución de Problemas

### Error: "Cannot find module 'express'"
```bash
npm install
```

### Error: "Port 8000 is already in use"
1. Cambiar puerto en `.env`: `PORT=3000`
2. O matar el proceso en puerto 8000:
   - Windows: `netstat -ano | findstr :8000` luego `taskkill /PID <PID> /F`
   - Mac/Linux: `lsof -ti:8000 | xargs kill`

### Error: "Connection refused" a la base de datos
1. Verificar que `DATABASE_URL` en `.env` sea correcta
2. Verificar conexión a internet
3. Probar conexión directa:
   ```bash
   psql "postgresql://postgres:FQDF3KSKrw4f9ZRj@db.dhirwwytreumhebccuht.supabase.co:5432/postgres"
   ```

### App Android no conecta
1. Verificar que el backend esté corriendo (`npm run dev`)
2. Verificar la IP en `AppConfig.java`
3. Verificar que ambos estén en la misma red WiFi
4. Desactivar firewall temporalmente para probar

---

## 📝 Notas Importantes

- ✅ El servidor debe estar corriendo para que la app funcione
- ✅ La base de datos ya tiene 158 alumnos cargados
- ✅ Los endpoints están listos para usar
- ⚠️ En desarrollo, el servidor se reinicia automáticamente al guardar cambios
- ⚠️ Para producción, usar `npm run build` y luego `npm start`

---

## 🎯 Siguiente Paso

Una vez que el backend esté corriendo localmente y probado:

1. **Deploy a Railway**
   - Crear proyecto en Railway
   - Conectar repositorio GitHub
   - Configurar variables de entorno
   - Deploy automático

2. **Actualizar App Android**
   - Cambiar `BASE_URL` a la URL de Railway
   - Compilar APK de producción

---

**¿Necesitas ayuda?**
- Revisa el archivo `README.md` para más detalles
- Revisa `MIGRACION_EXPRESS.md` para entender los cambios
- Verifica los logs del servidor para errores específicos
