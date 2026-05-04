from rest_framework import status, generics
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated
from rest_framework_simplejwt.views import TokenObtainPairView
from rest_framework_simplejwt.tokens import RefreshToken

from config.permissions import EsAdmin
from .models import Usuario
from .serializers import (
    CustomTokenObtainPairSerializer, UsuarioSerializer,
    CambiarPasswordSerializer, CrearUsuarioSerializer
)


class LoginView(TokenObtainPairView):
    serializer_class = CustomTokenObtainPairSerializer

    def post(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        return Response({
            'success': True,
            'message': 'Login exitoso',
            'data': serializer.validated_data,
        }, status=status.HTTP_200_OK)


class LogoutView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        try:
            refresh_token = request.data.get('refresh')
            if not refresh_token:
                return Response(
                    {'success': False, 'message': 'Se requiere el refresh token'},
                    status=status.HTTP_400_BAD_REQUEST
                )
            token = RefreshToken(refresh_token)
            token.blacklist()
            return Response({'success': True, 'message': 'Sesión cerrada correctamente'})
        except Exception:
            return Response(
                {'success': False, 'message': 'Token inválido o expirado'},
                status=status.HTTP_400_BAD_REQUEST
            )


class PerfilView(generics.RetrieveUpdateAPIView):
    serializer_class = UsuarioSerializer
    permission_classes = [IsAuthenticated]

    def get_object(self):
        return self.request.user

    def retrieve(self, request, *args, **kwargs):
        serializer = self.get_serializer(self.get_object())
        return Response({'success': True, 'data': serializer.data})

    def update(self, request, *args, **kwargs):
        kwargs['partial'] = True
        serializer = self.get_serializer(self.get_object(), data=request.data, partial=True)
        serializer.is_valid(raise_exception=True)
        serializer.save()
        return Response({'success': True, 'message': 'Perfil actualizado', 'data': serializer.data})


class CambiarPasswordView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        serializer = CambiarPasswordSerializer(data=request.data, context={'request': request})
        serializer.is_valid(raise_exception=True)
        request.user.set_password(serializer.validated_data['password_nuevo'])
        request.user.save()
        return Response({'success': True, 'message': 'Contraseña actualizada correctamente'})


class UsuarioListCreateView(generics.ListCreateAPIView):
    permission_classes = [EsAdmin]

    def get_serializer_class(self):
        if self.request.method == 'POST':
            return CrearUsuarioSerializer
        return UsuarioSerializer

    def get_queryset(self):
        qs = Usuario.objects.all().order_by('apellidos', 'nombres')
        rol = self.request.query_params.get('rol')
        if rol:
            qs = qs.filter(rol=rol)
        return qs

    def list(self, request, *args, **kwargs):
        qs = self.get_queryset()
        serializer = UsuarioSerializer(qs, many=True)
        return Response({'success': True, 'data': serializer.data})

    def create(self, request, *args, **kwargs):
        serializer = CrearUsuarioSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        usuario = serializer.save()
        return Response(
            {'success': True, 'message': 'Usuario creado', 'data': UsuarioSerializer(usuario).data},
            status=status.HTTP_201_CREATED
        )


class UsuarioDetailView(generics.RetrieveUpdateDestroyAPIView):
    permission_classes = [EsAdmin]
    serializer_class = UsuarioSerializer
    queryset = Usuario.objects.all()

    def retrieve(self, request, *args, **kwargs):
        serializer = self.get_serializer(self.get_object())
        return Response({'success': True, 'data': serializer.data})

    def update(self, request, *args, **kwargs):
        kwargs['partial'] = True
        serializer = self.get_serializer(self.get_object(), data=request.data, partial=True)
        serializer.is_valid(raise_exception=True)
        serializer.save()
        return Response({'success': True, 'message': 'Usuario actualizado', 'data': serializer.data})

    def destroy(self, request, *args, **kwargs):
        usuario = self.get_object()
        usuario.is_active = False
        usuario.save(update_fields=['is_active'])
        return Response({'success': True, 'message': 'Usuario desactivado'})
