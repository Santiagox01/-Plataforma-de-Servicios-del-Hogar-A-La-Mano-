import React, { useState } from 'react';
import {
  Modal,
  View,
  Text,
  StyleSheet,
  TextInput,
  TouchableOpacity,
  ScrollView,
  Alert,
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { THEME } from '../constants/theme';
import {
  loginUser,
  registerUser,
  resetPassword,
  validateEmail,
  validatePhone,
  validatePassword,
  DEMO_USERS
} from '../services/authService';

export default function AuthModal({ visible, onClose, onAuthSuccess, onOpenSupabaseConfig }) {
  const [mode, setMode] = useState('login'); // 'login' | 'register'
  const [role, setRole] = useState('client'); // 'client' | 'professional'

  // Campos formulario
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [zone, setZone] = useState('El Poblado');
  const [address, setAddress] = useState('');
  const [specialtyName, setSpecialtyName] = useState('Electricista Residencial');
  const [ccNumber, setCcNumber] = useState('');
  const [conteReg, setConteReg] = useState('');
  const [bio, setBio] = useState('');
  const [loading, setLoading] = useState(false);

  // Zonas de Medellín
  const ZONES = ['El Poblado', 'Laureles', 'Belén', 'Envigado', 'Robledo', 'Castilla', 'Buenos Aires', 'La Candelaria'];
  const SPECIALTIES = [
    'Electricista Residencial e Industrial',
    'Maestro Plomero e Hidráulico',
    'Técnico Gas y Calentadores',
    'Cerrajería 24/7 y Blindajes',
    'Pintura y Acabados de Hogar'
  ];

  const passValidation = validatePassword(password);

  const handleLogin = async () => {
    if (!email.trim() || !password) {
      Alert.alert('Datos Requeridos', 'Por favor ingresa tu correo y contraseña.');
      return;
    }
    setLoading(true);
    const res = await loginUser(email, password);
    setLoading(false);

    if (res.success) {
      Alert.alert(
        '¡Inicio de Sesión Exitoso!',
        `Bienvenido(a) de nuevo, ${res.user.name}.`
      );
      onAuthSuccess(res.user);
      onClose();
    } else {
      Alert.alert('Error de Autenticación', res.message || 'Credenciales inválidas.');
    }
  };

  const handleRegister = async () => {
    setLoading(true);
    const res = await registerUser({
      name,
      email,
      phone,
      password,
      role,
      zone,
      address,
      specialtyName: role === 'professional' ? specialtyName : null,
      ccNumber: role === 'professional' ? ccNumber : null,
      conteReg: role === 'professional' ? conteReg : null,
      bio: role === 'professional' ? bio : null
    });
    setLoading(false);

    if (res.success) {
      Alert.alert(
        '¡Cuenta Registrada!',
        res.message,
        [
          {
            text: 'Continuar a la App',
            onPress: () => {
              onAuthSuccess(res.user);
              onClose();
            }
          }
        ]
      );
    } else {
      Alert.alert('Atención al Registrarse', res.message);
    }
  };

  const handleForgotPassword = async () => {
    if (!email.trim() || !validateEmail(email)) {
      Alert.alert('Correo Requerido', 'Escribe tu correo electrónico en el campo superior para enviarte el enlace de restablecimiento.');
      return;
    }
    setLoading(true);
    const res = await resetPassword(email);
    setLoading(false);
    Alert.alert('Restablecimiento de Contraseña', res.message);
  };

  const handleQuickDemo = (userObj) => {
    onAuthSuccess(userObj);
    onClose();
    Alert.alert(
      'Acceso Demo Evaluativo',
      `Sesión activa como: ${userObj.name}\nRol: ${userObj.role.toUpperCase()}${userObj.status ? ` (Estado: ${userObj.status.toUpperCase()})` : ''}`
    );
  };

  return (
    <Modal visible={visible} animationType="slide" transparent onRequestClose={onClose}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        style={styles.overlay}
      >
        <View style={styles.sheet}>
          <View style={styles.handle} />

          {/* Header */}
          <View style={styles.header}>
            <View>
              <Text style={styles.title}>
                {mode === 'login' ? 'Iniciar Sesión en A La Mano' : 'Crear Cuenta en A La Mano'}
              </Text>
              <Text style={styles.subtitle}>Plataforma Móvil de Servicios • Medellín</Text>
            </View>
            <TouchableOpacity onPress={onClose} style={styles.closeBtn}>
              <Ionicons name="close" size={22} color={THEME.colors.textMuted} />
            </TouchableOpacity>
          </View>

          <ScrollView
            showsVerticalScrollIndicator={false}
            contentContainerStyle={styles.scrollContent}
            keyboardShouldPersistTaps="handled"
          >
            {/* Toggle Login / Register */}
            <View style={styles.tabToggle}>
              <TouchableOpacity
                style={[styles.tabBtn, mode === 'login' && styles.tabBtnActive]}
                onPress={() => setMode('login')}
              >
                <Text style={[styles.tabBtnText, mode === 'login' && styles.tabBtnTextActive]}>Iniciar Sesión</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.tabBtn, mode === 'register' && styles.tabBtnActive]}
                onPress={() => setMode('register')}
              >
                <Text style={[styles.tabBtnText, mode === 'register' && styles.tabBtnTextActive]}>Registrarse</Text>
              </TouchableOpacity>
            </View>

            {/* Accesos Rápidos de Demostración y Evaluación */}
            <View style={styles.demoSection}>
              <Text style={styles.demoLabel}>Cuentas Rápidas de Prueba y Sustentación:</Text>
              <View style={styles.demoGrid}>
                <TouchableOpacity style={styles.demoChip} onPress={() => handleQuickDemo(DEMO_USERS.client)}>
                  <Ionicons name="person" size={13} color={THEME.colors.primary} />
                  <Text style={styles.demoChipText}>Juanita (Cliente)</Text>
                </TouchableOpacity>

                <TouchableOpacity style={styles.demoChip} onPress={() => handleQuickDemo(DEMO_USERS.professional)}>
                  <Ionicons name="checkmark-done" size={13} color="#15803d" />
                  <Text style={styles.demoChipText}>Carlos (Técnico Verificado)</Text>
                </TouchableOpacity>

                <TouchableOpacity style={styles.demoChip} onPress={() => handleQuickDemo(DEMO_USERS.pendingPro)}>
                  <Ionicons name="time" size={13} color="#b45309" />
                  <Text style={styles.demoChipText}>Pedro (Técnico Pendiente)</Text>
                </TouchableOpacity>

                <TouchableOpacity style={styles.demoChip} onPress={() => handleQuickDemo(DEMO_USERS.admin)}>
                  <Ionicons name="shield-checkmark" size={13} color="#0f172a" />
                  <Text style={styles.demoChipText}>Laura (Auditora Admin)</Text>
                </TouchableOpacity>
              </View>
            </View>

            {mode === 'register' && (
              <>
                {/* Selector de Rol */}
                <Text style={styles.fieldLabel}>¿Cómo deseas utilizar la plataforma?</Text>
                <View style={styles.roleSelectorRow}>
                  <TouchableOpacity
                    style={[styles.roleBox, role === 'client' && styles.roleBoxActive]}
                    onPress={() => setRole('client')}
                  >
                    <Ionicons name="home" size={20} color={role === 'client' ? THEME.colors.primary : THEME.colors.textMuted} />
                    <Text style={[styles.roleBoxText, role === 'client' && styles.roleBoxTextActive]}>Soy Cliente</Text>
                    <Text style={styles.roleBoxSub}>Solicitar servicios y reparaciones</Text>
                  </TouchableOpacity>

                  <TouchableOpacity
                    style={[styles.roleBox, role === 'professional' && styles.roleBoxActive]}
                    onPress={() => setRole('professional')}
                  >
                    <Ionicons name="construct" size={20} color={role === 'professional' ? THEME.colors.primary : THEME.colors.textMuted} />
                    <Text style={[styles.roleBoxText, role === 'professional' && styles.roleBoxTextActive]}>Soy Técnico</Text>
                    <Text style={styles.roleBoxSub}>Acreditarme y ofrecer servicios</Text>
                  </TouchableOpacity>
                </View>

                {/* Nombre */}
                <View style={styles.inputGroup}>
                  <Text style={styles.fieldLabel}>Nombre Completo *</Text>
                  <TextInput
                    style={styles.input}
                    placeholder="Ej. Juan Fernando Restrepo"
                    value={name}
                    onChangeText={setName}
                  />
                </View>
              </>
            )}

            {/* Correo Electrónico */}
            <View style={styles.inputGroup}>
              <Text style={styles.fieldLabel}>Correo Electrónico *</Text>
              <TextInput
                style={styles.input}
                placeholder="ejemplo@alamano.co"
                keyboardType="email-address"
                autoCapitalize="none"
                value={email}
                onChangeText={setEmail}
              />
            </View>

            {mode === 'register' && (
              <>
                {/* Celular */}
                <View style={styles.inputGroup}>
                  <Text style={styles.fieldLabel}>Celular en Colombia (10 dígitos) *</Text>
                  <TextInput
                    style={styles.input}
                    placeholder="300 123 4567"
                    keyboardType="phone-pad"
                    value={phone}
                    onChangeText={setPhone}
                  />
                </View>

                {/* Zona en Medellín */}
                <View style={styles.inputGroup}>
                  <Text style={styles.fieldLabel}>Comuna / Zona de Cobertura *</Text>
                  <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.zonesScroll}>
                    {ZONES.map(z => (
                      <TouchableOpacity
                        key={z}
                        style={[styles.zoneChip, zone === z && styles.zoneChipActive]}
                        onPress={() => setZone(z)}
                      >
                        <Text style={[styles.zoneChipText, zone === z && styles.zoneChipTextActive]}>{z}</Text>
                      </TouchableOpacity>
                    ))}
                  </ScrollView>
                </View>

                {/* Dirección */}
                <View style={styles.inputGroup}>
                  <Text style={styles.fieldLabel}>Dirección / Barrio *</Text>
                  <TextInput
                    style={styles.input}
                    placeholder="Ej. Calle 10 # 43E-20, Apt 502"
                    value={address}
                    onChangeText={setAddress}
                  />
                </View>

                {/* Campos para Técnico Profesional (HU02) */}
                {role === 'professional' && (
                  <View style={styles.proSection}>
                    <View style={styles.proHeader}>
                      <Ionicons name="document-lock" size={16} color={THEME.colors.primary} />
                      <Text style={styles.proSectionTitle}>Expediente y Documentación Técnica</Text>
                    </View>

                    <Text style={styles.fieldLabel}>Especialidad Principal *</Text>
                    <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.zonesScroll}>
                      {SPECIALTIES.map(sp => (
                        <TouchableOpacity
                          key={sp}
                          style={[styles.zoneChip, specialtyName === sp && styles.zoneChipActive]}
                          onPress={() => setSpecialtyName(sp)}
                        >
                          <Text style={[styles.zoneChipText, specialtyName === sp && styles.zoneChipTextActive]}>{sp}</Text>
                        </TouchableOpacity>
                      ))}
                    </ScrollView>

                    <View style={styles.inputGroup}>
                      <Text style={styles.fieldLabel}>Número de Cédula de Ciudadanía (C.C.) *</Text>
                      <TextInput
                        style={styles.input}
                        placeholder="Ej. 71.394.205 (para cruce con Policía)"
                        keyboardType="numeric"
                        value={ccNumber}
                        onChangeText={setCcNumber}
                      />
                    </View>

                    <View style={styles.inputGroup}>
                      <Text style={styles.fieldLabel}>Matrícula Técnica / Tarjeta CONTE (Opcional)</Text>
                      <TextInput
                        style={styles.input}
                        placeholder="Ej. CONTE TE1-49201 o Certificación SENA"
                        value={conteReg}
                        onChangeText={setConteReg}
                      />
                    </View>

                    <View style={styles.inputGroup}>
                      <Text style={styles.fieldLabel}>Resumen de Experiencia Profesional</Text>
                      <TextInput
                        style={[styles.input, { height: 60 }]}
                        placeholder="Años de experiencia, marcas y especialidades..."
                        value={bio}
                        onChangeText={setBio}
                        multiline
                      />
                    </View>
                  </View>
                )}
              </>
            )}

            {/* Contraseña */}
            <View style={styles.inputGroup}>
              <View style={styles.passHeader}>
                <Text style={styles.fieldLabel}>Contraseña *</Text>
                {mode === 'login' && (
                  <TouchableOpacity onPress={handleForgotPassword}>
                    <Text style={styles.forgotText}>¿Olvidaste tu contraseña?</Text>
                  </TouchableOpacity>
                )}
              </View>

              <View style={styles.passwordWrapper}>
                <TextInput
                  style={styles.passwordInput}
                  placeholder="Mínimo 8 caracteres, mayúscula y número"
                  secureTextEntry={!showPassword}
                  value={password}
                  onChangeText={setPassword}
                />
                <TouchableOpacity
                  onPress={() => setShowPassword(!showPassword)}
                  style={styles.eyeBtn}
                >
                  <Ionicons
                    name={showPassword ? 'eye-off' : 'eye'}
                    size={18}
                    color={THEME.colors.textMuted}
                  />
                </TouchableOpacity>
              </View>

              {mode === 'register' && password.length > 0 && (
                <View style={styles.passStrengthRow}>
                  <View style={[styles.strengthItem, passValidation.hasLength && styles.strengthItemOk]}>
                    <Ionicons name={passValidation.hasLength ? 'checkmark' : 'close'} size={12} color={passValidation.hasLength ? '#15803d' : '#94a3b8'} />
                    <Text style={styles.strengthText}>8+ car.</Text>
                  </View>
                  <View style={[styles.strengthItem, passValidation.hasUpper && styles.strengthItemOk]}>
                    <Ionicons name={passValidation.hasUpper ? 'checkmark' : 'close'} size={12} color={passValidation.hasUpper ? '#15803d' : '#94a3b8'} />
                    <Text style={styles.strengthText}>Mayúscula</Text>
                  </View>
                  <View style={[styles.strengthItem, passValidation.hasNumber && styles.strengthItemOk]}>
                    <Ionicons name={passValidation.hasNumber ? 'checkmark' : 'close'} size={12} color={passValidation.hasNumber ? '#15803d' : '#94a3b8'} />
                    <Text style={styles.strengthText}>Número</Text>
                  </View>
                </View>
              )}
            </View>

            {/* Botón de Acción Principal */}
            <TouchableOpacity
              style={styles.actionBtn}
              onPress={mode === 'login' ? handleLogin : handleRegister}
              disabled={loading}
              activeOpacity={0.85}
            >
              {loading ? (
                <ActivityIndicator color="#ffffff" />
              ) : (
                <Text style={styles.actionBtnText}>
                  {mode === 'login' ? 'Entrar a Mi Cuenta' : 'Completar Registro'}
                </Text>
              )}
            </TouchableOpacity>
          </ScrollView>
        </View>
      </KeyboardAvoidingView>
    </Modal>
  );
}

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'flex-end'
  },
  sheet: {
    backgroundColor: '#ffffff',
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    maxHeight: '94%',
    paddingBottom: 24
  },
  handle: {
    width: 40,
    height: 4,
    backgroundColor: '#cbd5e1',
    borderRadius: 2,
    alignSelf: 'center',
    marginTop: 10
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingVertical: 14,
    borderBottomWidth: 1,
    borderBottomColor: THEME.colors.border
  },
  title: {
    fontSize: 15,
    fontWeight: '800',
    color: THEME.colors.textMain
  },
  subtitle: {
    fontSize: 11,
    color: THEME.colors.textMuted,
    marginTop: 2
  },
  closeBtn: {
    padding: 6
  },
  scrollContent: {
    padding: 20,
    gap: 12
  },
  tabToggle: {
    flexDirection: 'row',
    backgroundColor: '#f1f5f9',
    borderRadius: 12,
    padding: 4
  },
  tabBtn: {
    flex: 1,
    paddingVertical: 9,
    alignItems: 'center',
    borderRadius: 9
  },
  tabBtnActive: {
    backgroundColor: '#ffffff',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05,
    elevation: 1
  },
  tabBtnText: {
    fontSize: 12,
    fontWeight: '600',
    color: THEME.colors.textMuted
  },
  tabBtnTextActive: {
    color: THEME.colors.textMain,
    fontWeight: '700'
  },
  demoSection: {
    backgroundColor: '#f8fafc',
    borderRadius: 12,
    padding: 10,
    borderWidth: 1,
    borderColor: THEME.colors.border,
    gap: 6
  },
  demoLabel: {
    fontSize: 10.5,
    fontWeight: '700',
    color: THEME.colors.textMuted,
    textTransform: 'uppercase'
  },
  demoGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 6
  },
  demoChip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
    backgroundColor: '#ffffff',
    borderWidth: 1,
    borderColor: THEME.colors.border,
    paddingHorizontal: 8,
    paddingVertical: 6,
    borderRadius: 8
  },
  demoChipText: {
    fontSize: 11,
    fontWeight: '700',
    color: THEME.colors.textMain
  },
  roleSelectorRow: {
    flexDirection: 'row',
    gap: 10
  },
  roleBox: {
    flex: 1,
    padding: 12,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: THEME.colors.border,
    backgroundColor: '#f8fafc',
    alignItems: 'center'
  },
  roleBoxActive: {
    borderColor: THEME.colors.primary,
    backgroundColor: THEME.colors.primaryLight
  },
  roleBoxText: {
    fontSize: 12,
    fontWeight: '700',
    color: THEME.colors.textMuted,
    marginTop: 4
  },
  roleBoxTextActive: {
    color: THEME.colors.primary
  },
  roleBoxSub: {
    fontSize: 10,
    color: THEME.colors.textLight,
    textAlign: 'center',
    marginTop: 2
  },
  inputGroup: {
    gap: 4
  },
  fieldLabel: {
    fontSize: 11.5,
    fontWeight: '700',
    color: THEME.colors.textMain
  },
  input: {
    height: 42,
    borderWidth: 1,
    borderColor: THEME.colors.border,
    borderRadius: 12,
    paddingHorizontal: 12,
    fontSize: 12.5,
    backgroundColor: '#f8fafc',
    color: THEME.colors.textMain
  },
  passHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center'
  },
  forgotText: {
    fontSize: 11,
    color: THEME.colors.primary,
    fontWeight: '600'
  },
  passwordWrapper: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: THEME.colors.border,
    borderRadius: 12,
    backgroundColor: '#f8fafc'
  },
  passwordInput: {
    flex: 1,
    height: 42,
    paddingHorizontal: 12,
    fontSize: 12.5,
    color: THEME.colors.textMain
  },
  eyeBtn: {
    paddingHorizontal: 12
  },
  passStrengthRow: {
    flexDirection: 'row',
    gap: 8,
    marginTop: 4
  },
  strengthItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 3,
    backgroundColor: '#f1f5f9',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 6
  },
  strengthItemOk: {
    backgroundColor: '#dcfce7'
  },
  strengthText: {
    fontSize: 10,
    color: THEME.colors.textMuted
  },
  zonesScroll: {
    flexDirection: 'row',
    gap: 6,
    paddingVertical: 4
  },
  zoneChip: {
    backgroundColor: '#f1f5f9',
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: 'transparent'
  },
  zoneChipActive: {
    backgroundColor: THEME.colors.primaryLight,
    borderColor: THEME.colors.primary
  },
  zoneChipText: {
    fontSize: 11,
    color: THEME.colors.textMuted,
    fontWeight: '600'
  },
  zoneChipTextActive: {
    color: THEME.colors.primary,
    fontWeight: '700'
  },
  proSection: {
    backgroundColor: '#f0fdf4',
    padding: 12,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#bbf7d0',
    gap: 8
  },
  proHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6
  },
  proSectionTitle: {
    fontSize: 12,
    fontWeight: '800',
    color: THEME.colors.primary
  },
  actionBtn: {
    backgroundColor: THEME.colors.primary,
    paddingVertical: 13,
    borderRadius: 14,
    alignItems: 'center',
    marginTop: 8
  },
  actionBtnText: {
    fontSize: 13.5,
    fontWeight: '700',
    color: '#ffffff'
  },
  supabaseBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    paddingVertical: 10,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: THEME.colors.border,
    backgroundColor: '#f8fafc'
  },
  supabaseBtnText: {
    fontSize: 11.5,
    fontWeight: '600',
    color: THEME.colors.primary
  }
});
