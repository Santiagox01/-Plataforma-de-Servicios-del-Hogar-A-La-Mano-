import { DEFAULT_BASE_RATE } from '../constants/config';
 
export const PLATFORM_FEE = 3000;
export const URGENCY_RATE = 0.30;
 
// Strategy: cada estrategia decide cuánto recargo aplica
export class NormalPricing {
  surcharge() {
    return 0;
  }
}
 
export class UrgentPricing {
  constructor(rate = URGENCY_RATE) {
    this.rate = rate;
  }
  surcharge(baseRate) {
    return Math.round(baseRate * this.rate);
  }
}
 
export const getPricingStrategy = (isUrgent) =>
  isUrgent ? new UrgentPricing() : new NormalPricing();
 
const cop = (n) => `$${n.toLocaleString('es-CO')}`;
 
// Contexto: misma firma que antes, BookingModal.js no necesita cambios
export function calculatePrice(baseRate, isUrgent = false, strategy = getPricingStrategy(isUrgent)) {
  const rate = Number(baseRate) || DEFAULT_BASE_RATE;
  const urgencySurcharge = strategy.surcharge(rate);
  const total = rate + urgencySurcharge + PLATFORM_FEE;
 
  return {
    baseRate: rate,
    isUrgent,
    urgencySurcharge,
    platformFee: PLATFORM_FEE,
    totalAmount: total,
    formattedBaseRate: cop(rate),
    formattedUrgencySurcharge: `+${cop(urgencySurcharge)}`,
    formattedPlatformFee: cop(PLATFORM_FEE),
    formattedTotal: cop(total),
  };
}
