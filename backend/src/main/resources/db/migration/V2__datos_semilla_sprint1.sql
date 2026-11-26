-- Datos semilla Sprint 1 (SQL portable sin IDs quemados)
-- Contrasenas de prueba:
--   Admin:        Admin1234
--   Clientes:     Cliente123
--   Profesionales: Profesional1

INSERT INTO categorias_oficio (nombre, descripcion) VALUES
    ('Electricidad', 'Instalaciones eléctricas residenciales, tableros, tomas y cortocircuitos.'),
    ('Plomería', 'Reparación de fugas, tuberías, grifería, sanitarios y desagües.'),
    ('Gas', 'Revisión de redes domiciliarias de gas, estufas y calentadores.'),
    ('Cerrajería', 'Apertura de puertas, cambio de guardas y cerraduras de seguridad.'),
    ('Carpintería', 'Reparación e instalación de puertas, closets, muebles y madera.');

-- 1 Administrador
INSERT INTO usuarios (nombre, correo, celular, contrasena, rol, estado_cuenta, intentos_fallidos)
VALUES (
    'Administración A La Mano',
    'admin@alamano.co',
    '3000000001',
    '$2a$10$KJTtgL9asc2axLegNAs5pOaV.S0.dCaviTzgxDbJG2DBkA/6OoAbu',
    'ADMINISTRADOR',
    'ACTIVA',
    0
);

INSERT INTO administradores (id_usuario)
SELECT id_usuario FROM usuarios WHERE correo = 'admin@alamano.co';

-- 2 Clientes en Medellin
INSERT INTO usuarios (nombre, correo, celular, contrasena, rol, estado_cuenta, intentos_fallidos)
VALUES (
    'Carlos Ramírez',
    'carlos.cliente@alamano.co',
    '3001112233',
    '$2a$10$PTRRV6gwd/UT0XXFXKuEyeoNkhcxPjuVRjBMgOcvPwYyLGJHk2TWi',
    'CLIENTE',
    'ACTIVA',
    0
);

INSERT INTO clientes (id_usuario, ubicacion, latitud, longitud)
SELECT id_usuario, 'El Poblado, Medellín', 6.2088, -75.5695
FROM usuarios WHERE correo = 'carlos.cliente@alamano.co';

INSERT INTO usuarios (nombre, correo, celular, contrasena, rol, estado_cuenta, intentos_fallidos)
VALUES (
    'Laura Gómez',
    'laura.cliente@alamano.co',
    '3002223344',
    '$2a$10$PTRRV6gwd/UT0XXFXKuEyeoNkhcxPjuVRjBMgOcvPwYyLGJHk2TWi',
    'CLIENTE',
    'ACTIVA',
    0
);

INSERT INTO clientes (id_usuario, ubicacion, latitud, longitud)
SELECT id_usuario, 'Laureles, Medellín', 6.2447, -75.5950
FROM usuarios WHERE correo = 'laura.cliente@alamano.co';

-- 4 Profesionales (2 verificados y disponibles, 1 verificado no disponible, 1 pendiente de verificacion)
INSERT INTO usuarios (nombre, correo, celular, contrasena, rol, estado_cuenta, intentos_fallidos)
VALUES (
    'Juan Pérez',
    'juan.electricista@alamano.co',
    '3103334455',
    '$2a$10$e08QrON2CE7O1e.FyBIeWOzdqM0Ap64Ut8v90wDDycPOqKDm4c61G',
    'PROFESIONAL',
    'ACTIVA',
    0
);

INSERT INTO profesionales (id_usuario, ubicacion, latitud, longitud, documento_identidad, estado_verificacion, fecha_verificacion, id_admin_verificador, disponible)
SELECT u.id_usuario,
       'El Poblado, Medellín',
       6.2102,
       -75.5712,
       'seed/doc-juan.pdf',
       'VERIFICADO',
       CURRENT_TIMESTAMP,
       (SELECT id_usuario FROM usuarios WHERE correo = 'admin@alamano.co'),
       TRUE
FROM usuarios u WHERE u.correo = 'juan.electricista@alamano.co';

INSERT INTO profesional_certificaciones (id_profesional, ruta_archivo)
SELECT id_usuario, 'seed/conte-juan.pdf' FROM usuarios WHERE correo = 'juan.electricista@alamano.co';

INSERT INTO servicios (id_profesional, id_categoria, tarifa_base, precio_min, precio_max)
SELECT (SELECT id_usuario FROM usuarios WHERE correo = 'juan.electricista@alamano.co'),
       (SELECT id_categoria FROM categorias_oficio WHERE nombre = 'Electricidad'),
       50000.00, 40000.00, 80000.00;

INSERT INTO servicios (id_profesional, id_categoria, tarifa_base, precio_min, precio_max)
SELECT (SELECT id_usuario FROM usuarios WHERE correo = 'juan.electricista@alamano.co'),
       (SELECT id_categoria FROM categorias_oficio WHERE nombre = 'Gas'),
       55000.00, 45000.00, 90000.00;

INSERT INTO usuarios (nombre, correo, celular, contrasena, rol, estado_cuenta, intentos_fallidos)
VALUES (
    'María López',
    'maria.plomera@alamano.co',
    '3114445566',
    '$2a$10$e08QrON2CE7O1e.FyBIeWOzdqM0Ap64Ut8v90wDDycPOqKDm4c61G',
    'PROFESIONAL',
    'ACTIVA',
    0
);

INSERT INTO profesionales (id_usuario, ubicacion, latitud, longitud, documento_identidad, estado_verificacion, fecha_verificacion, id_admin_verificador, disponible)
SELECT u.id_usuario,
       'Laureles, Medellín',
       6.2460,
       -75.5915,
       'seed/doc-maria.pdf',
       'VERIFICADO',
       CURRENT_TIMESTAMP,
       (SELECT id_usuario FROM usuarios WHERE correo = 'admin@alamano.co'),
       TRUE
FROM usuarios u WHERE u.correo = 'maria.plomera@alamano.co';

INSERT INTO servicios (id_profesional, id_categoria, tarifa_base, precio_min, precio_max)
SELECT (SELECT id_usuario FROM usuarios WHERE correo = 'maria.plomera@alamano.co'),
       (SELECT id_categoria FROM categorias_oficio WHERE nombre = 'Plomería'),
       45000.00, 35000.00, 75000.00;

INSERT INTO servicios (id_profesional, id_categoria, tarifa_base, precio_min, precio_max)
SELECT (SELECT id_usuario FROM usuarios WHERE correo = 'maria.plomera@alamano.co'),
       (SELECT id_categoria FROM categorias_oficio WHERE nombre = 'Cerrajería'),
       40000.00, 30000.00, 65000.00;

INSERT INTO usuarios (nombre, correo, celular, contrasena, rol, estado_cuenta, intentos_fallidos)
VALUES (
    'Andrés Torres',
    'andres.carpintero@alamano.co',
    '3125556677',
    '$2a$10$e08QrON2CE7O1e.FyBIeWOzdqM0Ap64Ut8v90wDDycPOqKDm4c61G',
    'PROFESIONAL',
    'ACTIVA',
    0
);

INSERT INTO profesionales (id_usuario, ubicacion, latitud, longitud, documento_identidad, estado_verificacion, fecha_verificacion, id_admin_verificador, disponible)
SELECT u.id_usuario,
       'Belén, Medellín',
       6.2312,
       -75.6050,
       'seed/doc-andres.pdf',
       'VERIFICADO',
       CURRENT_TIMESTAMP,
       (SELECT id_usuario FROM usuarios WHERE correo = 'admin@alamano.co'),
       FALSE
FROM usuarios u WHERE u.correo = 'andres.carpintero@alamano.co';

INSERT INTO servicios (id_profesional, id_categoria, tarifa_base, precio_min, precio_max)
SELECT (SELECT id_usuario FROM usuarios WHERE correo = 'andres.carpintero@alamano.co'),
       (SELECT id_categoria FROM categorias_oficio WHERE nombre = 'Carpintería'),
       60000.00, 50000.00, 95000.00;

INSERT INTO usuarios (nombre, correo, celular, contrasena, rol, estado_cuenta, intentos_fallidos)
VALUES (
    'Pedro Restrepo',
    'pedro.cerrajero@alamano.co',
    '3136667788',
    '$2a$10$e08QrON2CE7O1e.FyBIeWOzdqM0Ap64Ut8v90wDDycPOqKDm4c61G',
    'PROFESIONAL',
    'ACTIVA',
    0
);

INSERT INTO profesionales (id_usuario, ubicacion, latitud, longitud, documento_identidad, estado_verificacion, disponible)
SELECT u.id_usuario,
       'Envigado, Valle de Aburrá',
       6.1719,
       -75.5863,
       'seed/doc-pedro.pdf',
       'PENDIENTE_VERIFICACION',
       FALSE
FROM usuarios u WHERE u.correo = 'pedro.cerrajero@alamano.co';

INSERT INTO servicios (id_profesional, id_categoria)
SELECT (SELECT id_usuario FROM usuarios WHERE correo = 'pedro.cerrajero@alamano.co'),
       (SELECT id_categoria FROM categorias_oficio WHERE nombre = 'Cerrajería');
