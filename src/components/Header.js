import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { THEME } from '../constants/theme';

export default function Header({ currentUser, onPressProfile }) {
  const role = currentUser?.role || 'client';
  const firstName = currentUser?.name ? currentUser.name.split(' ')[0] : 'Usuario';
  const roleLabel = role === 'admin' ? `Admin • ${firstName}` : role === 'professional' ? `Técnico • ${firstName}` : `Cliente • ${firstName}`;
  const roleIcon = role === 'admin' ? 'shield-checkmark' : role === 'professional' ? 'construct' : 'person';

  return (
    <View style={styles.safeArea}>
      <View style={styles.container}>
        <View style={styles.topRow}>
          <View style={styles.brandRow}>
            <View style={styles.logoIcon}>
              <Ionicons name="build" size={16} color="#ffffff" />
            </View>
            <View>
              <Text style={styles.appName}>A La Mano</Text>
              <Text style={styles.cityName}>Medellín • Valle de Aburrá</Text>
            </View>
          </View>

          <TouchableOpacity
            style={[styles.roleChip, role === 'admin' && styles.roleChipAdmin]}
            onPress={onPressProfile}
            activeOpacity={0.8}
          >
            <Ionicons
              name={roleIcon}
              size={13}
              color={role === 'admin' ? '#ffffff' : THEME.colors.primary}
            />
            <Text
              style={[
                styles.roleText,
                role === 'admin' && styles.roleTextAdmin
              ]}
              numberOfLines={1}
            >
              {roleLabel}
            </Text>
          </TouchableOpacity>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    backgroundColor: '#ffffff'
  },
  container: {
    paddingHorizontal: 16,
    paddingTop: 10,
    paddingBottom: 10,
    borderBottomWidth: 1,
    borderBottomColor: THEME.colors.border,
    backgroundColor: '#ffffff'
  },
  topRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between'
  },
  brandRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10
  },
  logoIcon: {
    width: 34,
    height: 34,
    borderRadius: 10,
    backgroundColor: THEME.colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
    shadowColor: THEME.colors.primary,
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 4,
    elevation: 3
  },
  appName: {
    fontSize: 16,
    fontWeight: '800',
    color: THEME.colors.textMain,
    letterSpacing: -0.3
  },
  cityName: {
    fontSize: 11,
    color: THEME.colors.textMuted,
    fontWeight: '500'
  },
  roleChip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 20,
    backgroundColor: THEME.colors.primaryLight,
    borderWidth: 1,
    borderColor: THEME.colors.primary
  },
  roleChipAdmin: {
    backgroundColor: '#0f172a',
    borderColor: '#334155'
  },
  roleText: {
    fontSize: 12,
    fontWeight: '700',
    color: THEME.colors.primary
  },
  roleTextAdmin: {
    color: '#ffffff'
  }
});
