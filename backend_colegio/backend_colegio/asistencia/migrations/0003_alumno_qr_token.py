import uuid
from django.db import migrations, models


def generar_qr_tokens(apps, schema_editor):
    Alumno = apps.get_model('asistencia', 'Alumno')
    for alumno in Alumno.objects.all():
        alumno.qr_token = uuid.uuid4()
        alumno.save(update_fields=['qr_token'])


class Migration(migrations.Migration):

    dependencies = [
        ('asistencia', '0002_initial'),
    ]

    operations = [
        migrations.AddField(
            model_name='alumno',
            name='qr_token',
            field=models.UUIDField(default=uuid.uuid4, editable=False),
        ),
        migrations.RunPython(generar_qr_tokens, migrations.RunPython.noop),
        migrations.AlterField(
            model_name='alumno',
            name='qr_token',
            field=models.UUIDField(default=uuid.uuid4, editable=False, unique=True),
        ),
    ]
