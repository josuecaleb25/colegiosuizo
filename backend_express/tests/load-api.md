# Pruebas de carga de la API

El ejecutor usa Autocannon y envía únicamente solicitudes GET. Por defecto prueba `http://127.0.0.1:8000/healthz` con 10 conexiones durante 15 segundos. Los recorridos usan `requests` de Autocannon para que la ruta configurada sí sea la ruta medida.

## Preparación

En una terminal, desde `backend_express`, inicia el backend:

```sh
npm run dev
```

En otra terminal, valida la configuración sin enviar solicitudes:

```sh
LOAD_DRY_RUN=1 npm run load:api
```

Para simular la actividad de alumnado en varias rutas, usa `npm run load:students`. Ese escenario crea tokens JWT sintéticos únicos con rol `alumno`, ejecuta solo GET y no escribe datos. Las identidades no corresponden a matrículas reales; las rutas de datos personales pueden responder sin información personalizada.

```sh
LOAD_DRY_RUN=1 npm run load:students
```

Ejecuta la prueba predeterminada:

```sh
npm run load:api
```

## Rutas de lectura permitidas

- `/healthz`
- `/api/horarios/test`
- `/api/horarios/alumno/:personaId`
- `/api/horarios/profesor/:personaId`
- `/api/horarios/seccion/:seccionId`
- `/api/mobile/test`

Las rutas que requieren autenticación reciben el token mediante `LOAD_BEARER_TOKEN`. El valor del token no se imprime.

## Parámetros

| Variable | Valor inicial | Límite | Uso |
| --- | ---: | ---: | --- |
| `LOAD_BASE_URL` | `http://127.0.0.1:8000` | localhost por defecto | Base URL del backend |
| `LOAD_PATH` | `/healthz` | rutas permitidas | Ruta GET que se medirá |
| `LOAD_CONNECTIONS` | `10` | 1–1000 | Conexiones concurrentes |
| `LOAD_DURATION_SECONDS` | `15` | 1–300 | Duración de la prueba |
| `LOAD_BEARER_TOKEN` | vacío | — | JWT para rutas protegidas |
| `LOAD_DRY_RUN` | `0` | — | `1` imprime la configuración sin conectar |
| `LOAD_CONFIRM_HIGH_CONCURRENCY` | `0` | — | `1` habilita más de 100 conexiones |
| `LOAD_ALLOW_REMOTE` | `0` | — | `1` permite usar una base URL remota |
| `LOAD_REQUESTS_PER_SECOND` | `100` en `load:students` | 1–1000 | Límite total de solicitudes del recorrido estudiantil |

Ejemplo con una ruta autenticada y carga moderada:

```sh
LOAD_PATH=/api/horarios/alumno/ID_DE_PRUEBA LOAD_BEARER_TOKEN='TOKEN_LOCAL' LOAD_CONNECTIONS=25 LOAD_DURATION_SECONDS=30 npm run load:api
```

Para subir la concurrencia, aumenta las conexiones gradualmente y revisa latencia, errores, CPU y conexiones de la base de datos entre ejecuciones. Más de 100 conexiones requiere `LOAD_CONFIRM_HIGH_CONCURRENCY=1`. Las pruebas contra hosts remotos requieren `LOAD_ALLOW_REMOTE=1`; no ejecutes pruebas de carga contra producción sin autorización y una ventana acordada. Autocannon también usa CPU local, así que un generador saturado puede limitar el resultado antes que el backend.

El recorrido estudiantil consulta horarios, `/api/comunicados`, `/api/secciones`, `/api/notificaciones`, `/api/notificaciones/no-leidas` y `/api/mobile/asistencia/dias-asistidos/:personaId`. No simula login, escrituras, FCM ni WebSockets.
