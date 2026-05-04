from django.urls import path
from .views import AsistenciaDocenteListView, EscanearQRDocenteView

urlpatterns = [
    path('', AsistenciaDocenteListView.as_view(), name='asistencia_docente_list'),
    path('escanear-qr/', EscanearQRDocenteView.as_view(), name='escanear_qr_docente'),
]
