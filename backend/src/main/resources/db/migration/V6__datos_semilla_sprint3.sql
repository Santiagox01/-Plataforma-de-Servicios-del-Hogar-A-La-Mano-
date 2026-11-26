-- Datos semilla Sprint 3 (tarjetas tokenizadas, solicitud PAGADA en custodia, reporte pendiente y caso de soporte)
-- Nota: no se agregan notificaciones al admin ni se modifican calificaciones de Juan para preservar pruebas de Sprint 1 y 2.

-- 1. Metodos de pago semilla para Carlos y Laura (solo token, tipo y ultimos 4 digitos)
INSERT INTO metodos_pago (
    id_cliente, token, tipo, franquicia, ultimos_digitos, titular, mes_vencimiento, anio_vencimiento, activo
)
SELECT
    (SELECT id_usuario FROM usuarios WHERE correo = 'carlos.cliente@alamano.co'),
    'tok_sim_ok_carlos_4242',
    'TARJETA_CREDITO',
    'VISA',
    '4242',
    'Carlos Ramírez',
    12,
    2028,
    TRUE;

INSERT INTO metodos_pago (
    id_cliente, token, tipo, franquicia, ultimos_digitos, titular, mes_vencimiento, anio_vencimiento, activo
)
SELECT
    (SELECT id_usuario FROM usuarios WHERE correo = 'laura.cliente@alamano.co'),
    'tok_sim_ok_laura_4444',
    'TARJETA_DEBITO',
    'MASTERCARD',
    '4444',
    'Laura Gómez',
    11,
    2028,
    TRUE;

-- 2. Solicitud PAGADA (Laura -> Maria, Cerrajeria) con pago RETENIDO en custodia
INSERT INTO solicitudes (
    id_cliente, id_profesional, id_servicio, descripcion,
    estado_solicitud, es_urgente, precio_estimado, precio_final,
    precio_final_confirmado, fecha_finalizacion, version
)
SELECT
    (SELECT id_usuario FROM usuarios WHERE correo = 'laura.cliente@alamano.co'),
    (SELECT id_usuario FROM usuarios WHERE correo = 'maria.plomera@alamano.co'),
    (SELECT s.id_servicio FROM servicios s
     JOIN usuarios u ON s.id_profesional = u.id_usuario
     JOIN categorias_oficio c ON s.id_categoria = c.id_categoria
     WHERE u.correo = 'maria.plomera@alamano.co' AND c.nombre = 'Cerrajería'),
    'Cambio de cerradura principal de apartamento en Laureles.',
    'PAGADA',
    FALSE,
    40000.00,
    40000.00,
    TRUE,
    CURRENT_TIMESTAMP,
    0;

INSERT INTO chats (id_solicitud)
SELECT sol.id_solicitud FROM solicitudes sol
JOIN usuarios uc ON sol.id_cliente = uc.id_usuario
JOIN usuarios up ON sol.id_profesional = up.id_usuario
WHERE uc.correo = 'laura.cliente@alamano.co'
  AND up.correo = 'maria.plomera@alamano.co'
  AND sol.estado_solicitud = 'PAGADA';

INSERT INTO pagos (
    id_solicitud, id_metodo_pago, monto_servicio, tasa_garantia, cantidad,
    referencia_idempotencia, referencia_pasarela, estado_pago, version
)
SELECT
    (SELECT sol.id_solicitud FROM solicitudes sol
     JOIN usuarios uc ON sol.id_cliente = uc.id_usuario
     JOIN usuarios up ON sol.id_profesional = up.id_usuario
     WHERE uc.correo = 'laura.cliente@alamano.co'
       AND up.correo = 'maria.plomera@alamano.co'
       AND sol.estado_solicitud = 'PAGADA'),
    (SELECT id_metodo_pago FROM metodos_pago WHERE token = 'tok_sim_ok_laura_4444'),
    40000.00,
    3000.00,
    43000.00,
    'PAY-SOL-SEED-PAGADA-1',
    'SIM-TX-SEED-0001',
    'RETENIDO',
    0;

-- 3. Reporte pendiente (Laura reporta a Maria sobre la solicitud ACEPTADA de Plomeria)
INSERT INTO reportes (
    id_solicitud, id_reportante, id_reportado, motivo, descripcion, es_prioritario, estado_reporte, version
)
SELECT
    (SELECT sol.id_solicitud FROM solicitudes sol
     JOIN usuarios uc ON sol.id_cliente = uc.id_usuario
     JOIN usuarios up ON sol.id_profesional = up.id_usuario
     WHERE uc.correo = 'laura.cliente@alamano.co'
       AND up.correo = 'maria.plomera@alamano.co'
       AND sol.estado_solicitud = 'ACEPTADA'),
    (SELECT id_usuario FROM usuarios WHERE correo = 'laura.cliente@alamano.co'),
    (SELECT id_usuario FROM usuarios WHERE correo = 'maria.plomera@alamano.co'),
    'NO_SE_PRESENTO',
    'Se retrasó más de una hora sin aviso previo en la primera visita.',
    FALSE,
    'PENDIENTE',
    0;

-- 4. Caso de soporte abierto (Carlos)
INSERT INTO casos_soporte (
    numero_caso, id_usuario, id_solicitud, asunto, descripcion, estado_caso
)
SELECT
    'SUP-2026-SEED01',
    (SELECT id_usuario FROM usuarios WHERE correo = 'carlos.cliente@alamano.co'),
    NULL,
    'Consulta sobre cobertura de tasa de garantía',
    'Quisiera conocer qué cubre exactamente la tasa de garantía de $3.000 COP.',
    'ABIERTO';
