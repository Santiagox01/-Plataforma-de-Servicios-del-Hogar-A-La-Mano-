import { peticionApi } from './clienteHttp';

export function listarMotivosReporteApi(token) {
  return peticionApi('/api/reportes/motivos', { token });
}

export function crearReporteApi(token, datos) {
  return peticionApi('/api/reportes', {
    metodo: 'POST',
    token,
    cuerpo: datos
  });
}

export function listarReportesEnviadosApi(token) {
  return peticionApi('/api/reportes/mis-enviados', { token });
}

export function listarReportesRecibidosApi(token) {
  return peticionApi('/api/reportes/recibidos', { token });
}

export function crearCasoSoporteApi(token, datos) {
  return peticionApi('/api/soporte/casos', {
    metodo: 'POST',
    token,
    cuerpo: datos
  });
}

export function listarMisCasosSoporteApi(token) {
  return peticionApi('/api/soporte/mis-casos', { token });
}
