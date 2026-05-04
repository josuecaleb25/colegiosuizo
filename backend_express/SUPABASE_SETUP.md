# 🔐 Configuración de Supabase

Este backend usa el **cliente oficial de Supabase** (`@supabase/supabase-js`) en lugar de conexión directa a PostgreSQL.

---

## ✅ Ventajas de Usar el Cliente de Supabase

1. **Más seguro** - Usa `anon key` en lugar de contraseña de base de datos
2. **Row Level Security (RLS)** - Supabase maneja permisos automáticamente
3. **Mejor rendimiento** - Optimizado para trabajar con Supabase
4. **Más fácil de configurar** - Solo necesitas URL y anon key
5. **Compatible con Edge Functions** - Funciona en Railway, Vercel, Netlify, etc.

---

## 📋 Obtener Credenciales de Supabase

### Paso 1: Ir al Dashboard de Supabase
1. Ve a: https://supabase.com/dashboard
2. Inicia sesión con tu cuenta
3. Selecciona tu proyecto: **dhirwwytreumhebccuht**

### Paso 2: Ir a Settings > API
1. En el menú lateral, haz clic en **Settings** (⚙️)
2. Luego haz clic en **API**

### Paso 3: Copiar las Credenciales
Verás dos secciones importantes:

#### Project URL
```
https://dhirwwytreumhebccuht.supabase.co
```
👆 Copia esto y pégalo en `SUPABASE_URL`

#### Project API keys
Verás varias keys, necesitas la **anon public**:

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRoaXJ3d3l0cmV1bWhlYmNjdWh0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE2ODk1MjM0NTYsImV4cCI6MjAwNTA5OTQ1Nn0.xxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```
👆 Copia esto y pégalo en `SUPABASE_ANON_KEY`

**⚠️ IMPORTANTE:** 
- **NO uses** la `service_role` key en el backend público
- La `anon` key es segura para usar porque Supabase maneja los permisos
- La `anon` key puede ser expuesta en el cliente

---

## 🔧 Configurar el Archivo .env

Crea un archivo `.env` en la raíz de `backend_express/`:

```env
# Supabase Configuration
SUPABASE_URL=https://dhirwwytreumhebccuht.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRoaXJ3d3l0cmV1bWhlYmNjdWh0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE2ODk1MjM0NTYsImV4cCI6MjAwNTA5OTQ1Nn0.xxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# Server
PORT=8000
NODE_ENV=development

# JWT (para autenticación custom)
JWT_SECRET=4j3n+6=2^fk_cp6q4)f8-+8zgekyz2zojx5uyamn(^cao6b(zw
JWT_EXPIRES_IN=7d

# CORS
ALLOWED_ORIGINS=*
```

---

## 🧪 Verificar la Conexión

Después de configurar el `.env`, inicia el servidor:

```bash
npm run dev
```

Deberías ver:
```
🚀 Servidor corriendo en puerto 8000
📍 Entorno: development
🔗 Supabase URL: https://dhirwwytreumhebccuht.supabase.co
✅ Supabase conectado correctamente
```

Si ves el mensaje "✅ Supabase conectado correctamente", ¡todo está funcionando!

---

## 🔍 Probar los Endpoints

### Test de Conexión
```bash
curl http://localhost:8000/api/mobile/test
```

Respuesta esperada:
```json
{
  "success": true,
  "message": "Endpoint móvil funcionando correctamente",
  "timestamp": "2026-05-04T..."
}
```

### Test con Datos
```bash
curl http://localhost:8000/api/mobile/test-alumnos
```

Respuesta esperada:
```json
{
  "success": true,
  "total": 10,
  "data": [
    {
      "nombre_completo": "LISETH SAYURI ABAD VILLANERA",
      "salon": "1ro A"
    },
    ...
  ]
}
```

---

## 🚨 Solución de Problemas

### Error: "Faltan variables de entorno"
```
Error: Faltan variables de entorno: SUPABASE_URL y SUPABASE_ANON_KEY son requeridas
```

**Solución:**
1. Verifica que el archivo `.env` existe en `backend_express/`
2. Verifica que las variables están correctamente escritas (sin espacios)
3. Reinicia el servidor: `Ctrl+C` y luego `npm run dev`

### Error: "Error al conectar con Supabase"
```
❌ Error al conectar con Supabase: Invalid API key
```

**Solución:**
1. Verifica que copiaste la `anon` key completa (es muy larga)
2. Verifica que no hay espacios al inicio o final
3. Verifica que estás usando la key correcta del proyecto

### Error: "Failed to fetch"
```
❌ Error de conexión: Failed to fetch
```

**Solución:**
1. Verifica tu conexión a internet
2. Verifica que el proyecto de Supabase está activo
3. Verifica que la URL es correcta

---

## 📊 Diferencias con PostgreSQL Directo

### Antes (PostgreSQL directo)
```typescript
import { Pool } from 'pg';

const pool = new Pool({
  connectionString: 'postgresql://postgres:password@...'
});

const result = await pool.query('SELECT * FROM tabla WHERE id = $1', [id]);
const data = result.rows;
```

### Ahora (Cliente Supabase)
```typescript
import { createClient } from '@supabase/supabase-js';

const supabase = createClient(url, anonKey);

const { data, error } = await supabase
  .from('tabla')
  .select('*')
  .eq('id', id);
```

**Ventajas:**
- ✅ Sintaxis más limpia y legible
- ✅ Manejo automático de errores
- ✅ TypeScript support mejorado
- ✅ Joins más fáciles con relaciones
- ✅ Filtros y ordenamiento más intuitivos

---

## 🔐 Seguridad

### ¿Es seguro usar la anon key?

**SÍ**, porque:
1. Supabase usa **Row Level Security (RLS)** para controlar acceso
2. La `anon` key solo da permisos básicos de lectura
3. Las operaciones sensibles requieren autenticación adicional
4. Puedes configurar políticas de RLS en Supabase Dashboard

### ¿Cuándo usar service_role key?

**NUNCA** en el backend público. Solo úsala para:
- Scripts de administración
- Migraciones de base de datos
- Operaciones de mantenimiento
- Backend privado (no expuesto a internet)

---

## 📚 Recursos Adicionales

- [Documentación de Supabase](https://supabase.com/docs)
- [Cliente JavaScript](https://supabase.com/docs/reference/javascript/introduction)
- [Row Level Security](https://supabase.com/docs/guides/auth/row-level-security)
- [API Reference](https://supabase.com/docs/reference/javascript/select)

---

**¿Necesitas ayuda?**
- Revisa los logs del servidor para errores específicos
- Verifica que las credenciales son correctas en Supabase Dashboard
- Asegúrate de que el proyecto de Supabase está activo
