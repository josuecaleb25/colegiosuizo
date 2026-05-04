from rest_framework import serializers
from .models import Grado, Seccion, Alumno, Curso, SesionClase, TokenQR, Asistencia


class GradoSerializer(serializers.ModelSerializer):
    class Meta:
        model = Grado
        fields = '__all__'


class SeccionSerializer(serializers.ModelSerializer):
    grado_nombre = serializers.CharField(source='grado.nombre', read_only=True)

    class Meta:
        model = Seccion
        fields = ['id', 'grado', 'grado_nombre', 'nombre', 'tutor', 'activa']


class AlumnoSerializer(serializers.ModelSerializer):
    nombre_completo = serializers.ReadOnlyField()
    seccion_nombre = serializers.CharField(source='seccion.__str__', read_only=True)
    qr_image = serializers.SerializerMethodField()
    id_estudiante = serializers.CharField(source='dni', read_only=True)

    class Meta:
        model = Alumno
        fields = ['id', 'codigo', 'nombres', 'apellidos', 'nombre_completo', 'id_estudiante', 'seccion', 'seccion_nombre', 'qr_token', 'qr_image', 'email_padre', 'activo']
        read_only_fields = ['qr_token', 'qr_image', 'id_estudiante']

    def get_qr_image(self, obj):
        import qrcode, io, base64
        from PIL import Image, ImageDraw, ImageFont

        # Generar QR
        qr = qrcode.QRCode(version=1, error_correction=qrcode.constants.ERROR_CORRECT_H, box_size=10, border=4)
        qr.add_data(str(obj.qr_token))
        qr.make(fit=True)
        qr_img = qr.make_image(fill_color="black", back_color="white").convert('RGB')

        qr_w, qr_h = qr_img.size

        # Área de texto debajo del QR
        padding = 12
        font_size = 22
        try:
            font = ImageFont.truetype("arial.ttf", font_size)
        except Exception:
            font = ImageFont.load_default()

        nombre = obj.nombre_completo
        codigo = obj.codigo

        # Calcular alto del texto
        dummy = Image.new('RGB', (1, 1))
        draw_dummy = ImageDraw.Draw(dummy)
        _, _, tw1, th1 = draw_dummy.textbbox((0, 0), nombre, font=font)
        _, _, tw2, th2 = draw_dummy.textbbox((0, 0), codigo, font=font)
        text_area_h = th1 + th2 + padding * 3

        # Canvas final
        total_h = qr_h + text_area_h
        canvas = Image.new('RGB', (qr_w, total_h), 'white')
        canvas.paste(qr_img, (0, 0))

        draw = ImageDraw.Draw(canvas)
        # Nombre centrado
        draw.text(((qr_w - tw1) // 2, qr_h + padding), nombre, fill='black', font=font)
        # Código centrado más pequeño
        try:
            font_small = ImageFont.truetype("arial.ttf", 16)
        except Exception:
            font_small = font
        _, _, tw2s, _ = draw.textbbox((0, 0), codigo, font=font_small)
        draw.text(((qr_w - tw2s) // 2, qr_h + padding + th1 + 4), codigo, fill='gray', font=font_small)

        buffer = io.BytesIO()
        canvas.save(buffer, format='PNG')
        return f"data:image/png;base64,{base64.b64encode(buffer.getvalue()).decode()}"


class CursoSerializer(serializers.ModelSerializer):
    class Meta:
        model = Curso
        fields = '__all__'


class SesionClaseSerializer(serializers.ModelSerializer):
    curso_nombre = serializers.CharField(source='curso.nombre', read_only=True)
    seccion_nombre = serializers.CharField(source='seccion.__str__', read_only=True)
    profesor_nombre = serializers.CharField(source='profesor.nombre_completo', read_only=True)
    tiene_qr_activo = serializers.SerializerMethodField()

    class Meta:
        model = SesionClase
        fields = [
            'id', 'curso', 'curso_nombre', 'seccion', 'seccion_nombre',
            'profesor', 'profesor_nombre', 'fecha', 'hora_inicio', 'hora_fin',
            'descripcion', 'cerrada', 'tiene_qr_activo', 'fecha_creacion'
        ]
        read_only_fields = ['profesor', 'cerrada']

    def get_tiene_qr_activo(self, obj):
        try:
            return obj.token_qr.esta_vigente
        except TokenQR.DoesNotExist:
            return False

    def create(self, validated_data):
        validated_data['profesor'] = self.context['request'].user
        return super().create(validated_data)


class AsistenciaSerializer(serializers.ModelSerializer):
    alumno_nombre = serializers.CharField(source='alumno.nombre_completo', read_only=True)
    alumno_codigo = serializers.CharField(source='alumno.codigo', read_only=True)

    class Meta:
        model = Asistencia
        fields = [
            'id', 'sesion', 'alumno', 'alumno_nombre', 'alumno_codigo',
            'estado', 'hora_registro', 'registrado_via_qr', 'observacion'
        ]

    def validate(self, attrs):
        if self.instance is None:
            exists = Asistencia.objects.filter(
                sesion=attrs['sesion'],
                alumno=attrs['alumno']
            ).exists()
            if exists:
                raise serializers.ValidationError('Ya existe un registro de asistencia para este alumno en esta sesión')
        return attrs


class AsistenciaUpdateSerializer(serializers.ModelSerializer):
    class Meta:
        model = Asistencia
        fields = ['estado', 'observacion']
