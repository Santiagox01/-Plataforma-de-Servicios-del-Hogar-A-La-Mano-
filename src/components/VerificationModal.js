import React, { useState } from 'react';
import {
  Modal,
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Alert,
  TextInput,
  KeyboardAvoidingView,
  Platform
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { THEME } from '../constants/theme';
import { uploadVerificationDocument } from '../services/databaseService';

export default function VerificationModal({
  visible,
  onClose,
  dossier,
  professionalName,
  isAuditor = false,
  onApproveDossier,
  onRejectDossier
}) {
  const [activeDossier, setActiveDossier] = useState(dossier);
  const [uploadingDocId, setUploadingDocId] = useState(null);
  const [docInputText, setDocInputText] = useState('');
  const [rejectReason, setRejectReason] = useState('');
  const [showRejectInput, setShowRejectInput] = useState(false);

  if (!dossier) return null;

  const documents = activeDossier?.documents || dossier.documents || [];
  const status = activeDossier?.status || dossier.status || 'pending';

  const handleSimulateUpload = async (doc) => {
    Alert.alert(
      `Cargar ${doc.title}`,
      `Selecciona cómo deseas adjuntar tu ${doc.title} para validación oficial:`,
      [
        {
          text: 'Tomar Foto con Cámara',
          onPress: () => processUpload(doc.id, 'Foto_Camara_' + Date.now())
        },
        {
          text: 'Subir Archivo PDF / Imagen',
          onPress: () => processUpload(doc.id, 'Documento_PDF_' + Date.now())
        },
        { text: 'Cancelar', style: 'cancel' }
      ]
    );
  };

  const processUpload = async (docId, sampleNumber) => {
    await uploadVerificationDocument(dossier.professionalId || 'usr_pro_4', docId, sampleNumber);
    // Actualizar estado local
    const updatedDocs = documents.map(d => {
      if (d.id === docId) {
        return { ...d, status: 'in_review', note: 'Documento cargado recientemente por el técnico' };
      }
      return d;
    });
    setActiveDossier({ ...activeDossier, documents: updatedDocs });
    Alert.alert(
      'Documento Radicado',
      'El documento ha sido cargado exitosamente y pasó a estado "En Revisión" por el equipo de auditoría.'
    );
  };

  const getStatusBadge = (docStatus) => {
    switch (docStatus) {
      case 'approved':
        return {
          label: 'Aprobado',
          color: '#15803d',
          bg: '#dcfce7',
          icon: 'checkmark-circle'
        };
      case 'in_review':
        return {
          label: 'En Revisión',
          color: '#b45309',
          bg: '#fef3c7',
          icon: 'time'
        };
      case 'rejected':
        return {
          label: 'Rechazado',
          color: '#b91c1c',
          bg: '#fee2e2',
          icon: 'alert-circle'
        };
      default:
        return {
          label: 'Pendiente',
          color: '#1d4ed8',
          bg: '#dbeafe',
          icon: 'cloud-upload'
        };
    }
  };

  const approvedCount = documents.filter(d => d.status === 'approved').length;
  const progressPercent = Math.round((approvedCount / (documents.length || 1)) * 100);

  return (
    <Modal visible={visible} animationType="slide" transparent onRequestClose={onClose}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        style={styles.overlay}
      >
        <View style={styles.sheet}>
          <View style={styles.handle} />

          {/* Encabezado */}
          <View style={styles.header}>
            <View style={{ flex: 1 }}>
              <View style={styles.titleRow}>
                <Ionicons name="shield-checkmark" size={20} color={THEME.colors.primary} />
                <Text style={styles.title}>Expediente y Verificación de Identidad</Text>
              </View>
              <Text style={styles.subtitle}>Técnico: {professionalName || dossier.name}</Text>
            </View>
            <TouchableOpacity onPress={onClose} style={styles.closeBtn}>
              <Ionicons name="close" size={22} color={THEME.colors.textMuted} />
            </TouchableOpacity>
          </View>

          <ScrollView
            showsVerticalScrollIndicator={false}
            contentContainerStyle={styles.content}
            keyboardShouldPersistTaps="handled"
          >
            {/* Tarjeta de Resumen de Auditoría */}
            <View style={styles.statusCard}>
              <View style={styles.statusHeader}>
                <View>
                  <Text style={styles.statusLabel}>Estado General del Profesional:</Text>
                  <Text style={[styles.statusValue, status === 'verified' ? styles.statusVerified : styles.statusPending]}>
                    {status === 'verified' ? '✓ PROFESIONAL VERIFICADO' : '⏳ EN PROCESO DE AUDITORÍA'}
                  </Text>
                </View>
                <View style={styles.progressCircle}>
                  <Text style={styles.progressPercent}>{progressPercent}%</Text>
                  <Text style={styles.progressSub}>Validado</Text>
                </View>
              </View>

              {/* Barra de progreso */}
              <View style={styles.progressBarBg}>
                <View style={[styles.progressBarFill, { width: `${progressPercent}%` }]} />
              </View>

              <Text style={styles.progressDetail}>
                {approvedCount} de {documents.length} documentos acreditados bajo la normativa colombiana (Ley 1264 de 2008 / RETIE / MinTrabajo).
              </Text>

              {dossier.verifiedAt && (
                <View style={styles.verifiedMetaRow}>
                  <Ionicons name="ribbon" size={14} color="#15803d" />
                  <Text style={styles.verifiedMetaText}>
                    Acreditado el {dossier.verifiedAt} por {dossier.verifiedBy}
                  </Text>
                </View>
              )}
            </View>

            {/* Listado de Documentos del Expediente */}
            <Text style={styles.sectionTitle}>Documentos Requeridos para Verificación:</Text>

            {documents.map((doc, idx) => {
              const badge = getStatusBadge(doc.status);
              return (
                <View key={doc.id || idx} style={styles.docCard}>
                  <View style={styles.docHeader}>
                    <View style={styles.docIconWrap}>
                      <Ionicons
                        name={
                          doc.type === 'cc' ? 'id-card' :
                          doc.type === 'police' ? 'shield' :
                          doc.type === 'pro_card' ? 'school' : 'person-circle'
                        }
                        size={22}
                        color={THEME.colors.primary}
                      />
                    </View>
                    <View style={{ flex: 1 }}>
                      <View style={styles.docTitleRow}>
                        <Text style={styles.docTitle}>{doc.title}</Text>
                        <View style={[styles.badge, { backgroundColor: badge.bg }]}>
                          <Ionicons name={badge.icon} size={11} color={badge.color} />
                          <Text style={[styles.badgeText, { color: badge.color }]}>{badge.label}</Text>
                        </View>
                      </View>
                      <Text style={styles.docNumber}>Ref / Código: {doc.number || 'Pendiente de radicación'}</Text>
                    </View>
                  </View>

                  <View style={styles.docBody}>
                    <Text style={styles.docNote}>{doc.note || 'Documento oficial en custodia segura.'}</Text>
                    {doc.expiry && (
                      <Text style={styles.docExpiry}>Vigencia: {doc.expiry}</Text>
                    )}
                  </View>

                  {/* Acciones para el Técnico */}
                  {!isAuditor && doc.status !== 'approved' && (
                    <TouchableOpacity
                      style={styles.uploadBtn}
                      onPress={() => handleSimulateUpload(doc)}
                      activeOpacity={0.8}
                    >
                      <Ionicons name="cloud-upload-outline" size={14} color={THEME.colors.primary} />
                      <Text style={styles.uploadBtnText}>Adjuntar o Actualizar Soporte</Text>
                    </TouchableOpacity>
                  )}
                </View>
              );
            })}

            {/* Acciones si el usuario es Auditor (HU03) */}
            {isAuditor && status !== 'verified' && (
              <View style={styles.auditorBox}>
                <Text style={styles.auditorTitle}>Acciones de Auditor de Seguridad UdeA:</Text>
                <Text style={styles.auditorDesc}>
                  Como auditor autorizado, valida si los soportes presentados cumplen con autenticidad y antecedentes antes de habilitar al técnico.
                </Text>

                {showRejectInput ? (
                  <View style={styles.rejectContainer}>
                    <Text style={styles.rejectLabel}>Motivo del Rechazo de Documentos:</Text>
                    <TextInput
                      style={styles.rejectInput}
                      placeholder="Ej. El certificado de antecedentes está vencido (>30 días)..."
                      value={rejectReason}
                      onChangeText={setRejectReason}
                      multiline
                    />
                    <View style={styles.rejectActionsRow}>
                      <TouchableOpacity
                        style={styles.cancelRejectBtn}
                        onPress={() => setShowRejectInput(false)}
                      >
                        <Text style={styles.cancelRejectText}>Cancelar</Text>
                      </TouchableOpacity>
                      <TouchableOpacity
                        style={styles.confirmRejectBtn}
                        onPress={() => {
                          if (!rejectReason.trim()) {
                            Alert.alert('Motivo requerido', 'Ingresa la razón del rechazo para notificar al técnico.');
                            return;
                          }
                          onRejectDossier && onRejectDossier(dossier.professionalId, rejectReason);
                          onClose();
                        }}
                      >
                        <Text style={styles.confirmRejectText}>Confirmar Rechazo</Text>
                      </TouchableOpacity>
                    </View>
                  </View>
                ) : (
                  <View style={styles.auditorActionsRow}>
                    <TouchableOpacity
                      style={styles.approveFullBtn}
                      onPress={() => {
                        onApproveDossier && onApproveDossier(dossier.professionalId);
                        onClose();
                      }}
                      activeOpacity={0.85}
                    >
                      <Ionicons name="checkmark-done-circle" size={18} color="#ffffff" />
                      <Text style={styles.approveFullText}>Aprobar y Certificar Técnico</Text>
                    </TouchableOpacity>

                    <TouchableOpacity
                      style={styles.rejectFullBtn}
                      onPress={() => setShowRejectInput(true)}
                      activeOpacity={0.85}
                    >
                      <Ionicons name="close-circle-outline" size={18} color="#991b1b" />
                      <Text style={styles.rejectFullText}>Rechazar</Text>
                    </TouchableOpacity>
                  </View>
                )}
              </View>
            )}
          </ScrollView>
        </View>
      </KeyboardAvoidingView>
    </Modal>
  );
}

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.55)',
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
    width: 44,
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
    fontSize: 15,
    fontWeight: '800',
    color: THEME.colors.textMain
  },
  subtitle: {
    fontSize: 12,
    color: THEME.colors.textMuted,
    marginTop: 2
  },
  closeBtn: {
    padding: 6
  },
  content: {
    padding: 20,
    gap: 14
  },
  statusCard: {
    backgroundColor: '#f8fafc',
    borderRadius: 16,
    padding: 16,
    borderWidth: 1,
    borderColor: THEME.colors.border
  },
  statusHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center'
  },
  statusLabel: {
    fontSize: 11,
    color: THEME.colors.textMuted,
    fontWeight: '600'
  },
  statusValue: {
    fontSize: 13,
    fontWeight: '800',
    marginTop: 2
  },
  statusVerified: {
    color: '#15803d'
  },
  statusPending: {
    color: '#b45309'
  },
  progressCircle: {
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#ffffff',
    width: 54,
    height: 54,
    borderRadius: 27,
    borderWidth: 2,
    borderColor: THEME.colors.primary
  },
  progressPercent: {
    fontSize: 13,
    fontWeight: '800',
    color: THEME.colors.primary
  },
  progressSub: {
    fontSize: 8.5,
    color: THEME.colors.textMuted,
    textTransform: 'uppercase'
  },
  progressBarBg: {
    height: 6,
    backgroundColor: '#e2e8f0',
    borderRadius: 3,
    marginVertical: 12,
    overflow: 'hidden'
  },
  progressBarFill: {
    height: '100%',
    backgroundColor: THEME.colors.primary,
    borderRadius: 3
  },
  progressDetail: {
    fontSize: 11.5,
    color: THEME.colors.textMuted,
    lineHeight: 16
  },
  verifiedMetaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginTop: 10,
    paddingTop: 8,
    borderTopWidth: 1,
    borderTopColor: '#e2e8f0'
  },
  verifiedMetaText: {
    fontSize: 11,
    color: '#15803d',
    fontWeight: '600'
  },
  sectionTitle: {
    fontSize: 13,
    fontWeight: '800',
    color: THEME.colors.textMain,
    marginTop: 4
  },
  docCard: {
    backgroundColor: '#ffffff',
    borderRadius: 14,
    padding: 14,
    borderWidth: 1,
    borderColor: THEME.colors.border,
    gap: 8
  },
  docHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12
  },
  docIconWrap: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: THEME.colors.primaryLight,
    alignItems: 'center',
    justifyContent: 'center'
  },
  docTitleRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center'
  },
  docTitle: {
    fontSize: 13,
    fontWeight: '700',
    color: THEME.colors.textMain
  },
  badge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    paddingHorizontal: 7,
    paddingVertical: 3,
    borderRadius: 8
  },
  badgeText: {
    fontSize: 10,
    fontWeight: '700'
  },
  docNumber: {
    fontSize: 11,
    color: THEME.colors.textMuted,
    marginTop: 2
  },
  docBody: {
    backgroundColor: '#f8fafc',
    padding: 8,
    borderRadius: 8,
    gap: 2
  },
  docNote: {
    fontSize: 11,
    color: THEME.colors.textMain
  },
  docExpiry: {
    fontSize: 10.5,
    color: THEME.colors.textMuted,
    fontStyle: 'italic'
  },
  uploadBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    paddingVertical: 7,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: THEME.colors.primary,
    backgroundColor: '#f0fdf4'
  },
  uploadBtnText: {
    fontSize: 11.5,
    fontWeight: '700',
    color: THEME.colors.primary
  },
  auditorBox: {
    backgroundColor: '#f1f5f9',
    borderRadius: 16,
    padding: 16,
    marginTop: 10,
    gap: 10
  },
  auditorTitle: {
    fontSize: 13,
    fontWeight: '800',
    color: THEME.colors.textMain
  },
  auditorDesc: {
    fontSize: 11.5,
    color: THEME.colors.textMuted,
    lineHeight: 16
  },
  auditorActionsRow: {
    flexDirection: 'row',
    gap: 10,
    marginTop: 4
  },
  approveFullBtn: {
    flex: 2,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    backgroundColor: '#15803d',
    paddingVertical: 11,
    borderRadius: 12
  },
  approveFullText: {
    color: '#ffffff',
    fontSize: 12.5,
    fontWeight: '700'
  },
  rejectFullBtn: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    backgroundColor: '#fee2e2',
    paddingVertical: 11,
    borderRadius: 12
  },
  rejectFullText: {
    color: '#991b1b',
    fontSize: 12.5,
    fontWeight: '700'
  },
  rejectContainer: {
    backgroundColor: '#ffffff',
    padding: 12,
    borderRadius: 12,
    gap: 8
  },
  rejectLabel: {
    fontSize: 11.5,
    fontWeight: '700',
    color: '#991b1b'
  },
  rejectInput: {
    borderWidth: 1,
    borderColor: '#fca5a5',
    borderRadius: 8,
    padding: 10,
    fontSize: 12,
    minHeight: 60,
    textAlignVertical: 'top'
  },
  rejectActionsRow: {
    flexDirection: 'row',
    gap: 8,
    justifyContent: 'flex-end'
  },
  cancelRejectBtn: {
    paddingHorizontal: 12,
    paddingVertical: 7,
    borderRadius: 8
  },
  cancelRejectText: {
    fontSize: 12,
    color: THEME.colors.textMuted,
    fontWeight: '600'
  },
  confirmRejectBtn: {
    backgroundColor: '#dc2626',
    paddingHorizontal: 14,
    paddingVertical: 7,
    borderRadius: 8
  },
  confirmRejectText: {
    fontSize: 12,
    color: '#ffffff',
    fontWeight: '700'
  }
});
