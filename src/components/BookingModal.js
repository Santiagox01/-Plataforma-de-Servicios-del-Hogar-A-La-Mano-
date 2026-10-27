import React, { useState } from 'react';
import {
  Modal,
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  TextInput,
  ScrollView,
  Image,
  Alert,
  KeyboardAvoidingView,
  Platform
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { THEME } from '../constants/theme';
import { calculatePrice } from '../services/pricing';

export default function BookingModal({ visible, professional, onClose, onConfirmBooking }) {
  if (!professional) return null;

  const [description, setDescription] = useState('');
  const [address, setAddress] = useState('Calle 10 # 43E-20, Apt 502, El Poblado');
  const [isUrgent, setIsUrgent] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState('nequi'); // 'nequi' | 'bancolombia'

  const pricing = calculatePrice(professional.rate, isUrgent);

  const handleSubmit = () => {
    if (!description.trim()) {
      Alert.alert('Campo requerido', 'Por favor describe el daño o requerimiento técnico.');
      return;
    }
    if (!address.trim()) {
      Alert.alert('Campo requerido', 'Por favor confirma la dirección en Medellín.');
      return;
    }

    onConfirmBooking({
      proId: professional.id,
      proName: professional.name,
      proSpecialty: professional.specialtyName,
      description,
      address,
      isUrgent,
      paymentMethod,
      pricing
    });

    setDescription('');
    setIsUrgent(false);
  };

  return (
    <Modal visible={visible} animationType="slide" transparent onRequestClose={onClose}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        style={styles.overlay}
      >
        <View style={styles.sheet}>
          <View style={styles.handle} />

          <View style={styles.header}>
            <View style={styles.proInfoRow}>
              <Image source={{ uri: professional.avatar }} style={styles.avatar} />
              <View>
                <Text style={styles.title}>Solicitar a {professional.name}</Text>
                <Text style={styles.subtitle}>{professional.specialtyName}</Text>
              </View>
            </View>
            <TouchableOpacity onPress={onClose} style={styles.closeBtn}>
              <Ionicons name="close" size={20} color={THEME.colors.textMuted} />
            </TouchableOpacity>
          </View>

          <ScrollView
            showsVerticalScrollIndicator={false}
            contentContainerStyle={styles.scrollContent}
            keyboardShouldPersistTaps="handled"
          >
            {/* Campo Daño */}
            <View style={styles.fieldGroup}>
              <Text style={styles.label}>¿Qué daño necesitas reparar? *</Text>
              <TextInput
                style={styles.textArea}
                multiline
                numberOfLines={3}
                placeholder="Ej. Fuga debajo del lavaplatos con goteo continuo..."
                placeholderTextColor={THEME.colors.textLight}
                value={description}
                onChangeText={setDescription}
              />
            </View>

            {/* Campo Dirección */}
            <View style={styles.fieldGroup}>
              <Text style={styles.label}>Dirección en Medellín *</Text>
              <View style={styles.inputWrapper}>
                <Ionicons name="home" size={16} color={THEME.colors.textLight} style={styles.inputIcon} />
                <TextInput
                  style={styles.input}
                  placeholder="Dirección exacta en Medellín"
                  value={address}
                  onChangeText={setAddress}
                />
              </View>
            </View>

            {/* Selector de Urgencia (+30%) */}
            <TouchableOpacity
              style={[styles.urgencyCard, isUrgent && styles.urgencyCardActive]}
              onPress={() => setIsUrgent(!isUrgent)}
              activeOpacity={0.8}
            >
              <View style={styles.urgencyLeft}>
                <View style={[styles.urgencyIcon, isUrgent && styles.urgencyIconActive]}>
                  <Ionicons name="flash" size={18} color={isUrgent ? '#ffffff' : THEME.colors.urgent} />
                </View>
                <View>
                  <Text style={styles.urgencyTitle}>Servicio de Urgencia (+30%)</Text>
                  <Text style={styles.urgencySub}>Atención inmediata y llegada en &lt; 45 min</Text>
                </View>
              </View>
              <Ionicons
                name={isUrgent ? 'checkbox' : 'square-outline'}
                size={22}
                color={isUrgent ? THEME.colors.primary : THEME.colors.textLight}
              />
            </TouchableOpacity>

            {/* Desglose de Precios */}
            <View style={styles.breakdownCard}>
              <Text style={styles.breakdownHeader}>DESGLOSE DE COTIZACIÓN</Text>
              
              <View style={styles.priceRow}>
                <Text style={styles.priceLabel}>Tarifa base profesional</Text>
                <Text style={styles.priceValue}>{pricing.formattedBaseRate}</Text>
              </View>

              {isUrgent && (
                <View style={styles.priceRow}>
                  <Text style={[styles.priceLabel, styles.urgencyText]}>Recargo urgencia (+30%)</Text>
                  <Text style={[styles.priceValue, styles.urgencyText]}>{pricing.formattedUrgencySurcharge}</Text>
                </View>
              )}

              <View style={styles.priceRow}>
                <Text style={styles.priceLabel}>Garantía de Pago Seguro</Text>
                <Text style={styles.priceValue}>$3.000</Text>
              </View>

              <View style={[styles.priceRow, styles.totalRow]}>
                <Text style={styles.totalLabel}>Total a Pagar (Protegido)</Text>
                <Text style={styles.totalValue}>{pricing.formattedTotal}</Text>
              </View>
            </View>

            {/* Método de Pago */}
            <Text style={styles.label}>Método de Pago Protegido</Text>
            <View style={styles.paymentMethodsRow}>
              <TouchableOpacity
                style={[styles.payMethod, paymentMethod === 'nequi' && styles.payMethodActive]}
                onPress={() => setPaymentMethod('nequi')}
              >
                <Ionicons name="wallet" size={16} color={paymentMethod === 'nequi' ? THEME.colors.primary : THEME.colors.textMuted} />
                <Text style={[styles.payMethodText, paymentMethod === 'nequi' && styles.payMethodTextActive]}>Nequi</Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={[styles.payMethod, paymentMethod === 'bancolombia' && styles.payMethodActive]}
                onPress={() => setPaymentMethod('bancolombia')}
              >
                <Ionicons name="card" size={16} color={paymentMethod === 'bancolombia' ? THEME.colors.primary : THEME.colors.textMuted} />
                <Text style={[styles.payMethodText, paymentMethod === 'bancolombia' && styles.payMethodTextActive]}>PSE Bancolombia</Text>
              </TouchableOpacity>
            </View>

            <TouchableOpacity style={styles.submitBtn} onPress={handleSubmit} activeOpacity={0.85}>
              <Ionicons name="shield-checkmark" size={16} color="#ffffff" />
              <Text style={styles.submitBtnText}>Confirmar y Proteger Pago</Text>
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
  proInfoRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10
  },
  avatar: {
    width: 40,
    height: 40,
    borderRadius: 20
  },
  title: {
    fontSize: 14.5,
    fontWeight: '700',
    color: THEME.colors.textMain
  },
  subtitle: {
    fontSize: 11.5,
    color: THEME.colors.textMuted
  },
  closeBtn: {
    padding: 6
  },
  scrollContent: {
    padding: 20,
    gap: 14
  },
  fieldGroup: {
    gap: 5
  },
  label: {
    fontSize: 12,
    fontWeight: '700',
    color: THEME.colors.textMain
  },
  inputWrapper: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: THEME.colors.border,
    borderRadius: 12,
    paddingHorizontal: 10,
    backgroundColor: '#f8fafc'
  },
  inputIcon: {
    marginRight: 6
  },
  input: {
    flex: 1,
    height: 42,
    fontSize: 12.5,
    color: THEME.colors.textMain
  },
  textArea: {
    borderWidth: 1,
    borderColor: THEME.colors.border,
    borderRadius: 12,
    padding: 10,
    fontSize: 12.5,
    color: THEME.colors.textMain,
    backgroundColor: '#f8fafc',
    textAlignVertical: 'top'
  },
  urgencyCard: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: '#fff1f2',
    borderWidth: 1,
    borderColor: '#fecdd3',
    padding: 12,
    borderRadius: 14
  },
  urgencyCardActive: {
    borderColor: THEME.colors.primary,
    backgroundColor: '#ffe4e6'
  },
  urgencyLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10
  },
  urgencyIcon: {
    width: 34,
    height: 34,
    borderRadius: 17,
    backgroundColor: '#ffffff',
    alignItems: 'center',
    justifyContent: 'center'
  },
  urgencyIconActive: {
    backgroundColor: THEME.colors.primary
  },
  urgencyTitle: {
    fontSize: 12.5,
    fontWeight: '700',
    color: THEME.colors.textMain
  },
  urgencySub: {
    fontSize: 11,
    color: THEME.colors.textMuted
  },
  breakdownCard: {
    backgroundColor: '#f8fafc',
    borderRadius: 14,
    padding: 14,
    borderWidth: 1,
    borderColor: THEME.colors.border,
    gap: 8
  },
  breakdownHeader: {
    fontSize: 10.5,
    fontWeight: '800',
    color: THEME.colors.textLight,
    letterSpacing: 0.5
  },
  priceRow: {
    flexDirection: 'row',
    justifyContent: 'space-between'
  },
  priceLabel: {
    fontSize: 12,
    color: THEME.colors.textMuted
  },
  priceValue: {
    fontSize: 12,
    fontWeight: '600',
    color: THEME.colors.textMain
  },
  urgencyText: {
    color: THEME.colors.urgent,
    fontWeight: '700'
  },
  totalRow: {
    borderTopWidth: 1,
    borderTopColor: '#e2e8f0',
    paddingTop: 8,
    marginTop: 4
  },
  totalLabel: {
    fontSize: 13,
    fontWeight: '800',
    color: THEME.colors.textMain
  },
  totalValue: {
    fontSize: 16,
    fontWeight: '800',
    color: THEME.colors.primary
  },
  paymentMethodsRow: {
    flexDirection: 'row',
    gap: 10
  },
  payMethod: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    paddingVertical: 10,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: THEME.colors.border,
    backgroundColor: '#ffffff'
  },
  payMethodActive: {
    borderColor: THEME.colors.primary,
    backgroundColor: THEME.colors.primaryLight
  },
  payMethodText: {
    fontSize: 12,
    fontWeight: '600',
    color: THEME.colors.textMuted
  },
  payMethodTextActive: {
    color: THEME.colors.primary,
    fontWeight: '700'
  },
  submitBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    backgroundColor: THEME.colors.primary,
    paddingVertical: 14,
    borderRadius: 14,
    marginTop: 8
  },
  submitBtnText: {
    fontSize: 13.5,
    fontWeight: '700',
    color: '#ffffff'
  }
});
