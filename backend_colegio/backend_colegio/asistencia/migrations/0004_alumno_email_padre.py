from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('asistencia', '0003_alumno_qr_token'),
    ]

    operations = [
        migrations.AddField(
            model_name='alumno',
            name='email_padre',
            field=models.EmailField(blank=True, null=True, unique=True),
        ),
    ]
