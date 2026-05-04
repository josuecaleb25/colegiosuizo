from django.contrib import admin
from django.urls import path, include
from django.conf import settings
from django.conf.urls.static import static

urlpatterns = [
    path('admin/', admin.site.urls),
    path('api/v1/auth/', include('usuarios.urls')),
    path('api/v1/asistencia/', include('asistencia.urls')),
    path('api/v1/asistencia-docente/', include('asistencia_docente.urls')),
    path('api/v1/comunicados/', include('comunicados.urls')),
    path('api/v1/permisos/', include('permisos.urls')),
    path('api/v1/reportes/', include('reportes.urls')),
    path('api/v1/portal-padres/', include('portal_padres.urls')),
    path('api/mobile/', include('mobile_api.urls')),  # Endpoints para app móvil
]

if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
