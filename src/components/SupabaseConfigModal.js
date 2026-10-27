import React, { useState, useEffect } from 'react';
import {
  Modal,
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  TextInput,
  ScrollView,
  Alert,
  ActivityIndicator
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { THEME } from '../constants/theme';
import {
  SUPABASE_PROJECT_REF,
  SUPABASE_URL,
  testSupabaseConnection,
  saveSupabaseAnonKey,
  getActiveAnonKey
} from '../services/supabase';

export default function SupabaseConfigModal({ visible, onClose, onConnectionChanged }) {
  const [anonKey, setAnonKey] = useState('');
  const [testing, setTesting] = useState(false);
  const [connStatus, setConnStatus] = useState(null);

  useEffect(() => {
    if (visible) {
      const current = getActiveAnonKey();
      setAnonKey(current || '');
      handleTest(current);
    }
  }, [visible]);

  const handleTest = async (keyToTest) => {
    setTesting(true);
    const res = await testSupabaseConnection(keyToTest !== undefined ? keyToTest : anonKey);
    setTesting(false);
    setConnStatus(res);
  };

  const handleSave = async () => {
    if (!anonKey.trim()) {
      Alert.alert('Clave requerida', 'Por favor ingresa o pega la clave pública anónima de Supabase.');
      return;
    }

    setTesting(true);
    const testResult = await saveSupabaseAnonKey(anonKey.trim());
    setTesting(false);
    setConnStatus(testResult);

    if (testResult.connected) {
      Alert.alert('¡Supabase Conectado!', 'La clave fue validada y guardada con éxito.');
      onConnectionChanged && onConnectionChanged(true);
      onClose();
    } else {
      Alert.alert('Atención', testResult.message);
    }
  };

  return (
    <Modal visible={visible} animationType="slide" transparent>
      <View style={styles.overlay}>
        <View style={styles.sheet}>
          <View style={styles.handle} />

          <View style={styles.header}>
            <View style={styles.titleRow}>
              <Ionicons name="server" size={18} color={THEME.colors.primary} />
              <Text style={styles.title}>Conexión Supabase (PostgreSQL)</Text>
            </View>
            <TouchableOpacity onPress={onClose} style={styles.closeBtn}>
              <Ionicons name="close" size={20} color={THEME.colors.textMuted} />
            </TouchableOpacity>
          </View>

          <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.content}>
            {/* Estado Actual */}
            <View style={[
              styles.statusBanner,
              connStatus?.connected ? styles.statusBannerSuccess : styles.statusBannerWarning
            ]}>
              <Ionicons
                name={connStatus?.connected ? 'checkmark-circle' : 'alert-circle'}
                size={22}
                color={connStatus?.connected ? '#15803d' : '#b45309'}
              />
              <View style={{ flex: 1 }}>
                <Text style={[
                  styles.statusTitle,
                  connStatus?.connected ? styles.statusTitleSuccess : styles.statusTitleWarning
                ]}>
                  {connStatus?.connected
                    ? '🟢 Conectado en Tiempo Real'
                    : '🟡 Esperando Clave Anon de Supabase'}
                </Text>
                <Text style={styles.statusDesc}>
                  {connStatus?.message || 'Verificando comunicación con el servidor...'}
                </Text>
              </View>
            </View>

            {/* Parámetros del Proyecto */}
            <View style={styles.infoBox}>
              <View style={styles.infoRow}>
                <Text style={styles.infoLabel}>Proyecto Supabase:</Text>
                <Text style={styles.infoVal}>{SUPABASE_PROJECT_REF}</Text>
              </View>
              <View style={styles.infoRow}>
                <Text style={styles.infoLabel}>Endpoint URL:</Text>
                <Text style={styles.infoVal} numberOfLines={1}>{SUPABASE_URL}</Text>
              </View>
              {connStatus?.latencyMs && (
                <View style={styles.infoRow}>
                  <Text style={styles.infoLabel}>Latencia de Red:</Text>
                  <Text style={[styles.infoVal, { color: '#15803d' }]}>{connStatus.latencyMs} ms</Text>
                </View>
              )}
            </View>

            {/* Formulario de Clave Anónima */}
            <View style={styles.inputGroup}>
              <Text style={styles.fieldLabel}>Clave Pública Anónima (Anon Public Key) *</Text>
              <Text style={styles.fieldHint}>
                Copia la clave desde tu consola Supabase en:{"\n"}
                <Text style={{ fontWeight: '700' }}>Project Settings → API → Project API Keys → anon public</Text>
              </Text>
              <TextInput
                style={styles.keyInput}
                placeholder="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                value={anonKey}
                onChangeText={setAnonKey}
                multiline
                numberOfLines={3}
                autoCapitalize="none"
                autoCorrect={false}
              />
            </View>

            {/* Botones de Acción */}
            <View style={styles.actionButtons}>
              <TouchableOpacity
                style={styles.testBtn}
                onPress={() => handleTest()}
                disabled={testing}
                activeOpacity={0.8}
              >
                {testing ? (
                  <ActivityIndicator size="small" color={THEME.colors.primary} />
                ) : (
                  <>
                    <Ionicons name="pulse" size={16} color={THEME.colors.primary} />
                    <Text style={styles.testBtnText}>Probar Conexión</Text>
                  </>
                )}
              </TouchableOpacity>

              <TouchableOpacity
                style={styles.saveBtn}
                onPress={handleSave}
                disabled={testing}
                activeOpacity={0.85}
              >
                <Ionicons name="save" size={16} color="#ffffff" />
                <Text style={styles.saveBtnText}>Guardar y Conectar</Text>
              </TouchableOpacity>
            </View>

            {/* Explicación didáctica para sustentación */}
            <View style={styles.academicNote}>
              <Ionicons name="information-circle" size={18} color="#0369a1" />
              <Text style={styles.academicText}>
                <Text style={{ fontWeight: '700' }}>Nota de Arquitectura:</Text> Esta clave es pública y segura para apps móviles según el modelo RLS (Row Level Security) de PostgreSQL en Supabase.
              </Text>
            </View>
          </ScrollView>
        </View>
      </View>
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
    maxHeight: '90%',
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
  titleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8
  },
  title: {
    fontSize: 14.5,
    fontWeight: '800',
    color: THEME.colors.textMain
  },
  closeBtn: {
    padding: 6
  },
  content: {
    padding: 20,
    gap: 14
  },
  statusBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    padding: 12,
    borderRadius: 12,
    borderWidth: 1
  },
  statusBannerSuccess: {
    backgroundColor: '#f0fdf4',
    borderColor: '#86efac'
  },
  statusBannerWarning: {
    backgroundColor: '#fffbeb',
    borderColor: '#fde68a'
  },
  statusTitle: {
    fontSize: 12.5,
    fontWeight: '800'
  },
  statusTitleSuccess: {
    color: '#15803d'
  },
  statusTitleWarning: {
    color: '#b45309'
  },
  statusDesc: {
    fontSize: 11,
    color: THEME.colors.textMuted,
    marginTop: 2
  },
  infoBox: {
    backgroundColor: '#f8fafc',
    borderRadius: 12,
    padding: 12,
    borderWidth: 1,
    borderColor: THEME.colors.border,
    gap: 6
  },
  infoRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center'
  },
  infoLabel: {
    fontSize: 11.5,
    color: THEME.colors.textMuted
  },
  infoVal: {
    fontSize: 11.5,
    fontWeight: '700',
    color: THEME.colors.textMain
  },
  inputGroup: {
    gap: 6
  },
  fieldLabel: {
    fontSize: 12,
    fontWeight: '700',
    color: THEME.colors.textMain
  },
  fieldHint: {
    fontSize: 11,
    color: THEME.colors.textMuted,
    lineHeight: 15
  },
  keyInput: {
    borderWidth: 1,
    borderColor: THEME.colors.border,
    borderRadius: 12,
    padding: 10,
    fontSize: 11.5,
    backgroundColor: '#f8fafc',
    fontFamily: 'monospace',
    minHeight: 70,
    textAlignVertical: 'top'
  },
  actionButtons: {
    flexDirection: 'row',
    gap: 10,
    marginTop: 6
  },
  testBtn: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    borderWidth: 1,
    borderColor: THEME.colors.primary,
    backgroundColor: '#f0fdf4',
    paddingVertical: 12,
    borderRadius: 12
  },
  testBtnText: {
    fontSize: 12.5,
    fontWeight: '700',
    color: THEME.colors.primary
  },
  saveBtn: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    backgroundColor: THEME.colors.primary,
    paddingVertical: 12,
    borderRadius: 12
  },
  saveBtnText: {
    fontSize: 12.5,
    fontWeight: '700',
    color: '#ffffff'
  },
  academicNote: {
    flexDirection: 'row',
    gap: 8,
    backgroundColor: '#f0f9ff',
    padding: 10,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: '#bae6fd'
  },
  academicText: {
    fontSize: 11,
    color: '#0369a1',
    flex: 1,
    lineHeight: 15
  }
});
