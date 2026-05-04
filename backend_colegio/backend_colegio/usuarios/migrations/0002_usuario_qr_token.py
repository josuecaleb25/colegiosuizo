import uuid
from django.db import migrations, models


def generar_qr_tokens(apps, schema_editor):
    Usuario = apps.get_model('usuarios', 'Usuario')
    for u in Usuario.objects.all():
        u.qr_token = uuid.uuid4()
        u.save(update_fields=['qr_token'])


class Migration(migrations.Migration):

    dependencies = [
        ('usuarios', '0001_initial'),
    ]

    operations = [
        migrations.AddField(
            model_name='usuario',
            name='qr_token',
            field=models.UUIDField(default=uuid.uuid4, editable=False),
        ),
        migrations.RunPython(generar_qr_tokens, migrations.RunPython.noop),
        migrations.AlterField(
            model_name='usuario',
            name='qr_token',
            field=models.UUIDField(default=uuid.uuid4, editable=False, unique=True),
        ),
    ]
