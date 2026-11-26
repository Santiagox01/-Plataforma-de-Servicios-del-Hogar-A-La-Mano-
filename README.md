# A La Mano — Plataforma de Servicios del Hogar (Medellín)

Monorepo de la implementación funcional completa de **A La Mano** (Análisis y Diseño de Sistemas II — Universidad de Antioquia), cubriendo **HU01 a HU18** en 3 sprints incrementales.

## Alcance implementado
- **Sprint 1 (HU01 – HU06):** Registro diferenciado (`CreadorUsuario`), verificación documental de profesionales con auditoría (`IComandoAdministrativo`) y URLs firmadas privadas (`IAlmacenamientoArchivos`), inicio/cierre de sesión con JWT, revocación de tokens y bloqueo por intentos fallidos, búsqueda georreferenciada por categoría y cercanía (Haversine) y perfil público verificado.
- **Sprint 2 (HU07 – HU12):** Configuración de tarifas por categoría (`Servicio.configurarTarifa`), cotización dinámica en tiempo real con patrón **Strategy** (`ITarificacionStrategy`), solicitudes con hasta 3 fotos, máquina de estados **State** (`IEstadoSolicitudState`) + `@Version` contra doble aceptación concurrente, registro inmutable/confirmación/disputa de precio final, chat REST con polling cada 2 s detrás de `IMensajeriaChat`, y reseñas que recalculan la reputación real del profesional (`ConsultaReputacionJpaAdaptador`).
- **Sprint 3 (HU13 – HU18 + PayPal Sandbox):**
  - Pagos en custodia (escrow) con **PayPal REST API v2 en modo Sandbox** (`PayPalAdapter` implementando `IPasarelaPagoRedireccion` / `IPasarelaPagos`, OAuth2 client credentials cacheado, conversión automática configurable COP → USD, `intent=AUTHORIZE` → `RETENIDO`, captura → `LIBERADO`, anulación `void` o reembolso `refund` → `REEMBOLSADO`), además de `PasarelaSimulada` para pruebas offline y el esqueleto `WompiAdapter`.
  - Métodos de pago sin almacenamiento de datos sensibles (`PAYPAL` cuando la pasarela es PayPal; tarjetas tokenizadas solo en modo `simulada`).
  - Reportes mutuos anónimos con prioridad automática por reincidencia, cola de moderación con sanciones polimórficas (`ISancionable`: `SancionCliente`, `SancionProfesional`), suspensión temporal/desactivación verificada al iniciar sesión, resolución inmutable auditada y módulo de soporte con tickets únicos (`CasoSoporte`) enlazado automáticamente desde disputas de precio final.

---

## Estructura del monorepo

```text
├── backend/                  # API REST Java 17 + Spring Boot 3 + Flyway (V1–V7) + JPA
│   └── Dockerfile            # Imagen Docker multi-stage (Maven build + JRE 17 no-root)
├── mobile/                   # App móvil React Native (Expo) sin lógica de negocio
├── api-tests/a-la-mano.http  # Colección completa de pruebas REST (HU01–HU18 + PayPal)
├── docker-compose.yml        # Stack completo PostgreSQL 16 + Backend Spring Boot
├── .env.example              # Plantilla de variables de entorno (sin secretos reales)
├── CAMBIOS_DISENO.md         # Registro de desviaciones y diagrama PlantUML final
└── GUION_DEMO_5MIN.md        # Guion cronometrado de demostración integral (5 min)
```

> Las carpetas `a-la-mano-app/`, `a-la-mano-movil/` y `sustentacion/` corresponden a entregas anteriores y permanecen intactas.

---

## Requisitos previos

- **Java 17+** (verificado con OpenJDK 21 compilando `--release 17`)
- **Apache Maven 3.9+**
- **Node.js 20+** y **npm**
- Base de datos **PostgreSQL** en **Supabase**, o **Docker Desktop** con `docker-compose.yml`
- Cuenta en **[PayPal Developer](https://developer.paypal.com/)** (modo Sandbox) para pagos reales de prueba
- App **Expo Go** en celular Android/iOS (o emulador Android)

---

## 1. Configuración de variables de entorno (`.env`)

1. Copia `.env.example` a `.env` en la raíz del repositorio:
   ```powershell
   Copy-Item .env.example .env
   ```
2. Configura **Supabase PostgreSQL**:
   ```dotenv
   SPRING_PROFILES_ACTIVE=local
   SUPABASE_DB_URL=jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres?sslmode=require
   SUPABASE_DB_USER=postgres.<tu_project_ref>
   SUPABASE_DB_PASSWORD=<tu_password_de_postgres>
   ```
3. Para activar **PayPal Sandbox** (`PAGOS_PASARELA=paypal`):
   - **Crear app Sandbox:** ingresa a [developer.paypal.com](https://developer.paypal.com/) → **Apps & Credentials** → pestaña **Sandbox** → **Create App** (nombre ej. `A La Mano Dev`, tipo **Merchant**). Copia **Client ID** y **Secret key** en `.env`:
     ```dotenv
     PAGOS_PASARELA=paypal
     PAYPAL_CLIENT_ID=<tu_client_id_sandbox>
     PAYPAL_CLIENT_SECRET=<tu_client_secret_sandbox>
     PAYPAL_BASE_URL=https://api-m.sandbox.paypal.com
     PAYPAL_RETURN_URL=http://<TU_IP_O_LOCALHOST>:8080/api/pagos/paypal/retorno
     PAYPAL_CANCEL_URL=http://<TU_IP_O_LOCALHOST>:8080/api/pagos/paypal/cancelado
     PAYPAL_TASA_CAMBIO_COP_USD=4000.00
     ```
   - **Cuenta compradora Sandbox (Buyer):** en el mismo panel ve a **Testing Tools → Sandbox Accounts** y ubica la cuenta de tipo **Personal** (o crea una nueva con país **US** o **CO** y saldo inicial en USD). Usa ese correo y contraseña cuando la app abra el navegador de PayPal para aprobar el pago.
   - **Nota sobre moneda:** las operaciones estándar de PayPal REST v2 no liquidan saldo en COP, por lo que el backend convierte `totalCOP / PAYPAL_TASA_CAMBIO_COP_USD` a `USD` (2 decimales), guarda ambos montos y muestra COP + USD en la app y en el comprobante.
4. Si pruebas desde un **celular físico con Expo Go**, usa tu IPv4 LAN (ej. `192.168.20.143`) en `APP_PUBLIC_BASE_URL`, `PAYPAL_RETURN_URL`, `PAYPAL_CANCEL_URL`, `CORS_ALLOWED_ORIGINS` y `EXPO_PUBLIC_API_URL`.

> **Importante:** nunca versiones `.env` ni pegues credenciales reales de PayPal o Supabase en archivos del repositorio.

---

## 2. Levantar el Backend

### Opción A: Ejecución directa con Maven (conectado a Supabase o Postgres local)
```powershell
cd backend
mvn spring-boot:run
```
Al iniciar, Flyway ejecutará automáticamente las 7 migraciones portables `V1`–`V7`.

### Opción B: Contenedores con Docker Compose (`postgres` + `backend`)
```powershell
docker compose up --build -d
```
El backend quedará expuesto en `http://localhost:8080` conectado al contenedor `a-la-mano-postgres` en el puerto `5432`.

### Pruebas automatizadas y compilación del JAR (H2 en modo PostgreSQL + `MockWebServer`)
El perfil `test` fuerza `app.pagos.pasarela=simulada` para las pruebas de integración y ejecuta las pruebas unitarias/de servicio de `PayPalAdapter` contra `MockWebServer` sin requerir red ni credenciales:
```powershell
mvn -f backend/pom.xml clean test
mvn -f backend/pom.xml package -DskipTests
```

---

## 3. Levantar la App Móvil (React Native / Expo)

La URL base del API se configura en un único archivo: `mobile/src/config/apiConfig.js`.

### Emulador Android (`http://10.0.2.2:8080` por defecto)
```powershell
cd mobile
npm install
npx expo start --android
```

### Celular físico con Expo Go (usando la IP local de tu PC)
1. Consulta la IPv4 de tu PC en Wi-Fi con `ipconfig` y permite el puerto `8080` en el firewall de Windows.
2. Inicia Expo apuntando a tu PC:
   ```powershell
   cd mobile
   npm install
   $env:EXPO_PUBLIC_API_URL="http://192.168.20.143:8080"
   npx expo start
   ```
3. Escanea el código QR con **Expo Go** en la misma red Wi-Fi.

---

## 4. Pasarelas disponibles (`PAGOS_PASARELA`)

- **`paypal` (Pasarela real MVP en Sandbox):** al pulsar **Pagar con PayPal**, el backend crea una orden en USD (`intent=AUTHORIZE`) y abre PayPal Sandbox con `expo-web-browser`. Tras aprobar con la cuenta compradora Sandbox, vuelve a la app y el backend verifica el estado en PayPal, autoriza (`RETENIDO`) y permite capturar cuando el cliente confirma servicio recibido (`LIBERADO`). Si un administrador resuelve reembolsar antes de capturar ejecuta `void`, o `refund` si ya fue capturado.
- **`simulada` (MVP offline):** tarjetas de prueba sin red externa:
  - Terminada en `4242`: aprueba registro y cobro (`RETENIDO`).
  - Terminada en `0002`: aprueba registro, rechaza cobro por `"Fondos insuficientes"`.
  - Terminada en `0001` o vencida: rechazada al registrar.
- **`wompi`:** esqueleto documentado (`WompiAdapter`) para futura integración productiva en Colombia.

---

## 5. Credenciales y datos semilla (`V2` + `V4` + `V6`)

| Rol | Correo | Contraseña | Estado / Datos semilla |
| :--- | :--- | :--- | :--- |
| **Administrador** | `admin@alamano.co` | `Admin1234` | Cola de verificación, reportes, auditoría y soporte |
| **Cliente 1** | `carlos.cliente@alamano.co` | `Cliente123` | El Poblado • Tarjeta `VISA •••• 4242` • Caso `SUP-2026-SEED01` |
| **Cliente 2** | `laura.cliente@alamano.co` | `Cliente123` | Laureles • Tarjeta `MASTERCARD •••• 4444` • Solicitud `PAGADA` (`RETENIDO`) |
| **Profesional 1** | `juan.electricista@alamano.co` | `Profesional1` | `VERIFICADO`, `disponible=true`, Electricidad y Gas (El Poblado) |
| **Profesional 2** | `maria.plomera@alamano.co` | `Profesional1` | `VERIFICADO`, `disponible=true`, Plomería y Cerrajería (Laureles) |
| **Profesional 3** | `andres.carpintero@alamano.co` | `Profesional1` | `VERIFICADO`, `disponible=false`, Carpintería (Belén) |
| **Profesional 4** | `pedro.cerrajero@alamano.co` | `Profesional1` | `PENDIENTE_VERIFICACION`, `disponible=false`, Cerrajería (Envigado) |

Categorías semilla: `Electricidad`, `Plomería`, `Gas`, `Cerrajería`, `Carpintería`.

---

## 6. Pruebas HTTP y Guion de Demo
- Colección completa de endpoints (Sprint 1 a 3 + PayPal): [api-tests/a-la-mano.http](api-tests/a-la-mano.http).
- Guion paso a paso de demostración de 5 minutos: [GUION_DEMO_5MIN.md](GUION_DEMO_5MIN.md).
