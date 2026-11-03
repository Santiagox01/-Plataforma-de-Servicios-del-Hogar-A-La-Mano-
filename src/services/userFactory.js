import { Role, ProStatus } from '../constants/statuses';
import { AVATARS, DEFAULT_ZONE } from '../constants/config';
 
//  (Factory Method)
class UserCreator {
  // Devuelve un mensaje de error o null si los datos del rol son válidos
  validate() {
    return null;
  }
 
  // cada subclase decide cómo construir su usuario
  build() {
    throw new Error('build() debe implementarse en la subclase');
  }
 
  
  base(d, role, defaultStatus, defaultAvatar) {
    return {
      id: d.id,
      name: d.name,
      email: d.email,
      phone: d.phone,
      role,
      status: d.status ?? defaultStatus,
      zone: d.zone ?? DEFAULT_ZONE,
      address: d.address ?? 'Medellín',
      specialtyName: d.specialtyName ?? null,
      ccNumber: d.ccNumber ?? null,
      conteReg: d.conteReg ?? null,
      avatar: d.avatar ?? defaultAvatar,
      isSupabase: !!d.isSupabase,
    };
  }
}
 
class ClientCreator extends UserCreator {
  build(d) {
    return this.base(d, Role.CLIENT, ProStatus.VERIFIED, AVATARS.client);
  }
}
 
class ProfessionalCreator extends UserCreator {
  validate(d) {
    if (!d.specialtyName || !String(d.specialtyName).trim()) {
      return 'Debes seleccionar o escribir tu especialidad técnica.';
    }
    if (!d.ccNumber || !String(d.ccNumber).trim()) {
      return 'La Cédula de Ciudadanía es obligatoria para la verificación oficial de identidad.';
    }
    return null;
  }
 
  build(d) {
    // Todo profesional nace "pending" hasta que el admin lo apruebe (HU02 / HU03)
    return this.base(d, Role.PROFESSIONAL, ProStatus.PENDING, AVATARS.professional);
  }
}
 
class AdminCreator extends UserCreator {
  build(d) {
    return this.base(d, Role.ADMIN, ProStatus.VERIFIED, AVATARS.client);
  }
}
 
const creators = {
  [Role.CLIENT]: new ClientCreator(),
  [Role.PROFESSIONAL]: new ProfessionalCreator(),
  [Role.ADMIN]: new AdminCreator(),
};
 
const creatorFor = (role) => {
  const creator = creators[role];
  if (!creator) throw new Error(`Rol no soportado: ${role}`);
  return creator;
};
 
export const validateRoleFields = (role, data) => creatorFor(role).validate(data);
export const createUser = (role, data) => creatorFor(role).build(data);