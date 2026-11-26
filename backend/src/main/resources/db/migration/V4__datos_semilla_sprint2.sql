-- Datos semilla Sprint 2: tarifas verificadas y solicitudes de ejemplo en distintos estados
-- Nota: Pedro (pedro.cerrajero@alamano.co) mantiene su tarifa en NULL para probar HU08/PF-03.

-- 1. Solicitud PENDIENTE urgente (Carlos -> Juan, Electricidad)
INSERT INTO solicitudes (
    id_cliente, id_profesional, id_servicio, descripcion,
    estado_solicitud, es_urgente, precio_estimado, precio_final_confirmado, version
)
SELECT
    (SELECT id_usuario FROM usuarios WHERE correo = 'carlos.cliente@alamano.co'),
    (SELECT id_usuario FROM usuarios WHERE correo = 'juan.electricista@alamano.co'),
    (SELECT s.id_servicio FROM servicios s
     JOIN usuarios u ON s.id_profesional = u.id_usuario
     JOIN categorias_oficio c ON s.id_categoria = c.id_categoria
     WHERE u.correo = 'juan.electricista@alamano.co' AND c.nombre = 'Electricidad'),
    'Cortocircuito en el tablero principal de la cocina; se requiere revisión prioritaria.',
    'PENDIENTE',
    TRUE,
    65000.00,
    FALSE,
    0;

-- 2. Solicitud ACEPTADA con chat y mensajes (Laura -> Maria, Plomeria)
INSERT INTO solicitudes (
    id_cliente, id_profesional, id_servicio, descripcion,
    estado_solicitud, es_urgente, precio_estimado, precio_final_confirmado, version
)
SELECT
    (SELECT id_usuario FROM usuarios WHERE correo = 'laura.cliente@alamano.co'),
    (SELECT id_usuario FROM usuarios WHERE correo = 'maria.plomera@alamano.co'),
    (SELECT s.id_servicio FROM servicios s
     JOIN usuarios u ON s.id_profesional = u.id_usuario
     JOIN categorias_oficio c ON s.id_categoria = c.id_categoria
     WHERE u.correo = 'maria.plomera@alamano.co' AND c.nombre = 'Plomería'),
    'Fuga constante debajo del lavamanos del baño principal.',
    'ACEPTADA',
    FALSE,
    45000.00,
    FALSE,
    0;

INSERT INTO chats (id_solicitud)
SELECT sol.id_solicitud FROM solicitudes sol
JOIN usuarios uc ON sol.id_cliente = uc.id_usuario
JOIN usuarios up ON sol.id_profesional = up.id_usuario
WHERE uc.correo = 'laura.cliente@alamano.co'
  AND up.correo = 'maria.plomera@alamano.co'
  AND sol.estado_solicitud = 'ACEPTADA';

INSERT INTO mensajes (id_chat, id_remitente, contenido, estado_mensaje)
SELECT
    (SELECT ch.id_chat FROM chats ch
     JOIN solicitudes sol ON ch.id_solicitud = sol.id_solicitud
     JOIN usuarios uc ON sol.id_cliente = uc.id_usuario
     WHERE uc.correo = 'laura.cliente@alamano.co' AND sol.estado_solicitud = 'ACEPTADA'),
    (SELECT id_usuario FROM usuarios WHERE correo = 'laura.cliente@alamano.co'),
    'Hola María, gracias por aceptar. ¿A qué hora podrías pasar hoy?',
    'LEIDO';

INSERT INTO mensajes (id_chat, id_remitente, contenido, estado_mensaje)
SELECT
    (SELECT ch.id_chat FROM chats ch
     JOIN solicitudes sol ON ch.id_solicitud = sol.id_solicitud
     JOIN usuarios uc ON sol.id_cliente = uc.id_usuario
     WHERE uc.correo = 'laura.cliente@alamano.co' AND sol.estado_solicitud = 'ACEPTADA'),
    (SELECT id_usuario FROM usuarios WHERE correo = 'maria.plomera@alamano.co'),
    'Hola Laura, llego en aproximadamente 35 minutos con los repuestos.',
    'ENVIADO';

-- 3. Solicitud FINALIZADA con chat y resena (Carlos -> Juan, Gas)
INSERT INTO solicitudes (
    id_cliente, id_profesional, id_servicio, descripcion,
    estado_solicitud, es_urgente, precio_estimado, precio_final,
    precio_final_confirmado, fecha_finalizacion, version
)
SELECT
    (SELECT id_usuario FROM usuarios WHERE correo = 'carlos.cliente@alamano.co'),
    (SELECT id_usuario FROM usuarios WHERE correo = 'juan.electricista@alamano.co'),
    (SELECT s.id_servicio FROM servicios s
     JOIN usuarios u ON s.id_profesional = u.id_usuario
     JOIN categorias_oficio c ON s.id_categoria = c.id_categoria
     WHERE u.correo = 'juan.electricista@alamano.co' AND c.nombre = 'Gas'),
    'Revisión y ajuste de válvula de gas en estufa domiciliaria.',
    'FINALIZADA',
    FALSE,
    55000.00,
    55000.00,
    TRUE,
    CURRENT_TIMESTAMP,
    0;

INSERT INTO chats (id_solicitud)
SELECT sol.id_solicitud FROM solicitudes sol
JOIN usuarios uc ON sol.id_cliente = uc.id_usuario
JOIN usuarios up ON sol.id_profesional = up.id_usuario
WHERE uc.correo = 'carlos.cliente@alamano.co'
  AND up.correo = 'juan.electricista@alamano.co'
  AND sol.estado_solicitud = 'FINALIZADA';

INSERT INTO resenas (id_solicitud, id_cliente, id_profesional, estrellas, comentario)
SELECT
    sol.id_solicitud,
    sol.id_cliente,
    sol.id_profesional,
    5,
    'Excelente servicio, muy puntual y dejó la instalación segura.'
FROM solicitudes sol
JOIN usuarios uc ON sol.id_cliente = uc.id_usuario
JOIN usuarios up ON sol.id_profesional = up.id_usuario
WHERE uc.correo = 'carlos.cliente@alamano.co'
  AND up.correo = 'juan.electricista@alamano.co'
  AND sol.estado_solicitud = 'FINALIZADA';
