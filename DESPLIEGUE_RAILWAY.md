# Despliegue del Backend en Railway — A La Mano

Documentación operativa del despliegue en producción (entorno académico / Sandbox) del backend de **A La Mano** (`backend/`, Java 17 + Spring Boot 3 + Flyway + PostgreSQL) en **Railway**.

---

## 1. URL pública y recursos desplegados

- **URL pública del backend:** `https://plataforma-de-servicios-del-hogar-a-la-mano-production.up.railway.app`
- **Repositorio GitHub conectado:** [`Santiagox01/-Plataforma-de-Servicios-del-Hogar-A-La-Mano-`](https://github.com/Santiagox01/-Plataforma-de-Servicios-del-Hogar-A-La-Mano-) (rama `main`; la entrega anterior quedó respaldada en la rama `entrega-anterior`).
- **Proyecto / Entorno en Railway:** `attractive-essence` / `production`
- **Servicio Backend:** `-Plataforma-de-Servicios-del-Hogar-A-La-Mano-`
  - **Root Directory:** `/backend`
  - **Builder:** `Dockerfile` (`/backend/Dockerfile`)
  - **Perfil Spring Boot activo:** `prod` (`application-prod.yml`)
  - **Puerto HTTP interno / Tomcat:** `8080` (inyectado vía `${PORT:8080}`)
- **Base de datos:** servicio **`Postgres`** en Railway (PostgreSQL 18.7 sobre red privada `postgres.railway.internal:5432/railway`, con volumen propio `postgres-volume` y 7 migraciones Flyway `V1`–`V7` aplicadas).
- **Almacenamiento persistente de archivos privados:** Volume de Railway montado en **`/data/uploads`** (`STORAGE_TIPO=local`, `LocalDiskStorageAdapter`), gestionado al arranque por `backend/docker-entrypoint.sh` con `su-exec` para compatibilidad de permisos con el usuario no-root `alamano`.

---

## 2. Resumen del proceso ejecutado

1. **Preparación del código (Paso 0):**
   - Ajuste de `server.port: ${PORT:${SERVER_PORT:8080}}` en `backend/src/main/resources/application.yml`.
   - Creación de `backend/src/main/resources/application-prod.yml` leyendo BD, seguridad JWT, firma HMAC, almacenamiento, CORS, URL pública y PayPal exclusivamente desde variables de entorno (sin secretos embebidos ni carga de `.env`).
   - Actualización de `backend/Dockerfile` + `backend/docker-entrypoint.sh` + `backend/.dockerignore` con perfil por defecto `prod` y manejo seguro de permisos del Volume con `su-exec`.
   - Creación de `.gitignore` en la raíz excluyendo `.env`, `node_modules/`, `target/`, `uploads/`, `.expo/`, carpetas legadas y borradores; barrido de secretos y `mvn -f backend/pom.xml clean test -B` (`29/29` en verde).
2. **Repositorio en GitHub:**
   - Respaldo del commit previo (`ce80092`) en `origin/entrega-anterior` y actualización *fast-forward* de `origin/main` sin `--force`.
3. **Configuración en Railway (Pasos 1–4):**
   - Creación de proyecto desde GitHub (`Root Directory = /backend`, builder `Dockerfile`).
   - Agregado del plugin **PostgreSQL** (`Postgres`) y enlace privado con `DB_URL`, `DB_USER` y `DB_PASSWORD` mediante referencias `${{Postgres.*}}`.
   - Creación del **Volume** persistente montado en `/data/uploads`.
   - Carga de las variables de entorno (ver sección siguiente, **sin valores secretos en este documento**).
4. **Despliegue, dominio público y redespliegue (Paso 5):**
   - Primer deploy verificando `Postgres` `Online`, Flyway `V1`–`V7` aplicadas en esquema `public` (versión `7`) y `Started ALaManoApplication` en el puerto `8080`.
   - Generación de dominio público en **Settings → Networking → Public Networking → Generate Domain**.
   - Actualización de `APP_PUBLIC_BASE_URL`, `PAYPAL_RETURN_URL`, `PAYPAL_CANCEL_URL` y `CORS_ALLOWED_ORIGINS` con el dominio `https://....up.railway.app` y segundo deploy.
5. **Verificación en vivo (Paso 6):**
   - `GET /api/categorias` → HTTP `200`, 5 categorías (`Electricidad`, `Plomería`, `Gas`, `Cerrajería`, `Carpintería`).
   - `POST /api/auth/login` (`admin@alamano.co`) → HTTP `200`, token JWT emitido (`ROL: ADMINISTRADOR`).
   - `GET /api/admin/profesionales/pendientes` (autenticado) → HTTP `200`.
   - `GET /api/profesionales/buscar?categoriaId=1&latitud=6.2086&longitud=-75.5659&radioKm=15` → HTTP `200`, profesionales semilla verificados devueltos (`Juan Pérez`).
   - **Prueba del Volume persistente tras redeploy:** subida de PDF de prueba vía `POST /api/auth/registro/profesional` (`idUsuario=8`), descarga por URL firmada temporal (`HTTP 200`, `application/pdf`, `49 bytes`, `SHA-256` verificado), ejecución de **Redeploy** (`5f83535c`) manteniendo el mismo volumen montado, y nueva descarga confirmando **HTTP `200` e idéntico hash SHA-256**.
   - Auditoría de **Deploy Logs**: cero secretos, contraseñas, tokens JWT ni números de tarjeta registrados.

---

## 3. Variables de entorno configuradas en Railway (solo nombres)

> Por seguridad, ningún valor secreto se documenta aquí. Los valores sensibles deben consultarse o rotarse únicamente desde **Railway → Service → Variables**.

| Variable | Propósito | Origen / Referencia |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Activa `application-prod.yml` | `prod` |
| `DB_URL` | Cadena JDBC privada de PostgreSQL en Railway | `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}` |
| `DB_USER` | Usuario de base de datos PostgreSQL | `${{Postgres.PGUSER}}` |
| `DB_PASSWORD` | Contraseña privada de PostgreSQL | `${{Postgres.PGPASSWORD}}` |
| `JWT_SECRET` | Clave secreta para firmar/validar JWT | Secreto aleatorio (CSPRNG 64 chars) |
| `JWT_EXPIRATION_MS` | Vigencia del token JWT en milisegundos | `86400000` (24 h) |
| `AUTH_MAX_INTENTOS_FALLIDOS` | Límite de intentos antes de bloqueo (HU04) | `5` |
| `AUTH_BLOQUEO_MINUTOS` | Duración del bloqueo temporal de cuenta | `15` |
| `STORAGE_TIPO` | Adaptador activo de `IAlmacenamientoArchivos` | `local` |
| `STORAGE_LOCAL_DIR` | Punto de montaje del Volume persistente | `/data/uploads` |
| `STORAGE_FIRMA_SECRET` | Secreto HMAC-SHA256 para URLs temporales privadas | Secreto aleatorio distinto al JWT (64 chars) |
| `STORAGE_URL_EXPIRACION_SEGUNDOS` | Vigencia de enlaces firmados de documentos | `300` (5 min) |
| `APP_PUBLIC_BASE_URL` | Base URL usada al generar URLs firmadas temporales | URL pública de Railway (`https://....up.railway.app`) |
| `CORS_ALLOWED_ORIGINS` | Orígenes HTTP autorizados por Spring Security | Dominio Railway + orígenes locales Expo (`8081`, `19006`, `10.0.2.2`) |
| `PAGOS_PASARELA` | Implementación activa de `IPasarelaPagos` | `paypal` |
| `PAYPAL_CLIENT_ID` | Identificador público de la app PayPal Sandbox | Panel PayPal Developer (Sandbox) |
| `PAYPAL_CLIENT_SECRET` | Secreto privado de la app PayPal Sandbox | Panel PayPal Developer (Sandbox) |
| `PAYPAL_BASE_URL` | Endpoint base de PayPal REST API v2 | `https://api-m.sandbox.paypal.com` |
| `PAYPAL_RETURN_URL` | URL de retorno al aprobar orden PayPal | `https://....up.railway.app/api/pagos/paypal/retorno` |
| `PAYPAL_CANCEL_URL` | URL de retorno al cancelar pago PayPal | `https://....up.railway.app/api/pagos/paypal/cancelado` |
| `PAYPAL_TASA_CAMBIO_COP_USD` | Tasa configurable de conversión COP → USD | `4000.00` |
| `MODERACION_DIAS_SUSPENSION_DEFECTO` | Días de suspensión por defecto en moderación | `7` |

---

## 4. Configurar y ejecutar la app móvil Expo contra Railway

La app móvil lee la URL base desde un único punto en [`mobile/src/config/apiConfig.js`](mobile/src/config/apiConfig.js) mediante la variable `EXPO_PUBLIC_API_URL`.

Desde PowerShell en `c:\Users\santy\OneDrive\Escritorio\Analisis2\mobile`:

```powershell
$env:EXPO_PUBLIC_API_URL="https://plataforma-de-servicios-del-hogar-a-la-mano-production.up.railway.app"
npx expo start
```

Para abrir directamente en emulador Android:

```powershell
$env:EXPO_PUBLIC_API_URL="https://plataforma-de-servicios-del-hogar-a-la-mano-production.up.railway.app"
npx expo start --android
```

*(Si prefieres no escribir la variable cada vez, crea un archivo local ignorado por Git en `mobile/.env` con `EXPO_PUBLIC_API_URL=https://plataforma-de-servicios-del-hogar-a-la-mano-production.up.railway.app`).*

---

## 5. Operación: cómo ver logs y redesplegar

- **Ver logs en Railway:**
  1. Entra al proyecto `attractive-essence` en [railway.com](https://railway.com) → selecciona el servicio `-Plataforma-de-Servicios-del-Hogar-A-La-Mano-`.
  2. Abre la pestaña **Deployments** y haz clic sobre el despliegue activo (`Active`) para inspeccionar:
     - **Build Logs:** compilación Maven (`mvn package -DskipTests -B`) y construcción de la imagen Docker.
     - **Deploy Logs:** arranque de Spring Boot, validación de Flyway (`V1`–`V7`), conexiones de HikariCP y errores HTTP en ejecución.
  - Asegúrate de que nunca se impriman credenciales en los logs.

- **Cómo redesplegar:**
  - **Automático por código:** al hacer `git push origin main` con cambios en `backend/`, Railway reconstruye y despliega automáticamente.
  - **Manual (sin cambios en Git, o tras editar variables):** en **Deployments**, abre el menú **`⋮`** del despliegue más reciente y selecciona **Redeploy** (o **Apply changes → Deploy** si editaste variables).

---

## 6. Limitaciones conocidas

1. **Plan Trial de Railway:** el proyecto corre sobre el plan `Trial`, sujeto a cuota temporal de créditos, memoria y disponibilidad del plan gratuito de prueba.
2. **Almacenamiento en disco y réplicas únicas:** el Volume `/data/uploads` solo puede montarse en **1 réplica simultánea**; si en el futuro se escala horizontalmente a múltiples instancias, debe migrarse `STORAGE_TIPO` a almacenamiento de objetos compartido (ej. `supabase` Storage / S3).
3. **PayPal en modo Sandbox y moneda:** opera contra `https://api-m.sandbox.paypal.com` convirtiendo COP a USD con `PAYPAL_TASA_CAMBIO_COP_USD`. Las autorizaciones PayPal (`intent=AUTHORIZE`, estado `RETENIDO`) caducan por política de PayPal tras ~29 días si no se capturan (`LIBERADO`) o anulan antes (deuda técnica documentada en `CAMBIOS_DISENO.md`).
4. **Registro de prueba en la base desplegada:** durante la verificación de persistencia del Volume quedó registrado un profesional de prueba pendiente de verificación (`idUsuario=8`, correo `prueba.volumen.1795714090@alamano.co`); puede dejarse sin aprobar o rechazarse desde el panel de administración sin afectar el sistema.

---

## 7. Problemas encontrados y cómo se resolvieron

| # | Problema encontrado | Causa raíz | Solución aplicada |
| :- | :--- | :--- | :--- |
| 1 | **Repositorio GitHub previo con archivos de entrega anterior (`ce80092`) y sin `.gitignore` raíz** | `origin/main` contenía la app legada (`a-la-mano-app`) y la carpeta local mezclaba artefactos y logs históricos con `.env`. | Se creó `.gitignore` excluyendo `.env`, `target/`, `node_modules/`, `uploads/` y carpetas legadas; se respaldó `ce80092` en `origin/entrega-anterior` y se creó el commit `9be6fcf` con `git commit-tree` sobre el árbol limpio de 273 archivos para actualizar `main` sin `--force`. |
| 2 | **Permisos de escritura en Railway Volume con usuario no-root (`alamano`)** | Railway monta los volúmenes persistentes como `root:root` en tiempo de ejecución; ejecutar `USER alamano` directo en `Dockerfile` causaría `AccessDeniedException` en `/data/uploads`. | Se agregó `backend/docker-entrypoint.sh` usando `su-exec` para crear/asignar `chown alamano:alamano` sobre `$STORAGE_LOCAL_DIR` antes de bajar a `alamano` e iniciar la JVM, normalizando CRLF→LF en `backend/Dockerfile`. |
| 3 | **Puerto dinámico asignado por Railway** | `application.yml` solo leía `SERVER_PORT`, ignorando `PORT` que inyecta Railway. | Se cambió a `server.port: ${PORT:${SERVER_PORT:8080}}` y se creó `application-prod.yml` usando `${PORT:8080}`. |
| 4 | **Credenciales locales `.env` sin formato Session Pooler IPv4 de Supabase** | `.env` local no contaba con host `*.pooler.supabase.com:5432` ni contraseña productiva configurada para conexión desde Railway. | Con aprobación del usuario, se provisionó el servicio **PostgreSQL** nativo de Railway (`Postgres`) y se enlazó por red privada mediante `DB_URL`, `DB_USER` y `DB_PASSWORD` (`${{Postgres.*}}`). |
| 5 | **Sandbox de automatización Web sin API Web Crypto** | El ejecutor de scripts del navegador no exponía `crypto.getRandomValues` para generar claves en cliente. | Se generaron `JWT_SECRET` y `STORAGE_FIRMA_SECRET` con `System.Security.Cryptography.RandomNumberGenerator` (48 bytes CSPRNG en base64url) y se cargaron en Railway sin imprimirlos en chat ni versionarlos. |
| 6 | **Dependencia circular inicial del dominio público para PayPal/CORS/URLs firmadas** | El dominio `*.up.railway.app` solo existe después del primer deploy, pero `APP_PUBLIC_BASE_URL`, `PAYPAL_RETURN_URL`, `PAYPAL_CANCEL_URL` y `CORS_ALLOWED_ORIGINS` dependen de él. | `application-prod.yml` usó valores temporales inocuos en el primer arranque; tras generar el dominio en **Networking**, se cargaron las 4 variables con la URL HTTPS definitiva y se redesplegó. |
| 7 | **Clave privada detectada en logs locales de carpeta legada** | `a-la-mano-app/.expo/dev/logs/` contenía logs antiguos con una clave secreta de Supabase. | Quedó totalmente excluida del repositorio mediante `.gitignore` (verificado `0` secretos en `git ls-files`) y se recomendó rotar la clave en Supabase. |
