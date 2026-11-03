import { Role, ProStatus } from '../constants/statuses';
 
export function filterProfessionals(professionals, { user, category, zone, query }) {
  return professionals.filter(pro => {
    // Un cliente solo ve técnicos verificados (HU05)
    if (user.role === Role.CLIENT && pro.status !== ProStatus.VERIFIED) return false;
 
    if (category !== 'all' && !pro.categories.includes(category)) return false;
    if (zone !== 'Todas' && pro.zone !== zone) return false;
 
    if (query.trim()) {
      const q = query.toLowerCase();
      return (
        pro.name.toLowerCase().includes(q) ||
        pro.specialtyName.toLowerCase().includes(q) ||
        pro.zone.toLowerCase().includes(q)
      );
    }
    return true;
  });
}