-- ============================================
-- SCRIPT PARA CREAR TABLAS DE EVALUACIONES Y CALIFICACIONES
-- ============================================

-- Tabla: evaluaciones
-- Almacena las evaluaciones de cada curso (Actitudes, Participación, Exámenes, etc.)
CREATE TABLE IF NOT EXISTS evaluaciones (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  curso_id UUID NOT NULL REFERENCES cursos(id) ON DELETE CASCADE,
  nombre VARCHAR(100) NOT NULL,
  peso DECIMAL(5,2) DEFAULT 1.0, -- Peso para el promedio ponderado
  orden INTEGER DEFAULT 0, -- Orden de visualización
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Índices para evaluaciones
CREATE INDEX IF NOT EXISTS idx_evaluaciones_curso ON evaluaciones(curso_id);
CREATE INDEX IF NOT EXISTS idx_evaluaciones_orden ON evaluaciones(curso_id, orden);

-- Tabla: calificaciones
-- Almacena las notas de cada alumno para cada evaluación
CREATE TABLE IF NOT EXISTS calificaciones (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  evaluacion_id UUID NOT NULL REFERENCES evaluaciones(id) ON DELETE CASCADE,
  alumno_id UUID NOT NULL REFERENCES personas(id) ON DELETE CASCADE,
  calificacion DECIMAL(5,2), -- Nota del alumno (0-20), NULL si no tiene nota aún
  observaciones TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  UNIQUE(evaluacion_id, alumno_id) -- Un alumno solo puede tener una calificación por evaluación
);

-- Índices para calificaciones
CREATE INDEX IF NOT EXISTS idx_calificaciones_evaluacion ON calificaciones(evaluacion_id);
CREATE INDEX IF NOT EXISTS idx_calificaciones_alumno ON calificaciones(alumno_id);
CREATE INDEX IF NOT EXISTS idx_calificaciones_alumno_evaluacion ON calificaciones(alumno_id, evaluacion_id);

-- Trigger para actualizar updated_at en evaluaciones
CREATE OR REPLACE FUNCTION update_evaluaciones_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_evaluaciones_updated_at
  BEFORE UPDATE ON evaluaciones
  FOR EACH ROW
  EXECUTE FUNCTION update_evaluaciones_updated_at();

-- Trigger para actualizar updated_at en calificaciones
CREATE OR REPLACE FUNCTION update_calificaciones_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_calificaciones_updated_at
  BEFORE UPDATE ON calificaciones
  FOR EACH ROW
  EXECUTE FUNCTION update_calificaciones_updated_at();

-- ============================================
-- POLÍTICAS RLS (Row Level Security)
-- ============================================

-- Habilitar RLS
ALTER TABLE evaluaciones ENABLE ROW LEVEL SECURITY;
ALTER TABLE calificaciones ENABLE ROW LEVEL SECURITY;

-- Políticas para evaluaciones
-- Los profesores pueden ver evaluaciones de sus cursos
CREATE POLICY "Profesores pueden ver evaluaciones de sus cursos"
  ON evaluaciones FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM cursos
      WHERE cursos.id = evaluaciones.curso_id
      AND cursos.profesor_id = auth.uid()
    )
  );

-- Los profesores pueden crear evaluaciones en sus cursos
CREATE POLICY "Profesores pueden crear evaluaciones en sus cursos"
  ON evaluaciones FOR INSERT
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM cursos
      WHERE cursos.id = evaluaciones.curso_id
      AND cursos.profesor_id = auth.uid()
    )
  );

-- Los profesores pueden actualizar evaluaciones de sus cursos
CREATE POLICY "Profesores pueden actualizar evaluaciones de sus cursos"
  ON evaluaciones FOR UPDATE
  USING (
    EXISTS (
      SELECT 1 FROM cursos
      WHERE cursos.id = evaluaciones.curso_id
      AND cursos.profesor_id = auth.uid()
    )
  );

-- Los profesores pueden eliminar evaluaciones de sus cursos
CREATE POLICY "Profesores pueden eliminar evaluaciones de sus cursos"
  ON evaluaciones FOR DELETE
  USING (
    EXISTS (
      SELECT 1 FROM cursos
      WHERE cursos.id = evaluaciones.curso_id
      AND cursos.profesor_id = auth.uid()
    )
  );

-- Políticas para calificaciones
-- Los profesores pueden ver calificaciones de sus cursos
CREATE POLICY "Profesores pueden ver calificaciones de sus cursos"
  ON calificaciones FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM evaluaciones
      JOIN cursos ON cursos.id = evaluaciones.curso_id
      WHERE evaluaciones.id = calificaciones.evaluacion_id
      AND cursos.profesor_id = auth.uid()
    )
  );

-- Los alumnos pueden ver sus propias calificaciones
CREATE POLICY "Alumnos pueden ver sus propias calificaciones"
  ON calificaciones FOR SELECT
  USING (alumno_id = auth.uid());

-- Los profesores pueden actualizar calificaciones de sus cursos
CREATE POLICY "Profesores pueden actualizar calificaciones de sus cursos"
  ON calificaciones FOR UPDATE
  USING (
    EXISTS (
      SELECT 1 FROM evaluaciones
      JOIN cursos ON cursos.id = evaluaciones.curso_id
      WHERE evaluaciones.id = calificaciones.evaluacion_id
      AND cursos.profesor_id = auth.uid()
    )
  );

-- ============================================
-- DATOS DE EJEMPLO (OPCIONAL)
-- ============================================

-- Insertar evaluaciones por defecto para un curso de ejemplo
-- Reemplaza 'TU_CURSO_ID' con un ID real de la tabla cursos
/*
INSERT INTO evaluaciones (curso_id, nombre, peso, orden) VALUES
  ('TU_CURSO_ID', 'Actitudes', 1.0, 1),
  ('TU_CURSO_ID', 'Participacion', 1.0, 2),
  ('TU_CURSO_ID', 'Proyecto', 1.0, 3),
  ('TU_CURSO_ID', 'Examen I', 1.0, 4),
  ('TU_CURSO_ID', 'Examen II', 1.0, 5),
  ('TU_CURSO_ID', 'Examen final', 1.0, 6);
*/

-- ============================================
-- VISTAS ÚTILES
-- ============================================

-- Vista: Promedio de calificaciones por alumno y curso
CREATE OR REPLACE VIEW vista_promedios_alumnos AS
SELECT 
  c.alumno_id,
  e.curso_id,
  p.nombre_completo,
  cu.nombre as curso_nombre,
  ROUND(
    SUM(c.calificacion * e.peso) / NULLIF(SUM(e.peso), 0),
    2
  ) as promedio_ponderado,
  COUNT(c.id) as total_evaluaciones,
  COUNT(c.calificacion) as evaluaciones_con_nota
FROM calificaciones c
JOIN evaluaciones e ON e.id = c.evaluacion_id
JOIN personas p ON p.id = c.alumno_id
JOIN cursos cu ON cu.id = e.curso_id
GROUP BY c.alumno_id, e.curso_id, p.nombre_completo, cu.nombre;

-- Vista: Resumen de evaluaciones por curso
CREATE OR REPLACE VIEW vista_resumen_evaluaciones AS
SELECT 
  e.curso_id,
  cu.nombre as curso_nombre,
  e.id as evaluacion_id,
  e.nombre as evaluacion_nombre,
  e.peso,
  e.orden,
  COUNT(c.id) as total_alumnos,
  COUNT(c.calificacion) as alumnos_con_nota,
  ROUND(AVG(c.calificacion), 2) as promedio_evaluacion,
  MIN(c.calificacion) as nota_minima,
  MAX(c.calificacion) as nota_maxima
FROM evaluaciones e
JOIN cursos cu ON cu.id = e.curso_id
LEFT JOIN calificaciones c ON c.evaluacion_id = e.id
GROUP BY e.curso_id, cu.nombre, e.id, e.nombre, e.peso, e.orden
ORDER BY e.curso_id, e.orden;

-- ============================================
-- COMENTARIOS EN LAS TABLAS
-- ============================================

COMMENT ON TABLE evaluaciones IS 'Almacena las evaluaciones de cada curso (Actitudes, Participación, Exámenes, etc.)';
COMMENT ON COLUMN evaluaciones.peso IS 'Peso para el cálculo del promedio ponderado';
COMMENT ON COLUMN evaluaciones.orden IS 'Orden de visualización en la interfaz';

COMMENT ON TABLE calificaciones IS 'Almacena las notas de cada alumno para cada evaluación';
COMMENT ON COLUMN calificaciones.calificacion IS 'Nota del alumno (0-20), NULL si no tiene nota aún';
