# 🚀 Guía de Despliegue en Render

## Pasos para desplegar el backend en Render

### 1. Preparar el repositorio
```bash
# Asegúrate de que todos los cambios estén en GitHub
git add .
git commit -m "Preparar backend para despliegue en Render"
git push origin josue
```

### 2. Crear cuenta en Render
1. Ve a [render.com](https://render.com)
2. Regístrate con tu cuenta de GitHub
3. Autoriza a Render para acceder a tus repositorios

### 3. Crear el servicio Web
1. Click en "New +" → "Web Service"
2. Conecta tu repositorio: `Sebin3/IEPeruanoSuizo`
3. Selecciona la rama: `josue`
4. Configura:
   - **Name:** `ieperuanosuizo-backend`
   - **Region:** Oregon (US West) o el más cercano
   - **Branch:** `josue`
   - **Root Directory:** `backend_colegio`
   - **Runtime:** `Python 3`
   - **Build Command:** `./build.sh`
   - **Start Command:** `cd backend_colegio && gunicorn config.wsgi:application`

### 4. Crear la base de datos PostgreSQL
1. En el dashboard de Render, click en "New +" → "PostgreSQL"
2. Configura:
   - **Name:** `ieperuanosuizo-db`
   - **Database:** `ieperuanosuizo`
   - **User:** `ieperuanosuizo`
   - **Region:** Mismo que el web service
   - **Plan:** Free
3. Click en "Create Database"
4. Copia la **Internal Database URL**

### 5. Configurar variables de entorno
En tu Web Service, ve a "Environment" y agrega:

```
SECRET_KEY=genera-una-clave-secreta-aqui-usa-https://djecrety.ir/
DEBUG=False
DATABASE_URL=pega-aqui-la-internal-database-url-de-postgresql
ALLOWED_HOSTS=tu-app.onrender.com
ACCESS_TOKEN_LIFETIME_MINUTES=60
REFRESH_TOKEN_LIFETIME_DAYS=7
QR_EXPIRATION_MINUTES=10
```

**Importante:** Render detecta automáticamente `RENDER_EXTERNAL_HOSTNAME`

### 6. Hacer el build.sh ejecutable
Si hay error de permisos, ejecuta localmente:
```bash
cd backend_colegio
chmod +x build.sh
git add build.sh
git commit -m "Make build.sh executable"
git push origin josue
```

### 7. Desplegar
1. Click en "Create Web Service"
2. Render automáticamente:
   - Instalará las dependencias
   - Ejecutará las migraciones
   - Cargará los datos iniciales (seed)
   - Iniciará el servidor

### 8. Verificar el despliegue
1. Espera a que el deploy termine (5-10 minutos)
2. Tu backend estará en: `https://tu-app.onrender.com`
3. Prueba los endpoints:
   - `https://tu-app.onrender.com/api/mobile/test/`
   - `https://tu-app.onrender.com/api/mobile/test-alumnos/`
   - `https://tu-app.onrender.com/admin/`

### 9. Crear superusuario (opcional)
1. En Render, ve a tu Web Service
2. Click en "Shell" (terminal)
3. Ejecuta:
```bash
cd backend_colegio
python manage.py createsuperuser
```

### 10. Actualizar la app Android
Cambia la URL en tu app:
```java
// AppConfig.java
public static final String BASE_URL = "https://tu-app.onrender.com/";
```

## 🔧 Solución de problemas

### Error: "Build failed"
- Verifica que `build.sh` sea ejecutable
- Revisa los logs en Render para ver el error específico

### Error: "Application failed to start"
- Verifica que `DATABASE_URL` esté configurada correctamente
- Revisa que `SECRET_KEY` esté configurada
- Verifica los logs en Render

### Error: "502 Bad Gateway"
- El servidor está iniciando, espera 1-2 minutos
- Si persiste, revisa los logs

### Base de datos vacía
- Ejecuta manualmente el seed:
```bash
# En el Shell de Render
cd backend_colegio
python manage.py seed
```

## 📝 Notas importantes

1. **Plan gratuito de Render:**
   - 750 horas/mes gratis
   - El servicio se "duerme" después de 15 minutos de inactividad
   - Primera petición después de dormir tarda ~30 segundos

2. **Base de datos PostgreSQL gratuita:**
   - 90 días gratis
   - Después: $7/mes o migrar a otro plan

3. **Actualizaciones:**
   - Cada push a la rama `josue` despliega automáticamente
   - Puedes desactivar auto-deploy en configuración

4. **Logs:**
   - Accede a logs en tiempo real desde el dashboard
   - Útil para debugging

## 🎉 ¡Listo!

Tu backend está desplegado y listo para usar. La URL será algo como:
`https://ieperuanosuizo-backend.onrender.com`

Copia esta URL y úsala en tu app Android.
