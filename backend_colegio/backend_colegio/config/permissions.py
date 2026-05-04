from rest_framework.permissions import BasePermission


class EsAdmin(BasePermission):
    def has_permission(self, request, view):
        return request.user.is_authenticated and request.user.rol == 'admin'


class EsProfesor(BasePermission):
    def has_permission(self, request, view):
        return request.user.is_authenticated and request.user.rol in ('profesor', 'admin')


class EsPadre(BasePermission):
    def has_permission(self, request, view):
        return request.user.is_authenticated and request.user.rol in ('padre', 'admin')


class EsAdminOProfesor(BasePermission):
    def has_permission(self, request, view):
        return request.user.is_authenticated and request.user.rol in ('admin', 'profesor')
