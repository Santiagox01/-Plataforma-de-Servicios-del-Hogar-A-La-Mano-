import { useState } from 'react';
import { INITIAL_SERVICES } from '../data/mockData';
import { createServiceOrder } from '../services/databaseService';
import { ServiceStatus, EscrowStatus } from '../constants/statuses';
 

// App.js solo dispara eventos; las reglas viven aquí.
export function useServices(currentUser) {
  const [services, setServices] = useState(INITIAL_SERVICES);
 
  const requestService = async (bookingData) => {
    const newService = {
      id: `SRV-2026-${(services.length + 1).toString().padStart(2, '0')}`,
      clientId: currentUser.id,
      proId: bookingData.proId,
      proName: bookingData.proName,
      proSpecialty: bookingData.proSpecialty,
      description: bookingData.description,
      address: bookingData.address,
      status: ServiceStatus.EN_CAMINO,
      totalAmount: bookingData.pricing.totalAmount,
      escrowStatus: EscrowStatus.PROTEGIDO,
      isUrgent: bookingData.isUrgent,
      scheduledAt: 'Hoy, Inmediato'
    };
 
    setServices(prev => [newService, ...prev]);
 
    createServiceOrder({
      ...bookingData,
      clientId: currentUser.id
    }).catch(err => console.warn('DB sync:', err));
 
    return newService;
  };
 
  const releasePayment = (serviceId) => {
    setServices(prev =>
      prev.map(s =>
        s.id === serviceId
          ? { ...s, status: ServiceStatus.FINALIZADO, escrowStatus: EscrowStatus.LIBERADO }
          : s
      )
    );
  };
 
  return { services, requestService, releasePayment };
}