import { DEFAULT_BASE_RATE, DEFAULT_ZONE, AVATARS } from '../../constants/config';
 
const DOC_TITLES = {
  cc_front: 'Cédula de Ciudadanía',
  police_record: 'Antecedentes Judiciales',
  conte_certificate: 'Matrícula Técnica',
};
 
// Adapter: traduce el esquema de Supabase (snake_case) al modelo que usa la UI
export class SupabaseAdapter {
  static professionalToDomain(item) {
    return {
      id: item.id,
      name: item.profiles?.full_name || 'Técnico Especialista',
      specialtyName: item.specialty_name,
      categories: item.categories || ['electricidad'],
      rate: Number(item.base_rate) || DEFAULT_BASE_RATE,
      rating: Number(item.rating) || 5.0,
      reviewCount: item.review_count || 0,
      distanceKm: Number(item.distance_km) || 2.0,
      zone: item.profiles?.zone || DEFAULT_ZONE,
      status: item.status,
      ccNumber: item.cc_number,
      conteReg: item.conte_reg,
      bio: item.bio,
      avatar: item.profiles?.avatar_url || AVATARS.professional,
    };
  }
 
  static documentToDomain(d) {
    return {
      id: d.id,
      type: d.document_type,
      title: DOC_TITLES[d.document_type] || 'Selfie Biométrica',
      status: d.status,
      rejectionReason: d.rejection_reason,
      fileUrl: d.file_url,
    };
  }
}