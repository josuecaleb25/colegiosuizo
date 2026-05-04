from rest_framework import serializers
from rest_framework_simplejwt.serializers import TokenObtainPairSerializer
from .models import Usuario


class CustomTokenObtainPairSerializer(TokenObtainPairSerializer):
    @classmethod
    def get_token(cls, user):
        token = super().get_token(user)
        token['nombre_completo'] = user.nombre_completo
        token['rol'] = user.rol
        token['email'] = user.email
        return token

    def validate(self, attrs):
        data = super().validate(attrs)
        data['usuario'] = UsuarioSerializer(self.user).data
        return data


class UsuarioSerializer(serializers.ModelSerializer):
    nombre_completo = serializers.ReadOnlyField()
    qr_image = serializers.SerializerMethodField()

    class Meta:
        model = Usuario
        fields = ['id', 'email', 'nombres', 'apellidos', 'nombre_completo', 'rol', 'telefono', 'is_active', 'qr_token', 'qr_image']
        read_only_fields = ['id', 'email', 'qr_token', 'qr_image']

    def get_qr_image(self, obj):
        import qrcode, io, base64
        from PIL import Image, ImageDraw, ImageFont

        qr = qrcode.QRCode(version=1, error_correction=qrcode.constants.ERROR_CORRECT_H, box_size=10, border=4)
        qr.add_data(str(obj.qr_token))
        qr.make(fit=True)
        qr_img = qr.make_image(fill_color="black", back_color="white").convert('RGB')
        qr_w, qr_h = qr_img.size

        padding = 12
        font_size = 22
        try:
            font = ImageFont.truetype("arial.ttf", font_size)
            font_small = ImageFont.truetype("arial.ttf", 16)
        except Exception:
            font = ImageFont.load_default()
            font_small = font

        nombre = obj.nombre_completo
        dummy = Image.new('RGB', (1, 1))
        draw_dummy = ImageDraw.Draw(dummy)
        _, _, tw1, th1 = draw_dummy.textbbox((0, 0), nombre, font=font)
        text_area_h = th1 + padding * 3

        canvas = Image.new('RGB', (qr_w, qr_h + text_area_h), 'white')
        canvas.paste(qr_img, (0, 0))
        draw = ImageDraw.Draw(canvas)
        draw.text(((qr_w - tw1) // 2, qr_h + padding), nombre, fill='black', font=font)
        _, _, tw2, _ = draw.textbbox((0, 0), obj.email, font=font_small)
        draw.text(((qr_w - tw2) // 2, qr_h + padding + th1 + 4), obj.email, fill='gray', font=font_small)

        buffer = io.BytesIO()
        canvas.save(buffer, format='PNG')
        return f"data:image/png;base64,{base64.b64encode(buffer.getvalue()).decode()}"


class CrearUsuarioSerializer(serializers.ModelSerializer):
    password = serializers.CharField(write_only=True, min_length=6)

    class Meta:
        model = Usuario
        fields = ['email', 'nombres', 'apellidos', 'rol', 'telefono', 'password']

    def create(self, validated_data):
        password = validated_data.pop('password')
        usuario = Usuario(**validated_data)
        usuario.set_password(password)
        usuario.save()
        return usuario


class CambiarPasswordSerializer(serializers.Serializer):
    password_actual = serializers.CharField(write_only=True)
    password_nuevo = serializers.CharField(write_only=True, min_length=8)
    password_confirmacion = serializers.CharField(write_only=True)

    def validate(self, attrs):
        if attrs['password_nuevo'] != attrs['password_confirmacion']:
            raise serializers.ValidationError({'password_confirmacion': 'Las contraseñas no coinciden'})
        return attrs

    def validate_password_actual(self, value):
        user = self.context['request'].user
        if not user.check_password(value):
            raise serializers.ValidationError('La contraseña actual es incorrecta')
        return value
