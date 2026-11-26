/* ==========================================================================
   A LA MANO — ENTREGA #3 EDITORIAL KEYNOTE PRO (APP LOGIC)
   ========================================================================== */

(() => {
  'use strict';

  const urlParams = new URLSearchParams(window.location.search);
  const slideParam = parseInt(urlParams.get('slide') || '0', 10);
  const slides = Array.from(document.querySelectorAll('.slide'));
  const totalSlides = slides.length;
  let currentSlide = 0;

  /* ----------------------------------------------------------------------
     1. CANVAS CONSTELACIÓN LIVIANO (Suave en fondo oscuro, sutil en claro)
     ---------------------------------------------------------------------- */
  function initCanvasFX() {
    const canvas = document.getElementById('fx-canvas');
    if (!canvas || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    const ctx = canvas.getContext('2d');
    let w = (canvas.width = window.innerWidth);
    let h = (canvas.height = window.innerHeight);

    const mouse = { x: -9999, y: -9999 };
    window.addEventListener('pointermove', (e) => { mouse.x = e.clientX; mouse.y = e.clientY; }, { passive: true });
    window.addEventListener('resize', () => {
      w = canvas.width = window.innerWidth;
      h = canvas.height = window.innerHeight;
    });

    const nodes = Array.from({ length: 36 }, () => ({
      x: Math.random() * w,
      y: Math.random() * h,
      vx: (Math.random() - 0.5) * 0.36,
      vy: (Math.random() - 0.5) * 0.36,
      r: 1.3 + Math.random() * 1.4
    }));

    function draw() {
      ctx.clearRect(0, 0, w, h);
      const isLight = document.body.getAttribute('data-surface') === 'light';
      const dotColor = isLight ? 'rgba(15, 23, 42, 0.25)' : 'rgba(251, 113, 133, 0.36)';
      const lineColor = isLight ? 'rgba(15, 23, 42, 0.14)' : 'rgba(148, 163, 184, 0.16)';

      for (let i = 0; i < nodes.length; i++) {
        const a = nodes[i];
        a.x += a.vx; a.y += a.vy;
        if (a.x < 0 || a.x > w) a.vx *= -1;
        if (a.y < 0 || a.y > h) a.vy *= -1;
        ctx.beginPath();
        ctx.arc(a.x, a.y, a.r, 0, Math.PI * 2);
        ctx.fillStyle = dotColor;
        ctx.fill();

        for (let j = i + 1; j < nodes.length; j++) {
          const b = nodes[j];
          const d = Math.hypot(a.x - b.x, a.y - b.y);
          if (d < 130) {
            ctx.beginPath();
            ctx.moveTo(a.x, a.y);
            ctx.lineTo(b.x, b.y);
            ctx.strokeStyle = lineColor;
            ctx.globalAlpha = 1 - d / 130;
            ctx.stroke();
            ctx.globalAlpha = 1;
          }
        }
      }
      requestAnimationFrame(draw);
    }
    requestAnimationFrame(draw);
  }

  function animateValue(el, target, duration, formatter) {
    if (!el) return;
    if (document.body.classList.contains('no-anim') || window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      el.textContent = formatter(target);
      return;
    }
    const start = performance.now();
    function step(now) {
      const p = Math.min(1, (now - start) / duration);
      const eased = 1 - Math.pow(1 - p, 3);
      el.textContent = formatter(target * eased);
      if (p < 1) requestAnimationFrame(step);
    }
    requestAnimationFrame(step);
  }

  /* ----------------------------------------------------------------------
     2. DATASETS OFICIALES (PUNTOS 7, 9, 10, 11)
     ---------------------------------------------------------------------- */
  const DEBT_DATA = [
    {
      id: 'volume', x: 392, y: 72, sev: 'Alta · Escalabilidad', short: 'Volume 1-réplica',
      title: 'Volume persistente acoplado a 1 instancia',
      cause: 'LocalDiskStorageAdapter guarda archivos en /data/uploads (Volume de Railway). Aunque persiste tras redeploy, limita el backend a una sola instancia.',
      fix: 'Cambiar STORAGE_TIPO hacia SupabaseStorageAdapter o Amazon S3 compartiendo objetos entre múltiples réplicas.'
    },
    {
      id: 'paypal', x: 356, y: 122, sev: 'Alta · Pagos', short: 'PayPal 29 días',
      title: 'Expiración de autorizaciones PayPal Sandbox',
      cause: 'Las autorizaciones AUTHORIZE vencen a los 29 días (ventana recomendada de captura: 3 días) si un servicio tarda en confirmarse.',
      fix: 'Agregar Spring Scheduler para monitorear vencimientos, emitir alertas y ejecutar reautorizaciones automáticas.'
    },
    {
      id: 'relaciones', x: 186, y: 175, sev: 'Media · Modelo', short: 'Relaciones por ID',
      title: 'Relaciones Usuarios–Mensajes–Reportes por ID',
      cause: 'Las asociaciones actuales entre usuarios, mensajes y reportes operan por identificador directo sin navegación enriquecida.',
      fix: 'Incorporar índices compuestos y mapeos relacionales dedicados al crecer la concurrencia y auditoría.'
    },
    {
      id: 'tarifa', x: 164, y: 226, sev: 'Media · Reglas', short: 'Tarifa estática',
      title: 'Cálculo fijo base + recargo 30% por urgencia',
      cause: 'Liquidación actual pondera tarifa base y factor 1.30 dentro de [min, max], sin distancia geodésica ni demanda horaria.',
      fix: 'Enchufar nuevas implementaciones de ITarificacionStrategy (OCP) por zona y franja horaria.'
    }
  ];

  const PU_ITEMS = [
    { id: 'PU-01 · HU09', ms: '11ms', fn: 'CalculadorTarifa.calcularPrecioEstimado(true)', res: '$50.000 × 1.30 = $65.000 → acotado (clamp) a precioMax = $60.000 COP.' },
    { id: 'PU-02 · HU10/14', ms: '8ms', fn: 'Solicitud.puedeLiberarCustodia()', res: 'En estado EN_DISPUTA ($85.000) retorna false y bloquea liberarFondos().' },
    { id: 'PU-03 · HU02', ms: '14ms', fn: 'CreadorProfesional.crearUsuario(...)', res: 'Asigna PENDIENTE_VERIFICACION y disponible = false hasta revisión Admin.' },
    { id: 'PU-04 · HU08', ms: '7ms', fn: 'Servicio.configurarTarifaBase(45k, 50k, 40k)', res: 'Rechaza por regla invariante precioMin > precioMax antes de persistir.' }
  ];

  const PF_DATA = {
    pf01: {
      img: 'assets/pf01_chat.png',
      phoneBadge: 'Pendiente → Aceptada + Chat',
      steps: [
        { kw: '1. DADO QUE', text: 'Un cliente autenticado consulta electricistas verificados en Medellín y abre una nueva solicitud.' },
        { kw: '2. CUANDO', text: 'Envía la orden con descripción del daño y 2 fotografías válidas al profesional elegido.' },
        { kw: '3. ENTONCES', text: 'Nace en "Pendiente de respuesta"; al aceptarla el técnico pasa a "Aceptada", notifica y habilita el chat.' }
      ],
      states: ['Solicitada (2 fotos)', 'Pendiente', 'Aceptada + Chat ON'],
      fail: 'Si falla el canal en tiempo real o excede tamaño máximo de foto, alerta claro y permite reintentar sin perder la solicitud.'
    },
    pf02: {
      img: 'assets/pf02_escrow.png',
      phoneBadge: 'PayPal APPROVED → Custodia',
      steps: [
        { kw: '1. DADO QUE', text: 'Un servicio en curso tiene registrado por el técnico un valor acordado de $60.000 COP.' },
        { kw: '2. CUANDO', text: 'El cliente confirma "Finalizada" y autoriza el pago mediante PayPal Sandbox (o pasarela simulada).' },
        { kw: '3. ENTONCES', text: 'El dinero queda en custodia (RETENIDO) hasta confirmar conformidad y pasar a LIBERADO.' }
      ],
      states: ['Finalizada ($60K)', 'PayPal APPROVED', 'RETENIDO', 'LIBERADO'],
      fail: 'Ante rechazo o token expirado, informa al usuario, habilita reintento con idempotencia y evita cobros duplicados.'
    },
    pf03: {
      img: 'assets/pf03_admin.png',
      phoneBadge: 'Admin Aprueba → Catálogo',
      steps: [
        { kw: '1. DADO QUE', text: 'El técnico "Pedro" se registra en cerrajería (Envigado) en estado PENDIENTE_VERIFICACION.' },
        { kw: '2. CUANDO', text: 'El cliente busca cerrajeros (no aparece); el admin aprueba su perfil y Pedro activa disponibilidad.' },
        { kw: '3. ENTONCES', text: 'Solo tras quedar Verificado y disponible = true aparece en los resultados del catálogo.' }
      ],
      states: ['Pendiente (Oculto)', 'Admin Aprueba', 'Verificado + Disp.', 'Visible en Catálogo'],
      fail: 'Si tras aprobar no se lista, el flujo revisa el filtro combinado (verificado && disponible) antes de cachear.'
    }
  };

  const SECURITY_THREATS = [
    {
      title: '1. Acceso no autorizado / Fuerza bruta',
      badge: 'BLOQUEADO · JWT + 5 INTENTOS',
      desc: 'Tokens JWT firmados, RBAC por rol y bloqueo temporal tras 5 intentos fallidos.'
    },
    {
      title: '2. Exposición documental en nube',
      badge: 'BLOQUEADO · HMAC TTL 300s',
      desc: 'URLs firmadas HMAC-SHA256 (5 min) sobre /data/uploads bajo usuario no-root "alamano" (SHA-256 verificado tras redeploy).'
    },
    {
      title: '3. Fraude en pagos o doble cobro',
      badge: 'BLOQUEADO · PAYPAL REST v2',
      desc: 'Validación server-side APPROVED en PayPal Sandbox (AUTHORIZE); jamás guarda tarjetas ni CVV.'
    },
    {
      title: '4. Suplantación o filtración de secretos',
      badge: 'BLOQUEADO · 0 LEAKS EN GIT/LOGS',
      desc: '22 variables aisladas en Railway, logs auditados sin datos sensibles y AuditoriaAdministrativa inmutable.'
    },
    {
      title: '5. Picos de tráfico concurrente',
      badge: 'MITIGADO · POSTGRESQL 18.7',
      desc: 'Consultas indexadas, respuestas paginadas y transición trazada hacia Object Storage multi-réplica.'
    }
  ];

  const BUDGET_ROWS = [
    { rubro: 'Personal (Equipo de 4 desarrolladores)', propios: 10000000, otras: 0, tipo: 'especie', nota: 'Valorización del trabajo de desarrollo' },
    { rubro: 'Equipos (Cómputo de integrantes)', propios: 1200000, otras: 0, tipo: 'especie', nota: 'Uso de portátiles propios del equipo' },
    { rubro: 'Software (GitHub, Miro, Expo, OSS)', propios: 0, otras: 600000, tipo: 'creditos', nota: 'Planes gratuitos/educativos y Open Source' },
    { rubro: 'Materiales (Papelería, conectividad, energía)', propios: 320000, otras: 0, tipo: 'desembolso', nota: 'Gastos operativos durante desarrollo' },
    { rubro: 'Salidas de campo (Validación usuarios Medellín)', propios: 20000, otras: 0, tipo: 'desembolso', nota: 'Transporte en encuestas y pruebas locales' },
    { rubro: 'Material bibliográfico (Biblioteca UdeA / docs)', propios: 0, otras: 250000, tipo: 'creditos', nota: 'Acceso institucional y guías oficiales' },
    { rubro: 'Publicaciones y registro (DNDA Colombia)', propios: 0, otras: 0, tipo: 'gratis', nota: 'Trámite oficial sin costo' },
    { rubro: 'Servicios técnicos (Railway Trial + PayPal Sandbox)', propios: 0, otras: 350000, tipo: 'creditos', nota: 'Créditos de prueba en la nube y pasarela' },
    { rubro: 'Viajes (Alcance local Medellín)', propios: 0, otras: 0, tipo: 'gratis', nota: 'Sin desplazamientos intermunicipales' }
  ];

  const DONUT_SEGMENTS = [
    { id: 'especie', label: 'Aporte en especie (Personal + Equipos)', pct: 87.9, val: '$11.200.000 COP', color: '#059669', desc: 'Esfuerzo académico y hardware disponible.' },
    { id: 'creditos', label: 'Herramientas gratuitas / créditos', pct: 9.4, val: '$1.200.000 COP', color: '#0284c7', desc: 'Railway Trial, PayPal Sandbox, GitHub, Miro, Expo y UdeA.' },
    { id: 'operativo', label: 'Desembolso efectivo real', pct: 2.7, val: '$340.000 COP', color: '#d97706', desc: '$320.000 materiales/conectividad + $20.000 salidas de campo.' }
  ];

  const SCENARIOS = {
    academico: {
      tag: 'Etapa Actual · Entrega #3 (2026-2)',
      headline: '$340.000 COP desembolso efectivo',
      sub: 'Viabilidad financiera máxima con stack completo desplegado en Railway.',
      bullets: [
        'Total valorizado: $12.740.000 COP ($11.540.000 propios + $1.200.000 otras fuentes).',
        'El 87,9% corresponde a trabajo intelectual y equipos propios del equipo.',
        '$0 directos en infraestructura cloud gracias a Railway Trial + PayPal Sandbox.'
      ]
    },
    piloto: {
      tag: 'Corto Plazo · Piloto Comercial Medellín',
      headline: '$150.000 – $250.000 COP / mes',
      sub: 'Operación real para primeros clientes y técnicos en el Valle de Aburrá.',
      bullets: [
        'Plan Railway Hobby + PostgreSQL gestionado activo 24/7 en contenedor Docker.',
        'Dominio propio (.com / .co) estimado en ~$50.000 COP anuales.',
        'Comisiones variables por transacción liquidada en pasarela de pagos real.'
      ]
    },
    escala: {
      tag: 'Mediano Plazo · Alta Disponibilidad',
      headline: 'Multi-réplica + Object Storage',
      sub: 'Supera el límite de disco local (Deuda 7.2) mediante configuración.',
      bullets: [
        'Cambiar STORAGE_TIPO de Volume local a Supabase Storage o Amazon S3.',
        'Habilitar múltiples réplicas del backend sin reescribir reglas del dominio.',
        'Automatizar alertas y capturas PayPal antes de los 29 días de expiración.'
      ]
    }
  };

  const TERMS_GROUPS = {
    acceso: [
      { id: 1, num: 'CLÁUSULA 1', title: 'Aceptación y registro', text: 'El uso de A La Mano implica aceptación de términos; registro exclusivo para mayores de edad con datos veraces.', apply: 'Autenticación JWT y validación por rol.' },
      { id: 2, num: 'CLÁUSULA 2', title: 'Uso adecuado', text: 'Prohibida la suplantación, evasión de custodia protegida o publicación de contenido ilícito.', apply: 'Control RBAC y registro en AuditoriaAdministrativa.' }
    ],
    partes: [
      { id: 3, num: 'CLÁUSULA 3', title: 'Profesional', text: 'Aportar documentos auténticos (cédula, matrícula CONTE/SENA), respetar tarifas y ejecutar con diligencia.', apply: 'Documentos privados con URL HMAC TTL 300s.' },
      { id: 4, num: 'CLÁUSULA 4', title: 'Cliente', text: 'Consultar técnicos acreditados, describir fallas reales y confirmar cierre honestamente.', apply: 'Solo perfiles VERIFICADOS y disponibles visibles.' }
    ],
    operacion: [
      { id: 5, num: 'CLÁUSULA 5', title: 'Solicitudes y precios', text: 'Tarifa base y recargo de urgencia (+30%) informados previamente dentro del rango [mín, máx].', apply: 'Invariante garantizada en CalculadorTarifa.' },
      { id: 6, num: 'CLÁUSULA 6', title: 'Chat y reseñas', text: 'Mensajería privada habilitada solo para órdenes activas; calificaciones basadas en hechos reales.', apply: 'Chat desbloqueado tras aceptación (PF-01).' },
      { id: 7, num: 'CLÁUSULA 7', title: 'Pagos y custodia', text: 'Autorización vía pasarela autorizada sin guardar jamás números de tarjeta ni códigos CVV.', apply: 'Custodia PayPal REST v2 validada en servidor.' }
    ],
    legal: [
      { id: 8, num: 'CLÁUSULA 8', title: 'Disputas y soporte', text: 'Canal formal de reportes que bloquea liberación de fondos hasta resolver controversia.', apply: 'puedeLiberarCustodia() == false en EN_DISPUTA.' },
      { id: 9, num: 'CLÁUSULA 9', title: 'Ley 1581 / Habeas Data', text: 'Tratamiento autorizado para autenticación, verificación y trazabilidad con derecho de supresión.', apply: 'Secretos aislados y cero exposición pública.' },
      { id: 10, num: 'CLÁUSULA 10', title: 'Responsabilidades', text: 'Plataforma de intermediación tecnológica que preserva derechos del consumidor colombiano.', apply: 'Delimitación transparente entre partes.' }
    ]
  };

  const SPEAKER_NOTES = [
    { expositor: 'Inicio · Todo el equipo', title: 'Apertura Entrega #3', points: ['Saludar al docente Joaquín Molina Pérez e introducir los Puntos 7, 9, 10 y 11.', 'Mostrar la Casa A La Mano en Voxel 3D interactivo y recordar que Release v2 (Punto 8) llega el 3 de diciembre.'], qa: 'Todo el ecosistema backend ya opera vivo en Railway Docker + PostgreSQL 18.7.' },
    { expositor: 'Inicio · Hoja de Ruta', title: 'Estructura de la Defensa', points: ['Orden: Salomé (P.7) → Amy (P.9) → Santiago (P.10) → Nataly (P.11).'], qa: 'Alternancia limpia y demostraciones reales en cada punto.' },
    { expositor: 'Salomé Toro · 7.1', title: 'SOLID y Swap PayPal ↔ Wompi', points: ['Alternar el botón PayPalAdapter ↔ WompiAdapter para demostrar DIP en vivo sin tocar el dominio.', 'Citar SRP (ServicioVerificacion, CalculadorTarifa), OCP (ITarificacionStrategy) y los 5 módulos Spring Boot.'], qa: 'Cambiar de pasarela o de Storage (STORAGE_TIPO) solo requiere un nuevo adaptador.' },
    { expositor: 'Salomé Toro · 7.2', title: 'Radar de Deuda Técnica', points: ['Señalar los 2 ítems rojos de prioridad alta: Volume limitado a 1 instancia (/data/uploads) y ventana de 29 días en PayPal.'], qa: 'La solución para escalar horizontalmente es migrar STORAGE_TIPO a Supabase Storage o S3.' },
    { expositor: 'Salomé Toro · 7.3', title: 'Suite JUnit 29/29 & Invariantes', points: ['Presionar "Ejecutar Suite (29/29)" en vivo.', 'Demostrar con el medidor cómo 50.000 × 1.30 = 65.000 queda acotado a $60.000 (PU-01) y cómo EN_DISPUTA cierra el candado (PU-02).'], qa: 'Validaciones de negocio protegidas siempre en backend.' },
    { expositor: 'Salomé Toro · 7.4', title: 'Pruebas Funcionales con App Real', points: ['Alternar PF-01 (Chat), PF-02 (PayPal Sandbox Custodia) y PF-03 (Aprobación Admin) mirando las capturas reales en el celular.', 'Presionar "Simular Fallo" para explicar la idempotencia anti doble cobro.'], qa: 'Cada transición de estado se valida contra PostgreSQL 18.7.' },
    { expositor: 'Salomé Toro · 7.5', title: 'Seguridad Verificada en Railway', points: ['Girar o explotar el Faro-Bóveda Voxel 3D mientras explicas las 5 evidencias concretas: JWT+5 intentos, HMAC TTL 300s (SHA-256 validado tras redeploy), PayPal server-side sin guardar tarjetas, 0 leaks y AuditoriaAdministrativa.'], qa: 'Todo el equipo auditó variables de entorno y logs en producción.' },
    { expositor: 'Amy Herazo · 9.1 y 9.2', title: 'Equipo y Herramientas (Vista Minimalista)', points: ['Explicar oralmente la asignación de roles para el documento (Puntos 7, 9, 10, 11) y para Release v2 (8.1 Front, 8.2 Back, 8.3 BD, 8.4 Demo).'], qa: 'Metodología iterativa corta con tablero USM en Miro y trazabilidad en GitHub.' },
    { expositor: 'Amy Herazo · 9.3 y 9.4', title: 'Cronograma, Hitos y 9.5 Dependencias (Oral)', points: ['Recorrer las 4 barras hasta HOY (26 nov) y el cierre del 3 dic.', '9.5 Dependencias (solo referencia oral): (1) BD e Infra antes de pruebas integrales; (2) API Backend antes de integración móvil v2; (3) Despliegue Railway antes de validar pagos y seguridad.'], qa: 'El trabajo paralelo desde el 12 de noviembre asegura llegar completos a Release v2.' },
    { expositor: 'Santiago Jaramillo · 10', title: 'Presupuesto $12.740.000 COP', points: ['Filtrar "Gasto Efectivo ($340K)" para probar que solo salieron $320K en materiales y $20K en salidas de campo.', 'Aclarar internamente si en Google Docs los párrafos 10.1/10.2 aún muestran $120.000/$12.840.000: la tabla oficial actualizada suma $12.740.000 con $20.000 en salidas.'], qa: 'Transparencia absoluta entre valorización en especie y gasto real.' },
    { expositor: 'Santiago Jaramillo · 10.1 y 10.2', title: 'Donut Viabilidad y Proyección', points: ['Mostrar el 87,9% cubierto en especie.', 'Cambiar a "Piloto Medellín" ($150K–$250K/mes en Railway Hobby + ~$50K/año dominio) y "Multi-réplica".'], qa: 'Modelo ligero y económicamente sostenible para piloto comercial.' },
    { expositor: 'Nataly Lievano · 11.1', title: 'Términos y Habeas Data (Ley 1581)', points: ['Recorrer el mapa rápido de 10 cláusulas y explicar el ciclo inferior de Ley 1581 de 2012.', 'Recalcar que nunca se guardan tarjetas ni CVV.'], qa: 'Cumplimiento claro tanto para clientes como para técnicos verificados.' },
    { expositor: 'Nataly Lievano · 11.2', title: 'Licencia Propietaria de Uso v1.0', points: ['Explicar las 3 columnas: permisos de uso sin ceder autoría, prohibición de copiar o sublicenciar el código original, y respeto por licencias OSS de terceros.'], qa: 'Preserva derechos irrenunciables del consumidor colombiano.' },
    { expositor: 'Cierre · Todo el equipo', title: 'Telemetría Railway + Cohete Release v2', points: ['Presionar "Consultar API en vivo" para evidenciar respuesta HTTP 200 desde PostgreSQL 18.7 si hay conexión en sala.', 'Cerrar invitando preguntas del docente Joaquín Molina Pérez.'], qa: 'Todo listo para concluir la fase 2 hacia el Release v2 el 3 de diciembre.' }
  ];

  /* ----------------------------------------------------------------------
     3. CORE NAVIGATION & HUD SYNC
     ---------------------------------------------------------------------- */
  const progressFill = document.getElementById('progress-fill');
  const slideCounter = document.getElementById('slide-counter');
  const slideDotsWrap = document.getElementById('slide-dots');
  const btnPrev = document.getElementById('btn-prev');
  const btnNext = document.getElementById('btn-next');
  const chapterPills = Array.from(document.querySelectorAll('.chapter-pill'));
  const overviewModal = document.getElementById('overview-modal');
  const overviewGrid = document.getElementById('overview-grid');
  const notesDrawer = document.getElementById('notes-drawer');
  const btnNotes = document.getElementById('btn-notes');
  const btnOverview = document.getElementById('btn-overview');

  let donutAnimateFn = null;
  let ganttReplayFn = null;
  let kpiCountFn = null;
  let faroWidget = null;

  function buildChrome() {
    slides.forEach((s, i) => {
      const dot = document.createElement('button');
      dot.className = 'slide-dot';
      dot.type = 'button';
      dot.title = `${i + 1}. ${s.dataset.title || ''}`;
      dot.addEventListener('click', () => goToSlide(i));
      slideDotsWrap.appendChild(dot);

      const item = document.createElement('button');
      item.className = 'overview-item';
      item.type = 'button';
      item.innerHTML = `<span class="overview-num">${String(i + 1).padStart(2, '0')} / ${totalSlides}</span><strong>${s.dataset.title}</strong>`;
      item.addEventListener('click', () => { goToSlide(i); toggleOverview(false); });
      overviewGrid.appendChild(item);
    });
  }

  function updateNotes(index) {
    const d = SPEAKER_NOTES[index];
    if (!d) return;
    document.getElementById('notes-expositor').textContent = d.expositor;
    document.getElementById('notes-title').textContent = d.title;
    document.getElementById('notes-body').innerHTML = `
      <div class="note-block"><strong>Libreto clave</strong><ul style="padding-left:16px;display:flex;flex-direction:column;gap:6px;">${d.points.map(p => `<li>${p}</li>`).join('')}</ul></div>
      <div class="note-block"><strong>Posible pregunta</strong><p>${d.qa}</p></div>
    `;
  }

  function goToSlide(index, skipSound) {
    const next = Math.max(0, Math.min(totalSlides - 1, index));
    const goingBack = next < currentSlide;
    currentSlide = next;

    slides.forEach((s, idx) => {
      s.classList.remove('prev-leave');
      if (idx !== next && goingBack) s.classList.add('prev-leave');
      s.classList.toggle('active', idx === next);
    });

    const active = slides[next];
    const theme = active.dataset.chapter || 'cover';
    const surface = active.dataset.surface || 'light';
    document.body.setAttribute('data-theme', theme);
    document.body.setAttribute('data-surface', surface);

    chapterPills.forEach(p => p.classList.toggle('active', p.dataset.chapter === theme));
    slideDotsWrap.querySelectorAll('.slide-dot').forEach((d, i) => d.classList.toggle('active', i === next));
    overviewGrid.querySelectorAll('.overview-item').forEach((g, i) => g.classList.toggle('active', i === next));

    progressFill.style.width = `${((next + 1) / totalSlides) * 100}%`;
    slideCounter.textContent = `${String(next + 1).padStart(2, '0')} / ${totalSlides}`;
    btnPrev.disabled = next === 0;
    btnNext.disabled = next === totalSlides - 1;

    updateNotes(next);
    active.querySelector('.slide-canvas')?.scrollTo({ top: 0 });

    if (!skipSound && document.body.dataset.booted === '1') {
      document.body.classList.remove('no-anim');
      window.VoxelFX?.StudioSFX?.slide();
    }
    document.body.dataset.booted = '1';

    if (next === 8 && ganttReplayFn) ganttReplayFn();
    if (next === 9 && kpiCountFn) kpiCountFn();
    if (next === 10 && donutAnimateFn) donutAnimateFn();
  }

  function toggleOverview(force) {
    const show = typeof force === 'boolean' ? force : overviewModal.hidden;
    overviewModal.hidden = !show;
    btnOverview.classList.toggle('active', show);
  }

  function toggleNotes(force) {
    const show = typeof force === 'boolean' ? force : notesDrawer.hidden;
    notesDrawer.hidden = !show;
    btnNotes.classList.toggle('active', show);
  }

  function initTimer() {
    const btn = document.getElementById('btn-timer');
    const disp = document.getElementById('timer-display');
    let sec = 0;
    let handle = null;
    btn.addEventListener('click', () => {
      if (handle) { clearInterval(handle); handle = null; btn.classList.remove('running'); }
      else {
        btn.classList.add('running');
        handle = setInterval(() => {
          sec += 1;
          disp.textContent = `${String(Math.floor(sec / 60)).padStart(2, '0')}:${String(sec % 60).padStart(2, '0')}`;
        }, 1000);
      }
    });
    btn.addEventListener('dblclick', () => { sec = 0; disp.textContent = '00:00'; });
  }

  document.addEventListener('click', (e) => {
    const jumper = e.target.closest('[data-go-slide]');
    if (jumper) {
      const target = Number(jumper.getAttribute('data-go-slide'));
      if (!Number.isNaN(target)) goToSlide(target);
    }
  });

  btnPrev.addEventListener('click', () => goToSlide(currentSlide - 1));
  btnNext.addEventListener('click', () => goToSlide(currentSlide + 1));
  btnOverview.addEventListener('click', () => toggleOverview());
  document.getElementById('cover-open-grid')?.addEventListener('click', () => toggleOverview(true));
  document.getElementById('btn-close-overview').addEventListener('click', () => toggleOverview(false));
  overviewModal.addEventListener('click', (e) => { if (e.target === overviewModal) toggleOverview(false); });
  btnNotes.addEventListener('click', () => toggleNotes());
  document.getElementById('btn-close-notes').addEventListener('click', () => toggleNotes(false));
  document.getElementById('btn-fullscreen').addEventListener('click', () => {
    if (!document.fullscreenElement) document.documentElement.requestFullscreen?.().catch(() => {});
    else document.exitFullscreen?.().catch(() => {});
  });

  document.addEventListener('keydown', (e) => {
    const tag = e.target?.tagName?.toLowerCase();
    if (tag === 'input' || tag === 'textarea') return;
    if (e.key === 'Escape') {
      if (!overviewModal.hidden) { toggleOverview(false); return; }
      if (!notesDrawer.hidden) { toggleNotes(false); return; }
    }
    if (e.key === 'ArrowRight' || e.key === 'PageDown' || (e.key === ' ' && overviewModal.hidden)) { e.preventDefault(); goToSlide(currentSlide + 1); }
    else if (e.key === 'ArrowLeft' || e.key === 'PageUp') { e.preventDefault(); goToSlide(currentSlide - 1); }
    else if (e.key === 'Home') { e.preventDefault(); goToSlide(0); }
    else if (e.key === 'End') { e.preventDefault(); goToSlide(totalSlides - 1); }
    else if (e.key.toLowerCase() === 'g') { e.preventDefault(); toggleOverview(); }
    else if (e.key.toLowerCase() === 'n' || e.key.toLowerCase() === 'p') { e.preventDefault(); toggleNotes(); }
    else if (e.key.toLowerCase() === 'f') {
      e.preventDefault();
      if (!document.fullscreenElement) document.documentElement.requestFullscreen?.().catch(() => {});
      else document.exitFullscreen?.().catch(() => {});
    }
  });

  /* ----------------------------------------------------------------------
     4. SLIDE 3: 7.1 PIPELINE SVG CONTRASTADO (LIGHT MODE)
     ---------------------------------------------------------------------- */
  function initSlide3() {
    const tabs = document.getElementById('adapter-swap-tabs');
    const svg = document.getElementById('pipeline-svg');

    function renderPipeline(adapterName) {
      const cfg = {
        PayPalAdapter: { color: '#0284c7', sub: 'REST v2 · AUTHORIZE (Live)' },
        PasarelaSimulada: { color: '#059669', sub: 'Determinista · Tests JUnit' },
        WompiAdapter: { color: '#d97706', sub: 'Esqueleto listo extensible' }
      }[adapterName] || { color: '#0284c7', sub: 'Adaptador activo' };

      svg.innerHTML = `
        <path d="M 85 70 L 820 70" stroke="#e2e8f0" stroke-width="4" stroke-linecap="round"/>
        <path class="flow-dash" d="M 85 70 L 820 70" stroke="${cfg.color}" stroke-width="3" stroke-linecap="round"/>
        <circle r="6" fill="#b80035">
          <animateMotion dur="2.2s" repeatCount="indefinite" path="M 85 70 L 820 70"/>
        </circle>

        <g transform="translate(95,70)">
          <rect x="-78" y="-34" width="156" height="68" rx="14" fill="#ffffff" stroke="#cbd5e1" stroke-width="2"/>
          <text y="-4" text-anchor="middle" fill="#0f172a" font-size="14" font-weight="800" font-family="Outfit, sans-serif">Expo App 58</text>
          <text y="16" text-anchor="middle" fill="#64748b" font-size="11" font-family="JetBrains Mono, monospace">HTTPS + JWT</text>
        </g>
        <g transform="translate(285,70)">
          <rect x="-82" y="-34" width="164" height="68" rx="14" fill="#ffffff" stroke="#cbd5e1" stroke-width="2"/>
          <text y="-4" text-anchor="middle" fill="#0f172a" font-size="14" font-weight="800" font-family="Outfit, sans-serif">Controller &amp; Service</text>
          <text y="16" text-anchor="middle" fill="#64748b" font-size="11" font-family="JetBrains Mono, monospace">SRP + OCP Strategy</text>
        </g>
        <g transform="translate(485,70)">
          <rect x="-88" y="-38" width="176" height="76" rx="14" fill="#fff1f2" stroke="#b80035" stroke-dasharray="5 3" stroke-width="2"/>
          <text y="-5" text-anchor="middle" fill="#b80035" font-size="13.5" font-weight="800" font-family="JetBrains Mono, monospace">«IPasarelaPagos»</text>
          <text y="16" text-anchor="middle" fill="#334155" font-size="11" font-family="Plus Jakarta Sans, sans-serif">Contrato Dominio (DIP)</text>
        </g>
        <g transform="translate(690,70)">
          <rect x="-90" y="-38" width="180" height="76" rx="14" fill="#ffffff" stroke="${cfg.color}" stroke-width="2.6"/>
          <text y="-5" text-anchor="middle" fill="${cfg.color}" font-size="14" font-weight="800" font-family="JetBrains Mono, monospace">${adapterName}</text>
          <text y="16" text-anchor="middle" fill="#334155" font-size="11" font-family="Plus Jakarta Sans, sans-serif">${cfg.sub}</text>
        </g>
        <g transform="translate(855,70)">
          <rect x="-56" y="-34" width="112" height="68" rx="14" fill="#ecfdf5" stroke="#059669" stroke-width="2"/>
          <text y="-4" text-anchor="middle" fill="#065f46" font-size="13.5" font-weight="800" font-family="Outfit, sans-serif">PG 18.7</text>
          <text y="16" text-anchor="middle" fill="#047857" font-size="11" font-family="JetBrains Mono, monospace">ACID Flyway</text>
        </g>
      `;
    }

    tabs.addEventListener('click', (e) => {
      const btn = e.target.closest('[data-adapter]');
      if (!btn) return;
      window.VoxelFX?.StudioSFX?.tap();
      tabs.querySelectorAll('.tab-light').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      renderPipeline(btn.dataset.adapter);
    });

    renderPipeline('PayPalAdapter');
  }

  /* ----------------------------------------------------------------------
     5. SLIDE 4: 7.2 CUADRANTE 2x2 LIGHT
     ---------------------------------------------------------------------- */
  function initSlide4() {
    const svg = document.getElementById('debt-quadrant-svg');
    const strip = document.getElementById('debt-selector-strip');
    const detail = document.getElementById('debt-detail');

    strip.innerHTML = DEBT_DATA.map((d, i) => `
      <button class="debt-chip-ed ${i === 0 ? 'active' : ''}" data-debt="${d.id}" type="button">${d.short}</button>
    `).join('');

    function selectDebt(id) {
      const d = DEBT_DATA.find(x => x.id === id) || DEBT_DATA[0];
      strip.querySelectorAll('.debt-chip-ed').forEach(b => b.classList.toggle('active', b.dataset.debt === d.id));

      svg.innerHTML = `
        <rect x="52" y="18" width="446" height="242" rx="14" fill="#f8fafc" stroke="#cbd5e1" stroke-width="1.5"/>
        <rect x="275" y="18" width="223" height="121" fill="rgba(184, 0, 53, 0.06)"/>
        <line x1="275" y1="18" x2="275" y2="260" stroke="#cbd5e1" stroke-dasharray="4 4"/>
        <line x1="52" y1="139" x2="498" y2="139" stroke="#cbd5e1" stroke-dasharray="4 4"/>
        <text x="386" y="38" text-anchor="middle" fill="#b80035" font-size="11" font-weight="800" font-family="JetBrains Mono, monospace">PRIORIDAD ALTA</text>
        <text x="275" y="285" text-anchor="middle" fill="#475569" font-size="12" font-weight="700" font-family="Plus Jakarta Sans, sans-serif">Impacto en Escalabilidad / Negocio →</text>
        <text x="24" y="142" text-anchor="middle" fill="#475569" font-size="12" font-weight="700" transform="rotate(-90 24 142)" font-family="Plus Jakarta Sans, sans-serif">Criticidad →</text>
        ${DEBT_DATA.map(item => {
          const active = item.id === d.id;
          const col = item.x > 275 ? '#b80035' : '#d97706';
          return `
            <g class="q-node" data-debt="${item.id}" style="cursor:pointer;">
              ${active ? `<circle cx="${item.x}" cy="${item.y}" r="22" fill="none" stroke="${col}" stroke-width="2"><animate attributeName="r" values="12;25;12" dur="2s" repeatCount="indefinite"/><animate attributeName="opacity" values="0.85;0.12;0.85" dur="2s" repeatCount="indefinite"/></circle>` : ''}
              <circle cx="${item.x}" cy="${item.y}" r="${active ? 10 : 7}" fill="${col}" stroke="#ffffff" stroke-width="2.5"/>
              <text x="${item.x}" y="${item.y + 24}" text-anchor="middle" fill="#0f172a" font-size="12" font-weight="${active ? '800' : '600'}" font-family="Plus Jakarta Sans, sans-serif">${item.short}</text>
            </g>
          `;
        }).join('')}
      `;

      detail.innerHTML = `
        <div class="debt-card-inner">
          <span>Hallazgo · ${d.sev}</span>
          <h4>${d.title}</h4>
          <p>${d.cause}</p>
        </div>
        <div class="debt-card-inner debt-card-fix">
          <span>Resolución arquitectónica recomendada</span>
          <p>${d.fix}</p>
        </div>
      `;
    }

    strip.addEventListener('click', (e) => {
      const btn = e.target.closest('[data-debt]');
      if (btn) { window.VoxelFX?.StudioSFX?.tap(); selectDebt(btn.dataset.debt); }
    });
    svg.addEventListener('click', (e) => {
      const node = e.target.closest('.q-node');
      if (node) { window.VoxelFX?.StudioSFX?.tap(); selectDebt(node.dataset.debt); }
    });
    selectDebt(DEBT_DATA[0].id);
  }

  /* ----------------------------------------------------------------------
     6. SLIDE 5: 7.3 RUNNER 29/29 & MEDIDOR TARIFA + BÓVEDA (DARK)
     ---------------------------------------------------------------------- */
  function initSlide5() {
    const listWrap = document.getElementById('pu-cards-list');
    const barFill = document.getElementById('suite-bar-fill');
    const counter = document.getElementById('suite-counter');
    const btnRun = document.getElementById('btn-run-suite');

    function renderPuCards(activeIdx, passedCount) {
      listWrap.innerHTML = PU_ITEMS.map((item, i) => {
        const cls = i === activeIdx ? 'running' : (i < passedCount ? 'passed' : '');
        const tag = i === activeIdx ? 'RUN...' : (i < passedCount ? `PASS · ${item.ms}` : 'WAIT');
        return `
          <div class="pu-item ${cls}">
            <div class="pu-top"><strong>${item.id} · <code>${item.fn}</code></strong><span class="pu-ms">${tag}</span></div>
            <p>${item.res}</p>
          </div>
        `;
      }).join('');
    }
    renderPuCards(-1, 4);

    btnRun.addEventListener('click', () => {
      btnRun.disabled = true;
      barFill.style.width = '6%';
      counter.textContent = '0 / 29...';
      renderPuCards(0, 0);

      PU_ITEMS.forEach((_, idx) => {
        setTimeout(() => {
          renderPuCards(idx, idx);
          barFill.style.width = `${Math.round(((idx + 1) / PU_ITEMS.length) * 100)}%`;
          counter.textContent = `${Math.round(((idx + 1) / PU_ITEMS.length) * 29)} / 29...`;
          if (idx === PU_ITEMS.length - 1) {
            setTimeout(() => {
              renderPuCards(-1, 4);
              barFill.style.width = '100%';
              counter.textContent = '29 / 29 PASS ✓';
              window.VoxelFX?.StudioSFX?.pass();
              btnRun.disabled = false;
            }, 220);
          }
        }, idx * 200);
      });
    });

    const baseEl = document.getElementById('sim-base');
    const minEl = document.getElementById('sim-min');
    const maxEl = document.getElementById('sim-max');
    const urgEl = document.getElementById('sim-urgente');
    const dispEl = document.getElementById('sim-disputa');
    const fmt = (n) => '$' + Math.round(n).toLocaleString('es-CO');

    function updateSandbox() {
      const base = Number(baseEl.value) || 0;
      const min = Number(minEl.value) || 0;
      const max = Number(maxEl.value) || 0;
      const urg = urgEl.checked;
      const disp = dispEl.checked;

      const gaugeFormula = document.getElementById('gauge-formula');
      const gaugeFinal = document.getElementById('gauge-final');
      const gaugeFill = document.getElementById('gauge-fill');
      const gaugeMarker = document.getElementById('gauge-marker');

      document.getElementById('lbl-min').textContent = fmt(min);
      document.getElementById('lbl-max').textContent = fmt(max);

      if (min > max) {
        gaugeFormula.textContent = `Inválido: ${fmt(min)} > ${fmt(max)}`;
        gaugeFinal.textContent = 'PU-04 Exception!';
        gaugeFinal.style.color = '#fecdd3';
        gaugeFill.style.width = '100%';
        gaugeMarker.style.left = '96%';
      } else {
        const raw = urg ? base * 1.3 : base;
        const clamped = Math.max(min, Math.min(max, raw));
        const pct = max === min ? 100 : Math.max(4, Math.min(98, ((clamped - min) / (max - min)) * 100));
        gaugeFormula.textContent = `Bruto: ${fmt(raw)}${urg ? ' (+30%)' : ''}`;
        gaugeFinal.textContent = `${fmt(clamped)} COP${clamped !== raw ? ' (Clamp Máx)' : ''}`;
        gaugeFinal.style.color = '#6ee7b7';
        gaugeFill.style.width = `${pct}%`;
        gaugeMarker.style.left = `${pct}%`;
      }

      const vaultBox = document.getElementById('vault-box');
      const vaultIcon = document.getElementById('vault-icon');
      const vaultTitle = document.getElementById('vault-title');
      const vaultSub = document.getElementById('vault-sub');
      const vaultBadge = document.getElementById('vault-badge');

      if (disp) {
        vaultBox.className = 'vault-dark locked';
        vaultIcon.innerHTML = `<svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="#fb7185" stroke-width="2.2"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>`;
        vaultTitle.textContent = 'Custodia BLOQUEADA · puedeLiberarCustodia() → false';
        vaultSub.textContent = 'EN_DISPUTA impide liberarFondos() (PU-02)';
        vaultBadge.className = 'badge-block-dark';
        vaultBadge.textContent = 'RETENIDO';
      } else {
        vaultBox.className = 'vault-dark unlocked';
        vaultIcon.innerHTML = `<svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="#34d399" stroke-width="2.2"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 9.9-1"/></svg>`;
        vaultTitle.textContent = 'Custodia DESBLOQUEADA · puedeLiberarCustodia() → true';
        vaultSub.textContent = 'Servicio Finalizado: liberación habilitada';
        vaultBadge.className = 'badge-ok-dark';
        vaultBadge.textContent = 'LIBERADO ✓';
      }
    }

    [baseEl, minEl, maxEl, urgEl].forEach(el => el.addEventListener('input', updateSandbox));
    dispEl.addEventListener('input', () => { updateSandbox(); window.VoxelFX?.StudioSFX?.lock(dispEl.checked); });
    document.getElementById('sim-reset-pu').addEventListener('click', () => {
      baseEl.value = 50000; minEl.value = 30000; maxEl.value = 60000; urgEl.checked = true; dispEl.checked = true;
      updateSandbox();
    });
    updateSandbox();
  }

  /* ----------------------------------------------------------------------
     7. SLIDE 6: 7.4 APP REAL EN SMARTPHONE + GHERKIN
     ---------------------------------------------------------------------- */
  function initSlide6() {
    const tabs = document.getElementById('pf-tabs');
    const stage = document.getElementById('pf-stage');
    const phoneImg = document.getElementById('pf-phone-img');
    const phoneBadge = document.getElementById('pf-phone-badge');
    const btnPlay = document.getElementById('btn-play-pf');
    const btnFail = document.getElementById('btn-fail-pf');
    let key = 'pf01';
    let step = 2;
    let failMode = false;

    function render() {
      const d = PF_DATA[key];
      phoneImg.src = d.img;
      phoneBadge.textContent = failMode ? '⚡ Contingencia / Idempotencia activa' : d.states[Math.min(step, d.states.length - 1)] || d.phoneBadge;

      stage.innerHTML = `
        <div class="pf-steps-light">
          ${d.steps.map((s, i) => `
            <div class="pf-step-ed ${i === step ? 'active' : ''}" data-step="${i}">
              <div class="pf-kw-ed">${s.kw}</div>
              <p>${s.text}</p>
            </div>
          `).join('')}
        </div>
        <div class="pf-footer-ed ${failMode ? 'fail' : ''}">
          <div style="flex:1;">
            <strong style="display:block;font-size:0.84rem;color:${failMode ? '#b45309' : '#0f172a'};margin-bottom:3px;">
              ${failMode ? '⚡ Plan de contingencia ante fallo:' : 'Transición de estados verificada:'}
            </strong>
            <span style="font-size:0.88rem;color:#334155;">${failMode ? d.fail : 'Ejecución validada en la app móvil y API PostgreSQL.'}</span>
          </div>
          <div class="pf-state-badges">
            ${d.states.map((st, idx) => `<span class="st-badge-ed ${idx <= step && !failMode ? 'lit' : ''}">${st}</span>`).join('')}
          </div>
        </div>
      `;

      stage.querySelectorAll('.pf-step-ed').forEach(c => {
        c.addEventListener('click', () => { step = Number(c.dataset.step); failMode = false; btnFail.classList.remove('active'); render(); });
      });
    }

    tabs.addEventListener('click', (e) => {
      const btn = e.target.closest('[data-pf]');
      if (!btn) return;
      window.VoxelFX?.StudioSFX?.tap();
      tabs.querySelectorAll('.pf-tab-ed').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      key = btn.dataset.pf;
      step = 2;
      failMode = false;
      btnFail.classList.remove('active');
      render();
    });

    btnPlay.addEventListener('click', () => {
      failMode = false; btnFail.classList.remove('active'); btnPlay.disabled = true;
      step = 0; render();
      setTimeout(() => { step = 1; render(); }, 650);
      setTimeout(() => { step = 2; render(); window.VoxelFX?.StudioSFX?.pass(); btnPlay.disabled = false; }, 1300);
    });

    btnFail.addEventListener('click', () => {
      failMode = !failMode;
      btnFail.classList.toggle('active', failMode);
      window.VoxelFX?.StudioSFX?.alert();
      render();
    });

    render();
  }

  /* ----------------------------------------------------------------------
     8. SLIDE 7: 7.5 SEGURIDAD & FARO VOXEL 3D
     ---------------------------------------------------------------------- */
  function initSlide7() {
    const listWrap = document.getElementById('sec-threats-list');
    const statusTag = document.getElementById('shield-status');

    function selectThreat(idx, fromClick) {
      const t = SECURITY_THREATS[idx] || SECURITY_THREATS[0];
      statusTag.textContent = `${t.badge} — ${t.title}`;
      listWrap.querySelectorAll('.sec-threat-btn').forEach((b, i) => b.classList.toggle('active', i === idx));
      if (fromClick) {
        window.VoxelFX?.StudioSFX?.tap();
        faroWidget?.model?.flashShield?.();
      }
    }

    listWrap.innerHTML = SECURITY_THREATS.map((t, i) => `
      <button class="sec-threat-btn ${i === 1 ? 'active' : ''}" data-t="${i}" type="button">
        <div class="st-num">0${i + 1}</div>
        <div>
          <strong>${t.title} · <span style="color:#6ee7b7;font-family:JetBrains Mono,monospace;font-size:0.78rem;">${t.badge}</span></strong>
          <p>${t.desc}</p>
        </div>
      </button>
    `).join('');

    listWrap.addEventListener('click', (e) => {
      const btn = e.target.closest('[data-t]');
      if (btn) selectThreat(Number(btn.dataset.t), true);
    });

    selectThreat(1, false);
  }

  /* ----------------------------------------------------------------------
     9. SLIDE 9: 9.3 GANTT LIGHT & 9.4 HITOS (AMY MINIMALISTA)
     ---------------------------------------------------------------------- */
  function initSlide9() {
    const svg = document.getElementById('gantt-svg');
    const hitosWrap = document.getElementById('hitos-row');
    const railFill = document.getElementById('hitos-rail-fill');
    let currentHito = 2;

    const tasks = [
      { label: 'Análisis y diseño inicial', owner: 'Todo el equipo', dates: '23 sep – 1 oct', startX: 270, endX: 385, color: '#4f46e5', hito: 0 },
      { label: 'Arquitectura y patrones', owner: 'Todo el equipo', dates: '15 oct – 5 nov', startX: 455, endX: 660, color: '#6366f1', hito: 1 },
      { label: 'Documento: puntos 7, 9, 10, 11', owner: 'Salomé, Amy, Santiago, Nataly', dates: '12 nov – 26 nov', startX: 705, endX: 835, color: '#059669', hito: 2 },
      { label: 'Implementación funcional', owner: 'Todos los integrantes', dates: '12 nov – 3 dic', startX: 705, endX: 960, color: '#b80035', hito: 3 }
    ];

    const ticks = [
      { x: 270, anchor: 'start', text: '23 sep' },
      { x: 385, anchor: 'middle', text: '1 oct · E1' },
      { x: 515, anchor: 'middle', text: '22 oct' },
      { x: 660, anchor: 'middle', text: '5 nov · E2' },
      { x: 835, anchor: 'middle', text: '26 nov · HOY' },
      { x: 960, anchor: 'end', text: '3 dic · v2' }
    ];

    function renderGantt(activeHito, animateBars) {
      let markup = '';
      ticks.forEach(t => {
        const isToday = t.text.includes('HOY');
        markup += `
          <line x1="${t.x}" y1="18" x2="${t.x}" y2="188"
            stroke="${isToday ? '#059669' : '#e2e8f0'}"
            stroke-width="${isToday ? '2.2' : '1'}" stroke-dasharray="${isToday ? '4 4' : ''}" />
          <text x="${t.x}" y="210" text-anchor="${t.anchor}" fill="${isToday ? '#047857' : '#64748b'}"
            font-size="11.5" font-family="JetBrains Mono, monospace" font-weight="${isToday ? '800' : '600'}">${t.text}</text>
        `;
      });

      tasks.forEach((t, idx) => {
        const y = 26 + idx * 41;
        const active = activeHito === t.hito;
        const w = t.endX - t.startX;
        markup += `
          <g class="gantt-row" data-hito="${t.hito}" style="cursor:pointer;">
            <text x="6" y="${y + 15}" fill="#0f172a" font-size="13" font-weight="${active ? '800' : '700'}" font-family="Plus Jakarta Sans, sans-serif">${t.label}</text>
            <text x="6" y="${y + 30}" fill="#64748b" font-size="11" font-family="Plus Jakarta Sans, sans-serif">${t.owner}</text>
            <rect x="270" y="${y + 2}" width="690" height="25" rx="8" fill="#f1f5f9"/>
            <rect class="${animateBars ? 'gantt-bar-anim' : ''}" style="animation-delay:${idx * 110}ms"
              x="${t.startX}" y="${y + 2}" width="${w}" height="25" rx="8"
              fill="${t.color}" opacity="${active ? '1' : '0.78'}" stroke="${active ? '#0f172a' : 'none'}" stroke-width="1.8"/>
            <text x="${t.startX + w / 2}" y="${y + 19}" text-anchor="middle" fill="#ffffff" font-size="11" font-weight="700" font-family="JetBrains Mono, monospace">${t.dates}</text>
          </g>
        `;
      });

      svg.innerHTML = markup;
    }

    function setHito(idx, animateBars) {
      currentHito = idx;
      renderGantt(idx, animateBars);
      hitosWrap.querySelectorAll('.hito-ed').forEach(b => b.classList.toggle('active', Number(b.dataset.hito) === idx));
      railFill.style.width = `${([18, 45, 73, 100])[idx] || 73}%`;
    }

    ganttReplayFn = () => setHito(currentHito, true);

    svg.addEventListener('click', (e) => {
      const row = e.target.closest('.gantt-row');
      if (row) { window.VoxelFX?.StudioSFX?.tap(); setHito(Number(row.dataset.hito), false); }
    });
    hitosWrap.addEventListener('click', (e) => {
      const card = e.target.closest('[data-hito]');
      if (card) { window.VoxelFX?.StudioSFX?.tap(); setHito(Number(card.dataset.hito), false); }
    });
    setHito(2, true);
  }

  /* ----------------------------------------------------------------------
     10. SLIDE 10: 10 PRESUPUESTO & BARRAS PROPORCIONALES
     ---------------------------------------------------------------------- */
  function initSlide10() {
    const filters = document.getElementById('budget-filters');
    const tbody = document.getElementById('budget-tbody');
    const subLabel = document.getElementById('budget-subtotal-label');
    const subVal = document.getElementById('budget-subtotal-val');
    const fmt = (n) => '$' + Math.round(n).toLocaleString('es-CO');

    kpiCountFn = () => {
      animateValue(document.getElementById('kpi-total-num'), 12740000, 320, v => fmt(v) + ' COP');
      animateValue(document.getElementById('kpi-propios-num'), 11540000, 320, v => fmt(v) + ' COP');
      animateValue(document.getElementById('kpi-otras-num'), 1200000, 280, v => fmt(v) + ' COP');
    };

    function renderBudget(filter) {
      const rows = BUDGET_ROWS.filter(r => {
        if (filter === 'propios') return r.propios > 0;
        if (filter === 'otras') return r.otras > 0;
        if (filter === 'desembolso') return r.tipo === 'desembolso';
        return true;
      });
      const sp = rows.reduce((a, r) => a + r.propios, 0);
      const so = rows.reduce((a, r) => a + r.otras, 0);
      const st = sp + so;

      subLabel.textContent = `Subtotal (${rows.length} rubros):`;
      animateValue(subVal, st, 420, v => fmt(v) + ' COP');

      tbody.innerHTML = rows.map(r => {
        const total = r.propios + r.otras;
        const pct = Math.min(100, (total / 12740000) * 100);
        return `
          <tr>
            <td><strong>${r.rubro}</strong><div class="rubro-bar-bg"><div class="rubro-bar-fill" style="width:${pct.toFixed(1)}%"></div></div></td>
            <td class="num">${fmt(r.propios)}</td>
            <td class="num">${fmt(r.otras)}</td>
            <td class="num"><strong>${fmt(total)}</strong></td>
            <td>${r.nota}</td>
          </tr>
        `;
      }).join('') + `
        <tr class="total-row">
          <td>TOTAL (${rows.length} rubros)</td>
          <td class="num">${fmt(sp)}</td>
          <td class="num">${fmt(so)}</td>
          <td class="num">${fmt(st)}</td>
          <td>Totales verificados</td>
        </tr>
      `;
    }

    filters.addEventListener('click', (e) => {
      const btn = e.target.closest('[data-filter]');
      if (!btn) return;
      window.VoxelFX?.StudioSFX?.tap();
      filters.querySelectorAll('.tab-light').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      renderBudget(btn.dataset.filter);
    });
    renderBudget('all');
  }

  /* ----------------------------------------------------------------------
     11. SLIDE 11: 10.1 DONUT LIGHT + 10.2 ESCENARIOS
     ---------------------------------------------------------------------- */
  function initSlide11() {
    const svg = document.getElementById('donut-svg');
    const legend = document.getElementById('donut-legend');
    const cVal = document.getElementById('donut-center-val');
    const cLabel = document.getElementById('donut-center-label');
    const r = 90;
    const circ = 2 * Math.PI * r;
    let activeSeg = 'especie';

    function drawDonut(id, animateSweep) {
      activeSeg = id;
      let offset = 0;
      let html = `<circle cx="120" cy="120" r="${r}" fill="none" stroke="#e2e8f0" stroke-width="28"/>`;

      DONUT_SEGMENTS.forEach(s => {
        const dash = (s.pct / 100) * circ;
        const isSel = s.id === id;
        html += `
          <circle data-seg="${s.id}" cx="120" cy="120" r="${r}" fill="none" stroke="${s.color}"
            stroke-width="${isSel ? 34 : 27}"
            stroke-dasharray="${animateSweep ? `0 ${circ}` : `${dash} ${circ - dash}`}"
            stroke-dashoffset="${-offset}"
            transform="rotate(-90 120 120)"
            style="cursor:pointer;transition:stroke-dasharray 0.78s cubic-bezier(0.22,1,0.36,1), stroke-width 0.2s ease;"/>
        `;
        offset += dash;
      });
      svg.innerHTML = html;

      if (animateSweep) {
        requestAnimationFrame(() => {
          svg.querySelectorAll('[data-seg]').forEach(el => {
            const seg = DONUT_SEGMENTS.find(s => s.id === el.dataset.seg);
            if (seg) {
              const dash = (seg.pct / 100) * circ;
              el.setAttribute('stroke-dasharray', `${dash} ${circ - dash}`);
            }
          });
        });
      }

      const sel = DONUT_SEGMENTS.find(s => s.id === id) || DONUT_SEGMENTS[0];
      cVal.textContent = `${sel.pct.toString().replace('.', ',')}%`;
      cLabel.textContent = sel.val;
      legend.querySelectorAll('.leg-ed-btn').forEach(b => b.classList.toggle('active', b.dataset.seg === id));
    }

    legend.innerHTML = DONUT_SEGMENTS.map(s => `
      <button class="leg-ed-btn" data-seg="${s.id}" type="button">
        <span class="leg-dot" style="background:${s.color};"></span>
        <div><strong>${s.label} (${s.pct.toString().replace('.', ',')}%)</strong><small>${s.val} — ${s.desc}</small></div>
      </button>
    `).join('');

    donutAnimateFn = () => drawDonut(activeSeg, true);
    svg.addEventListener('click', (e) => { const s = e.target.closest('[data-seg]'); if (s) drawDonut(s.dataset.seg, false); });
    legend.addEventListener('click', (e) => { const s = e.target.closest('[data-seg]'); if (s) drawDonut(s.dataset.seg, false); });
    drawDonut('especie', false);

    const sTabs = document.getElementById('scenario-tabs');
    const sStage = document.getElementById('scenario-detail');
    function renderScen(key) {
      const s = SCENARIOS[key];
      sStage.innerHTML = `
        <div class="scen-hero-ed"><span>${s.tag}</span><strong>${s.headline}</strong><p>${s.sub}</p></div>
        <ul class="scen-list-ed">${s.bullets.map(b => `<li>${b}</li>`).join('')}</ul>
      `;
    }
    sTabs.addEventListener('click', (e) => {
      const b = e.target.closest('[data-scen]');
      if (!b) return;
      window.VoxelFX?.StudioSFX?.tap();
      sTabs.querySelectorAll('.tab-light').forEach(x => x.classList.remove('active'));
      b.classList.add('active');
      renderScen(b.dataset.scen);
    });
    renderScen('academico');
  }

  /* ----------------------------------------------------------------------
     12. SLIDE 12: 11.1 TÉRMINOS Y 10 CLÁUSULAS LIGHT
     ---------------------------------------------------------------------- */
  function initSlide12() {
    const tabs = document.getElementById('terms-tabs');
    const grid = document.getElementById('terms-grid');
    const idxStrip = document.getElementById('terms-index-strip');
    const all = Object.entries(TERMS_GROUPS).flatMap(([g, arr]) => arr.map(item => ({ ...item, g })));
    let currentId = 1;

    function render(key, hlId) {
      const items = TERMS_GROUPS[key];
      currentId = hlId || (items[0] ? items[0].id : 1);

      tabs.querySelectorAll('.tab-light').forEach(b => b.classList.toggle('active', b.dataset.term === key));
      grid.innerHTML = items.map(t => `
        <article class="ed-card t-card-ed ${t.id === currentId ? 'hl' : ''}">
          <span class="t-num-ed">${t.num}</span>
          <h4>${t.title}</h4>
          <p>${t.text}</p>
          <div class="t-apply-ed"><strong>En A La Mano:</strong> ${t.apply}</div>
        </article>
      `).join('');

      idxStrip.innerHTML = all.map(c => `
        <button class="t-mini-ed ${c.id === currentId ? 'active' : ''}" data-g="${c.g}" data-cid="${c.id}" type="button">
          <strong>${c.id}.</strong> ${c.title}
        </button>
      `).join('');
    }

    tabs.addEventListener('click', (e) => {
      const b = e.target.closest('[data-term]');
      if (b) { window.VoxelFX?.StudioSFX?.tap(); render(b.dataset.term); }
    });
    idxStrip.addEventListener('click', (e) => {
      const m = e.target.closest('[data-g]');
      if (m) { window.VoxelFX?.StudioSFX?.tap(); render(m.dataset.g, Number(m.dataset.cid)); }
    });
    render('acceso', 1);
  }

  /* ----------------------------------------------------------------------
     13. SLIDE 14: TELEMETRÍA EN VIVO RAILWAY
     ---------------------------------------------------------------------- */
  function initSlide14() {
    const btn = document.getElementById('btn-ping-railway');
    const out = document.getElementById('railway-ping-result');
    const directUrl = 'https://plataforma-de-servicios-del-hogar-a-la-mano-production.up.railway.app/api/categorias';

    function escapeHtml(str) {
      return String(str).replace(/[&<>"']/g, ch => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]));
    }

    function renderSuccess(cats, latencyMs) {
      const names = cats.map(c => c.nombre || c.name || c).filter(Boolean);
      window.VoxelFX?.StudioSFX?.pass();
      const latencyPart = typeof latencyMs === 'number' ? ` · ${latencyMs} ms` : '';
      out.innerHTML = `
        <strong style="color:#6ee7b7;">200 OK${latencyPart} · ${cats.length} categorías en vivo desde Railway PostgreSQL 18.7:</strong>
        ${names.map(n => `<span class="cat-live-pill">${escapeHtml(n)}</span>`).join('')}
      `;
    }

    btn.addEventListener('click', async () => {
      btn.disabled = true;
      out.innerHTML = 'Consultando Railway en vivo...';
      const isHttp = window.location.protocol.startsWith('http');

      try {
        if (isHttp) {
          const ctrl = new AbortController();
          const t = setTimeout(() => ctrl.abort(), 9000);
          try {
            const proxyRes = await fetch('/api/live-categorias', { signal: ctrl.signal });
            clearTimeout(t);
            const contentType = proxyRes.headers.get('content-type') || '';
            if (proxyRes.ok && contentType.includes('application/json')) {
              const payload = await proxyRes.json();
              if (payload?.ok && Array.isArray(payload.categorias)) {
                renderSuccess(payload.categorias, payload.latencyMs);
                return;
              }
            }
          } catch (_) {
            clearTimeout(t);
          }
        }

        const ctrlDirect = new AbortController();
        const tDirect = setTimeout(() => ctrlDirect.abort(), 7000);
        const started = performance.now();
        const res = await fetch(directUrl, { signal: ctrlDirect.signal });
        clearTimeout(tDirect);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        const cats = Array.isArray(data) ? data : (Array.isArray(data?.content) ? data.content : []);
        renderSuccess(cats, Math.round(performance.now() - started));
      } catch (_) {
        out.innerHTML = `
          <span style="color:#fde68a;">
            CORS estricto activo en producción (Control 7.5): el backend en Railway bloquea orígenes externos no autorizados.
            Inicia con <code>python sustentacion-entrega-3/serve.py</code> para consultar vía proxy local o
            <a href="${directUrl}" target="_blank" rel="noopener noreferrer" style="color:#6ee7b7;font-weight:700;">abrir JSON directo en nueva pestaña ↗</a>.
          </span>
        `;
      } finally {
        btn.disabled = false;
      }
    });
  }

  /* ----------------------------------------------------------------------
     14. MONTAR LOS 3 DIORAMAS VOXEL 3D + ARRANCAR CON ?slide=N
     ---------------------------------------------------------------------- */
  function initSfxAndVoxels() {
    const btnSfx = document.getElementById('btn-sfx');
    const sfxIcon = document.getElementById('sfx-icon');
    if (btnSfx && window.VoxelFX) {
      btnSfx.addEventListener('click', () => {
        const next = !window.VoxelFX.StudioSFX.isEnabled();
        window.VoxelFX.StudioSFX.setEnabled(next);
        btnSfx.classList.toggle('active', next);
        sfxIcon.textContent = next ? '🔊' : '🔇';
      });
    }

    if (window.VoxelFX) {
      window.VoxelFX.mountVoxelCanvas({
        canvasId: 'voxel-cover',
        explodeBtnId: 'btn-explode-cover',
        model: 'casa',
        slideIndex: 0,
        glowColor: 'rgba(225, 29, 72, 0.25)'
      });
      faroWidget = window.VoxelFX.mountVoxelCanvas({
        canvasId: 'voxel-faro',
        explodeBtnId: 'btn-explode-faro',
        model: 'faro',
        slideIndex: 6,
        glowColor: 'rgba(16, 185, 129, 0.24)'
      });
      window.VoxelFX.mountVoxelCanvas({
        canvasId: 'voxel-cohete',
        explodeBtnId: 'btn-explode-cohete',
        model: 'cohete',
        slideIndex: 13,
        glowColor: 'rgba(225, 29, 72, 0.26)'
      });
      window.VoxelFX.startVoxelLoop(() => currentSlide);
    }
  }

  if (urlParams.has('slide')) {
    document.body.classList.add('no-anim');
  }
  initCanvasFX();
  buildChrome();
  initTimer();
  initSfxAndVoxels();
  initSlide3();
  initSlide4();
  initSlide5();
  initSlide6();
  initSlide7();
  initSlide9();
  initSlide10();
  initSlide11();
  initSlide12();
  initSlide14();

  const startSlide = Number.isFinite(slideParam) ? Math.max(0, Math.min(totalSlides - 1, slideParam)) : 0;
  goToSlide(startSlide, true);
})();
