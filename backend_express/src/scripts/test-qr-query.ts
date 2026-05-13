import supabase from '../config/database';

async function testQRQuery() {
  try {
    // Obtener un alumno
    const { data: alumno } = await supabase
      .from('alumnos')
      .select('id, codigo_alumno, persona_id')
      .limit(1)
      .single();

    console.log('Alumno:', alumno);

    if (alumno) {
      // Buscar su código QR
      const { data: qr, error } = await supabase
        .from('codigos_qr')
        .select('*')
        .eq('persona_id', alumno.persona_id)
        .eq('activo', true)
        .single();

      console.log('QR encontrado:', qr);
      console.log('Error:', error);
    }
  } catch (error: any) {
    console.error('Error:', error.message);
  }
}

testQRQuery();
