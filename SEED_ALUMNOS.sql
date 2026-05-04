-- ============================================================
-- SEED: 158 ALUMNOS DE 1RO A-E
-- IE Peruano Suizo - Año Lectivo 2026
-- ============================================================

-- Obtener IDs necesarios
DO $$
DECLARE
    v_anio_id UUID;
    v_seccion_1a UUID;
    v_seccion_1b UUID;
    v_seccion_1c UUID;
    v_seccion_1d UUID;
    v_seccion_1e UUID;
BEGIN
    -- Obtener año lectivo 2026
    SELECT id INTO v_anio_id FROM anios_lectivos WHERE nombre = '2026' LIMIT 1;
    
    -- Obtener IDs de secciones
    SELECT s.id INTO v_seccion_1a 
    FROM secciones s 
    JOIN grados g ON s.grado_id = g.id 
    WHERE g.nombre = '1ro' AND s.nombre = 'A' AND s.anio_lectivo_id = v_anio_id;
    
    SELECT s.id INTO v_seccion_1b 
    FROM secciones s 
    JOIN grados g ON s.grado_id = g.id 
    WHERE g.nombre = '1ro' AND s.nombre = 'B' AND s.anio_lectivo_id = v_anio_id;
    
    SELECT s.id INTO v_seccion_1c 
    FROM secciones s 
    JOIN grados g ON s.grado_id = g.id 
    WHERE g.nombre = '1ro' AND s.nombre = 'C' AND s.anio_lectivo_id = v_anio_id;
    
    SELECT s.id INTO v_seccion_1d 
    FROM secciones s 
    JOIN grados g ON s.grado_id = g.id 
    WHERE g.nombre = '1ro' AND s.nombre = 'D' AND s.anio_lectivo_id = v_anio_id;
    
    SELECT s.id INTO v_seccion_1e 
    FROM secciones s 
    JOIN grados g ON s.grado_id = g.id 
    WHERE g.nombre = '1ro' AND s.nombre = 'E' AND s.anio_lectivo_id = v_anio_id;

    -- ============================================================
    -- 1RO A - 32 ALUMNOS
    -- ============================================================
    
    -- Función helper para insertar alumno
    CREATE TEMP TABLE temp_alumnos_1a (
        apellidos VARCHAR(100),
        nombres VARCHAR(100),
        email VARCHAR(150)
    );
    
    INSERT INTO temp_alumnos_1a VALUES
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
    ('VILCHEZ PARAGUAY', 'NAHIDU SOFIA', 'vilchezparaguay@peruanosuizo.edu.pe');
    
    -- Insertar personas, alumnos, matrículas y códigos QR para 1ro A
    INSERT INTO personas (dni, nombres, apellidos, correo, fecha_nacimiento)
    SELECT 
        LPAD((ROW_NUMBER() OVER())::TEXT, 8, '0'),
        nombres,
        apellidos,
        email,
        '2010-01-01'::DATE
    FROM temp_alumnos_1a;
    
    INSERT INTO alumnos (persona_id, codigo_alumno, fecha_ingreso, estado)
    SELECT 
        p.id,
        '1A' || LPAD((ROW_NUMBER() OVER())::TEXT, 3, '0'),
        '2026-03-01'::DATE,
        'activo'
    FROM personas p
    WHERE p.correo IN (SELECT email FROM temp_alumnos_1a);
    
    INSERT INTO matriculas (alumno_id, seccion_id, anio_lectivo_id, fecha_matricula, estado)
    SELECT 
        a.id,
        v_seccion_1a,
        v_anio_id,
        '2026-03-01'::DATE,
        'activo'
    FROM alumnos a
    JOIN personas p ON a.persona_id = p.id
    WHERE p.correo IN (SELECT email FROM temp_alumnos_1a);
    
    INSERT INTO codigos_qr (persona_id, codigo, activo)
    SELECT 
        p.id,
        'QR1A' || LPAD((ROW_NUMBER() OVER())::TEXT, 3, '0'),
        TRUE
    FROM personas p
    WHERE p.correo IN (SELECT email FROM temp_alumnos_1a);
    
    DROP TABLE temp_alumnos_1a;
    
    RAISE NOTICE '✅ 1ro A: 32 alumnos cargados';

END $$;

-- Nota: Este es solo 1ro A como ejemplo
-- El archivo completo sería muy largo (158 alumnos)
-- ¿Quieres que continúe con 1ro B, C, D, E?
