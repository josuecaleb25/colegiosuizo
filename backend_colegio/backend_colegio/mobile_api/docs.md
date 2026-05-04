# API Móvil - IE Peruano Suizo

## Endpoints disponibles para la app móvil

### Autenticación

#### POST /api/mobile/auth/login/
Login de usuario
```json
{
  "email": "usuario@ejemplo.com",
  "password": "contraseña"
}
```
Respuesta:
```json
{
  "success": true,
  "message": "Login exitoso",
  "data": {
    "user": {
      "id": 1,
      "email": "usuario@ejemplo.com",
      "nombres": "Juan",
      "apellidos": "Pérez",
      "nombre_completo": "Juan Pérez",
      "rol": "padre",
      "telefono": "123456789"
    },
    "tokens": {
      "access": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
      "refresh": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
    }
  }
}
```

#### POST /api/mobile/auth/register/
Registro de nuevo usuario (solo padres)
```json
{
  "email": "nuevo@ejemplo.com",
  "password": "contraseña",
  "nombres": "María",
  "apellidos": "García",
  "telefono": "987654321"
}
```

### Perfil

#### GET /api/mobile/profile/
Obtener datos del usuario autenticado
Requiere: Authorization: Bearer {token}

### Asistencia

#### GET /api/mobile/asistencia/alumnos/
Lista de alumnos con su asistencia del día
Query params opcionales:
- salon: Filtrar por salón (ej: "1ro A")
- search: Buscar por nombre

Respuesta:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "nombre_completo": "Carlos Ramos",
      "salon": "1ro - Secundaria - A",
      "hora_entrada": "06:50 am",
      "estado_entrada": "presente"
    }
  ]
}
```

#### POST /api/mobile/asistencia/escanear-qr/
Registrar asistencia por QR
```json
{
  "qr_token": "uuid-del-qr"
}
```

### Datos generales

#### GET /api/mobile/salones/
Lista de salones/secciones disponibles

#### GET /api/mobile/cursos/
Lista de cursos/materias

## Estados de asistencia
- `presente`: Llegó a tiempo (antes de 7:31 AM)
- `tardanza`: Llegó tarde (después de 7:31 AM)
- `ausente`: No registró asistencia
- `justificado`: Falta justificada

## Autenticación
Todos los endpoints excepto login, register y escanear-qr requieren el header:
```
Authorization: Bearer {access_token}
```