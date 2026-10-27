import React from 'react';
import { View, Text, StyleSheet, Image, TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { THEME } from '../constants/theme';

export default function ProfessionalCard({ professional, onOpenDetail, onOpenBooking, onSelect, onBook }) {
  const isVerified = professional.status === 'verified';
  const handleDetail = () => (onOpenDetail || onSelect)?.(professional);
  const handleBook = () => (onOpenBooking || onBook)?.(professional);

  return (
    <View style={styles.card}>
      <View style={styles.cardHeader}>
        <Image source={{ uri: professional.avatar }} style={styles.avatar} />
        <View style={styles.headerInfo}>
          <Text style={styles.name}>{professional.name}</Text>
          <Text style={styles.specialty}>{professional.specialtyName}</Text>
          <View style={styles.ratingRow}>
            <Ionicons name="star" size={14} color="#f59e0b" />
            <Text style={styles.ratingText}>{professional.rating.toFixed(1)}</Text>
            <Text style={styles.reviewCount}>({professional.reviewCount} reseñas)</Text>
          </View>
        </View>

        <View style={[styles.statusBadge, isVerified ? styles.statusBadgeVerified : styles.statusBadgePending]}>
          <View style={[styles.statusDot, { backgroundColor: isVerified ? THEME.colors.verified : THEME.colors.pending }]} />
          <Text style={[styles.statusText, { color: isVerified ? THEME.colors.verified : THEME.colors.pending }]}>
            {isVerified ? 'Verificado' : 'Pendiente'}
          </Text>
        </View>
      </View>

      <View style={styles.chipsRow}>
        <View style={styles.chip}>
          <Ionicons name="location" size={12} color={THEME.colors.primary} />
          <Text style={styles.chipText}>{professional.distanceKm} km • {professional.zone}</Text>
        </View>
        <View style={styles.urgentChip}>
          <Ionicons name="flash" size={12} color={THEME.colors.urgent} />
          <Text style={styles.urgentChipText}>Atiende Urgencias</Text>
        </View>
      </View>

      <View style={styles.footerRow}>
        <View>
          <Text style={styles.rateLabel}>Tarifa base desde</Text>
          <Text style={styles.rateAmount}>
            ${professional.rate.toLocaleString('es-CO')}
            <Text style={styles.rateUnit}> / hora</Text>
          </Text>
        </View>

        <View style={styles.actionsRow}>
          <TouchableOpacity style={styles.btnSecondary} onPress={handleDetail}>
            <Text style={styles.btnSecondaryText}>Ver Perfil</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.btnPrimary} onPress={handleBook}>
            <Text style={styles.btnPrimaryText}>Solicitar</Text>
          </TouchableOpacity>
        </View>
      </View>
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
    borderColor: THEME.colors.border,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.05,
    shadowRadius: 6,
    elevation: 2
  },
  cardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12
  },
  avatar: {
    width: 52,
    height: 52,
    borderRadius: 26,
    backgroundColor: '#e2e8f0'
  },
  headerInfo: {
    flex: 1
  },
  name: {
    fontSize: 14.5,
    fontWeight: '700',
    color: THEME.colors.textMain
  },
  specialty: {
    fontSize: 12,
    color: THEME.colors.textMuted,
    marginTop: 1
  },
  ratingRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    marginTop: 3
  },
  ratingText: {
    fontSize: 12,
    fontWeight: '700',
    color: THEME.colors.textMain
  },
  reviewCount: {
    fontSize: 11,
    color: THEME.colors.textLight
  },
  statusBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    paddingVertical: 3,
    paddingHorizontal: 8,
    borderRadius: 12
  },
  statusBadgeVerified: {
    backgroundColor: THEME.colors.verifiedBg
  },
  statusBadgePending: {
    backgroundColor: THEME.colors.pendingBg
  },
  statusDot: {
    width: 6,
    height: 6,
    borderRadius: 3
  },
  statusText: {
    fontSize: 10.5,
    fontWeight: '700'
  },
  chipsRow: {
    flexDirection: 'row',
    gap: 8,
    marginTop: 10,
    paddingTop: 8,
    borderTopWidth: 1,
    borderTopColor: '#f1f5f9'
  },
  chip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    backgroundColor: '#f8fafc',
    paddingVertical: 4,
    paddingHorizontal: 8,
    borderRadius: 8
  },
  chipText: {
    fontSize: 11,
    color: THEME.colors.textMuted,
    fontWeight: '500'
  },
  urgentChip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 3,
    backgroundColor: THEME.colors.urgentBg,
    paddingVertical: 4,
    paddingHorizontal: 8,
    borderRadius: 8
  },
  urgentChipText: {
    fontSize: 11,
    color: THEME.colors.urgent,
    fontWeight: '700'
  },
  footerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: 12,
    paddingTop: 10,
    borderTopWidth: 1,
    borderTopColor: '#f1f5f9'
  },
  rateLabel: {
    fontSize: 10,
    color: THEME.colors.textLight,
    fontWeight: '600',
    textTransform: 'uppercase'
  },
  rateAmount: {
    fontSize: 16,
    fontWeight: '800',
    color: THEME.colors.textMain
  },
  rateUnit: {
    fontSize: 11,
    fontWeight: '400',
    color: THEME.colors.textMuted
  },
  actionsRow: {
    flexDirection: 'row',
    gap: 8
  },
  btnSecondary: {
    paddingVertical: 7,
    paddingHorizontal: 12,
    backgroundColor: '#f1f5f9',
    borderRadius: 10
  },
  btnSecondaryText: {
    fontSize: 12,
    fontWeight: '600',
    color: THEME.colors.textMain
  },
  btnPrimary: {
    paddingVertical: 7,
    paddingHorizontal: 14,
    backgroundColor: THEME.colors.primary,
    borderRadius: 10
  },
  btnPrimaryText: {
    fontSize: 12,
    fontWeight: '700',
    color: '#ffffff'
  }
});
