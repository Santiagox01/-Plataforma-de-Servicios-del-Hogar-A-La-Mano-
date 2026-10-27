import 'react-native-url-polyfill/auto';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { createClient } from '@supabase/supabase-js';

export const SUPABASE_PROJECT_REF = 'ucpzdfslwfegiyypxnkg';
export const SUPABASE_URL = `https://${SUPABASE_PROJECT_REF}.supabase.co`;

const STORAGE_KEY_CUSTOM_ANON = 'a_la_mano_supabase_anon_key_v1';

// Adaptador seguro para AsyncStorage en Expo / React Native
const safeStorageAdapter = {
  getItem: async (key) => {
    try {
      return await AsyncStorage.getItem(key);
    } catch (e) {
      console.warn('AsyncStorage safe getItem error:', e);
      return null;
    }
  },
  setItem: async (key, value) => {
    try {
      await AsyncStorage.setItem(key, value);
    } catch (e) {
      console.warn('AsyncStorage safe setItem error:', e);
    }
  },
  removeItem: async (key) => {
    try {
      await AsyncStorage.removeItem(key);
    } catch (e) {
      console.warn('AsyncStorage safe removeItem error:', e);
    }
  }
};

// Variable interna de la clave activa
let activeAnonKey =
  process.env.EXPO_PUBLIC_SUPABASE_ANON_KEY || '';

export let supabase = createClient(SUPABASE_URL, activeAnonKey || 'placeholder-anon-key', {
  auth: {
    storage: safeStorageAdapter,
    autoRefreshToken: true,
    persistSession: true,
    detectSessionInUrl: false
  }
});

// Inicializar y cargar clave personalizada si fue guardada
export async function initializeSupabaseClient() {
  try {
    const savedKey = await AsyncStorage.getItem(STORAGE_KEY_CUSTOM_ANON);
    if (savedKey && savedKey.trim().length > 10) {
      activeAnonKey = savedKey.trim();
      supabase = createClient(SUPABASE_URL, activeAnonKey, {
        auth: {
          storage: safeStorageAdapter,
          autoRefreshToken: true,
          persistSession: true,
          detectSessionInUrl: false
        }
      });
      return { initialized: true, key: activeAnonKey };
    }
  } catch (err) {
    console.warn('Could not read saved custom anon key:', err);
  }
  return { initialized: false, key: activeAnonKey };
}

// Guardar nueva clave de Supabase
export async function saveSupabaseAnonKey(newKey) {
  const clean = String(newKey).trim();
  if (!clean) {
    await AsyncStorage.removeItem(STORAGE_KEY_CUSTOM_ANON);
    activeAnonKey = '';
    return { success: false, message: 'La clave no puede estar vacía' };
  }

  activeAnonKey = clean;
  await AsyncStorage.setItem(STORAGE_KEY_CUSTOM_ANON, clean);

  supabase = createClient(SUPABASE_URL, activeAnonKey, {
    auth: {
      storage: safeStorageAdapter,
      autoRefreshToken: true,
      persistSession: true,
      detectSessionInUrl: false
    }
  });

  // Validar conexión real
  return await testSupabaseConnection(clean);
}

// Probar conexión real a la API de Supabase
export async function testSupabaseConnection(keyToTest = activeAnonKey) {
  const testKey = keyToTest || activeAnonKey;
  if (!testKey || testKey === 'placeholder-anon-key') {
    return {
      connected: false,
      message: 'Falta la clave pública anónima (anon public key) de Supabase.',
      projectRef: SUPABASE_PROJECT_REF
    };
  }

  const start = Date.now();
  try {
    const response = await fetch(`${SUPABASE_URL}/auth/v1/settings`, {
      method: 'GET',
      headers: {
        apikey: testKey,
        Authorization: `Bearer ${testKey}`
      }
    });

    const elapsed = Date.now() - start;

    if (response.status === 200) {
      return {
        connected: true,
        latencyMs: elapsed,
        message: `¡Conexión exitosa a Supabase (${elapsed}ms)! Servidor en línea.`,
        projectRef: SUPABASE_PROJECT_REF
      };
    } else if (response.status === 401) {
      const body = await response.json().catch(() => ({}));
      return {
        connected: false,
        message: body.message || 'La clave anon ingresada es inválida para este proyecto.',
        projectRef: SUPABASE_PROJECT_REF
      };
    } else {
      return {
        connected: false,
        message: `Respuesta de Supabase HTTP ${response.status}`,
        projectRef: SUPABASE_PROJECT_REF
      };
    }
  } catch (error) {
    return {
      connected: false,
      message: `Error de red al conectar con ${SUPABASE_URL}: ${error.message}`,
      projectRef: SUPABASE_PROJECT_REF
    };
  }
}

export function getActiveAnonKey() {
  return activeAnonKey;
}
