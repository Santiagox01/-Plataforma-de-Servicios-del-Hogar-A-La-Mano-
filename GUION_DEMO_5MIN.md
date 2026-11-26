# Guion de Demo Funcional — A La Mano (5 minutos)

Este guion cronometrado recorre el flujo completo de principio a fin (HU01–HU18) en **5 minutos**, usando el backend Spring Boot conectado a PostgreSQL (Supabase / Docker) y la app móvil Expo.

---

## Preparación previa (antes de iniciar el cronómetro)
1. Backend encendido (`mvn spring-boot:run` en `backend/`, con Flyway `V1`–`V7` aplicadas y `PAGOS_PASARELA=paypal` con credenciales Sandbox en `.env`, o `simulada` para demo offline).
2. App móvil abierta en dos emuladores o en emulador + Expo Go (o cambiando de sesión con cuentas semilla/nuevas).
3. Ten listo un archivo PDF/imagen de prueba en el celular/emulador para el documento de identidad.

---

## Minuto 0:00 – 0:50 | Registro de profesional y verificación administrativa (HU02, HU03, HU04)
1. **[0:00 – 0:25] Registro del técnico (`CreadorProfesional` Factory Method):**
   - En la pantalla de inicio, toca **Registrarme como Profesional**.
   - Ingresa un técnico nuevo (ej. `Mateo Ríos`, correo `mateo.demo@alamano.co`, celular `3158889900`, clave `DemoPro1`, zona `El Poblado`, categoría `Electricidad`, adjunta documento PDF y acepta términos).
   - Destaca en voz alta: la cuenta nace en estado `PENDIENTE_VERIFICACION` y `disponible = false` (`aviso` inmediato en pantalla).
2. **[0:25 – 0:50] Verificación por el Administrador (Command + Auditoría + URL firmada):**
   - Inicia sesión como Admin (`admin@alamano.co` / `Admin1234`).
   - En **Verificación de Identidad**, abre el expediente de `Mateo Ríos`, pulsa **Ver documento de identidad** (enlace HMAC temporal privado `inline`) y presiona **Aprobar profesional**.
   - Entra brevemente como `mateo.demo@alamano.co`, activa **Disponible para recibir solicitudes** y entra a **Configurar tarifas por categoría** (`Tarifa base = 50000`, `Mín = 40000`, `Máx = 60000`).

---

## Minuto 0:50 – 1:45 | Búsqueda georreferenciada, perfil y cotización Strategy (HU05, HU06, HU08, HU09)
1. **[0:50 – 1:15] Búsqueda por cercanía Haversine:**
   - Inicia sesión como Cliente (`carlos.cliente@alamano.co` / `Cliente123`).
   - En **Buscar Profesionales**, selecciona **Electricidad**, zona **El Poblado**, radio `10 km` → **Buscar profesionales verificados**.
   - Abre el perfil de **Mateo Ríos** (o **Juan Pérez**): señala el sello `Verificado`, calificación y comentarios reales (`IConsultaReputacion`).
2. **[1:15 – 1:45] Solicitud + cotización en vivo (`ITarificacionStrategy`):**
   - Toca **Solicitar servicio**, escribe `"Cortocircuito en toma de cocina"`.
   - Muestra el precio estimado estándar (`$50.000 COP`) y activa el switch **¿Requiere atención urgente?** → recalcula en tiempo real desde el backend con factor `1.30` acotado al máximo configurado (`$60.000 COP`, regla `PU-01`).
   - Presiona **Enviar solicitud** (`PENDIENTE`).

---

## Minuto 1:45 – 2:35 | Aceptación concurrente, chat 2 s y precio final (HU07, HU10, HU11)
1. **[1:45 – 2:10] Aceptación (`State` + `@Version`) y Chat (`IMensajeriaChat`):**
   - Cambia a sesión del profesional, abre **Bandeja de solicitudes** → selecciona la solicitud → toca **Aceptar solicitud** (`→ ACEPTADA`).
   - Abre **Abrir chat de coordinación**, envía un mensaje (`"Voy en camino con el multímetro"`).
2. **[2:10 – 2:35] Registro y confirmación de precio final:**
   - En el mismo **Detalle de Solicitud** (profesional), registra precio final `58000` (`inmutable`).
   - En sesión de Carlos (cliente), abre la solicitud, mira el chat actualizado en ≤ 2 s (`LEIDO`) y presiona **Confirmar precio final** (`→ FINALIZADA`).

---

## Minuto 2:35 – 3:45 | Pago en custodia Escrow y liberación de fondos (HU13, HU14)
1. **[2:35 – 3:15] Pago en custodia (`IPasarelaPagoRedireccion` / `PayPalAdapter` + `TarifaConfig`):**
   - Toca **Pagar servicio en custodia** en el detalle de la solicitud.
   - Muestra el desglose del servidor en **COP y su equivalente en USD** (`$58.000` servicio + `$3.000` garantía fija = `$61.000 COP`, convertido con `tasaCambioCopUsd`).
   - Con `PAGOS_PASARELA=paypal`, toca **Pagar con PayPal**: la app abre PayPal Sandbox en `expo-web-browser`, apruebas con la **cuenta Buyer Sandbox** (`intent = AUTHORIZE`), regresa por deep link (`alamano://paypal-retorno`) y el backend valida/autoriza la orden en PayPal. *(Si ejecutas en modo offline `simulada`, elige `VISA •••• 4242` y toca **Pagar con custodia**).*
   - Señala el resultado: `Pago RETENIDO`, `Solicitud PAGADA` y **Comprobante de pago** estructurado con monto COP, monto USD y tasa aplicada visible para cliente y técnico.
2. **[3:15 – 3:45] Confirmación de satisfacción y liberación:**
   - Toca **Confirmar servicio recibido** (`Solicitud.puedeLiberarCustodia() == true`) → `PayPalAdapter` ejecuta `capture` sobre la autorización (o `PasarelaSimulada`), el pago pasa a `LIBERADO` y el profesional recibe la notificación `PAGO_LIBERADO`.

---

## Minuto 3:45 – 4:15 | Reseña y actualización inmediata de reputación (HU12)
- En **Detalle de Solicitud**, Carlos pulsa **Calificar servicio (Reseña)**, elige `★ 5`, agrega `"Puntual y seguro"`, envía y muestra que el botón desaparece (1 reseña por solicitud) y el promedio en el perfil público (`HU06`) se actualizó de inmediato.

---

## Minuto 4:15 – 5:00 | Reporte mutuo, reincidencia y resolución inmutable (HU15, HU16, HU17, HU18)
1. **[4:15 – 4:35] Reporte mutuo y anonimato:**
   - Desde una solicitud (ej. la semilla `ACEPTADA` de `laura.cliente@alamano.co` → `maria.plomera@alamano.co` o una nueva sobre `Mateo`), toca **Reportar profesional**, elige motivo y envía.
   - Destaca: si el técnico ya tiene un reporte previo (`V6` para María o un segundo reporte), el sistema marca `esPrioritario = true`. El reportado en **Ayuda y soporte → Reportes recibidos** ve el caso **sin identidad del reportante**.
2. **[4:35 – 5:00] Moderación polimórfica + Soporte:**
   - Entra como Admin (`admin@alamano.co` / `Admin1234`) → **Cola de reportes (Moderación)** (prioritarios arriba).
   - Abre el caso prioritario, selecciona **Suspensión temporal** (`7` días), escribe justificación y presiona **Confirmar resolución**.
   - Muestra brevemente **Bandeja de soporte** (ticket semilla `SUP-2026-SEED01` + casos generados automáticamente desde `EN_DISPUTA`).
   - Cierra demostrando que `Mateo`/`María` suspendido recibe el aviso de restricción al intentar iniciar sesión (`HTTP 403`) y desaparece de búsquedas.
