// Probar la conversión de días
const fechas = [
  '2026-05-18', // Lunes
  '2026-05-19', // Martes  
  '2026-05-20', // Miércoles
  '2026-05-21', // Jueves
  '2026-05-22', // Viernes
  '2026-05-23', // Sábado
  '2026-05-24'  // Domingo
];

fechas.forEach(fecha => {
  const fechaObj = new Date(fecha + 'T00:00:00');
  const jsDay = fechaObj.getDay();
  const nombres = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];
  
  let resultado = '';
  if (jsDay === 0 || jsDay === 6) {
    resultado = 'FIN DE SEMANA - No hay clases';
  } else {
    resultado = `Día escolar: ${jsDay}`;
  }
  
  console.log(`${fecha} (${nombres[jsDay]}) -> JS: ${jsDay} -> ${resultado}`);
});

process.exit(0);