from django.urls import path, include
from rest_framework.routers import DefaultRouter
from .views import PermisoSalidaViewSet

router = DefaultRouter()
router.register('', PermisoSalidaViewSet, basename='permiso')

urlpatterns = [path('', include(router.urls))]
