from django.urls import path, include
from rest_framework.routers import DefaultRouter
from .views import (
    GradoViewSet, SeccionViewSet, AlumnoViewSet, CursoViewSet,
    SesionClaseViewSet, AsistenciaViewSet, RegistrarAsistenciaQRView,
    EscanearQRAlumnoView
)

router = DefaultRouter()
router.register('grados', GradoViewSet, basename='grado')
router.register('secciones', SeccionViewSet, basename='seccion')
router.register('alumnos', AlumnoViewSet, basename='alumno')
router.register('cursos', CursoViewSet, basename='curso')
router.register('sesiones', SesionClaseViewSet, basename='sesion')
router.register('registros', AsistenciaViewSet, basename='asistencia')

urlpatterns = [
    path('', include(router.urls)),
    path('registrar-qr/', RegistrarAsistenciaQRView.as_view(), name='registrar_qr'),
    path('escanear-qr/', EscanearQRAlumnoView.as_view(), name='escanear_qr_alumno'),
]
