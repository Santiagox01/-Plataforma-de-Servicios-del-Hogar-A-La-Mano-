import { Platform } from 'react-native';

/**
 * Unico punto de configuracion de la URL base del backend REST.
 *
 * - Emulador Android: http://10.0.2.2:8080
 * - Simulador iOS / Web local: http://localhost:8080
 * - Celular real con Expo Go: define EXPO_PUBLIC_API_URL=http://TU_IP_LOCAL:8080
 *   (por ejemplo http://192.168.1.25:8080) antes de ejecutar npx expo start.
 */
const URL_POR_DEFECTO = Platform.OS === 'android'
  ? 'http://10.0.2.2:8080'
  : 'http://localhost:8080';

export const API_BASE_URL = (process.env.EXPO_PUBLIC_API_URL || URL_POR_DEFECTO).replace(/\/+$/, '');
