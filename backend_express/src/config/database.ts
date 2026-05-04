import { createClient, SupabaseClient } from '@supabase/supabase-js';
import dotenv from 'dotenv';

dotenv.config();

// Validar variables de entorno
if (!process.env.SUPABASE_URL || !process.env.SUPABASE_ANON_KEY) {
  throw new Error('Faltan variables de entorno: SUPABASE_URL y SUPABASE_ANON_KEY son requeridas');
}

// Crear cliente de Supabase
const supabase: SupabaseClient = createClient(
  process.env.SUPABASE_URL,
  process.env.SUPABASE_ANON_KEY,
  {
    auth: {
      autoRefreshToken: true,
      persistSession: false
    }
  }
);

// Verificar conexión
const testConnection = async () => {
  try {
    const { data, error } = await supabase
      .from('alumnos')
      .select('count')
      .limit(1);
    
    if (error) {
      console.error('❌ Error al conectar con Supabase:', error.message);
    } else {
      console.log('✅ Supabase conectado correctamente');
    }
  } catch (err: any) {
    console.error('❌ Error de conexión:', err.message);
  }
};

// Ejecutar test de conexión
testConnection();

export default supabase;
