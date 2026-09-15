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
      console.log('Using Firebase credentials from environment variable');
      serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
    } 
    // OPCIÓN 2: Archivo local (para desarrollo)
    else {
      const serviceAccountPath = path.join(__dirname, '../../serviceAccountKey.json');
      
      if (!fs.existsSync(serviceAccountPath)) {
        console.warn('serviceAccountKey.json not found and FIREBASE_SERVICE_ACCOUNT is not configured.');
        console.warn('Push notifications will be unavailable.');
        console.warn('For development, place the file in backend_express/serviceAccountKey.json.');
        console.warn('For production, configure the FIREBASE_SERVICE_ACCOUNT variable.');
        return;
      }

      console.log('Using Firebase credentials from local file');
      serviceAccount = require(serviceAccountPath);
    }

    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount)
    });

    firebaseInitialized = true;
    console.log('Firebase Admin SDK initialized successfully');
  } catch (error: any) {
    console.error('Failed to initialize Firebase Admin SDK:', error.message);
    console.warn('Push notifications will be unavailable.');
  }
};

// Inicializar al cargar el módulo
initializeFirebase();

// Exportar el servicio de mensajería
export const messaging = firebaseInitialized ? admin.messaging() : null;
export default admin;
