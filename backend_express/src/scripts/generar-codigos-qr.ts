import supabase from '../config/database';
import { v4 as uuidv4 } from 'uuid';

async function generarCodigosQR() {
  try {
    console.log('🔧 Generando códigos QR para todos los alumnos...\n');

    // Obtener todos los alumnos con su persona_id
    const { data: alumnos, error: errorAlumnos } = await supabase
      .from('alumnos')
      .select(`
        id,
        codigo_alumno,
        persona_id
      `)
      .eq('estado', 'activo');

    if (errorAlumnos) {
      console.error('❌ Error al obtener alumnos:', errorAlumnos);
      return;
    }

    if (!alumnos || alumnos.length === 0) {
      console.log('⚠️ No hay alumnos en la base de datos');
      return;
    }

    console.log(`✅ Se encontraron ${alumnos.length} alumnos\n`);

    let creados = 0;
    let actualizados = 0;
    let errores = 0;

    for (const alumno of alumnos) {
      try {
        // Verificar si ya tiene un código QR activo
        const { data: qrExistente } = await supabase
          .from('codigos_qr')
          .select('id, codigo')
          .eq('persona_id', alumno.persona_id)
          .eq('activo', true)
          .single();

        if (qrExistente) {
          console.log(`✓ Alumno ${alumno.codigo_alumno} ya tiene QR: ${qrExistente.codigo}`);
          actualizados++;
        } else {
          // Usar el codigo_alumno como código QR
          const codigoQR = alumno.codigo_alumno;

          // Insertar nuevo código QR
          const { error: errorInsert } = await supabase
            .from('codigos_qr')
            .insert({
              persona_id: alumno.persona_id,
              codigo: codigoQR,
              activo: true
            });

          if (errorInsert) {
            console.error(`❌ Error al crear QR para ${alumno.codigo_alumno}:`, errorInsert.message);
            errores++;
          } else {
            console.log(`✅ QR creado para ${alumno.codigo_alumno}: ${codigoQR}`);
            creados++;
          }
        }
      } catch (error: any) {
        console.error(`❌ Error procesando alumno ${alumno.codigo_alumno}:`, error.message);
        errores++;
      }
    }

    console.log('\n📊 Resumen:');
    console.log(`   - Códigos QR creados: ${creados}`);
    console.log(`   - Códigos QR existentes: ${actualizados}`);
    console.log(`   - Errores: ${errores}`);
    console.log(`   - Total procesados: ${alumnos.length}`);

  } catch (error: any) {
    console.error('❌ Error general:', error.message);
  }
}

generarCodigosQR()
  .then(() => {
    console.log('\n🎉 Proceso completado');
    process.exit(0);
  })
  .catch((error) => {
    console.error('💥 Error fatal:', error);
    process.exit(1);
  });
