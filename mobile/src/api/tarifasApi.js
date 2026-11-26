import { peticionApi } from './clienteHttp';

export function obtenerCotizacionApi(token, servicioId, esUrgente) {
  const params = new URLSearchParams({
    servicioId: String(servicioId),
    esUrgente: String(Boolean(esUrgente))
  });
  return peticionApi(`/api/cotizaciones/estimar?${params.toString()}`, { token });
}

export function listarMisServiciosApi(token) {
  return peticionApi('/api/profesionales/me/servicios', { token });
}

export function configurarTarifaApi(token, datos) {
  return peticionApi('/api/profesionales/me/tarifas', {
    metodo: 'PUT',
    token,
    cuerpo: datos
  });
}
