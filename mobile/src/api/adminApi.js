import { peticionApi } from './clienteHttp';

export function listarProfesionalesPendientesApi(token) {
  return peticionApi('/api/admin/profesionales/pendientes', { token });
}

export function obtenerUrlsDocumentosApi(token, idProfesional) {
  return peticionApi(`/api/admin/profesionales/${idProfesional}/documento-url`, { token });
}

export function aprobarProfesionalApi(token, idProfesional) {
  return peticionApi(`/api/admin/profesionales/${idProfesional}/aprobar`, {
    metodo: 'POST',
    token
  });
}

export function rechazarProfesionalApi(token, idProfesional, motivo) {
  return peticionApi(`/api/admin/profesionales/${idProfesional}/rechazar`, {
    metodo: 'POST',
    token,
    cuerpo: { motivo }
  });
}

export function listarColaReportesApi(token, estado = null) {
  const qs = estado ? `?estado=${encodeURIComponent(estado)}` : '';
  return peticionApi(`/api/admin/reportes${qs}`, { token });
}

export function resolverReporteApi(token, idReporte, datos) {
  return peticionApi(`/api/admin/reportes/${idReporte}/resolver`, {
    metodo: 'POST',
    token,
    cuerpo: datos
  });
}

export function listarCasosSoporteAdminApi(token) {
  return peticionApi('/api/admin/soporte/casos', { token });
}

export function responderCasoSoporteApi(token, idCaso, datos) {
  return peticionApi(`/api/admin/soporte/casos/${idCaso}/responder`, {
    metodo: 'POST',
    token,
    cuerpo: datos
  });
}
