from django.urls import path
from .views import MiHijoView, HistorialHijoView, ComunicadosPadreView

urlpatterns = [
    path('mi-hijo/', MiHijoView.as_view(), name='mi_hijo'),
    path('historial/', HistorialHijoView.as_view(), name='historial_hijo'),
    path('comunicados/', ComunicadosPadreView.as_view(), name='comunicados_padre'),
]
