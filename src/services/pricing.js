export const PLATFORM_FEE = 3000;
export const URGENCY_RATE = 0.30;

export function calculatePrice(baseRate, isUrgent = false) {
  const rate = Number(baseRate) || 45000;
  const urgencySurcharge = isUrgent ? Math.round(rate * URGENCY_RATE) : 0;
  const total = rate + urgencySurcharge + PLATFORM_FEE;

  return {
    baseRate: rate,
    isUrgent,
    urgencySurcharge,
    platformFee: PLATFORM_FEE,
    totalAmount: total,
    formattedBaseRate: `$${rate.toLocaleString('es-CO')}`,
    formattedUrgencySurcharge: `+$${urgencySurcharge.toLocaleString('es-CO')}`,
    formattedPlatformFee: `$${PLATFORM_FEE.toLocaleString('es-CO')}`,
    formattedTotal: `$${total.toLocaleString('es-CO')}`
  };
}
