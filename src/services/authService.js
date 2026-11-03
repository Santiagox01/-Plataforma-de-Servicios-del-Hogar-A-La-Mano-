import { supabase, testSupabaseConnection } from './supabase';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Role, ProStatus } from '../constants/statuses';
import { AVATARS } from '../constants/config';
import { createUser, validateRoleFields } from './userFactory';

const LOCAL_USER_KEY = 'a_la_mano_current_user_v1';

export const DEMO_USERS = {
  client: {
    id: 'a1111111-1111-1111-1111-111111111111',
    name: 'Juanita Restrepo V.',
    email: 'cliente@alamano.co',
    phone: '3004567890',
    role: Role.CLIENT,
    zone: 'El Poblado',
    address: 'Calle 10 # 43E-20, Apt 502, El Poblado, Medellín',
    avatar: AVATARS.client
  },
  professional: {
    id: 'a2222222-2222-2222-2222-222222222222',
    name: 'Carlos Mario Restrepo',
    email: 'carlos@alamano.co',
    phone: '3117894561',
    role: Role.PROFESSIONAL,
    status: ProStatus.VERIFIED,
    specialtyName: 'Electricista Residencial e Industrial',
    conteReg: 'CONTE TE1-49201',
    ccNumber: '71.394.205',
    zone: 'Laureles',
    address: 'Circular 4 # 73-15, Laureles, Medellín',
    avatar: AVATARS.professional
  },
  pendingPro: {
    id: 'a5555555-5555-5555-5555-555555555555',
    name: 'Pedro Pablo Jaramillo',
    email: 'pedro@alamano.co',
    phone: '3189901122',
    role: Role.PROFESSIONAL,
    status: ProStatus.PENDING,
    specialtyName: 'Cerrajería 24/7 y Acabados',
    conteReg: 'Certificado SENA #CER-8401',
    ccNumber: '1.036.711.592',
    zone: 'Envigado',
    address: 'Calle 38 Sur # 43A-12, Envigado',
    avatar: 'https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=150&auto=format&fit=crop&q=80'
  },
  admin: {
    id: 'a9999999-9999-9999-9999-999999999999',
    name: 'Laura Restrepo (Auditoría)',
    email: 'admin@alamano.co',
    phone: '3000000000',
    role: Role.ADMIN,
    zone: 'El Poblado',
    address: 'Sede Operativa A La Mano, Medellín',
    avatar: 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=150&auto=format&fit=crop&q=80'
  }
};

// Índice por correo: reemplaza los 4 bloques "if (cleanEmail === ...)" repetidos
const DEMO_BY_EMAIL = Object.fromEntries(
  Object.values(DEMO_USERS).map((u) => [u.email, u])
);

// Validaciones para Colombia (HU01)
export function validateEmail(email) {
  const re = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
  return re.test(String(email).trim().toLowerCase());
}

export function validatePhone(phone) {
  const clean = String(phone).replace(/\D/g, '');
  return clean.length === 10 && clean.startsWith('3');
}

export function validatePassword(password) {
  const pass = String(password || '');
  return {
    hasLength: pass.length >= 8,
    hasUpper: /[A-Z]/.test(pass),
    hasNumber: /[0-9]/.test(pass),
    isValid: pass.length >= 8 && /[A-Z]/.test(pass) && /[0-9]/.test(pass)
  };
}

export async function getLocalUser() {
  try {
    const raw = await AsyncStorage.getItem(LOCAL_USER_KEY);
    if (raw) return JSON.parse(raw);
  } catch (e) {
    console.warn('Error reading local user:', e);
  }
  return DEMO_USERS.client;
}

export async function setLocalUser(user) {
  try {
    await AsyncStorage.setItem(LOCAL_USER_KEY, JSON.stringify(user));
  } catch (e) {
    console.warn('Error saving local user:', e);
  }
}

// Si el correo es de una cuenta demo, inicia sesión con ella; si no, devuelve null
async function tryDemoLogin(cleanEmail) {
  const demo = DEMO_BY_EMAIL[cleanEmail];
  if (!demo) return null;
  await setLocalUser(demo);
  return { success: true, user: demo, isDemo: true };
}

// Iniciar sesión (con soporte Supabase Auth y fallback demo)
export async function loginUser(email, password) {
  const cleanEmail = String(email || '').trim().toLowerCase();

  if (!cleanEmail || !password) {
    return { success: false, message: 'Ingresa correo y contraseña.' };
  }

  // 1. Intentar con Supabase Auth si está conectado
  try {
    const { data: authData, error: authError } = await supabase.auth.signInWithPassword({
      email: cleanEmail,
      password: String(password)
    });

    if (!authError && authData?.user) {
      // Consultar perfil en tabla profiles
      const { data: profile } = await supabase
        .from('profiles')
        .select('*')
        .eq('id', authData.user.id)
        .maybeSingle();

      let proData = null;
      if (
        profile?.role === Role.PROFESSIONAL ||
        (!profile && authData.user.user_metadata?.role === Role.PROFESSIONAL)
      ) {
        const { data: pro } = await supabase
          .from('professionals')
          .select('*')
          .eq('profile_id', authData.user.id)
          .maybeSingle();
        proData = pro;
      }

      // Factory Method: el rol decide cómo se construye el usuario
      const userObj = createUser(
        profile?.role || authData.user.user_metadata?.role || Role.CLIENT,
        {
          id: authData.user.id,
          name: profile?.full_name || authData.user.user_metadata?.full_name || cleanEmail.split('@')[0],
          email: cleanEmail,
          phone: profile?.phone || authData.user.user_metadata?.phone || '3000000000',
          status: proData?.status,
          zone: profile?.zone,
          address: profile?.address,
          specialtyName: proData?.specialty_name || authData.user.user_metadata?.specialtyName,
          ccNumber: proData?.cc_number || authData.user.user_metadata?.ccNumber,
          conteReg: proData?.conte_reg || authData.user.user_metadata?.conteReg,
          avatar: profile?.avatar_url,
          isSupabase: true
        }
      );

      await setLocalUser(userObj);
      return { success: true, user: userObj, isSupabase: true };
    } else if (authError) {
      console.log('Supabase auth sign in error:', authError.message);
      // Si el error no es de conexión sino de credenciales erróneas en Supabase:
      if (authError.message.includes('Invalid login credentials')) {
        // Revisar si coincide con cuentas demo para facilitar pruebas
        const demo = await tryDemoLogin(cleanEmail);
        if (demo) return demo;
        return { success: false, message: 'Credenciales inválidas en Supabase. Verifica tu correo y contraseña.' };
      }
    }
  } catch (e) {
    console.log('Supabase offline or exception, using smart fallback.');
  }

  // 2. Fallback inteligente para cuentas demo de evaluación académica
  const demo = await tryDemoLogin(cleanEmail);
  if (demo) return demo;

  // Si no está en Supabase y no es demo, crear usuario local para no bloquear la experiencia
  const customUser = createUser(Role.CLIENT, {
    id: `usr_${Date.now()}`,
    name: cleanEmail.split('@')[0].toUpperCase(),
    email: cleanEmail,
    phone: '3001234567',
    address: 'Medellín, Antioquia'
  });
  await setLocalUser(customUser);
  return { success: true, user: customUser, isLocalFallback: true };
}

// Registro de usuario (HU01 y HU02)
export async function registerUser(formData) {
  const {
    name,
    email,
    phone,
    password,
    role = Role.CLIENT,
    zone = 'El Poblado',
    address = 'Medellín',
    specialtyName,
    ccNumber,
    conteReg,
    bio
  } = formData;

  if (!name || !name.trim()) {
    return { success: false, message: 'Ingresa tu nombre completo.' };
  }
  if (!email || !validateEmail(email)) {
    return { success: false, message: 'Ingresa un correo electrónico válido.' };
  }
  if (!phone || !validatePhone(phone)) {
    return { success: false, message: 'El celular debe tener 10 dígitos y comenzar por 3 (Ej: 300 123 4567).' };
  }
  const passCheck = validatePassword(password);
  if (!passCheck.isValid) {
    return { success: false, message: 'La contraseña debe tener mínimo 8 caracteres, al menos una mayúscula y un número.' };
  }

  // Validación específica del rol (la define cada Creator de la fábrica)
  const roleError = validateRoleFields(role, formData);
  if (roleError) {
    return { success: false, message: roleError };
  }

  const cleanEmail = email.trim().toLowerCase();
  const cleanPhone = phone.replace(/\D/g, '');
  let registeredUserId = `usr_${Date.now()}`;
  let registeredInSupabase = false;

  // 1. Intentar registrar en Supabase Auth y Postgres
  try {
    const { data: authData, error: authError } = await supabase.auth.signUp({
      email: cleanEmail,
      password: String(password),
      options: {
        data: {
          full_name: name.trim(),
          phone: cleanPhone,
          role,
          zone,
          address,
          specialtyName: specialtyName || null,
          ccNumber: ccNumber || null,
          conteReg: conteReg || null
        }
      }
    });

    if (authError) {
      console.log('Supabase signUp error:', authError.message);
    }

    if (!authError && authData?.user) {
      registeredUserId = authData.user.id;
      registeredInSupabase = true;

      // Insertar en tabla profiles
      await supabase.from('profiles').upsert([
        {
          id: registeredUserId,
          auth_user_id: registeredUserId,
          email: cleanEmail,
          full_name: name.trim(),
          phone: cleanPhone,
          role,
          zone,
          address,
          avatar_url: role === Role.PROFESSIONAL ? AVATARS.professional : AVATARS.client
        }
      ]);

      // Si es profesional, insertar en tabla professionals y crear expediente de documentos
      if (role === Role.PROFESSIONAL) {
        const { data: proData } = await supabase.from('professionals').insert([
          {
            profile_id: registeredUserId,
            specialty_name: specialtyName.trim(),
            categories: [specialtyName.toLowerCase().includes('elec') ? 'electricidad' : specialtyName.toLowerCase().includes('plom') ? 'plomeria' : specialtyName.toLowerCase().includes('gas') ? 'gas' : 'cerrajeria'],
            base_rate: 45000.00,
            distance_km: 2.5,
            rating: 5.0,
            review_count: 0,
            cc_number: ccNumber.trim(),
            conte_reg: conteReg ? conteReg.trim() : null,
            bio: bio || `Especialista en ${specialtyName} en el Valle de Aburrá.`,
            status: ProStatus.PENDING,
            is_available: false
          }
        ]).select().maybeSingle();

        if (proData?.id) {
          // Crear registros iniciales de documentos de verificación (HU02)
          await supabase.from('verification_documents').insert([
            {
              professional_id: proData.id,
              document_type: 'cc_front',
              file_url: 'https://alamano.co/docs/cc_front_demo.pdf',
              status: 'submitted'
            },
            {
              professional_id: proData.id,
              document_type: 'police_record',
              file_url: 'https://alamano.co/docs/policia_demo.pdf',
              status: 'submitted'
            }
          ]);
        }
      }
    }
  } catch (err) {
    console.log('Supabase offline or exception in register:', err);
  }

  // Factory Method: construir el usuario de la sesión según el rol
  // (el profesional siempre nace en estado "pending", HU02/HU03)
  const newUser = createUser(role, {
    id: registeredUserId,
    name: name.trim(),
    email: cleanEmail,
    phone: cleanPhone,
    zone,
    address,
    specialtyName,
    ccNumber,
    conteReg,
    isSupabase: registeredInSupabase
  });

  await setLocalUser(newUser);

  return {
    success: true,
    user: newUser,
    isSupabase: registeredInSupabase,
    message: role === Role.PROFESSIONAL
      ? '¡Registro completado! Tu expediente técnico y cédula han sido radicados para auditoría y verificación. Puedes revisar el estado de tus documentos.'
      : '¡Bienvenido a A La Mano! Tu cuenta de cliente fue creada exitosamente.'
  };
}

// Cerrar sesión
export async function logoutUser() {
  try {
    await supabase.auth.signOut();
  } catch (e) {
    console.warn('No se pudo cerrar sesión en Supabase:', e?.message);
  }
  try {
    await AsyncStorage.removeItem(LOCAL_USER_KEY);
  } catch (e) {
    console.warn('No se pudo borrar el usuario local:', e?.message);
  }
  return true;
}

// Recuperar contraseña
export async function resetPassword(email) {
  const cleanEmail = String(email || '').trim().toLowerCase();
  if (!cleanEmail || !validateEmail(cleanEmail)) {
    return { success: false, message: 'Ingresa un correo electrónico válido.' };
  }
  try {
    const { error } = await supabase.auth.resetPasswordForEmail(cleanEmail);
    if (!error) {
      return { success: true, message: `Te hemos enviado un enlace de recuperación a ${cleanEmail}.` };
    }
    return { success: false, message: error.message };
  } catch (e) {
    return { success: true, message: `Enlace de restablecimiento simulado enviado a ${cleanEmail}.` };
  }
}
