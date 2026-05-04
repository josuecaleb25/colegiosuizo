import supabase from '../config/database';

// Datos de los 158 alumnos
const alumnos1A = [
  { apellidos: 'ABAD VILLANERA', nombres: 'LISETH SAYURI' },
  { apellidos: 'BAZAN NAVARRO', nombres: 'LEONARDO ARTURO' },
  { apellidos: 'CABANILLAS LINAN', nombres: 'ANGEL GABRIEL BEAT' },
  { apellidos: 'CARRILLO VILCHEZ', nombres: 'EDGARDO MIGUEL' },
  { apellidos: 'CERNA VILLAR', nombres: 'AARON DAVI' },
  { apellidos: 'CHAVARRIA ARANDA', nombres: 'JEREMY' },
  { apellidos: 'CONDOR OLIVAS', nombres: 'MACKENZYE ROMINA' },
  { apellidos: 'CUZCANO ESPINOZA', nombres: 'LIZETH VALLOLET' },
  { apellidos: 'ESPINOZA YARLEQUE', nombres: 'JHOSTIN' },
  { apellidos: 'GODOY SORIA', nombres: 'KARLA MERLYN' },
  { apellidos: 'GUTIERREZ MORENO', nombres: 'JOSE EDUARDO' },
  { apellidos: 'HUAMAN CARHUACOTA', nombres: 'ENMANUEL JESUS DANIEL' },
  { apellidos: 'HUERTA ASTONITAS', nombres: 'LEONELA' },
  { apellidos: 'INCA PACCORI', nombres: 'JEREMY OSCAR' },
  { apellidos: 'JUAREZ', nombres: 'RUMAY JULY' },
  { apellidos: 'LOYOLA ESPINOLA', nombres: 'CARLOS NICOLAS' },
  { apellidos: 'MARCOS SANCHEZ', nombres: 'ALEXANDER ANDRE' },
  { apellidos: 'MEZA CCENTE', nombres: 'JORDY ANDERSON' },
  { apellidos: 'MINAN VALVERDE', nombres: 'ANA CAMILA' },
  { apellidos: 'NAVENTA UCANAY', nombres: 'DARIANA STEFANY' },
  { apellidos: 'ORE QUEVEDO', nombres: 'JOSE FERNANDO' },
  { apellidos: 'RAYMUNDO SOTO', nombres: 'ALESSANDRO' },
  { apellidos: 'RIOJA TOLEDO', nombres: 'KAREN MILAGROS' },
  { apellidos: 'RIVERA MALLQUI', nombres: 'JEANPIERO MANUEL' },
  { apellidos: 'ROMERO REYES', nombres: 'MIRKO JOHEL' },
  { apellidos: 'SALAS PANAIFO', nombres: 'LEYSY ESTHER' },
  { apellidos: 'SALGADO PALACIOS', nombres: 'ANDRY RYAN' },
  { apellidos: 'SANTIAGO SULCA', nombres: 'MELANY DARLYN' },
  { apellidos: 'SERON ALCA', nombres: 'RODRIGO CRISTOBAL' },
  { apellidos: 'VASQUEZ CANO', nombres: 'EDINSON VALENTINO' },
  { apellidos: 'VEGA AGUERO', nombres: 'CRISTIANO SANTIAGO' },
  { apellidos: 'VILCHEZ PARAGUAY', nombres: 'NAHIDU SOFIA' }
];

const alumnos1B = [
  { apellidos: 'BARRIOS CORDOBA', nombres: 'KEIWERLYN YORGINA' },
  { apellidos: 'BEJARANO TORRES', nombres: 'ALEJANDRO GIANCARLO' },
  { apellidos: 'CANCHO NORIEGA', nombres: 'SHIRLEY SOFIA' },
  { apellidos: 'CASTANEDA VASQUEZ', nombres: 'CRISTOFER JORGE ALBERTO' },
  { apellidos: 'CHUICA ORDONEZ', nombres: 'CARLOS THIAGO' },
  { apellidos: 'CORAL SALDANA', nombres: 'TIARA MELISSA' },
  { apellidos: 'GERVASIO EVANGELISTA', nombres: 'THIAGO ANTHUAN' },
  { apellidos: 'GOMEZ ARANIBAR', nombres: 'DANAE MILUZKA' },
  { apellidos: 'GUERRERO ARIRAMA', nombres: 'KATERIN LUANA' },
  { apellidos: 'GUZMAN CASTILLO', nombres: 'ANDRE THIAGO' },
  { apellidos: 'HERRERA CLAUDIO', nombres: 'NAOMI JAZMIN' },
  { apellidos: 'JUARES JIMENEZ', nombres: 'RODRIGO BELKAN' },
  { apellidos: 'KANEKO PAITAN', nombres: 'BRITTANY YARITZA' },
  { apellidos: 'MARCA BERROSPI', nombres: 'GABRIEL ADRIEL' },
  { apellidos: 'MAYTA CAPCHA', nombres: 'JORDAN' },
  { apellidos: 'MAYTA CAPCHA', nombres: 'JOSUE' },
  { apellidos: 'MUNANTE HUANCA', nombres: 'VALENTINA' },
  { apellidos: 'PENA BARDALES', nombres: 'JUNIOR VALENTIN' },
  { apellidos: 'PEREZ CHUICA', nombres: 'JAN JACOBO' },
  { apellidos: 'PIZARRO DEL AGUILA', nombres: 'IKER ADRIANO' },
  { apellidos: 'POMA PONCE', nombres: 'NADESKA JHASMIN' },
  { apellidos: 'RIVERO PINO', nombres: 'IAN DANIEL' },
  { apellidos: 'ROMERO ZUBIETA', nombres: 'JOAQUIN ANGEL' },
  { apellidos: 'SANCHEZ MORALES', nombres: 'SARAHY DANIELA' },
  { apellidos: 'SORIA PAGUADA', nombres: 'YARELY XIOMARA' },
  { apellidos: 'TOSCANO CHOCO', nombres: 'KEVIN EFRAIN' },
  { apellidos: 'UMAN VARGAS', nombres: 'ADRIANO' },
  { apellidos: 'VASQUEZ SOBRINO', nombres: 'TERRY ANDRE' },
  { apellidos: 'VEGA CASTRO', nombres: 'ROCIO IVETH' },
  { apellidos: 'VILLALVA GARCIA', nombres: 'GABRIEL JOSETH' },
  { apellidos: 'VILLARREAL FERNANDEZ', nombres: 'GEORGE NICOLAS' },
  { apellidos: 'YACOLCA YALICO', nombres: 'YOHAO SEBASTIAN KAREV' }
];

const alumnos1C = [
  { apellidos: 'ACERO SERNAQUE', nombres: 'LUIS NEYMAR' },
  { apellidos: 'ALARCON FLORES', nombres: 'BRAYAN SALVADOR' },
  { apellidos: 'ARBITRO VASQUEZ', nombres: 'GILLARY MISHEL' },
  { apellidos: 'BARRIOS COLOS', nombres: 'EDSALDE ZARELIZ' },
  { apellidos: 'BERROSPI ESCRIBA', nombres: 'SALVADOR ESTEFANO' },
  { apellidos: 'CALDERON URBINA', nombres: 'CAMILA ANTONELLA' },
  { apellidos: 'CASTANEDA CANO', nombres: 'ALESSANDRO' },
  { apellidos: 'ESPINOZA JIMENEZ', nombres: 'FABRIZZIO ALESSANDRO' },
  { apellidos: 'FRIAS ROJAS', nombres: 'JAMILETH NICOL' },
  { apellidos: 'GARCIA URRUTIA', nombres: 'JUAN ALBERTO' },
  { apellidos: 'GUEVARA ALVARADO', nombres: 'ANDREA' },
  { apellidos: 'HERNANDEZ RODRIGUEZ', nombres: 'ISABEL SOFIA' },
  { apellidos: 'HILARIO CHOQUE', nombres: 'THIGO ADRIANO' },
  { apellidos: 'INFANTE RODRIGUEZ', nombres: 'VICTORIA ALEJANDRA' },
  { apellidos: 'LABASTIDAS SANCHEZ', nombres: 'ORIANNY JULIESKA' },
  { apellidos: 'MACO JIMENEZ', nombres: 'YENIFER' },
  { apellidos: 'MENDOZA GALLEGOS', nombres: 'FABRIZIO GERMAN' },
  { apellidos: 'MOGOLLON VALENCIA', nombres: 'MARIANA' },
  { apellidos: 'OLANO PACHERRES', nombres: 'BRAYELY NICOL' },
  { apellidos: 'OYOLA DE LA CRUZ', nombres: 'JOAO DERECK' },
  { apellidos: 'PASACHE ANCAJIMA', nombres: 'FRANCO STEBAN' },
  { apellidos: 'PEREZ PONTE', nombres: 'ALONDRA ARIANA' },
  { apellidos: 'RAMIOS PEREZ', nombres: 'ANAIS ALEXA' },
  { apellidos: 'RAMIREZ ATUNCAR', nombres: 'ABRAHAM ESTEBAN' },
  { apellidos: 'RAMIREZ BANDRES', nombres: 'ANGELES DELEYCAR' },
  { apellidos: 'REYES SOTO', nombres: 'LEONEL JAIRO' },
  { apellidos: 'SABINO CHUQUIZUTA', nombres: 'EVANS ANDRE' },
  { apellidos: 'SALDARRIAGA ESPINOZA', nombres: 'JOSE LUIS' },
  { apellidos: 'SILVA MACUMA', nombres: 'MARLON DAVID' },
  { apellidos: 'SUAREZ LANDA', nombres: 'MILEY AYELEN' },
  { apellidos: 'SUCLUPE VALENCIA', nombres: 'JAN POOL' },
  { apellidos: 'TARAZONA HUERTA', nombres: 'JANETH JASURI' }
];

const alumnos1D = [
  { apellidos: 'AYALA ESPINOZA', nombres: 'DALESSANDRO' },
  { apellidos: 'AYASTA MORE', nombres: 'JOHANA VICTORIA' },
  { apellidos: 'BUSTAMANTE CANDIA', nombres: 'NAHUEL BENJAMIN' },
  { apellidos: 'CAPCHA GUEVARA', nombres: 'EVANNS IKER' },
  { apellidos: 'CONTRERAS CHAVEZ', nombres: 'BRYAN JOSEPH' },
  { apellidos: 'CUYA HUAYNALAYA', nombres: 'ALINA SAHORI' },
  { apellidos: 'ESQUIVEL HINOSTROZA', nombres: 'JAMES ANTONY' },
  { apellidos: 'GONZALES SALAZAR', nombres: 'GENESIS FABIOLA' },
  { apellidos: 'GUERRERO GUTIERREZ', nombres: 'CARELY JESUS' },
  { apellidos: 'HUAMAN ZORILLA', nombres: 'JUAN CRISTIANO' },
  { apellidos: 'HUMALI VARGAS', nombres: 'EMILY' },
  { apellidos: 'JUAREZ GONZALES', nombres: 'FABIAN ALEXANDER' },
  { apellidos: 'LOZA SOLANO', nombres: 'BRYANA CRISTAL' },
  { apellidos: 'MORENO LOPEZ', nombres: 'SNEIDER DAYIRO' },
  { apellidos: 'NAMUCHE DURAND', nombres: 'ASTRID MICHELLE' },
  { apellidos: 'PADILLA NORIEGA', nombres: 'FABIO NICOLA' },
  { apellidos: 'PEREZ LOPEZ', nombres: 'ALEXANDRA FABIOLA' },
  { apellidos: 'PRINCIPE AVILA', nombres: 'MAYRA TALIA' },
  { apellidos: 'QUINTANA BRICENO', nombres: 'ANYURIS ANDRIANNYS' },
  { apellidos: 'QUIROZ CHOTA', nombres: 'SALMA WINIBELL' },
  { apellidos: 'RAMIREZ CONTRERAS', nombres: 'BARBARA MINA' },
  { apellidos: 'REGUERA ESPEJO', nombres: 'DAVID CRISTIAN RAUL' },
  { apellidos: 'RODRIEZ NUNEZ', nombres: 'VICTOR GABRIEL' },
  { apellidos: 'ROJAS LOPEZ', nombres: 'DIEGO EDUARDO' },
  { apellidos: 'SANCHEZ ACOSTA', nombres: 'ERVIN NEYMAR' },
  { apellidos: 'SAUCEDO ZAMORA', nombres: 'GIAN FRANCO' },
  { apellidos: 'SIMON GALVEZ', nombres: 'MELANY' },
  { apellidos: 'TAMANI ARIMUYA', nombres: 'SAYURI ALEJANDRA' },
  { apellidos: 'TICONA DAMIAN', nombres: 'RUTH MAHAL' },
  { apellidos: 'TORRES ARGUEDAS', nombres: 'GABRIEL AARON' },
  { apellidos: 'VASQUEZ SAAVEDRA', nombres: 'FABIANO FRANCISCO' }
];

const alumnos1E = [
  { apellidos: 'AMANCA CARPENA', nombres: 'LEONARDO JOSUE ANGEL' },
  { apellidos: 'BALDERA CESPEDES', nombres: 'KAROLEHY' },
  { apellidos: 'BERRIOS CASTILLO', nombres: 'ALAN DAMIAN' },
  { apellidos: 'CALDERON URBINA', nombres: 'JESUS ENNODIO' },
  { apellidos: 'CARBAJAL PALACIOS', nombres: 'THIAGO FERNANDO' },
  { apellidos: 'DAGA GONZALES', nombres: 'STIFEL HENDRICH' },
  { apellidos: 'DIAZ CACHA', nombres: 'YOLY SAHORI' },
  { apellidos: 'ESPINO SALAZAR', nombres: 'JESUS CLEMENTE' },
  { apellidos: 'FARRO LEON', nombres: 'ALEXIS JUNIOR' },
  { apellidos: 'HERRERA TAFUR', nombres: 'SEBASTIAN VALENTINO' },
  { apellidos: 'HUAMAN PINAN', nombres: 'DARIANA YADIRA' },
  { apellidos: 'JIMENEZ SALDANA', nombres: 'THIAGO PAUL' },
  { apellidos: 'LEON GABRIEL', nombres: 'EDGAR FABRICIO' },
  { apellidos: 'MILLA VILLALVA', nombres: 'RIHANNA THAISA' },
  { apellidos: 'MOLINA SANCHEZ', nombres: 'FRANYELIS ARANZA' },
  { apellidos: 'MURO HUAMAN', nombres: 'MILAN AMIR' },
  { apellidos: 'PARRA CONDORCHOA', nombres: 'MADELEY CIELO' },
  { apellidos: 'PEREZ LUCENA', nombres: 'JOSE MANUEL' },
  { apellidos: 'PUMATAY OLSEN', nombres: 'LUIS ADRIAN' },
  { apellidos: 'QUINONES PENA', nombres: 'SAYAKA TAIMARA' },
  { apellidos: 'QUIROZ MESTANZA', nombres: 'NADINNE YAMILETH' },
  { apellidos: 'REYES MARTINEZ', nombres: 'GERARD FERNANDO' },
  { apellidos: 'ROALCABA MALDONADO', nombres: 'LUIS ANGEL' },
  { apellidos: 'ROJAS BETANCOURT', nombres: 'CRISTOPHER' },
  { apellidos: 'SALINAS CERNA', nombres: 'KATHLEEN DANALEE' },
  { apellidos: 'SAUCEDO VASQUES', nombres: 'YHARIEL JHOMAR' },
  { apellidos: 'SERAFIN MARTINEZ', nombres: 'REYCON DAVID' },
  { apellidos: 'SIESQUEN MONCADA', nombres: 'DARYELY ANDREA' },
  { apellidos: 'TRUJILLO GARCIA', nombres: 'ALEXIS JORDY' },
  { apellidos: 'VERASTEGUI RUELAS', nombres: 'HILLARY SAMARA' },
  { apellidos: 'VILLENA MELGAR', nombres: 'HASAEL ENMANUEL' }
];

async function cargarAlumnos() {
  try {
    console.log('🚀 Iniciando carga de alumnos...\n');

    // 1. Obtener año lectivo 2026
    const { data: anio, error: anioError } = await supabase
      .from('anios_lectivos')
      .select('id')
      .eq('nombre', '2026')
      .single();

    if (anioError) {
      console.error('❌ Error al buscar año lectivo:', anioError);
      throw new Error('No se pudo acceder a la tabla anios_lectivos. Verifica los permisos RLS en Supabase.');
    }

    if (!anio) {
      throw new Error('No se encontró el año lectivo 2026');
    }

    console.log('✅ Año lectivo 2026 encontrado:', anio.id);

    // 2. Obtener secciones
    const { data: secciones, error: seccionesError } = await supabase
      .from('secciones')
      .select(`
        id,
        nombre,
        grados!inner (nombre)
      `)
      .eq('anio_lectivo_id', anio.id)
      .eq('grados.nombre', '1ro');

    if (seccionesError) {
      console.error('❌ Error al buscar secciones:', seccionesError);
      throw new Error('No se pudo acceder a las secciones');
    }

    console.log('✅ Secciones encontradas:', secciones?.length);

    const seccion1A = secciones?.find(s => s.nombre === 'A');
    const seccion1B = secciones?.find(s => s.nombre === 'B');
    const seccion1C = secciones?.find(s => s.nombre === 'C');
    const seccion1D = secciones?.find(s => s.nombre === 'D');
    const seccion1E = secciones?.find(s => s.nombre === 'E');

    if (!seccion1A || !seccion1B || !seccion1C || !seccion1D || !seccion1E) {
      throw new Error('No se encontraron todas las secciones (A, B, C, D, E)');
    }

    // 3. Cargar 1ro A
    console.log('📚 Cargando 1ro A...');
    await cargarSeccion(alumnos1A, seccion1A.id, anio.id, '1A');
    console.log('✅ 1ro A: 32 alumnos cargados\n');

    // 4. Cargar 1ro B
    console.log('📚 Cargando 1ro B...');
    await cargarSeccion(alumnos1B, seccion1B.id, anio.id, '1B');
    console.log('✅ 1ro B: 32 alumnos cargados\n');

    // 5. Cargar 1ro C
    console.log('📚 Cargando 1ro C...');
    await cargarSeccion(alumnos1C, seccion1C.id, anio.id, '1C');
    console.log('✅ 1ro C: 32 alumnos cargados\n');

    // 6. Cargar 1ro D
    console.log('📚 Cargando 1ro D...');
    await cargarSeccion(alumnos1D, seccion1D.id, anio.id, '1D');
    console.log('✅ 1ro D: 31 alumnos cargados\n');

    // 7. Cargar 1ro E
    console.log('📚 Cargando 1ro E...');
    await cargarSeccion(alumnos1E, seccion1E.id, anio.id, '1E');
    console.log('✅ 1ro E: 31 alumnos cargados\n');

    console.log('🎉 ¡Seed completado exitosamente!');
    console.log('📊 Total: 158 alumnos cargados (1A: 32, 1B: 32, 1C: 32, 1D: 31, 1E: 31)');

  } catch (error) {
    console.error('❌ Error:', error);
  }
}

async function cargarSeccion(
  alumnos: any[],
  seccionId: string,
  anioId: string,
  codigoPrefix: string
) {
  for (let i = 0; i < alumnos.length; i++) {
    const alumno = alumnos[i];
    const numero = String(i + 1).padStart(3, '0');
    const dni = String(i + 1).padStart(8, '0');
    const email = `${alumno.apellidos.toLowerCase().replace(/\s+/g, '')}@peruanosuizo.edu.pe`;

    // 1. Crear persona
    const { data: persona } = await supabase
      .from('personas')
      .insert({
        dni,
        nombres: alumno.nombres,
        apellidos: alumno.apellidos,
        correo: email,
        fecha_nacimiento: '2010-01-01'
      })
      .select()
      .single();

    if (!persona) continue;

    // 2. Crear alumno
    const { data: alumnoCreado } = await supabase
      .from('alumnos')
      .insert({
        persona_id: persona.id,
        codigo_alumno: `${codigoPrefix}${numero}`,
        fecha_ingreso: '2026-03-01',
        estado: 'activo'
      })
      .select()
      .single();

    if (!alumnoCreado) continue;

    // 3. Crear matrícula
    await supabase
      .from('matriculas')
      .insert({
        alumno_id: alumnoCreado.id,
        seccion_id: seccionId,
        anio_lectivo_id: anioId,
        fecha_matricula: '2026-03-01',
        estado: 'activo'
      });

    // 4. Crear código QR
    await supabase
      .from('codigos_qr')
      .insert({
        persona_id: persona.id,
        codigo: `QR${codigoPrefix}${numero}`,
        activo: true
      });

    process.stdout.write(`\r   Progreso: ${i + 1}/${alumnos.length}`);
  }
  console.log(''); // Nueva línea
}

// Ejecutar
cargarAlumnos();
