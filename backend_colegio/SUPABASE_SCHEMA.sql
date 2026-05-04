-- ============================================
-- ESQUEMA COMPLETO PARA SUPABASE (PostgreSQL)
-- Sistema de Asistencia - IE Peruano Suizo
-- ============================================

-- Habilitar extensión para UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- TABLA: usuarios_usuario
-- ============================================
CREATE TABLE usuarios_usuario (
    id SERIAL PRIMARY KEY,
    password VARCHAR(128) NOT NULL,
    last_login TIMESTAMP WITH TIME ZONE,
    is_superuser BOOLEAN NOT NULL DEFAULT FALSE,
    email VARCHAR(254) UNIQUE NOT NULL,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    rol VARCHAR(20) NOT NULL CHECK (rol IN ('admin', 'profesor', 'padre')),
    telefono VARCHAR(15),
    qr_token UUID UNIQUE NOT NULL DEFAULT uuid_generate_v4(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_staff BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_usuarios_email ON usuarios_usuario(email);
CREATE INDEX idx_usuarios_rol ON usuarios_usuario(rol);
CREATE INDEX idx_usuarios_qr_token ON usuarios_usuario(qr_token);

-- ============================================
-- TABLA: asistencia_grado
-- ============================================
CREATE TABLE asistencia_grado (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    nivel VARCHAR(20) NOT NULL CHECK (nivel IN ('primaria', 'secundaria')),
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_grado_nivel ON asistencia_grado(nivel);

-- ============================================
-- TABLA: asistencia_seccion
-- ============================================
CREATE TABLE asistencia_seccion (
    id SERIAL PRIMARY KEY,
    grado_id INTEGER NOT NULL REFERENCES asistencia_grado(id) ON DELETE CASCADE,
    nombre VARCHAR(10) NOT NULL,
    tutor_id INTEGER REFERENCES usuarios_usuario(id) ON DELETE SET NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE(grado_id, nombre)
);

CREATE INDEX idx_seccion_grado ON asistencia_seccion(grado_id);
CREATE INDEX idx_seccion_tutor ON asistencia_seccion(tutor_id);

-- ============================================
-- TABLA: asistencia_alumno
-- ============================================
CREATE TABLE asistencia_alumno (
    id SERIAL PRIMARY KEY,
    codigo VARCHAR(20) UNIQUE NOT NULL,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    dni VARCHAR(8) UNIQUE NOT NULL,
    seccion_id INTEGER NOT NULL REFERENCES asistencia_seccion(id) ON DELETE RESTRICT,
    fecha_nacimiento DATE NOT NULL,
    qr_token UUID UNIQUE NOT NULL DEFAULT uuid_generate_v4(),
    email_padre VARCHAR(254) UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_alumno_codigo ON asistencia_alumno(codigo);
CREATE INDEX idx_alumno_dni ON asistencia_alumno(dni);
CREATE INDEX idx_alumno_seccion ON asistencia_alumno(seccion_id);
CREATE INDEX idx_alumno_qr_token ON asistencia_alumno(qr_token);
CREATE INDEX idx_alumno_email_padre ON asistencia_alumno(email_padre);

-- ============================================
-- TABLA: asistencia_curso
-- ============================================
CREATE TABLE asistencia_curso (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    codigo VARCHAR(10) UNIQUE NOT NULL,
    grado_id INTEGER NOT NULL REFERENCES asistencia_grado(id) ON DELETE CASCADE,
    profesor_id INTEGER REFERENCES usuarios_usuario(id) ON DELETE SET NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_curso_grado ON asistencia_curso(grado_id);
CREATE INDEX idx_curso_profesor ON asistencia_curso(profesor_id);

-- ============================================
-- TABLA: asistencia_sesionclase
-- ============================================
CREATE TABLE asistencia_sesionclase (
    id SERIAL PRIMARY KEY,
    curso_id INTEGER NOT NULL REFERENCES asistencia_curso(id) ON DELETE CASCADE,
    seccion_id INTEGER NOT NULL REFERENCES asistencia_seccion(id) ON DELETE CASCADE,
    profesor_id INTEGER NOT NULL REFERENCES usuarios_usuario(id) ON DELETE CASCADE,
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME,
    descripcion VARCHAR(200),
    cerrada BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sesion_fecha ON asistencia_sesionclase(fecha);
CREATE INDEX idx_sesion_curso ON asistencia_sesionclase(curso_id);
CREATE INDEX idx_sesion_seccion ON asistencia_sesionclase(seccion_id);
CREATE INDEX idx_sesion_profesor ON asistencia_sesionclase(profesor_id);

-- ============================================
-- TABLA: asistencia_tokenqr
-- ============================================
CREATE TABLE asistencia_tokenqr (
    id SERIAL PRIMARY KEY,
    token UUID UNIQUE NOT NULL DEFAULT uuid_generate_v4(),
    sesion_id INTEGER UNIQUE NOT NULL REFERENCES asistencia_sesionclase(id) ON DELETE CASCADE,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expira_en TIMESTAMP WITH TIME ZONE NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_tokenqr_token ON asistencia_tokenqr(token);
CREATE INDEX idx_tokenqr_sesion ON asistencia_tokenqr(sesion_id);

-- ============================================
-- TABLA: asistencia_asistencia
-- ============================================
CREATE TABLE asistencia_asistencia (
    id SERIAL PRIMARY KEY,
    sesion_id INTEGER NOT NULL REFERENCES asistencia_sesionclase(id) ON DELETE CASCADE,
    alumno_id INTEGER NOT NULL REFERENCES asistencia_alumno(id) ON DELETE CASCADE,
    estado VARCHAR(15) NOT NULL CHECK (estado IN ('presente', 'tardanza', 'ausente', 'justificado')) DEFAULT 'ausente',
    hora_registro TIMESTAMP WITH TIME ZONE,
    registrado_via_qr BOOLEAN NOT NULL DEFAULT FALSE,
    observacion TEXT,
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(sesion_id, alumno_id)
);

CREATE INDEX idx_asistencia_sesion ON asistencia_asistencia(sesion_id);
CREATE INDEX idx_asistencia_alumno ON asistencia_asistencia(alumno_id);
CREATE INDEX idx_asistencia_estado ON asistencia_asistencia(estado);

-- ============================================
-- TABLA: asistencia_docente_asistenciadocente
-- ============================================
CREATE TABLE asistencia_docente_asistenciadocente (
    id SERIAL PRIMARY KEY,
    docente_id INTEGER NOT NULL REFERENCES usuarios_usuario(id) ON DELETE CASCADE,
    fecha DATE NOT NULL,
    estado VARCHAR(15) NOT NULL CHECK (estado IN ('presente', 'tardanza', 'ausente', 'justificado')) DEFAULT 'ausente',
    hora_registro TIMESTAMP WITH TIME ZONE,
    registrado_via_qr BOOLEAN NOT NULL DEFAULT FALSE,
    observacion TEXT,
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(docente_id, fecha)
);

CREATE INDEX idx_asistencia_docente_fecha ON asistencia_docente_asistenciadocente(fecha);
CREATE INDEX idx_asistencia_docente_docente ON asistencia_docente_asistenciadocente(docente_id);

-- ============================================
-- TABLA: comunicados_comunicado
-- ============================================
CREATE TABLE comunicados_comunicado (
    id SERIAL PRIMARY KEY,
    titulo VARCHAR(200) NOT NULL,
    contenido TEXT NOT NULL,
    tipo VARCHAR(10) NOT NULL CHECK (tipo IN ('general', 'grado', 'seccion')) DEFAULT 'general',
    prioridad VARCHAR(12) NOT NULL CHECK (prioridad IN ('normal', 'importante', 'urgente')) DEFAULT 'normal',
    autor_id INTEGER NOT NULL REFERENCES usuarios_usuario(id) ON DELETE CASCADE,
    publicado BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_publicacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comunicado_autor ON comunicados_comunicado(autor_id);
CREATE INDEX idx_comunicado_fecha ON comunicados_comunicado(fecha_publicacion);
CREATE INDEX idx_comunicado_tipo ON comunicados_comunicado(tipo);

-- ============================================
-- TABLA: comunicados_comunicado_grados (ManyToMany)
-- ============================================
CREATE TABLE comunicados_comunicado_grados (
    id SERIAL PRIMARY KEY,
    comunicado_id INTEGER NOT NULL REFERENCES comunicados_comunicado(id) ON DELETE CASCADE,
    grado_id INTEGER NOT NULL REFERENCES asistencia_grado(id) ON DELETE CASCADE,
    UNIQUE(comunicado_id, grado_id)
);

CREATE INDEX idx_comunicado_grados_comunicado ON comunicados_comunicado_grados(comunicado_id);
CREATE INDEX idx_comunicado_grados_grado ON comunicados_comunicado_grados(grado_id);

-- ============================================
-- TABLA: comunicados_comunicado_secciones (ManyToMany)
-- ============================================
CREATE TABLE comunicados_comunicado_secciones (
    id SERIAL PRIMARY KEY,
    comunicado_id INTEGER NOT NULL REFERENCES comunicados_comunicado(id) ON DELETE CASCADE,
    seccion_id INTEGER NOT NULL REFERENCES asistencia_seccion(id) ON DELETE CASCADE,
    UNIQUE(comunicado_id, seccion_id)
);

CREATE INDEX idx_comunicado_secciones_comunicado ON comunicados_comunicado_secciones(comunicado_id);
CREATE INDEX idx_comunicado_secciones_seccion ON comunicados_comunicado_secciones(seccion_id);

-- ============================================
-- TABLA: comunicados_lecturacomunicado
-- ============================================
CREATE TABLE comunicados_lecturacomunicado (
    id SERIAL PRIMARY KEY,
    comunicado_id INTEGER NOT NULL REFERENCES comunicados_comunicado(id) ON DELETE CASCADE,
    usuario_id INTEGER NOT NULL REFERENCES usuarios_usuario(id) ON DELETE CASCADE,
    leido_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(comunicado_id, usuario_id)
);

CREATE INDEX idx_lectura_comunicado ON comunicados_lecturacomunicado(comunicado_id);
CREATE INDEX idx_lectura_usuario ON comunicados_lecturacomunicado(usuario_id);

-- ============================================
-- TABLA: permisos_permisosalida
-- ============================================
CREATE TABLE permisos_permisosalida (
    id SERIAL PRIMARY KEY,
    alumno_id INTEGER NOT NULL REFERENCES asistencia_alumno(id) ON DELETE CASCADE,
    solicitante_id INTEGER NOT NULL REFERENCES usuarios_usuario(id) ON DELETE CASCADE,
    tipo VARCHAR(25) NOT NULL CHECK (tipo IN ('salida_anticipada', 'tardanza_justificada', 'ausencia_justificada')),
    motivo TEXT NOT NULL,
    fecha DATE NOT NULL,
    estado VARCHAR(10) NOT NULL CHECK (estado IN ('pendiente', 'aprobado', 'rechazado')) DEFAULT 'pendiente',
    revisado_por_id INTEGER REFERENCES usuarios_usuario(id) ON DELETE SET NULL,
    observacion_revision TEXT,
    fecha_solicitud TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    fecha_revision TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_permiso_alumno ON permisos_permisosalida(alumno_id);
CREATE INDEX idx_permiso_solicitante ON permisos_permisosalida(solicitante_id);
CREATE INDEX idx_permiso_estado ON permisos_permisosalida(estado);
CREATE INDEX idx_permiso_fecha ON permisos_permisosalida(fecha);

-- ============================================
-- TABLAS DE DJANGO (necesarias para el framework)
-- ============================================

-- django_content_type
CREATE TABLE django_content_type (
    id SERIAL PRIMARY KEY,
    app_label VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    UNIQUE(app_label, model)
);

-- django_migrations
CREATE TABLE django_migrations (
    id SERIAL PRIMARY KEY,
    app VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    applied TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- django_session
CREATE TABLE django_session (
    session_key VARCHAR(40) PRIMARY KEY,
    session_data TEXT NOT NULL,
    expire_date TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_django_session_expire ON django_session(expire_date);

-- ============================================
-- TABLAS PARA JWT (Simple JWT)
-- ============================================

-- token_blacklist_outstandingtoken
CREATE TABLE token_blacklist_outstandingtoken (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES usuarios_usuario(id) ON DELETE CASCADE,
    jti VARCHAR(255) UNIQUE NOT NULL,
    token TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_outstanding_token_user ON token_blacklist_outstandingtoken(user_id);
CREATE INDEX idx_outstanding_token_jti ON token_blacklist_outstandingtoken(jti);

-- token_blacklist_blacklistedtoken
CREATE TABLE token_blacklist_blacklistedtoken (
    id SERIAL PRIMARY KEY,
    token_id INTEGER UNIQUE NOT NULL REFERENCES token_blacklist_outstandingtoken(id) ON DELETE CASCADE,
    blacklisted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ============================================
-- PERMISOS DE DJANGO
-- ============================================

-- auth_permission
CREATE TABLE auth_permission (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    content_type_id INTEGER NOT NULL REFERENCES django_content_type(id) ON DELETE CASCADE,
    codename VARCHAR(100) NOT NULL,
    UNIQUE(content_type_id, codename)
);

-- usuarios_usuario_groups (ManyToMany)
CREATE TABLE usuarios_usuario_groups (
    id SERIAL PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES usuarios_usuario(id) ON DELETE CASCADE,
    group_id INTEGER NOT NULL,
    UNIQUE(usuario_id, group_id)
);

-- usuarios_usuario_user_permissions (ManyToMany)
CREATE TABLE usuarios_usuario_user_permissions (
    id SERIAL PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES usuarios_usuario(id) ON DELETE CASCADE,
    permission_id INTEGER NOT NULL REFERENCES auth_permission(id) ON DELETE CASCADE,
    UNIQUE(usuario_id, permission_id)
);

-- ============================================
-- TRIGGERS PARA ACTUALIZAR fecha_actualizacion
-- ============================================

-- Función para actualizar timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.fecha_actualizacion = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger para usuarios_usuario
CREATE TRIGGER update_usuarios_usuario_updated_at BEFORE UPDATE ON usuarios_usuario
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Trigger para asistencia_asistencia
CREATE TRIGGER update_asistencia_asistencia_updated_at BEFORE UPDATE ON asistencia_asistencia
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Trigger para comunicados_comunicado
CREATE TRIGGER update_comunicados_comunicado_updated_at BEFORE UPDATE ON comunicados_comunicado
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- COMENTARIOS EN LAS TABLAS
-- ============================================

COMMENT ON TABLE usuarios_usuario IS 'Usuarios del sistema (admin, profesores, padres)';
COMMENT ON TABLE asistencia_grado IS 'Grados académicos (1ro, 2do, etc.)';
COMMENT ON TABLE asistencia_seccion IS 'Secciones por grado (A, B, C, etc.)';
COMMENT ON TABLE asistencia_alumno IS 'Estudiantes del colegio';
COMMENT ON TABLE asistencia_curso IS 'Cursos/materias';
COMMENT ON TABLE asistencia_sesionclase IS 'Sesiones de clase';
COMMENT ON TABLE asistencia_asistencia IS 'Registro de asistencia de alumnos';
COMMENT ON TABLE asistencia_docente_asistenciadocente IS 'Registro de asistencia de docentes';
COMMENT ON TABLE comunicados_comunicado IS 'Comunicados y anuncios';
COMMENT ON TABLE permisos_permisosalida IS 'Permisos de salida y justificaciones';

-- ============================================
-- FIN DEL ESQUEMA
-- ============================================
