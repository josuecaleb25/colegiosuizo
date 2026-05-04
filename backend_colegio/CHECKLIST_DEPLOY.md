# ✅ Checklist de Despliegue - Render

## Antes de desplegar

- [ ] Todos los cambios están commiteados
- [ ] El código está en la rama `josue` en GitHub
- [ ] Has probado el backend localmente
- [ ] Tienes cuenta en Render.com

## Archivos creados para producción

- [x] `requirements.txt` - Dependencias de Python
- [x] `build.sh` - Script de construcción para Render
- [x] `runtime.txt` - Versión de Python
- [x] `render.yaml` - Configuración automática (opcional)
- [x] `.env.example` - Ejemplo de variables de entorno
- [x] `DEPLOY_RENDER.md` - Guía completa de despliegue
- [x] `generate_secret_key.py` - Generador de SECRET_KEY

## Cambios en settings.py

- [x] Importado `dj_database_url` y `os`
- [x] Configurado `ALLOWED_HOSTS` para Render
- [x] Agregado middleware `WhiteNoiseMiddleware`
- [x] Configurado base de datos PostgreSQL con fallback a SQLite
- [x] Configurado `CORS_ALLOW_ALL_ORIGINS = True` para app móvil
- [x] Agregado `STATICFILES_STORAGE` para WhiteNoise
- [x] Configurado seguridad para producción (SSL, cookies seguras, etc.)

## En Render.com

### 1. Crear PostgreSQL Database
- [ ] New + → PostgreSQL
- [ ] Name: `ieperuanosuizo-db`
- [ ] Plan: Free
- [ ] Copiar "Internal Database URL"

### 2. Crear Web Service
- [ ] New + → Web Service
- [ ] Conectar repositorio: `Sebin3/IEPeruanoSuizo`
- [ ] Branch: `josue`
- [ ] Root Directory: `backend_colegio`
- [ ] Build Command: `./build.sh`
- [ ] Start Command: `cd backend_colegio && gunicorn config.wsgi:application`

### 3. Variables de Entorno
Agregar en Environment:

```
SECRET_KEY=<generar-con-generate_secret_key.py>
DEBUG=False
DATABASE_URL=<internal-database-url-de-postgresql>
ALLOWED_HOSTS=<tu-app>.onrender.com
ACCESS_TOKEN_LIFETIME_MINUTES=60
REFRESH_TOKEN_LIFETIME_DAYS=7
QR_EXPIRATION_MINUTES=10
```

### 4. Desplegar
- [ ] Click "Create Web Service"
- [ ] Esperar 5-10 minutos
- [ ] Verificar que el deploy sea exitoso

### 5. Verificar
- [ ] Abrir `https://<tu-app>.onrender.com/api/mobile/test/`
- [ ] Debe responder: `{"success": true, "message": "..."}`
- [ ] Probar: `https://<tu-app>.onrender.com/api/mobile/test-alumnos/`
- [ ] Debe mostrar lista de alumnos

### 6. Crear Superusuario (Opcional)
- [ ] En Render → Shell
- [ ] `cd backend_colegio`
- [ ] `python manage.py createsuperuser`
- [ ] Acceder a `https://<tu-app>.onrender.com/admin/`

## Actualizar App Android

- [ ] Abrir `AppConfig.java`
- [ ] Cambiar `BASE_URL` a `https://<tu-app>.onrender.com/`
- [ ] Compilar y probar la app

## Comandos útiles

### Generar SECRET_KEY
```bash
cd backend_colegio
python generate_secret_key.py
```

### Hacer build.sh ejecutable (si es necesario)
```bash
cd backend_colegio
chmod +x build.sh
git add build.sh
git commit -m "Make build.sh executable"
git push origin josue
```

### Probar localmente con PostgreSQL
```bash
# Instalar PostgreSQL localmente
# Crear base de datos
# Configurar DATABASE_URL en .env
python manage.py migrate
python manage.py seed
python manage.py runserver
```

## Solución de problemas comunes

### Build falla
- Verificar que `build.sh` sea ejecutable
- Revisar logs en Render
- Verificar que `requirements.txt` esté correcto

### Application failed to start
- Verificar `DATABASE_URL` en variables de entorno
- Verificar `SECRET_KEY` en variables de entorno
- Revisar logs para ver error específico

### 502 Bad Gateway
- Esperar 1-2 minutos (el servidor está iniciando)
- Si persiste, revisar logs

### No hay datos en la base de datos
- Ejecutar seed manualmente en Shell de Render:
  ```bash
  cd backend_colegio
  python manage.py seed
  ```

## URLs importantes

- **Render Dashboard:** https://dashboard.render.com
- **Documentación Render:** https://render.com/docs
- **Tu backend:** https://<tu-app>.onrender.com
- **Admin Django:** https://<tu-app>.onrender.com/admin/

## Notas finales

✅ El backend está configurado para:
- Usar PostgreSQL en producción
- Usar SQLite en desarrollo
- Servir archivos estáticos con WhiteNoise
- Permitir CORS para la app móvil
- Seguridad habilitada en producción
- Auto-deploy desde GitHub

🎉 ¡Todo listo para desplegar!
