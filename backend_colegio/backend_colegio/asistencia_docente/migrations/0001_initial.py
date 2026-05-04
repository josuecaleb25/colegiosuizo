from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):

    initial = True

    dependencies = [
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.CreateModel(
            name='AsistenciaDocente',
            fields=[
                ('id', models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('fecha', models.DateField()),
                ('estado', models.CharField(choices=[('presente', 'Presente'), ('tardanza', 'Tardanza'), ('ausente', 'Ausente'), ('justificado', 'Justificado')], default='ausente', max_length=15)),
                ('hora_registro', models.DateTimeField(blank=True, null=True)),
                ('registrado_via_qr', models.BooleanField(default=False)),
                ('observacion', models.TextField(blank=True)),
                ('fecha_creacion', models.DateTimeField(auto_now_add=True)),
                ('docente', models.ForeignKey(limit_choices_to={'rol': 'profesor'}, on_delete=django.db.models.deletion.CASCADE, related_name='asistencias_docente', to=settings.AUTH_USER_MODEL)),
            ],
            options={'ordering': ['-fecha', 'docente__apellidos'], 'verbose_name': 'Asistencia Docente', 'unique_together': {('docente', 'fecha')}},
        ),
    ]
