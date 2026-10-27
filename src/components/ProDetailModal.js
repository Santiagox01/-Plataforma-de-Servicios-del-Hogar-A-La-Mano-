import React from 'react';
import { Modal, View, Text, StyleSheet, TouchableOpacity, ScrollView, Image } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { THEME } from '../constants/theme';

export default function ProDetailModal({ visible, professional, onClose, onOpenBooking }) {
  if (!professional) return null;

  return (
    <Modal visible={visible} animationType="slide" transparent>
      <View style={styles.overlay}>
        <View style={styles.sheet}>
          <View style={styles.handle} />

          <View style={styles.header}>
            <Text style={styles.title}>Hoja de Vida Técnica</Text>
            <TouchableOpacity onPress={onClose} style={styles.closeBtn}>
              <Ionicons name="close" size={20} color={THEME.colors.textMuted} />
            </TouchableOpacity>
          </View>

          <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.scrollContent}>
            {/* Top Pro Banner */}
            <View style={styles.proRow}>
              <Image source={{ uri: professional.avatar }} style={styles.avatar} />
              <View style={{ flex: 1 }}>
                <Text style={styles.name}>{professional.name}</Text>
                <Text style={styles.specialty}>{professional.specialtyName}</Text>
                <Text style={styles.location}>
                  <Ionicons name="location" size={12} color={THEME.colors.primary} /> {professional.zone}, Medellín
                </Text>
              </View>
            </View>

            {/* Acreditación Oficial CONTE / EPM */}
            <View style={styles.credentialCard}>
              <View style={styles.credRow}>
                <Ionicons name="shield-checkmark" size={20} color={THEME.colors.verified} />
                <View>
                  <Text style={styles.credTitle}>
                    {professional.conteReg ? professional.conteReg : 'Identidad Verificada C.C.'}
                  </Text>
                  <Text style={styles.credSub}>Cédula: {professional.ccNumber} • Antecedentes Policiales Limpios</Text>
                </View>
              </View>
            </View>

            {/* Biografía Técnica */}
            <View style={styles.section}>
              <Text style={styles.sectionTitle}>Experiencia y Perfil</Text>
              <Text style={styles.bioText}>{professional.bio}</Text>
            </View>

            {/* Métricas */}
            <View style={styles.metricsRow}>
              <View style={styles.metricBox}>
                <Text style={styles.metricVal}>{professional.rating.toFixed(1)} ★</Text>
                <Text style={styles.metricLabel}>{professional.reviewCount} clientes satisfechos</Text>
              </View>

              <View style={styles.metricBox}>
                <Text style={styles.metricVal}>${professional.rate.toLocaleString('es-CO')}</Text>
                <Text style={styles.metricLabel}>Tarifa base por hora</Text>
              </View>
            </View>

            {/* Botón Solicitar */}
            <TouchableOpacity
              style={styles.hireBtn}
              onPress={() => {
                onClose();
                onOpenBooking(professional);
              }}
              activeOpacity={0.85}
            >
              <Ionicons name="calendar" size={16} color="#ffffff" />
              <Text style={styles.hireBtnText}>Solicitar Servicio a Este Profesional</Text>
            </TouchableOpacity>
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
    maxHeight: '85%',
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
    fontWeight: '700',
    color: THEME.colors.textMain
  },
  closeBtn: {
    padding: 6
  },
  scrollContent: {
    padding: 20,
    gap: 14
  },
  proRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12
  },
  avatar: {
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: '#e2e8f0'
  },
  name: {
    fontSize: 16,
    fontWeight: '800',
    color: THEME.colors.textMain
  },
  specialty: {
    fontSize: 13,
    color: THEME.colors.primary,
    fontWeight: '600'
  },
  location: {
    fontSize: 11.5,
    color: THEME.colors.textMuted,
    marginTop: 2
  },
  credentialCard: {
    backgroundColor: THEME.colors.verifiedBg,
    borderWidth: 1,
    borderColor: '#a7f3d0',
    padding: 12,
    borderRadius: 12
  },
  credRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10
  },
  credTitle: {
    fontSize: 12.5,
    fontWeight: '800',
    color: '#065f46'
  },
  credSub: {
    fontSize: 10.5,
    color: '#047857',
    marginTop: 1
  },
  section: {
    gap: 4
  },
  sectionTitle: {
    fontSize: 12,
    fontWeight: '700',
    color: THEME.colors.textMain,
    textTransform: 'uppercase',
    letterSpacing: 0.5
  },
  bioText: {
    fontSize: 12.5,
    lineHeight: 18,
    color: THEME.colors.textMuted
  },
  metricsRow: {
    flexDirection: 'row',
    gap: 10
  },
  metricBox: {
    flex: 1,
    backgroundColor: '#f8fafc',
    padding: 12,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: THEME.colors.border,
    alignItems: 'center'
  },
  metricVal: {
    fontSize: 15,
    fontWeight: '800',
    color: THEME.colors.textMain
  },
  metricLabel: {
    fontSize: 10,
    color: THEME.colors.textLight,
    marginTop: 2,
    textAlign: 'center'
  },
  hireBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    backgroundColor: THEME.colors.primary,
    paddingVertical: 14,
    borderRadius: 14,
    marginTop: 6
  },
  hireBtnText: {
    fontSize: 13.5,
    fontWeight: '700',
    color: '#ffffff'
  }
});
