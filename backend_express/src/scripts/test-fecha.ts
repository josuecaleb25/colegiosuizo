const fecha = new Date('2026-05-18T00:00:00');
console.log('Fecha:', fecha.toISOString().split('T')[0]);
console.log('Día de la semana:', fecha.getDay()); // 0=domingo, 1=lunes, 2=martes...
console.log('Día nombre:', ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'][fecha.getDay()]);

const fecha2 = new Date('2026-05-19T00:00:00');
console.log('\nFecha:', fecha2.toISOString().split('T')[0]);
console.log('Día de la semana:', fecha2.getDay());
console.log('Día nombre:', ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'][fecha2.getDay()]);

process.exit(0);