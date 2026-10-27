import React from 'react';
import { View, Text, StyleSheet, Image, TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { THEME } from '../constants/theme';

export default function AdminAuditCard({ professional, onApprove, onReject, onInspect }) {
  const isVerified = professional.status === 'verified';
  const isRejected = professional.status === 'rejected';

  return (
    <View style={[styles.card, isVerified && styles.cardVerified, isRejected && styles.cardRejected]}>
      <View style={styles.cardHeader}>
        <Image source={{ uri: professional.avatar }} style={styles.avatar} />
        <View style={{ flex: 1 }}>
          <View style={styles.titleRow}>
            <Text style={styles.name}>{professional.name}</Text>
            <View style={[
              styles.badge,
              isVerified ? styles.badgeVerified : isRejected ? styles.badgeRejected : styles.badgePending
            ]}>
              <Ionicons
                name={isVerified ? 'checkmark-circle' : isRejected ? 'close-circle' : 'time'}
                size={11}
                color={isVerified ? '#15803d' : isRejected ? '#991b1b' : THEME.colors.pending}
              />
              <Text style={[
                styles.badgeText,
                isVerified ? styles.badgeTextVerified : isRejected ? styles.badgeTextRejected : styles.badgeTextPending
              ]}>
                {isVerified ? 'Aprobado' : isRejected ? 'Rechazado' : 'Pendiente de Revisión'}
              </Text>
            </View>
          </View>
          <Text style={styles.specialty}>{professional.specialtyName}</Text>
          <Text style={styles.zone}>{professional.zone}, Medellín</Text>
        </View>
      </View>

      {/* Resumen de Documentos Radicados */}
      <View style={styles.docBox}>
        <View style={styles.docRow}>
          <View style={styles.docItemLeft}>
            <Ionicons name="id-card" size={13} color={THEME.colors.primary} />
            <Text style={styles.docLabel}>Cédula Ciudadanía:</Text>
          </View>
          <Text style={styles.docVal}>{professional.ccNumber || '1.036.711.592'}</Text>
        </View>

        <View style={styles.docRow}>
          <View style={styles.docItemLeft}>
            <Ionicons name="shield-checkmark" size={13} color="#15803d" />
            <Text style={styles.docLabel}>Antecedentes PONAL:</Text>
          </View>
          <Text style={[styles.docVal, { color: '#15803d' }]}>Sin Anotaciones Penales</Text>
        </View>

        <View style={styles.docRow}>
          <View style={styles.docItemLeft}>
            <Ionicons name="school" size={13} color="#0284c7" />
            <Text style={styles.docLabel}>Matrícula Técnica:</Text>
          </View>
          <Text style={styles.docVal}>{professional.conteReg || 'Acreditación SENA Radicada'}</Text>
        </View>
      </View>

      {/* Botón para examinar expediente completo */}
      <TouchableOpacity
        style={styles.inspectBtn}
        onPress={() => onInspect && onInspect(professional)}
        activeOpacity={0.8}
      >
        <Ionicons name="document-text-outline" size={15} color={THEME.colors.primary} />
        <Text style={styles.inspectText}>Examinar Expediente Completo y Fotos</Text>
      </TouchableOpacity>

      {/* Acciones de Auditoría */}
      {!isVerified && (
        <View style={styles.actionsRow}>
          <TouchableOpacity
            style={styles.approveBtn}
            onPress={() => onApprove(professional.id)}
            activeOpacity={0.85}
          >
            <Ionicons name="checkmark-circle" size={16} color="#ffffff" />
            <Text style={styles.approveText}>Aprobar Técnico</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.rejectBtn}
            onPress={() => onReject(professional.id)}
            activeOpacity={0.85}
          >
            <Ionicons name="close-circle" size={16} color="#ba1a1a" />
            <Text style={styles.rejectText}>Rechazar</Text>
          </TouchableOpacity>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: '#ffffff',
    borderRadius: 16,
    padding: 14,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: '#fde68a',
    backgroundColor: '#fffdf5'
  },
  cardVerified: {
    borderColor: '#bbf7d0',
    backgroundColor: '#f0fdf4'
  },
  cardRejected: {
    borderColor: '#fecdd3',
    backgroundColor: '#fff1f2'
  },
  cardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12
  },
  avatar: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: '#e2e8f0'
  },
  titleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between'
  },
  name: {
    fontSize: 14,
    fontWeight: '800',
    color: THEME.colors.textMain
  },
  badge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    paddingVertical: 2,
    paddingHorizontal: 8,
    borderRadius: 10
  },
  badgePending: {
    backgroundColor: '#fef3c7'
  },
  badgeVerified: {
    backgroundColor: '#dcfce7'
  },
  badgeRejected: {
    backgroundColor: '#fee2e2'
  },
  badgeText: {
    fontSize: 10,
    fontWeight: '700'
  },
  badgeTextPending: {
    color: '#b45309'
  },
  badgeTextVerified: {
    color: '#15803d'
  },
  badgeTextRejected: {
    color: '#991b1b'
  },
  specialty: {
    fontSize: 12,
    color: THEME.colors.primary,
    fontWeight: '600',
    marginTop: 1
  },
  zone: {
    fontSize: 11,
    color: THEME.colors.textMuted
  },
  docBox: {
    backgroundColor: '#ffffff',
    borderRadius: 12,
    padding: 10,
    marginVertical: 10,
    borderWidth: 1,
    borderColor: THEME.colors.border,
    gap: 6
  },
  docRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center'
  },
  docItemLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6
  },
  docLabel: {
    fontSize: 11.5,
    color: THEME.colors.textMuted
  },
  docVal: {
    fontSize: 11.5,
    fontWeight: '700',
    color: THEME.colors.textMain
  },
  inspectBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    paddingVertical: 8,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: THEME.colors.primary,
    backgroundColor: '#ffffff',
    marginBottom: 8
  },
  inspectText: {
    fontSize: 11.5,
    fontWeight: '700',
    color: THEME.colors.primary
  },
  actionsRow: {
    flexDirection: 'row',
    gap: 10
  },
  approveBtn: {
    flex: 2,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    backgroundColor: '#15803d',
    paddingVertical: 10,
    borderRadius: 10
  },
  approveText: {
    fontSize: 12,
    fontWeight: '700',
    color: '#ffffff'
  },
  rejectBtn: {
    flex: 1,
    paddingHorizontal: 12,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 4,
    backgroundColor: '#ffdad6',
    paddingVertical: 10,
    borderRadius: 10
  },
  rejectText: {
    fontSize: 12,
    fontWeight: '700',
    color: '#93000a'
  }
});
