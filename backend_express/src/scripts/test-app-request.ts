import fetch from 'node-fetch';

async function testAppRequest() {
  try {
    console.log('🧪 Simulando petición de la app móvil...\n');

    // 1. Login como alumno
    console.log('1️⃣ Login como alumno...');
    const loginResponse = await fetch('http://192.168.101.7:3000/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: 'liseth.abad@peruanosuizo.edu.pe',
        password: 'Suizo2026*'
      })
    });

    const loginData = await loginResponse.json();
    
    if (!loginData.success) {
      console.log('❌ Login falló:', loginData.message);
      return;
    }

    const userId = loginData.data.user.id;
    const token = loginData.data.tokens.access;
    
    console.log('✅ Login exitoso');
    console.log(`   User ID: ${userId}`);
    console.log(`   Nombre: ${loginData.data.user.nombre_completo}`);
    console.log(`   Token: ${token.substring(0, 20)}...`);

    // 2. Obtener horarios
    console.log('\n2️⃣ Obteniendo horarios para el lunes 18 mayo 2026...');
    const fecha = '2026-05-18';
    const horariosUrl = `http://192.168.101.7:3000/api/horarios/alumno/${userId}?fecha=${fecha}`;
    
    console.log(`   URL: ${horariosUrl}`);
    
    const horariosResponse = await fetch(horariosUrl, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });

    const horariosData = await horariosResponse.json();
    
    console.log(`   Status: ${horariosResponse.status}`);
    console.log(`   Success: ${horariosData.success}`);
    console.log(`   Horarios encontrados: ${horariosData.data?.length || 0}`);
    
    if (horariosData.data && horariosData.data.length > 0) {
      console.log('\n✅ Horarios del lunes:');
      horariosData.data.forEach((h: any) => {
        console.log(`   ${h.hora_inicio}-${h.hora_fin}: ${h.curso} - ${h.profesor}`);
      });
    } else {
      console.log('\n❌ No se encontraron horarios');
      console.log('   Mensaje:', horariosData.message);
    }

  } catch (error: any) {
    console.error('❌ Error:', error.message);
  }
}

testAppRequest().then(() => {
  console.log('\n🏁 Test completado');
  process.exit(0);
});