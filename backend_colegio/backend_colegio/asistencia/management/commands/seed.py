from django.core.management.base import BaseCommand
from django.utils import timezone


class Command(BaseCommand):
    help = 'Carga datos iniciales: admin, docentes turno mañana y alumnos 1A'

    def handle(self, *args, **kwargs):
        self._seed_admin()
        self._seed_docentes()
        self._seed_alumnos_1A()
        self._seed_alumnos_1B()
        self._seed_alumnos_1C()
        self._seed_alumnos_1D()
        self._seed_alumnos_1E()
        self._seed_cursos()
        self.stdout.write(self.style.SUCCESS('\nSeed completado exitosamente.'))

    # ------------------------------------------------------------------
    def _seed_admin(self):
        from usuarios.models import Usuario
        
        # Admin principal
        admin, creado = Usuario.objects.get_or_create(
            email='admin@peruanosuizo.edu.pe',
            defaults={
                'nombres': 'Admin', 'apellidos': 'Sistema',
                'rol': 'administrador', 'is_staff': True, 'is_superuser': True,
            }
        )
        if creado:
            admin.set_password('123456789')
            admin.save()
            self.stdout.write(f'  Admin creado: {admin.email}')
        else:
            self.stdout.write(f'  Admin ya existe: {admin.email}')
            
        # Admin para app móvil
        admin_mobile, creado = Usuario.objects.get_or_create(
            email='admin@colegio.com',
            defaults={
                'nombres': 'Admin', 'apellidos': 'Sistema',
                'rol': 'administrador', 'is_staff': True, 'is_superuser': True,
            }
        )
        if creado:
            admin_mobile.set_password('admin123')
            admin_mobile.save()
            self.stdout.write(f'  Admin móvil creado: {admin_mobile.email}')
        else:
            self.stdout.write(f'  Admin móvil ya existe: {admin_mobile.email}')

    # ------------------------------------------------------------------
    def _seed_docentes(self):
        from usuarios.models import Usuario
        docentes = [
            ('Anibal',    'Moreno',            'anibalmoreno@peruanosuizo.edu.pe'),
            ('Aydee',     'Arellano Cabada',   'aydeearellano@peruanosuizo.edu.pe'),
            ('Blanca',    'Rodriguez Reyes',   'blancarodriguez@peruanosuizo.edu.pe'),
            ('Carmen',    'Moreno Vasquez',    'carmenmoreno@peruanosuizo.edu.pe'),
            ('Edgar',     'Vega Quiñones',     'edgarvega@peruanosuizo.edu.pe'),
            ('Elcy',      'Hernandez Rodas',   'elcyhernandez@peruanosuizo.edu.pe'),
            ('Esther',    'Estrada Huerta',    'estherestrada@peruanosuizo.edu.pe'),
            ('Freddy',    'Estelo Castañeda',  'freddyestelo@peruanosuizo.edu.pe'),
            ('Hilmer',    'Yacupoma Aguirre',  'hilmeryacupoma@peruanosuizo.edu.pe'),
            ('Jessica',   'Herrera Sanchez',   'jessicaherrera@peruanosuizo.edu.pe'),
            ('Maricella', 'Timoteo Gahona',    'maricellatimoteo@peruanosuizo.edu.pe'),
            ('Mariluz',   'Huaman Inga',       'mariluzhuaman@peruanosuizo.edu.pe'),
            ('Milton',    'Purizaca Martinez', 'miltonpurizaca@peruanosuizo.edu.pe'),
            ('Nelly',     'Mujica Galvez',     'nellymujica@peruanosuizo.edu.pe'),
            ('Pablo',     'Veramendi Rivera',  'pabloveramendi@peruanosuizo.edu.pe'),
            ('Richer',    'Orduna Vergara',    'richerorduna@peruanosuizo.edu.pe'),
            ('Ronald',    'Gogin Carreño',     'ronaldgogin@peruanosuizo.edu.pe'),
            ('Rosmery',   'Correa Caytano',    'rosmerycorrea@peruanosuizo.edu.pe'),
            ('Susy',      'Alberto',           'susyalberto@peruanosuizo.edu.pe'),
            ('Walter',    'Castro Valdivia',   'castrovaldivia@peruanosuizo.edu.pe'),
        ]
        
        # Agregar profesor para app móvil (mantener para pruebas)
        docentes.append(('Juan', 'Pérez', 'profesor@colegio.com'))
        
        creados = 0
        for nombres, apellidos, email in docentes:
            u, creado = Usuario.objects.get_or_create(email=email, defaults={
                'nombres': nombres, 'apellidos': apellidos, 'rol': 'profesor',
            })
            if creado:
                if email == 'profesor@colegio.com':
                    u.set_password('profesor123')
                else:
                    u.set_password('Suizo2026*')  # Misma contraseña que los alumnos
                u.save()
                creados += 1
        self.stdout.write(f'  Docentes creados: {creados} / {len(docentes)}')

    # ------------------------------------------------------------------
    def _seed_alumnos_1A(self):
        from usuarios.models import Usuario
        from asistencia.models import Grado, Seccion, Alumno, Curso, SesionClase

        prof = Usuario.objects.filter(rol='profesor').first()
        grado, _ = Grado.objects.get_or_create(nombre='1ro', nivel='secundaria')
        seccion, _ = Seccion.objects.get_or_create(
            grado=grado, nombre='A', defaults={'tutor': prof}
        )

        # Datos reales de 1ro A - 2026
        alumnos_1a = [
            ('ABAD VILLANERA', 'LISETH SAYURI', 'abadvillanera@peruanosuizo.edu.pe'),
            ('BAZAN NAVARRO', 'LEONARDO ARTURO', 'bazannavarro@peruanosuizo.edu.pe'),
            ('CABANILLAS LINAN', 'ANGEL GABRIEL BEAT', 'cabanillaslinan@peruanosuizo.edu.pe'),
            ('CARRILLO VILCHEZ', 'EDGARDO MIGUEL', 'carrillovilchez@peruanosuizo.edu.pe'),
            ('CERNA VILLAR', 'AARON DAVI', 'cernavillar@peruanosuizo.edu.pe'),
            ('CHAVARRIA ARANDA', 'JEREMY', 'chavarriaaranda@peruanosuizo.edu.pe'),
            ('CONDOR OLIVAS', 'MACKENZYE ROMINA', 'condorolivas@peruanosuizo.edu.pe'),
            ('CUZCANO ESPINOZA', 'LIZETH VALLOLET', 'cuzcanoespinoza@peruanosuizo.edu.pe'),
            ('ESPINOZA YARLEQUE', 'JHOSTIN', 'espinozayarleque@peruanosuizo.edu.pe'),
            ('GODOY SORIA', 'KARLA MERLYN', 'godoysoria@peruanosuizo.edu.pe'),
            ('GUTIERREZ MORENO', 'JOSE EDUARDO', 'gutierrezmoreno@peruanosuizo.edu.pe'),
            ('HUAMAN CARHUACOTA', 'ENMANUEL JESUS DANIEL', 'huamancarhuacota@peruanosuizo.edu.pe'),
            ('HUERTA ASTONITAS', 'LEONELA', 'huertaastonitas@peruanosuizo.edu.pe'),
            ('INCA PACCORI', 'JEREMY OSCAR', 'incapaccori@peruanosuizo.edu.pe'),
            ('JUAREZ', 'RUMAY JULY', 'juarez@peruanosuizo.edu.pe'),
            ('LOYOLA ESPINOLA', 'CARLOS NICOLAS', 'loyolaespinola@peruanosuizo.edu.pe'),
            ('MARCOS SANCHEZ', 'ALEXANDER ANDRE', 'marcossanchez@peruanosuizo.edu.pe'),
            ('MEZA CCENTE', 'JORDY ANDERSON', 'mezaccente@peruanosuizo.edu.pe'),
            ('MINAN VALVERDE', 'ANA CAMILA', 'minanvalverde@peruanosuizo.edu.pe'),
            ('NAVENTA UCANAY', 'DARIANA STEFANY', 'naventaucanay@peruanosuizo.edu.pe'),
            ('ORE QUEVEDO', 'JOSE FERNANDO', 'orequevedo@peruanosuizo.edu.pe'),
            ('RAYMUNDO SOTO', 'ALESSANDRO', 'raymundosoto@peruanosuizo.edu.pe'),
            ('RIOJA TOLEDO', 'KAREN MILAGROS', 'riojatoledo@peruanosuizo.edu.pe'),
            ('RIVERA MALLQUI', 'JEANPIERO MANUEL', 'riveramallqui@peruanosuizo.edu.pe'),
            ('ROMERO REYES', 'MIRKO JOHEL', 'romeroreyes@peruanosuizo.edu.pe'),
            ('SALAS PANAIFO', 'LEYSY ESTHER', 'salaspanaifo@peruanosuizo.edu.pe'),
            ('SALGADO PALACIOS', 'ANDRY RYAN', 'salgadopalacios@peruanosuizo.edu.pe'),
            ('SANTIAGO SULCA', 'MELANY DARLYN', 'santiagosulca@peruanosuizo.edu.pe'),
            ('SERON ALCA', 'RODRIGO CRISTOBAL', 'seronalca@peruanosuizo.edu.pe'),
            ('VASQUEZ CANO', 'EDINSON VALENTINO', 'vasquezcano@peruanosuizo.edu.pe'),
            ('VEGA AGUERO', 'CRISTIANO SANTIAGO', 'vegaaguero@peruanosuizo.edu.pe'),
            ('VILCHEZ PARAGUAY', 'NAHIDU SOFIA', 'vilchezparaguay@peruanosuizo.edu.pe'),
        ]

        creados = 0
        for i, (apellidos, nombres, email) in enumerate(alumnos_1a, start=1):
            # Generar ID secuencial simple
            dni = str(i).zfill(8)
            
            # Crear email del padre basado en el alumno
            email_padre = email.replace('@peruanosuizo.edu.pe', '_padre@peruanosuizo.edu.pe')

            alumno, creado = Alumno.objects.get_or_create(
                dni=dni,
                defaults={
                    'codigo': f'1A{str(i).zfill(3)}',
                    'nombres': nombres,
                    'apellidos': apellidos,
                    'seccion': seccion,
                    'fecha_nacimiento': '2010-01-01',
                    'email_padre': email_padre,
                }
            )
            if creado:
                creados += 1

            # Crear cuenta de padre si no existe
            padre, padre_creado = Usuario.objects.get_or_create(
                email=email_padre,
                defaults={
                    'nombres': nombres,
                    'apellidos': apellidos,
                    'rol': 'padre',
                }
            )
            if padre_creado:
                padre.set_password('Suizo2026*')
                padre.save()

            # Crear cuenta del alumno (para login si es necesario)
            alumno_user, alumno_user_creado = Usuario.objects.get_or_create(
                email=email,
                defaults={
                    'nombres': nombres,
                    'apellidos': apellidos,
                    'rol': 'alumno',
                }
            )
            if alumno_user_creado:
                alumno_user.set_password('Suizo2026*')
                alumno_user.save()

            # Vincular email_padre al alumno si no estaba
            if not alumno.email_padre:
                alumno.email_padre = email_padre
                alumno.save(update_fields=['email_padre'])

        self.stdout.write(f'  Alumnos 1A creados: {creados} / {len(alumnos_1a)}')

        # Sesión de hoy si no existe
        curso, _ = Curso.objects.get_or_create(
            codigo='MAT01',
            defaults={'nombre': 'Matematica', 'grado': grado, 'profesor': prof}
        )
        hoy = timezone.now().date()
        _, creada = SesionClase.objects.get_or_create(
            curso=curso, seccion=seccion, fecha=hoy,
            defaults={'profesor': prof, 'hora_inicio': '07:30'}
        )
        if creada:
            self.stdout.write(f'  Sesion de hoy creada para 1A')
            
    def _seed_alumnos_1B(self):
        from usuarios.models import Usuario
        from asistencia.models import Grado, Seccion, Alumno, Curso, SesionClase

        prof = Usuario.objects.filter(rol='profesor').first()
        grado, _ = Grado.objects.get_or_create(nombre='1ro', nivel='secundaria')
        seccion, _ = Seccion.objects.get_or_create(
            grado=grado, nombre='B', defaults={'tutor': prof}
        )

        # Datos reales de 1ro B - 2026
        alumnos_1b = [
            ('BARRIOS CORDOBA', 'KEIWERLYN YORGINA', 'barrioscordoba@peruanosuizo.edu.pe'),
            ('BEJARANO TORRES', 'ALEJANDRO GIANCARLO', 'bejaranotorres@peruanosuizo.edu.pe'),
            ('CANCHO NORIEGA', 'SHIRLEY SOFIA', 'canchonoriega@peruanosuizo.edu.pe'),
            ('CASTANEDA VASQUEZ', 'CRISTOFER JORGE ALBERTO', 'castanedavasquez@peruanosuizo.edu.pe'),
            ('CHUICA ORDONEZ', 'CARLOS THIAGO', 'chuicaordonez@peruanosuizo.edu.pe'),
            ('CORAL SALDANA', 'TIARA MELISSA', 'coralsaldana@peruanosuizo.edu.pe'),
            ('GERVASIO EVANGELISTA', 'THIAGO ANTHUAN', 'gervasioevangelista@peruanosuizo.edu.pe'),
            ('GOMEZ ARANIBAR', 'DANAE MILUZKA', 'gomezaranibar@peruanosuizo.edu.pe'),
            ('GUERRERO ARIRAMA', 'KATERIN LUANA', 'guerreroarirama@peruanosuizo.edu.pe'),
            ('GUZMAN CASTILLO', 'ANDRE THIAGO', 'guzmancastillo@peruanosuizo.edu.pe'),
            ('HERRERA CLAUDIO', 'NAOMI JAZMIN', 'herreraclaudio@peruanosuizo.edu.pe'),
            ('JUARES JIMENEZ', 'RODRIGO BELKAN', 'juaresjimenez@peruanosuizo.edu.pe'),
            ('KANEKO PAITAN', 'BRITTANY YARITZA', 'kanekopaitan@peruanosuizo.edu.pe'),
            ('MARCA BERROSPI', 'GABRIEL ADRIEL', 'marcaberrospi@peruanosuizo.edu.pe'),
            ('MAYTA CAPCHA', 'JORDAN', 'maytacapcha@peruanosuizo.edu.pe'),
            ('MAYTA CAPCHA', 'JOSUE', 'josuemaytacapcha@peruanosuizo.edu.pe'),
            ('MUNANTE HUANCA', 'VALENTINA', 'munantehuanca@peruanosuizo.edu.pe'),
            ('PENA BARDALES', 'JUNIOR VALENTIN', 'penabardales@peruanosuizo.edu.pe'),
            ('PEREZ CHUICA', 'JAN JACOBO', 'perezchuica@peruanosuizo.edu.pe'),
            ('PIZARRO DEL AGUILA', 'IKER ADRIANO', 'pizarrodelaguila@peruanosuizo.edu.pe'),
            ('POMA PONCE', 'NADESKA JHASMIN', 'pomaponce@peruanosuizo.edu.pe'),
            ('RIVERO PINO', 'IAN DANIEL', 'riveropino@peruanosuizo.edu.pe'),
            ('ROMERO ZUBIETA', 'JOAQUIN ANGEL', 'romerozubieta@peruanosuizo.edu.pe'),
            ('SANCHEZ MORALES', 'SARAHY DANIELA', 'sanchezmorales@peruanosuizo.edu.pe'),
            ('SORIA PAGUADA', 'YARELY XIOMARA', 'soriapaguada@peruanosuizo.edu.pe'),
            ('TOSCANO CHOCO', 'KEVIN EFRAIN', 'toscanochoco@peruanosuizo.edu.pe'),
            ('UMAN VARGAS', 'ADRIANO', 'umanvargas@peruanosuizo.edu.pe'),
            ('VASQUEZ SOBRINO', 'TERRY ANDRE', 'vasquezsobrino@peruanosuizo.edu.pe'),
            ('VEGA CASTRO', 'ROCIO IVETH', 'vegacastro@peruanosuizo.edu.pe'),
            ('VILLALVA GARCIA', 'GABRIEL JOSETH', 'villalvagarcia@peruanosuizo.edu.pe'),
            ('VILLARREAL FERNANDEZ', 'GEORGE NICOLAS', 'villarrealfernandez@peruanosuizo.edu.pe'),
            ('YACOLCA YALICO', 'YOHAO SEBASTIAN KAREV', 'yacolcayalico@peruanosuizo.edu.pe'),
        ]

        creados = 0
        for i, (apellidos, nombres, email) in enumerate(alumnos_1b, start=1):
            # Generar ID secuencial simple para 1B (empezar desde 33)
            dni = str(i + 32).zfill(8)
            
            # Crear email del padre basado en el alumno
            email_padre = email.replace('@peruanosuizo.edu.pe', '_padre@peruanosuizo.edu.pe')

            alumno, creado = Alumno.objects.get_or_create(
                dni=dni,
                defaults={
                    'codigo': f'1B{str(i).zfill(3)}',
                    'nombres': nombres,
                    'apellidos': apellidos,
                    'seccion': seccion,
                    'fecha_nacimiento': '2010-01-01',
                    'email_padre': email_padre,
                }
            )
            if creado:
                creados += 1

            # Crear cuenta de padre si no existe
            padre, padre_creado = Usuario.objects.get_or_create(
                email=email_padre,
                defaults={
                    'nombres': nombres,
                    'apellidos': apellidos,
                    'rol': 'padre',
                }
            )
            if padre_creado:
                padre.set_password('Suizo2026*')
                padre.save()

            # Crear cuenta del alumno (para login si es necesario)
            alumno_user, alumno_user_creado = Usuario.objects.get_or_create(
                email=email,
                defaults={
                    'nombres': nombres,
                    'apellidos': apellidos,
                    'rol': 'alumno',
                }
            )
            if alumno_user_creado:
                alumno_user.set_password('Suizo2026*')
                alumno_user.save()

            # Vincular email_padre al alumno si no estaba
            if not alumno.email_padre:
                alumno.email_padre = email_padre
                alumno.save(update_fields=['email_padre'])

        self.stdout.write(f'  Alumnos 1B creados: {creados} / {len(alumnos_1b)}')

        # Sesión de hoy si no existe para 1B
        curso, _ = Curso.objects.get_or_create(
            codigo='MAT01',
            defaults={'nombre': 'Matematica', 'grado': grado, 'profesor': prof}
        )
        hoy = timezone.now().date()
        _, creada = SesionClase.objects.get_or_create(
            curso=curso, seccion=seccion, fecha=hoy,
            defaults={'profesor': prof, 'hora_inicio': '07:30'}
        )
        if creada:
            self.stdout.write(f'  Sesion de hoy creada para 1B')
            
    def _seed_alumnos_1C(self):
        from usuarios.models import Usuario
        from asistencia.models import Grado, Seccion, Alumno, Curso, SesionClase

        prof = Usuario.objects.filter(rol='profesor').first()
        grado, _ = Grado.objects.get_or_create(nombre='1ro', nivel='secundaria')
        seccion, _ = Seccion.objects.get_or_create(
            grado=grado, nombre='C', defaults={'tutor': prof}
        )

        # Datos reales de 1ro C - 2026
        alumnos_1c = [
            ('ACERO SERNAQUE', 'LUIS NEYMAR', 'acerosernaque@peruanosuizo.edu.pe'),
            ('ALARCON FLORES', 'BRAYAN SALVADOR', 'alarconflores@peruanosuizo.edu.pe'),
            ('ARBITRO VASQUEZ', 'GILLARY MISHEL', 'arbitrovasquez@peruanosuizo.edu.pe'),
            ('BARRIOS COLOS', 'EDSALDE ZARELIZ', 'barrioscolos@peruanosuizo.edu.pe'),
            ('BERROSPI ESCRIBA', 'SALVADOR ESTEFANO', 'berrospiescriba@peruanosuizo.edu.pe'),
            ('CALDERON URBINA', 'CAMILA ANTONELLA', 'calderonurbina@peruanosuizo.edu.pe'),
            ('CASTANEDA CANO', 'ALESSANDRO', 'castanedacano@peruanosuizo.edu.pe'),
            ('ESPINOZA JIMENEZ', 'FABRIZZIO ALESSANDRO', 'espinozajimenez@peruanosuizo.edu.pe'),
            ('FRIAS ROJAS', 'JAMILETH NICOL', 'friasrojas@peruanosuizo.edu.pe'),
            ('GARCIA URRUTIA', 'JUAN ALBERTO', 'garciaurrutia@peruanosuizo.edu.pe'),
            ('GUEVARA ALVARADO', 'ANDREA', 'guevaraalvarado@peruanosuizo.edu.pe'),
            ('HERNANDEZ RODRIGUEZ', 'ISABEL SOFIA', 'hernandezrodriguez@peruanosuizo.edu.pe'),
            ('HILARIO CHOQUE', 'THIGO ADRIANO', 'hilariochoque@peruanosuizo.edu.pe'),
            ('INFANTE RODRIGUEZ', 'VICTORIA ALEJANDRA', 'infanterodriguez@peruanosuizo.edu.pe'),
            ('LABASTIDAS SANCHEZ', 'ORIANNY JULIESKA', 'labastidassanchez@peruanosuizo.edu.pe'),
            ('MACO JIMENEZ', 'YENIFER', 'macojimenez@peruanosuizo.edu.pe'),
            ('MENDOZA GALLEGOS', 'FABRIZIO GERMAN', 'mendozagallegos@peruanosuizo.edu.pe'),
            ('MOGOLLON VALENCIA', 'MARIANA', 'mogollonvalencia@peruanosuizo.edu.pe'),
            ('OLANO PACHERRES', 'BRAYELY NICOL', 'olanopacherres@peruanosuizo.edu.pe'),
            ('OYOLA DE LA CRUZ', 'JOAO DERECK', 'oyoladelacruz@peruanosuizo.edu.pe'),
            ('PASACHE ANCAJIMA', 'FRANCO STEBAN', 'pasacheancajima@peruanosuizo.edu.pe'),
            ('PEREZ PONTE', 'ALONDRA ARIANA', 'perezponte@peruanosuizo.edu.pe'),
            ('RAMIOS PEREZ', 'ANAIS ALEXA', 'ramiosperez@peruanosuizo.edu.pe'),
            ('RAMIREZ ATUNCAR', 'ABRAHAM ESTEBAN', 'ramirezatuncar@peruanosuizo.edu.pe'),
            ('RAMIREZ BANDRES', 'ANGELES DELEYCAR', 'ramirezbandres@peruanosuizo.edu.pe'),
            ('REYES SOTO', 'LEONEL JAIRO', 'reyessoto@peruanosuizo.edu.pe'),
            ('SABINO CHUQUIZUTA', 'EVANS ANDRE', 'sabinochuquizuta@peruanosuizo.edu.pe'),
            ('SALDARRIAGA ESPINOZA', 'JOSE LUIS', 'saldarriagaespinoza@peruanosuizo.edu.pe'),
            ('SILVA MACUMA', 'MARLON DAVID', 'silvamacuma@peruanosuizo.edu.pe'),
            ('SUAREZ LANDA', 'MILEY AYELEN', 'suarezlanda@peruanosuizo.edu.pe'),
            ('SUCLUPE VALENCIA', 'JAN POOL', 'suclupevalencia@peruanosuizo.edu.pe'),
            ('TARAZONA HUERTA', 'JANETH JASURI', 'tarazonahuerta@peruanosuizo.edu.pe'),
        ]

        creados = 0
        for i, (apellidos, nombres, email) in enumerate(alumnos_1c, start=1):
            # Generar ID secuencial simple para 1C (empezar desde 65)
            dni = str(i + 64).zfill(8)
            
            # Crear email del padre basado en el alumno
            email_padre = email.replace('@peruanosuizo.edu.pe', '_padre@peruanosuizo.edu.pe')

            alumno, creado = Alumno.objects.get_or_create(
                dni=dni,
                defaults={
                    'codigo': f'1C{str(i).zfill(3)}',
                    'nombres': nombres,
                    'apellidos': apellidos,
                    'seccion': seccion,
                    'fecha_nacimiento': '2010-01-01',
                    'email_padre': email_padre,
                }
            )
            if creado:
                creados += 1

            # Crear cuenta de padre si no existe
            padre, padre_creado = Usuario.objects.get_or_create(
                email=email_padre,
                defaults={
                    'nombres': nombres,
                    'apellidos': apellidos,
                    'rol': 'padre',
                }
            )
            if padre_creado:
                padre.set_password('Suizo2026*')
                padre.save()

            # Crear cuenta del alumno (para login si es necesario)
            alumno_user, alumno_user_creado = Usuario.objects.get_or_create(
                email=email,
                defaults={
                    'nombres': nombres,
                    'apellidos': apellidos,
                    'rol': 'alumno',
                }
            )
            if alumno_user_creado:
                alumno_user.set_password('Suizo2026*')
                alumno_user.save()

            # Vincular email_padre al alumno si no estaba
            if not alumno.email_padre:
                alumno.email_padre = email_padre
                alumno.save(update_fields=['email_padre'])

        self.stdout.write(f'  Alumnos 1C creados: {creados} / {len(alumnos_1c)}')

        # Sesión de hoy si no existe para 1C
        curso, _ = Curso.objects.get_or_create(
            codigo='MAT01',
            defaults={'nombre': 'Matematica', 'grado': grado, 'profesor': prof}
        )
        hoy = timezone.now().date()
        _, creada = SesionClase.objects.get_or_create(
            curso=curso, seccion=seccion, fecha=hoy,
            defaults={'profesor': prof, 'hora_inicio': '07:30'}
        )
        if creada:
            self.stdout.write(f'  Sesion de hoy creada para 1C')
            
    def _seed_alumnos_1D(self):
        from usuarios.models import Usuario
        from asistencia.models import Grado, Seccion, Alumno, Curso, SesionClase

        prof = Usuario.objects.filter(rol='profesor').first()
        grado, _ = Grado.objects.get_or_create(nombre='1ro', nivel='secundaria')
        seccion, _ = Seccion.objects.get_or_create(
            grado=grado, nombre='D', defaults={'tutor': prof}
        )

        # Datos reales de 1ro D - 2026
        alumnos_1d = [
            ('AYALA ESPINOZA', 'DALESSANDRO', 'ayalaespinoza@peruanosuizo.edu.pe'),
            ('AYASTA MORE', 'JOHANA VICTORIA', 'ayastamore@peruanosuizo.edu.pe'),
            ('BUSTAMANTE CANDIA', 'NAHUEL BENJAMIN', 'bustamantecandia@peruanosuizo.edu.pe'),
            ('CAPCHA GUEVARA', 'EVANNS IKER', 'capchaguevara@peruanosuizo.edu.pe'),
            ('CONTRERAS CHAVEZ', 'BRYAN JOSEPH', 'contreraschavez@peruanosuizo.edu.pe'),
            ('CUYA HUAYNALAYA', 'ALINA SAHORI', 'cuyahuaynalaya@peruanosuizo.edu.pe'),
            ('ESQUIVEL HINOSTROZA', 'JAMES ANTONY', 'esquivelhinostroza@peruanosuizo.edu.pe'),
            ('GONZALES SALAZAR', 'GENESIS FABIOLA', 'gonzalessalazar@peruanosuizo.edu.pe'),
            ('GUERRERO GUTIERREZ', 'CARELY JESUS', 'guerrerogutierrez@peruanosuizo.edu.pe'),
            ('HUAMAN ZORILLA', 'JUAN CRISTIANO', 'huamanzorilla@peruanosuizo.edu.pe'),
            ('HUMALI VARGAS', 'EMILY', 'humalivargas@peruanosuizo.edu.pe'),
            ('JUAREZ GONZALES', 'FABIAN ALEXANDER', 'juarezgonzales@peruanosuizo.edu.pe'),
            ('LOZA SOLANO', 'BRYANA CRISTAL', 'lozasolano@peruanosuizo.edu.pe'),
            ('MORENO LOPEZ', 'SNEIDER DAYIRO', 'morenolopez@peruanosuizo.edu.pe'),
            ('NAMUCHE DURAND', 'ASTRID MICHELLE', 'namuchedurand@peruanosuizo.edu.pe'),
            ('PADILLA NORIEGA', 'FABIO NICOLA', 'padillanoriega@peruanosuizo.edu.pe'),
            ('PEREZ LOPEZ', 'ALEXANDRA FABIOLA', 'perezlopez@peruanosuizo.edu.pe'),
            ('PRINCIPE AVILA', 'MAYRA TALIA', 'principeavila@peruanosuizo.edu.pe'),
            ('QUINTANA BRICENO', 'ANYURIS ANDRIANNYS', 'quintanabriceno@peruanosuizo.edu.pe'),
            ('QUIROZ CHOTA', 'SALMA WINIBELL', 'quirozchota@peruanosuizo.edu.pe'),
            ('RAMIREZ CONTRERAS', 'BARBARA MINA', 'ramirezcontreras@peruanosuizo.edu.pe'),
            ('REGUERA ESPEJO', 'DAVID CRISTIAN RAUL', 'regueraespejo@peruanosuizo.edu.pe'),
            ('RODRIEZ NUNEZ', 'VICTOR GABRIEL', 'rodrieznunez@peruanosuizo.edu.pe'),
            ('ROJAS LOPEZ', 'DIEGO EDUARDO', 'rojaslopez@peruanosuizo.edu.pe'),
            ('SANCHEZ ACOSTA', 'ERVIN NEYMAR', 'sanchezacosta@peruanosuizo.edu.pe'),
            ('SAUCEDO ZAMORA', 'GIAN FRANCO', 'saucedozamora@peruanosuizo.edu.pe'),
            ('SIMON GALVEZ', 'MELANY', 'simongalvez@peruanosuizo.edu.pe'),
            ('TAMANI ARIMUYA', 'SAYURI ALEJANDRA', 'tamaniarimuya@peruanosuizo.edu.pe'),
            ('TICONA DAMIAN', 'RUTH MAHAL', 'ticonadamian@peruanosuizo.edu.pe'),
            ('TORRES ARGUEDAS', 'GABRIEL AARON', 'torresarguedas@peruanosuizo.edu.pe'),
            ('VASQUEZ SAAVEDRA', 'FABIANO FRANCISCO', 'vasquezsaavedra@peruanosuizo.edu.pe'),
        ]

        creados = 0
        for i, (apellidos, nombres, email) in enumerate(alumnos_1d, start=1):
            # Generar ID secuencial simple para 1D (empezar desde 97)
            dni = str(i + 96).zfill(8)
            
            # Crear email del padre basado en el alumno
            email_padre = email.replace('@peruanosuizo.edu.pe', '_padre@peruanosuizo.edu.pe')

            alumno, creado = Alumno.objects.get_or_create(
                dni=dni,
                defaults={
                    'codigo': f'1D{str(i).zfill(3)}',
                    'nombres': nombres,
                    'apellidos': apellidos,
                    'seccion': seccion,
                    'fecha_nacimiento': '2010-01-01',
                    'email_padre': email_padre,
                }
            )
            if creado:
                creados += 1

            # Crear cuenta de padre si no existe
            padre, padre_creado = Usuario.objects.get_or_create(
                email=email_padre,
                defaults={
                    'nombres': nombres,
                    'apellidos': apellidos,
                    'rol': 'padre',
                }
            )
            if padre_creado:
                padre.set_password('Suizo2026*')
                padre.save()

            # Crear cuenta del alumno (para login si es necesario)
            alumno_user, alumno_user_creado = Usuario.objects.get_or_create(
                email=email,
                defaults={
                    'nombres': nombres,
                    'apellidos': apellidos,
                    'rol': 'alumno',
                }
            )
            if alumno_user_creado:
                alumno_user.set_password('Suizo2026*')
                alumno_user.save()

            # Vincular email_padre al alumno si no estaba
            if not alumno.email_padre:
                alumno.email_padre = email_padre
                alumno.save(update_fields=['email_padre'])

        self.stdout.write(f'  Alumnos 1D creados: {creados} / {len(alumnos_1d)}')

        # Sesión de hoy si no existe para 1D
        curso, _ = Curso.objects.get_or_create(
            codigo='MAT01',
            defaults={'nombre': 'Matematica', 'grado': grado, 'profesor': prof}
        )
        hoy = timezone.now().date()
        _, creada = SesionClase.objects.get_or_create(
            curso=curso, seccion=seccion, fecha=hoy,
            defaults={'profesor': prof, 'hora_inicio': '07:30'}
        )
        if creada:
            self.stdout.write(f'  Sesion de hoy creada para 1D')
            
    def _seed_alumnos_1E(self):
        from usuarios.models import Usuario
        from asistencia.models import Grado, Seccion, Alumno, Curso, SesionClase

        prof = Usuario.objects.filter(rol='profesor').first()
        grado, _ = Grado.objects.get_or_create(nombre='1ro', nivel='secundaria')
        seccion, _ = Seccion.objects.get_or_create(
            grado=grado, nombre='E', defaults={'tutor': prof}
        )

        # Datos reales de 1ro E - 2026
        alumnos_1e = [
            ('AMANCA CARPENA', 'LEONARDO JOSUE ANGEL', 'amancacarpena@peruanosuizo.edu.pe'),
            ('BALDERA CESPEDES', 'KAROLEHY', 'balderacespedes@peruanosuizo.edu.pe'),
            ('BERRIOS CASTILLO', 'ALAN DAMIAN', 'berrioscastillo@peruanosuizo.edu.pe'),
            ('CALDERON URBINA', 'JESUS ENNODIO', 'calderonurbina2@peruanosuizo.edu.pe'),
            ('CARBAJAL PALACIOS', 'THIAGO FERNANDO', 'carbajalpalacios@peruanosuizo.edu.pe'),
            ('DAGA GONZALES', 'STIFEL HENDRICH', 'dagagonzales@peruanosuizo.edu.pe'),
            ('DIAZ CACHA', 'YOLY SAHORI', 'diazcacha@peruanosuizo.edu.pe'),
            ('ESPINO SALAZAR', 'JESUS CLEMENTE', 'espinosalazar@peruanosuizo.edu.pe'),
            ('FARRO LEON', 'ALEXIS JUNIOR', 'farroleon@peruanosuizo.edu.pe'),
            ('HERRERA TAFUR', 'SEBASTIAN VALENTINO', 'herreratafur@peruanosuizo.edu.pe'),
            ('HUAMAN PINAN', 'DARIANA YADIRA', 'huamanpinan@peruanosuizo.edu.pe'),
            ('JIMENEZ SALDANA', 'THIAGO PAUL', 'jimenezsaldana@peruanosuizo.edu.pe'),
            ('LEON GABRIEL', 'EDGAR FABRICIO', 'leongabriel@peruanosuizo.edu.pe'),
            ('MILLA VILLALVA', 'RIHANNA THAISA', 'millavillalva@peruanosuizo.edu.pe'),
            ('MOLINA SANCHEZ', 'FRANYELIS ARANZA', 'molinasanchez@peruanosuizo.edu.pe'),
            ('MURO HUAMAN', 'MILAN AMIR', 'murohuaman@peruanosuizo.edu.pe'),
            ('PARRA CONDORCHOA', 'MADELEY CIELO', 'parracondorchoa@peruanosuizo.edu.pe'),
            ('PEREZ LUCENA', 'JOSE MANUEL', 'perezlucena@peruanosuizo.edu.pe'),
            ('PUMATAY OLSEN', 'LUIS ADRIAN', 'pumatayolsen@peruanosuizo.edu.pe'),
            ('QUINONES PENA', 'SAYAKA TAIMARA', 'quinonespena@peruanosuizo.edu.pe'),
            ('QUIROZ MESTANZA', 'NADINNE YAMILETH', 'quirozmestanza@peruanosuizo.edu.pe'),
            ('REYES MARTINEZ', 'GERARD FERNANDO', 'reyesmartinez@peruanosuizo.edu.pe'),
            ('ROALCABA MALDONADO', 'LUIS ANGEL', 'roalcabamaldonado@peruanosuizo.edu.pe'),
            ('ROJAS BETANCOURT', 'CRISTOPHER', 'rojasbetancourt@peruanosuizo.edu.pe'),
            ('SALINAS CERNA', 'KATHLEEN DANALEE', 'salinascerna@peruanosuizo.edu.pe'),
            ('SAUCEDO VASQUES', 'YHARIEL JHOMAR', 'saucedovasques@peruanosuizo.edu.pe'),
            ('SERAFIN MARTINEZ', 'REYCON DAVID', 'serafinmartinez@peruanosuizo.edu.pe'),
            ('SIESQUEN MONCADA', 'DARYELY ANDREA', 'siesquenmoncada@peruanosuizo.edu.pe'),
            ('TRUJILLO GARCIA', 'ALEXIS JORDY', 'trujillogarcia@peruanosuizo.edu.pe'),
            ('VERASTEGUI RUELAS', 'HILLARY SAMARA', 'verasteguiruelas@peruanosuizo.edu.pe'),
            ('VILLENA MELGAR', 'HASAEL ENMANUEL', 'villenamelgar@peruanosuizo.edu.pe'),
        ]

        creados = 0
        for i, (apellidos, nombres, email) in enumerate(alumnos_1e, start=1):
            # Generar ID secuencial simple para 1E (empezar desde 128)
            dni = str(i + 127).zfill(8)
            
            # Crear email del padre basado en el alumno
            email_padre = email.replace('@peruanosuizo.edu.pe', '_padre@peruanosuizo.edu.pe')

            alumno, creado = Alumno.objects.get_or_create(
                dni=dni,
                defaults={
                    'codigo': f'1E{str(i).zfill(3)}',
                    'nombres': nombres,
                    'apellidos': apellidos,
                    'seccion': seccion,
                    'fecha_nacimiento': '2010-01-01',
                    'email_padre': email_padre,
                }
            )
            if creado:
                creados += 1

            # Crear cuenta de padre si no existe
            padre, padre_creado = Usuario.objects.get_or_create(
                email=email_padre,
                defaults={
                    'nombres': nombres,
                    'apellidos': apellidos,
                    'rol': 'padre',
                }
            )
            if padre_creado:
                padre.set_password('Suizo2026*')
                padre.save()

            # Crear cuenta del alumno (para login si es necesario)
            alumno_user, alumno_user_creado = Usuario.objects.get_or_create(
                email=email,
                defaults={
                    'nombres': nombres,
                    'apellidos': apellidos,
                    'rol': 'alumno',
                }
            )
            if alumno_user_creado:
                alumno_user.set_password('Suizo2026*')
                alumno_user.save()

            # Vincular email_padre al alumno si no estaba
            if not alumno.email_padre:
                alumno.email_padre = email_padre
                alumno.save(update_fields=['email_padre'])

        self.stdout.write(f'  Alumnos 1E creados: {creados} / {len(alumnos_1e)}')

        # Sesión de hoy si no existe para 1E
        curso, _ = Curso.objects.get_or_create(
            codigo='MAT01',
            defaults={'nombre': 'Matematica', 'grado': grado, 'profesor': prof}
        )
        hoy = timezone.now().date()
        _, creada = SesionClase.objects.get_or_create(
            curso=curso, seccion=seccion, fecha=hoy,
            defaults={'profesor': prof, 'hora_inicio': '07:30'}
        )
        if creada:
            self.stdout.write(f'  Sesion de hoy creada para 1E')
            
    def _seed_cursos(self):
        from usuarios.models import Usuario
        from asistencia.models import Grado, Curso
        
        # Obtener grados y profesores
        grado_1ro = Grado.objects.filter(nombre='1ro').first()
        profesores = Usuario.objects.filter(rol='profesor')
        
        if not grado_1ro or not profesores.exists():
            return
            
        cursos_data = [
            ('MAT01', 'Matemática'),
            ('COM01', 'Comunicación'),
            ('CTA01', 'Ciencia y Tecnología'),
            ('SOC01', 'Ciencias Sociales'),
            ('ING01', 'Inglés'),
            ('EDF01', 'Educación Física'),
            ('ART01', 'Arte y Cultura'),
            ('REL01', 'Educación Religiosa'),
        ]
        
        creados = 0
        for i, (codigo, nombre) in enumerate(cursos_data):
            profesor = profesores[i % profesores.count()]
            
            curso, creado = Curso.objects.get_or_create(
                codigo=codigo,
                defaults={
                    'nombre': nombre,
                    'grado': grado_1ro,
                    'profesor': profesor,
                }
            )
            if creado:
                creados += 1
                
        self.stdout.write(f'  Cursos creados: {creados} / {len(cursos_data)}')
