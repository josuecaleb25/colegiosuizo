import supabase from '../config/database';
import * as fs from 'fs';

async function exportarDatos() {
  console.log('=== EXPORTANDO DATOS DE SUPABASE ===\n');

  let sqlOutput = `-- ============================================================
-- DATOS EXPORTADOS DESDE SUPABASE
-- Fecha: ${new Date().toISOString()}
-- ============================================================\n\n`;

  // 1. Exportar personas
  console.log('Exportando personas...');
  const { data: personas } = await supabase
    .from('personas')
    .select('*');

  if (personas && personas.length > 0) {
    sqlOutput += `-- PERSONAS (${personas.length} registros)\n`;
    personas.forEach((p: any) => {
      const dni = p.dni ? `'${p.dni}'` : 'NULL';
      const fechaNac = p.fecha_nacimiento ? `'${p.fecha_nacimiento}'` : 'NULL';
      const genero = p.genero ? `'${p.genero}'` : 'NULL';
      const telefono = p.telefono ? `'${p.telefono}'` : 'NULL';
      const correo = p.correo ? `'${p.correo}'` : 'NULL';
      const fotoUrl = p.foto_url ? `'${p.foto_url}'` : 'NULL';
      
      sqlOutput += `INSERT INTO personas (id, dni, nombres, apellidos, fecha_nacimiento, genero, telefono, correo, foto_url, creado_en, actualizado_en)
VALUES ('${p.id}', ${dni}, '${p.nombres}', '${p.apellidos}', ${fechaNac}, ${genero}, ${telefono}, ${correo}, ${fotoUrl}, '${p.creado_en}', '${p.actualizado_en}')
ON CONFLICT (id) DO NOTHING;\n`;
    });
    sqlOutput += '\n';
    console.log(`✅ ${personas.length} personas exportadas`);
  }

  // 2. Exportar alumnos
  console.log('Exportando alumnos...');
  const { data: alumnos } = await supabase
    .from('alumnos')
    .select('*');

  if (alumnos && alumnos.length > 0) {
    sqlOutput += `-- ALUMNOS (${alumnos.length} registros)\n`;
    alumnos.forEach((a: any) => {
      const fechaIngreso = a.fecha_ingreso || 'CURRENT_DATE';
      sqlOutput += `INSERT INTO alumnos (id, persona_id, codigo_alumno, fecha_ingreso, estado, creado_en, actualizado_en)
VALUES ('${a.id}', '${a.persona_id}', '${a.codigo_alumno}', '${fechaIngreso}', '${a.estado}', '${a.creado_en}', '${a.actualizado_en}')
ON CONFLICT (id) DO NOTHING;\n`;
    });
    sqlOutput += '\n';
    console.log(`✅ ${alumnos.length} alumnos exportados`);
  }

  // 3. Exportar matrículas
  console.log('Exportando matrículas...');
  const { data: matriculas } = await supabase
    .from('matriculas')
    .select('*');

  if (matriculas && matriculas.length > 0) {
    sqlOutput += `-- MATRÍCULAS (${matriculas.length} registros)\n`;
    matriculas.forEach((m: any) => {
      const fechaMatricula = m.fecha_matricula || 'CURRENT_DATE';
      sqlOutput += `INSERT INTO matriculas (id, alumno_id, seccion_id, anio_lectivo_id, fecha_matricula, estado, creado_en, actualizado_en)
VALUES ('${m.id}', '${m.alumno_id}', '${m.seccion_id}', '${m.anio_lectivo_id}', '${fechaMatricula}', '${m.estado}', '${m.creado_en}', '${m.actualizado_en}')
ON CONFLICT (id) DO NOTHING;\n`;
    });
    sqlOutput += '\n';
    console.log(`✅ ${matriculas.length} matrículas exportadas`);
  }

  // 4. Exportar códigos QR
  console.log('Exportando códigos QR...');
  const { data: codigosQr } = await supabase
    .from('codigos_qr')
    .select('*');

  if (codigosQr && codigosQr.length > 0) {
    sqlOutput += `-- CÓDIGOS QR (${codigosQr.length} registros)\n`;
    codigosQr.forEach((qr: any) => {
      const expiraEn = qr.expira_en ? `'${qr.expira_en}'` : 'NULL';
      sqlOutput += `INSERT INTO codigos_qr (id, persona_id, codigo, activo, generado_en, expira_en)
VALUES ('${qr.id}', '${qr.persona_id}', '${qr.codigo}', ${qr.activo}, '${qr.generado_en}', ${expiraEn})
ON CONFLICT (id) DO NOTHING;\n`;
    });
    sqlOutput += '\n';
    console.log(`✅ ${codigosQr.length} códigos QR exportados`);
  }

  // 5. Exportar usuarios
  console.log('Exportando usuarios...');
  const { data: usuarios } = await supabase
    .from('usuarios')
    .select('*');

  if (usuarios && usuarios.length > 0) {
    sqlOutput += `-- USUARIOS (${usuarios.length} registros)\n`;
    usuarios.forEach((u: any) => {
      const ultimoAcceso = u.ultimo_acceso ? `'${u.ultimo_acceso}'` : 'NULL';
      sqlOutput += `INSERT INTO usuarios (id, persona_id, email, password, rol, activo, ultimo_acceso, creado_en, actualizado_en)
VALUES ('${u.id}', '${u.persona_id}', '${u.email}', '${u.password}', '${u.rol}', ${u.activo}, ${ultimoAcceso}, '${u.creado_en}', '${u.actualizado_en}')
ON CONFLICT (id) DO NOTHING;\n`;
    });
    sqlOutput += '\n';
    console.log(`✅ ${usuarios.length} usuarios exportados`);
  }

  // 6. Exportar docentes
  console.log('Exportando docentes...');
  const { data: docentes } = await supabase
    .from('docentes')
    .select('*');

  if (docentes && docentes.length > 0) {
    sqlOutput += `-- DOCENTES (${docentes.length} registros)\n`;
    docentes.forEach((d: any) => {
      const especialidad = d.especialidad ? `'${d.especialidad}'` : 'NULL';
      const tipoContrato = d.tipo_contrato ? `'${d.tipo_contrato}'` : 'NULL';
      sqlOutput += `INSERT INTO docentes (id, persona_id, codigo_docente, especialidad, tipo_contrato, estado, creado_en, actualizado_en)
VALUES ('${d.id}', '${d.persona_id}', '${d.codigo_docente}', ${especialidad}, ${tipoContrato}, '${d.estado}', '${d.creado_en}', '${d.actualizado_en}')
ON CONFLICT (id) DO NOTHING;\n`;
    });
    sqlOutput += '\n';
    console.log(`✅ ${docentes.length} docentes exportados`);
  }

  // 7. Exportar asignaciones
  console.log('Exportando asignaciones...');
  const { data: asignaciones } = await supabase
    .from('asignaciones')
    .select('*');

  if (asignaciones && asignaciones.length > 0) {
    sqlOutput += `-- ASIGNACIONES (${asignaciones.length} registros)\n`;
    asignaciones.forEach((a: any) => {
      sqlOutput += `INSERT INTO asignaciones (id, docente_id, curso_id, seccion_id, anio_lectivo_id, creado_en)
VALUES ('${a.id}', '${a.docente_id}', '${a.curso_id}', '${a.seccion_id}', '${a.anio_lectivo_id}', '${a.creado_en}')
ON CONFLICT (id) DO NOTHING;\n`;
    });
    sqlOutput += '\n';
    console.log(`✅ ${asignaciones.length} asignaciones exportadas`);
  }

  // 8. Exportar evaluaciones
  console.log('Exportando evaluaciones...');
  const { data: evaluaciones } = await supabase
    .from('evaluaciones')
    .select('*');

  if (evaluaciones && evaluaciones.length > 0) {
    sqlOutput += `-- EVALUACIONES (${evaluaciones.length} registros)\n`;
    evaluaciones.forEach((e: any) => {
      sqlOutput += `INSERT INTO evaluaciones (id, asignacion_id, nombre, peso, orden, activo, creado_en)
VALUES ('${e.id}', '${e.asignacion_id}', '${e.nombre}', ${e.peso}, ${e.orden}, ${e.activo}, '${e.creado_en}')
ON CONFLICT (id) DO NOTHING;\n`;
    });
    sqlOutput += '\n';
    console.log(`✅ ${evaluaciones.length} evaluaciones exportadas`);
  }

  // 9. Exportar calificaciones
  console.log('Exportando calificaciones...');
  const { data: calificaciones } = await supabase
    .from('calificaciones')
    .select('*');

  if (calificaciones && calificaciones.length > 0) {
    sqlOutput += `-- CALIFICACIONES (${calificaciones.length} registros)\n`;
    calificaciones.forEach((c: any) => {
      const nota = c.nota !== null ? c.nota : 'NULL';
      const observaciones = c.observaciones ? `'${c.observaciones.replace(/'/g, "''")}'` : 'NULL';
      const fechaRegistro = c.fecha_registro || 'CURRENT_DATE';
      sqlOutput += `INSERT INTO calificaciones (id, evaluacion_id, alumno_id, nota, observaciones, fecha_registro, creado_en, actualizado_en)
VALUES ('${c.id}', '${c.evaluacion_id}', '${c.alumno_id}', ${nota}, ${observaciones}, '${fechaRegistro}', '${c.creado_en}', '${c.actualizado_en}')
ON CONFLICT (id) DO NOTHING;\n`;
    });
    sqlOutput += '\n';
    console.log(`✅ ${calificaciones.length} calificaciones exportadas`);
  }

  // 10. Exportar horarios
  console.log('Exportando horarios...');
  const { data: horarios } = await supabase
    .from('horarios')
    .select('*');

  if (horarios && horarios.length > 0) {
    sqlOutput += `-- HORARIOS (${horarios.length} registros)\n`;
    horarios.forEach((h: any) => {
      const salon = h.salon ? `'${h.salon}'` : 'NULL';
      sqlOutput += `INSERT INTO horarios (id, asignacion_id, dia_semana, hora_inicio, hora_fin, salon, activo, creado_en)
VALUES ('${h.id}', '${h.asignacion_id}', ${h.dia_semana}, '${h.hora_inicio}', '${h.hora_fin}', ${salon}, ${h.activo}, '${h.creado_en}')
ON CONFLICT (id) DO NOTHING;\n`;
    });
    sqlOutput += '\n';
    console.log(`✅ ${horarios.length} horarios exportados`);
  }

  // Guardar archivo
  const filename = 'datos_exportados_supabase.sql';
  fs.writeFileSync(filename, sqlOutput);
  
  console.log(`\n✅ Datos exportados exitosamente a: ${filename}`);
  console.log('\nResumen:');
  console.log(`- Personas: ${personas?.length || 0}`);
  console.log(`- Alumnos: ${alumnos?.length || 0}`);
  console.log(`- Matrículas: ${matriculas?.length || 0}`);
  console.log(`- Códigos QR: ${codigosQr?.length || 0}`);
  console.log(`- Usuarios: ${usuarios?.length || 0}`);
  console.log(`- Docentes: ${docentes?.length || 0}`);
  console.log(`- Asignaciones: ${asignaciones?.length || 0}`);
  console.log(`- Evaluaciones: ${evaluaciones?.length || 0}`);
  console.log(`- Calificaciones: ${calificaciones?.length || 0}`);
  console.log(`- Horarios: ${horarios?.length || 0}`);

  process.exit(0);
}

exportarDatos().catch(console.error);
