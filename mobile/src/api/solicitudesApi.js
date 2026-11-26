import { peticionApi } from './clienteHttp';

export function crearSolicitudApi(token, { idServicio, descripcion, esUrgente, fotos = [] }) {
  const formData = new FormData();
  formData.append('idServicio', String(idServicio));
  formData.append('descripcion', descripcion);
  formData.append('esUrgente', String(Boolean(esUrgente)));

  fotos.forEach((foto, idx) => {
    formData.append('fotos', {
      uri: foto.uri,
      name: foto.name || `foto_${idx + 1}.jpg`,
      type: foto.mimeType || 'image/jpeg'
    });
  });

  return peticionApi('/api/solicitudes', {
    metodo: 'POST',
    token,
    cuerpo: formData,
    esMultipart: true
  });
}

export function listarMisSolicitudesApi(token) {
  return peticionApi('/api/solicitudes/mis-solicitudes', { token });
}

export function obtenerDetalleSolicitudApi(token, idSolicitud) {
  return peticionApi(`/api/solicitudes/${idSolicitud}`, { token });
}

export function aceptarSolicitudApi(token, idSolicitud) {
  return peticionApi(`/api/solicitudes/${idSolicitud}/aceptar`, {
    metodo: 'POST',
    token
  });
}

export function rechazarSolicitudApi(token, idSolicitud, motivo) {
  return peticionApi(`/api/solicitudes/${idSolicitud}/rechazar`, {
    metodo: 'POST',
    token,
    cuerpo: { motivo }
  });
}

export function registrarPrecioFinalApi(token, idSolicitud, precioFinal) {
  return peticionApi(`/api/solicitudes/${idSolicitud}/precio-final`, {
    metodo: 'POST',
    token,
    cuerpo: { precioFinal: Number(precioFinal) }
  });
}

export function confirmarPrecioFinalApi(token, idSolicitud) {
  return peticionApi(`/api/solicitudes/${idSolicitud}/confirmar-precio-final`, {
    metodo: 'POST',
    token
  });
}

export function rechazarPrecioFinalApi(token, idSolicitud, motivo) {
  return peticionApi(`/api/solicitudes/${idSolicitud}/rechazar-precio-final`, {
    metodo: 'POST',
    token,
    cuerpo: { motivo }
  });
}

export function crearResenaApi(token, idSolicitud, { estrellas, comentario }) {
  return peticionApi(`/api/solicitudes/${idSolicitud}/resena`, {
    metodo: 'POST',
    token,
    cuerpo: { estrellas, comentario }
  });
}
