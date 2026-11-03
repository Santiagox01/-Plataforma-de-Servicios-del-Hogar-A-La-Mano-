import { supabase } from './supabase';
import { INITIAL_PROFESSIONALS } from '../data/mockData';
import { ProStatus } from '../constants/statuses';
import { SupabaseAdapter } from './adapters/supabaseAdapter';
 
// Expedientes iniciales de verificación (HU02 / HU03)
export const INITIAL_VERIFICATION_DOSSIERS = {
  usr_pro_1: {
    professionalId: 'usr_pro_1',
    name: 'Carlos Mario Restrepo',
    status: ProStatus.VERIFIED,
    score: '100%',
    verifiedAt: '2026-01-15',
    verifiedBy: 'Laura Restrepo (Auditoría UdeA)',
    documents: [
      { id: 'doc_1', type: 'cc', title: 'Cédula de Ciudadanía', number: '71.394.205', status: 'approved', expiry: 'Vigente', note: 'Verificado ante Registraduría' },
      { id: 'doc_2', type: 'police', title: 'Antecedentes Judiciales', number: 'POL-2026-8812', status: 'approved', expiry: 'Valido 30 días', note: 'Sin antecedentes penales en PONAL' },
      { id: 'doc_3', type: 'pro_card', title: 'Matrícula Técnica CONTE', number: 'TE1-49201', status: 'approved', expiry: '2029-12-31', note: 'Acreditado clase TE-1' },
      { id: 'doc_4', type: 'selfie', title: 'Prueba Biométrica / Selfie', number: 'BIO-OK', status: 'approved', expiry: 'Verificado', note: 'Coincidencia facial 98.4%' }
    ]
  },
  usr_pro_4: {
    professionalId: 'usr_pro_4',
    name: 'Pedro Pablo Jaramillo',
    status: ProStatus.PENDING,
    score: '75%',
    verifiedAt: null,
    verifiedBy: null,
    documents: [
      { id: 'doc_5', type: 'cc', title: 'Cédula de Ciudadanía', number: '1.036.711.592', status: 'approved', expiry: 'Vigente', note: 'Cédula digital verificada' },
      { id: 'doc_6', type: 'police', title: 'Antecedentes Judiciales', number: 'POL-2026-3019', status: 'approved', expiry: 'Emitido hace 3 días', note: 'Certificado de Policía Nacional limpio' },
      { id: 'doc_7', type: 'pro_card', title: 'Certificación SENA Cerrajería', number: 'SENA-CER-8401', status: 'in_review', expiry: 'En validación', note: 'Esperando confirmación con base de datos SENA' },
      { id: 'doc_8', type: 'selfie', title: 'Prueba Biométrica / Selfie', number: 'Pendiente', status: 'submitted', expiry: 'Por auditar', note: 'Selfie con documento en mano adjuntada' }
    ]
  }
};
 
// Cargar profesionales desde Supabase o fallback local
export async function fetchProfessionals() {
  try {
    const { data, error } = await supabase
      .from('professionals')
      .select(`
        id,
        specialty_name,
        categories,
        base_rate,
        distance_km,
        rating,
        review_count,
        cc_number,
        conte_reg,
        bio,
        status,
        profiles (
          id,
          full_name,
          email,
          phone,
          zone,
          address,
          avatar_url
        )
      `);
 
    if (!error && data && data.length > 0) {
      return data.map(SupabaseAdapter.professionalToDomain);
    }
  } catch (e) {
    console.log('Using local professionals data.');
  }
 
  return INITIAL_PROFESSIONALS;
}
 
// Obtener expediente de verificación de un técnico (HU02 / HU03)
export async function fetchVerificationDossier(proId) {
  try {
    const { data, error } = await supabase
      .from('verification_documents')
      .select('*')
      .eq('professional_id', proId);
 
    if (!error && data && data.length > 0) {
      return {
        professionalId: proId,
        documents: data.map(SupabaseAdapter.documentToDomain)
      };
    }
  } catch (e) {
    console.log('Using local verification dossier.');
  }
 
  return INITIAL_VERIFICATION_DOSSIERS[proId] || INITIAL_VERIFICATION_DOSSIERS.usr_pro_4;
}
 
// Actualizar estado de verificación del técnico por auditor (HU03)
export async function updateProfessionalStatus(proId, newStatus, reason = null, adminName = 'Auditor UdeA') {
  let persisted = false;
  try {
    const { error } = await supabase
      .from('professionals')
      .update({
        status: newStatus,
        verified_at: newStatus === ProStatus.VERIFIED ? new Date().toISOString() : null
      })
      .eq('id', proId);
 
    persisted = !error;
    if (error) {
      console.warn('Supabase no actualizó el estado:', error.message);
    }
  } catch (e) {
    console.warn('Supabase offline, estado actualizado solo en local:', e.message);
  }
 
  // Actualizar también en el dossier local
  if (INITIAL_VERIFICATION_DOSSIERS[proId]) {
    INITIAL_VERIFICATION_DOSSIERS[proId].status = newStatus;
    INITIAL_VERIFICATION_DOSSIERS[proId].verifiedAt = newStatus === ProStatus.VERIFIED ? new Date().toISOString().split('T')[0] : null;
    INITIAL_VERIFICATION_DOSSIERS[proId].verifiedBy = newStatus === ProStatus.VERIFIED ? adminName : null;
    if (newStatus === ProStatus.VERIFIED) {
      INITIAL_VERIFICATION_DOSSIERS[proId].score = '100%';
      INITIAL_VERIFICATION_DOSSIERS[proId].documents.forEach(d => d.status = 'approved');
    } else if (newStatus === ProStatus.REJECTED) {
      INITIAL_VERIFICATION_DOSSIERS[proId].rejectionReason = reason;
      INITIAL_VERIFICATION_DOSSIERS[proId].documents[2].status = 'rejected';
      INITIAL_VERIFICATION_DOSSIERS[proId].documents[2].note = reason;
    }
  }
 
  return { success: true, persisted, status: newStatus };
}
 
// Subir o simular carga de documento de identidad (HU02)
export async function uploadVerificationDocument(proId, docType, docNumber) {
  try {
    await supabase.from('verification_documents').insert([
      {
        professional_id: proId,
        document_type: docType,
        file_url: `https://alamano.co/uploads/${proId}_${docType}.pdf`,
        status: 'submitted'
      }
    ]);
  } catch (e) {
    console.warn('No se pudo radicar el documento en Supabase:', e.message);
  }
 
  if (INITIAL_VERIFICATION_DOSSIERS[proId]) {
    const doc = INITIAL_VERIFICATION_DOSSIERS[proId].documents.find(d => d.type === docType || d.id === docType);
    if (doc) {
      doc.status = 'in_review';
      doc.number = docNumber || doc.number;
      doc.note = 'Documento radicado recientemente por el profesional';
    }
  }
 
  return { success: true, message: 'Documento cargado exitosamente para revisión.' };
}
 
// Crear orden de servicio en base de datos (HU05)
export async function createServiceOrder(orderData) {
  try {
    const { data, error } = await supabase.from('service_requests').insert([
      {
        service_code: `SRV-${Date.now().toString().slice(-6)}`,
        client_id: orderData.clientId || 'a1111111-1111-1111-1111-111111111111',
        professional_id: orderData.proId,
        category: 'reparaciones',
        description: orderData.description,
        service_address: orderData.address,
        zone: 'El Poblado',
        is_urgent: orderData.isUrgent,
        base_rate: orderData.pricing.baseRate,
        urgency_surcharge: orderData.pricing.urgencySurcharge,
        platform_fee: orderData.pricing.platformFee,
        total_amount: orderData.pricing.totalAmount,
        payment_method: orderData.paymentMethod,
        escrow_status: 'en_custodia',
        service_status: 'solicitado'
      }
    ]).select().maybeSingle();
 
    if (!error && data) {
      return { success: true, persisted: true, service: data };
    }
    if (error) {
      console.warn('No se guardó la orden en Supabase:', error.message);
    }
  } catch (e) {
    console.warn('Orden guardada solo en modo local:', e.message);
  }
 
  return { success: true, persisted: false };
}
