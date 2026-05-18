const fecha = new Date('2026-05-23T00:00:00'); // Sábado
console.log('Fecha:', fecha.toISOString().split('T')[0]);
console.log('Día de la semana:', fecha.getDay()); // 0=domingo, 1=lunes, 2=martes...
console.log('Día nombre:', ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'][fecha.getDay()]);

const domingo = new Date('2026-05-24T00:00:00'); // Domingo
console.log('\nFecha:', domingo.toISOString().split('T')[0]);
console.log('Día de la semana:', domingo.getDay());
console.log('Día nombre:', ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'][domingo.getDay()]);

process.exit(0);