-- ============================================================
-- SISTEMA DE GESTIÓN ESCOLAR - IE PERUANO SUIZO
-- Ejecutar en: Supabase > SQL Editor > New Query
-- ============================================================

-- Habilitar extensión UUID
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- MÓDULO 1: ESTRUCTURA DEL COLEGIO
-- ============================================================

CREATE TABLE colegio (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre          VARCHAR(200) NOT NULL DEFAULT 'IE Peruano Suizo',
    direccion       TEXT,
    telefono        VARCHAR(20),
    logo_url        TEXT,
    creado_en       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    actualizado_en  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE anios_lectivos (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre          VARCHAR(20) NOT NULL,        -- "2025", "2026"
    fecha_inicio    DATE NOT NULL,
    fecha_fin       DATE NOT NULL,
    activo          BOOLEAN DEFAULT FALSE,
    creado_en       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    actualizado_en  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE grados (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre          VARCHAR(50) NOT NULL,        -- "1ro", "2do", "3ro", "4to", "5to"
    orden           SMALLINT NOT NULL,           -- 1, 2, 3, 4, 5
    nivel           VARCHAR(50) DEFAULT 'secundaria',
    creado_en       TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE secciones (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    grado_id            UUID NOT NULL REFERENCES grados(id) ON DELETE CASCADE,
    anio_lectivo_id     UUID NOT NULL REFERENCES anios_lectivos(id) ON DELETE CASCADE,
    nombre              VARCHAR(5) NOT NULL,     -- "A", "B", "C", "D", "E"
    capacidad           SMALLINT DEFAULT 35,
    aula                VARCHAR(50),
    creado_en           TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    actualizado_en      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(grado_id, anio_lectivo_id, nombre)
);

-- ============================================================
-- MÓDULO 2: PERSONAS Y USUARIOS
-- ============================================================

CREATE TABLE personas (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dni                 VARCHAR(20) UNIQUE,
    nombres             VARCHAR(100) NOT NULL,
    apellidos           VARCHAR(100) NOT NULL,
    fecha_nacimiento    DATE,
    genero              CHAR(1) CHECK (genero IN ('M', 'F')),
    telefono            VARCHAR(20),
    correo              VARCHAR(150) UNIQUE,
    foto_url            TEXT,
    creado_en           TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    actualizado_en      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Usuarios para login (admin, profesores, padres)
CREATE TABLE usuarios (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    persona_id      UUID NOT NULL REFERENCES personas(id) ON DELETE CASCADE,
    email           VARCHAR(254) UNIQUE NOT NULL,
    password        VARCHAR(128) NOT NULL,
    rol             VARCHAR(30) NOT NULL CHECK (rol IN ('administrador', 'profesor', 'padre')),
    activo          BOOLEAN DEFAULT TRUE,
    ultimo_acceso   TIMESTAMP WITH TIME ZONE,
    creado_en       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    actualizado_en  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ============================================================
-- MÓDULO 3: ALUMNOS Y MATRÍCULAS
-- ============================================================

CREATE TABLE alumnos (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    persona_id      UUID NOT NULL REFERENCES personas(id) ON DELETE CASCADE,
    codigo_alumno   VARCHAR(20) UNIQUE NOT NULL,
    fecha_ingreso   DATE DEFAULT CURRENT_DATE,
    estado          VARCHAR(20) DEFAULT 'activo' CHECK (estado IN ('activo', 'inactivo', 'trasladado')),
    creado_en       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    actualizado_en  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE matriculas (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    alumno_id           UUID NOT NULL REFERENCES alumnos(id) ON DELETE CASCADE,
    seccion_id          UUID NOT NULL REFERENCES secciones(id) ON DELETE CASCADE,
    anio_lectivo_id     UUID NOT NULL REFERENCES anios_lectivos(id) ON DELETE CASCADE,
    fecha_matricula     DATE DEFAULT CURRENT_DATE,
    estado              VARCHAR(20) DEFAULT 'activo' CHECK (estado IN ('activo', 'retirado')),
    creado_en           TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    actualizado_en      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(alumno_id, anio_lectivo_id)
);

-- ============================================================
-- MÓDULO 4: DOCENTES
-- ============================================================

CREATE TABLE docentes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    persona_id      UUID NOT NULL REFERENCES personas(id) ON DELETE CASCADE,
    codigo_docente  VARCHAR(20) UNIQUE NOT NULL,
    especialidad    VARCHAR(100),
    tipo_contrato   VARCHAR(50),
    estado          VARCHAR(20) DEFAULT 'activo' CHECK (estado IN ('activo', 'inactivo')),
    creado_en       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    actualizado_en  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Tutores por sección
CREATE TABLE tutor_seccion (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    docente_id          UUID NOT NULL REFERENCES docentes(id) ON DELETE CASCADE,
    seccion_id          UUID NOT NULL REFERENCES secciones(id) ON DELETE CASCADE,
    anio_lectivo_id     UUID NOT NULL REFERENCES anios_lectivos(id) ON DELETE CASCADE,
    creado_en           TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(seccion_id, anio_lectivo_id)
);

-- ============================================================
-- MÓDULO 5: ASISTENCIA POR QR
-- ============================================================

CREATE TABLE codigos_qr (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    persona_id      UUID NOT NULL REFERENCES personas(id) ON DELETE CASCADE,
    codigo          VARCHAR(255) UNIQUE NOT NULL,
    activo          BOOLEAN DEFAULT TRUE,
    generado_en     TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expira_en       TIMESTAMP WITH TIME ZONE
);

CREATE TABLE asistencias (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    persona_id      UUID NOT NULL REFERENCES personas(id) ON DELETE CASCADE,
    tipo_persona    VARCHAR(20) NOT NULL CHECK (tipo_persona IN ('alumno', 'docente')),
    fecha           DATE NOT NULL,
    hora_entrada    TIME,
    hora_salida     TIME,
    estado          VARCHAR(20) DEFAULT 'presente' CHECK (estado IN ('presente', 'tardanza', 'falta', 'justificado')),
    registrado_por  UUID REFERENCES usuarios(id),
    observacion     TEXT,
    creado_en       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(persona_id, fecha)
);

CREATE TABLE justificaciones (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asistencia_id   UUID NOT NULL REFERENCES asistencias(id) ON DELETE CASCADE,
    motivo          TEXT NOT NULL,
    documento_url   TEXT,
    aprobado_por    UUID REFERENCES usuarios(id),
    creado_en       TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ============================================================
-- MÓDULO 6: COMUNICADOS
-- ============================================================

CREATE TABLE comunicados (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    titulo          VARCHAR(200) NOT NULL,
    contenido       TEXT NOT NULL,
    tipo            VARCHAR(30) DEFAULT 'general' CHECK (tipo IN ('general', 'grado', 'seccion')),
    grado_id        UUID REFERENCES grados(id),
    seccion_id      UUID REFERENCES secciones(id),
    archivo_url     TEXT,
    publicado       BOOLEAN DEFAULT FALSE,
    publicado_en    TIMESTAMP WITH TIME ZONE,
    creado_por      UUID REFERENCES usuarios(id),
    creado_en       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    actualizado_en  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE comunicado_lecturas (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    comunicado_id   UUID NOT NULL REFERENCES comunicados(id) ON DELETE CASCADE,
    usuario_id      UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    leido_en        TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(comunicado_id, usuario_id)
);

-- ============================================================
-- ÍNDICES
-- ============================================================

-- Asistencias
CREATE INDEX idx_asistencias_persona_fecha    ON asistencias(persona_id, fecha);
CREATE INDEX idx_asistencias_fecha            ON asistencias(fecha);
CREATE INDEX idx_asistencias_estado           ON asistencias(estado);

-- Matrículas
CREATE INDEX idx_matriculas_alumno            ON matriculas(alumno_id);
CREATE INDEX idx_matriculas_seccion           ON matriculas(seccion_id);
CREATE INDEX idx_matriculas_anio              ON matriculas(anio_lectivo_id);

-- Comunicados
CREATE INDEX idx_comunicados_grado            ON comunicados(grado_id);
CREATE INDEX idx_comunicados_seccion          ON comunicados(seccion_id);
CREATE INDEX idx_comunicados_publicado        ON comunicados(publicado);

-- Códigos QR
CREATE INDEX idx_codigos_qr_persona           ON codigos_qr(persona_id);
CREATE INDEX idx_codigos_qr_codigo            ON codigos_qr(codigo);

-- Usuarios
CREATE INDEX idx_usuarios_email               ON usuarios(email);
CREATE INDEX idx_usuarios_rol                 ON usuarios(rol);

-- Personas
CREATE INDEX idx_personas_dni                 ON personas(dni);
CREATE INDEX idx_personas_correo              ON personas(correo);

-- ============================================================
-- TRIGGERS
-- ============================================================

CREATE OR REPLACE FUNCTION actualizar_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.actualizado_en = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_anios_lectivos_actualizado
BEFORE UPDATE ON anios_lectivos
FOR EACH ROW EXECUTE FUNCTION actualizar_timestamp();

CREATE TRIGGER trg_secciones_actualizado
BEFORE UPDATE ON secciones
FOR EACH ROW EXECUTE FUNCTION actualizar_timestamp();

CREATE TRIGGER trg_personas_actualizado
BEFORE UPDATE ON personas
FOR EACH ROW EXECUTE FUNCTION actualizar_timestamp();

CREATE TRIGGER trg_usuarios_actualizado
BEFORE UPDATE ON usuarios
FOR EACH ROW EXECUTE FUNCTION actualizar_timestamp();

CREATE TRIGGER trg_alumnos_actualizado
BEFORE UPDATE ON alumnos
FOR EACH ROW EXECUTE FUNCTION actualizar_timestamp();

CREATE TRIGGER trg_matriculas_actualizado
BEFORE UPDATE ON matriculas
FOR EACH ROW EXECUTE FUNCTION actualizar_timestamp();

CREATE TRIGGER trg_docentes_actualizado
BEFORE UPDATE ON docentes
FOR EACH ROW EXECUTE FUNCTION actualizar_timestamp();

CREATE TRIGGER trg_comunicados_actualizado
BEFORE UPDATE ON comunicados
FOR EACH ROW EXECUTE FUNCTION actualizar_timestamp();

-- ============================================================
-- DATOS INICIALES
-- ============================================================

-- Colegio
INSERT INTO colegio (nombre, direccion, telefono)
VALUES ('IE Peruano Suizo', 'Lima, Perú', '01-1234567');

-- Año lectivo 2026
INSERT INTO anios_lectivos (nombre, fecha_inicio, fecha_fin, activo)
VALUES ('2026', '2026-03-01', '2026-12-15', TRUE);

-- 5 grados de secundaria
INSERT INTO grados (nombre, orden, nivel) VALUES
('1ro', 1, 'secundaria'),
('2do', 2, 'secundaria'),
('3ro', 3, 'secundaria'),
('4to', 4, 'secundaria'),
('5to', 5, 'secundaria');

-- Secciones A, B, C, D, E para cada grado en 2026
INSERT INTO secciones (grado_id, anio_lectivo_id, nombre, capacidad)
SELECT g.id, a.id, s.seccion, 35
FROM grados g
JOIN anios_lectivos a ON a.nombre = '2026'
CROSS JOIN (VALUES ('A'), ('B'), ('C'), ('D'), ('E')) AS s(seccion);

-- Usuario administrador
INSERT INTO personas (dni, nombres, apellidos, correo)
VALUES ('12345678', 'Admin', 'Sistema', 'admin@colegio.com');

INSERT INTO usuarios (persona_id, email, password, rol)
SELECT id, 'admin@colegio.com', '$2a$10$placeholder', 'administrador'
FROM personas WHERE correo = 'admin@colegio.com';

-- Usuario profesor
INSERT INTO personas (dni, nombres, apellidos, correo)
VALUES ('87654321', 'Juan', 'Pérez', 'profesor@colegio.com');

INSERT INTO usuarios (persona_id, email, password, rol)
SELECT id, 'profesor@colegio.com', '$2a$10$placeholder', 'profesor'
FROM personas WHERE correo = 'profesor@colegio.com';

-- ============================================================
-- FIN DEL ESQUEMA
-- ============================================================
