import * as admin from 'firebase-admin';
import * as path from 'path';
import * as fs from 'fs';

let firebaseInitialized = false;

// Inicializar Firebase Admin SDK
const initializeFirebase = () => {
  if (firebaseInitialized) {
    return;
  }

  try {
    let serviceAccount: any;

    // OPCIÓN 1: Variable de entorno (para Railway/Render/producción)
    if (process.env.FIREBASE_SERVICE_ACCOUNT) {
      console.log('📦 Usando credenciales de Firebase desde variable de entorno');
      serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
    } 
    // OPCIÓN 2: Archivo local (para desarrollo)
    else {
      const serviceAccountPath = path.join(__dirname, '../../serviceAccountKey.json');
      
      if (!fs.existsSync(serviceAccountPath)) {
        console.warn('⚠️  serviceAccountKey.json no encontrado y FIREBASE_SERVICE_ACCOUNT no está configurado.');
        console.warn('   Las notificaciones push no estarán disponibles.');
        console.warn('   Para desarrollo: Coloca el archivo en backend_express/serviceAccountKey.json');
        console.warn('   Para producción: Configura la variable FIREBASE_SERVICE_ACCOUNT');
        return;
      }

      console.log('📦 Usando credenciales de Firebase desde archivo local');
      serviceAccount = require(serviceAccountPath);
    }

    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount)
    });

    firebaseInitialized = true;
    console.log('✅ Firebase Admin SDK inicializado correctamente');
  } catch (error: any) {
    console.error('❌ Error al inicializar Firebase Admin SDK:', error.message);
    console.warn('   Las notificaciones push no estarán disponibles.');
  }
};

// Inicializar al cargar el módulo
initializeFirebase();

// Exportar el servicio de mensajería
export const messaging = firebaseInitialized ? admin.messaging() : null;
export default admin;
