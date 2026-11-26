-- Migracion Sprint 3 (extension): soporte para PayPal Sandbox (REST API v2), metodos PAYPAL y conversion COP -> USD

ALTER TABLE metodos_pago DROP CONSTRAINT chk_metodos_pago_tipo;
ALTER TABLE metodos_pago ADD CONSTRAINT chk_metodos_pago_tipo CHECK (
    tipo IN ('TARJETA_CREDITO', 'TARJETA_DEBITO', 'PAYPAL')
);

ALTER TABLE metodos_pago DROP CONSTRAINT chk_metodos_pago_franquicia;
ALTER TABLE metodos_pago ADD CONSTRAINT chk_metodos_pago_franquicia CHECK (
    franquicia IN ('VISA', 'MASTERCARD', 'AMEX', 'PAYPAL', 'OTRA')
);

ALTER TABLE metodos_pago DROP CONSTRAINT chk_metodos_pago_mes;
ALTER TABLE metodos_pago DROP CONSTRAINT chk_metodos_pago_anio;
ALTER TABLE metodos_pago ALTER COLUMN mes_vencimiento DROP NOT NULL;
ALTER TABLE metodos_pago ALTER COLUMN anio_vencimiento DROP NOT NULL;

ALTER TABLE metodos_pago ADD CONSTRAINT chk_metodos_pago_mes CHECK (
    mes_vencimiento IS NULL OR (mes_vencimiento >= 1 AND mes_vencimiento <= 12)
);
ALTER TABLE metodos_pago ADD CONSTRAINT chk_metodos_pago_anio CHECK (
    anio_vencimiento IS NULL OR anio_vencimiento >= 2020
);

ALTER TABLE pagos DROP CONSTRAINT chk_pagos_estado;
ALTER TABLE pagos ADD CONSTRAINT chk_pagos_estado CHECK (
    estado_pago IN ('PENDIENTE_APROBACION', 'RETENIDO', 'LIBERADO', 'RECHAZADO', 'REEMBOLSADO', 'CANCELADO')
);

ALTER TABLE pagos ADD COLUMN pasarela VARCHAR(30) NOT NULL DEFAULT 'simulada';
ALTER TABLE pagos ADD COLUMN monto_usd NUMERIC(12, 2);
ALTER TABLE pagos ADD COLUMN tasa_cambio_usd NUMERIC(12, 4);
ALTER TABLE pagos ADD COLUMN referencia_orden_externa VARCHAR(120);
ALTER TABLE pagos ADD COLUMN url_aprobacion VARCHAR(1000);
ALTER TABLE pagos ADD COLUMN referencia_captura VARCHAR(120);
ALTER TABLE pagos ADD COLUMN autorizacion_expira_en TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_pagos_orden_externa ON pagos (referencia_orden_externa);
