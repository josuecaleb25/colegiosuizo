from django.urls import path
from .views import DashboardView, ReporteAsistenciaCSVView, ReporteDocenteCSVView

urlpatterns = [
    path('dashboard/', DashboardView.as_view(), name='dashboard'),
    path('asistencia-csv/', ReporteAsistenciaCSVView.as_view(), name='reporte_asistencia_csv'),
    path('docentes-csv/', ReporteDocenteCSVView.as_view(), name='reporte_docentes_csv'),
]
