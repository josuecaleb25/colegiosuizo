# Backend Sistema de Asistencia - Colegio

Django + DRF backend para sistema de asistencia QR, comunicados y permisos.

## Estructura

```
backend_colegio/
├── config/               # Configuración global
│   ├── settings.py       # Settings con python-decouple
│   ├── urls.py           # URLs raíz
│   ├── exceptions.py     # Handler de errores customizado
│   └── permissions.py    # Permisos por rol
├── usuarios/             # Auth + usuarios del sistema
├── asistencia/           # QR, sesiones, alumnos, asistencias
├── comunicados/          # Comunicados del colegio
├── permisos/             # Permisos de salida/tardanza
├── reportes/             # Endpoints de reportes
├── requirements.txt
├── seed_data.py          # Datos de prueba
└── .env.example
```

## Setup rápido

```bash
# 1. Clonar y entrar al proyecto
cd backend_colegio

# 2. Crear entorno virtual
python -m venv venv
source venv/bin/activate        # Linux/Mac
venv\Scripts\activate           # Windows

# 3. Instalar dependencias
pip install -r requirements.txt

# 4. Configurar variables de entorno
cp .env.example .env
# Editar .env con tus datos de PostgreSQL y SECRET_KEY

# 5. Crear la base de datos en PostgreSQL
# psql -U postgres -c "CREATE DATABASE colegio_db;"

# 6. Migraciones
python manage.py migrate

# 7. Cargar datos de prueba (opcional)
python manage.py shell < seed_data.py

# 8. Correr el servidor
python manage.py runserver
```

## Endpoints principales

### Auth
| Método | URL | Descripción | Rol |
|--------|-----|-------------|-----|
| POST | `/api/v1/auth/login/` | Login | Todos |
| POST | `/api/v1/auth/logout/` | Logout (blacklist token) | Todos |
| POST | `/api/v1/auth/token/refresh/` | Renovar token | Todos |
| GET/PATCH | `/api/v1/auth/perfil/` | Ver/editar perfil | Todos |
| POST | `/api/v1/auth/cambiar-password/` | Cambiar contraseña | Todos |

### Asistencia
| Método | URL | Descripción | Rol |
|--------|-----|-------------|-----|
| GET | `/api/v1/asistencia/sesiones/` | Listar sesiones | Admin/Profesor |
| POST | `/api/v1/asistencia/sesiones/` | Crear sesión | Admin/Profesor |
| POST | `/api/v1/asistencia/sesiones/{id}/generar-qr/` | Generar QR | Admin/Profesor |
| POST | `/api/v1/asistencia/sesiones/{id}/cerrar/` | Cerrar sesión | Admin/Profesor |
| GET | `/api/v1/asistencia/sesiones/{id}/asistencias/` | Ver asistencias | Admin/Profesor |
| POST | `/api/v1/asistencia/registrar-qr/` | Registrar asistencia via QR | Todos |
| GET | `/api/v1/asistencia/alumnos/?seccion=1` | Alumnos por sección | Admin/Profesor |

### Comunicados
| Método | URL | Descripción |
|--------|-----|-------------|
| GET | `/api/v1/comunicados/` | Listar comunicados |
| POST | `/api/v1/comunicados/` | Crear comunicado (Admin/Profesor) |
| POST | `/api/v1/comunicados/{id}/marcar-leido/` | Marcar como leído |

### Permisos
| Método | URL | Descripción |
|--------|-----|-------------|
| GET | `/api/v1/permisos/?estado=pendiente` | Listar permisos |
| POST | `/api/v1/permisos/` | Solicitar permiso |
| POST | `/api/v1/permisos/{id}/revisar/` | Aprobar/rechazar (Admin/Profesor) |

### Reportes
| Método | URL | Descripción |
|--------|-----|-------------|
| GET | `/api/v1/reportes/asistencia/sesion/{id}/` | Reporte por sesión |
| GET | `/api/v1/reportes/asistencia/alumno/{id}/` | Reporte por alumno |
| GET | `/api/v1/reportes/asistencia/seccion/{id}/?fecha=2024-01-15` | Reporte por sección y fecha |

## Flujo QR

1. Profesor crea una `SesionClase` (POST `/sesiones/`)
2. Profesor genera el QR (POST `/sesiones/{id}/generar-qr/`) → recibe imagen base64 + token
3. El QR tiene expiración configurable (default: 10 min, variable `QR_EXPIRATION_MINUTES`)
4. App Android escanea el QR y envía `{token, alumno_id}` a `/registrar-qr/`
5. Al terminar la clase, el profesor cierra la sesión (POST `/sesiones/{id}/cerrar/`)
   - Todos los alumnos sin registro quedan como "ausente" automáticamente
   - El QR queda invalidado

## Roles
- `admin` → acceso total
- `profesor` → ve solo sus cursos/sesiones, puede gestionar asistencias y aprobar permisos
- `padre` → ve comunicados generales y puede solicitar permisos (módulo expandible)

## Próximos módulos
- `padres/` → relación padre-alumno, notificaciones
- `pagos/` → control de pensiones
- `horarios/` → horario escolar por sección
