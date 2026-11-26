import { peticionApi } from './clienteHttp';

export function obtenerConfigPagosApi(token) {
  return peticionApi('/api/metodos-pago/config', { token });
}

export function listarMetodosPagoApi(token) {
  return peticionApi('/api/metodos-pago', { token });
}

export function registrarMetodoPagoApi(token, datos) {
  return peticionApi('/api/metodos-pago', {
    metodo: 'POST',
    token,
    cuerpo: datos
  });
}

export function registrarMetodoPayPalApi(token, correoOAlias = null) {
  return peticionApi('/api/metodos-pago/paypal', {
    metodo: 'POST',
    token,
    cuerpo: { correoOAlias }
  });
}

export function eliminarMetodoPagoApi(token, idMetodoPago) {
  return peticionApi(`/api/metodos-pago/${idMetodoPago}`, {
    metodo: 'DELETE',
    token
  });
}

export function obtenerResumenPagoApi(token, idSolicitud) {
  return peticionApi(`/api/solicitudes/${idSolicitud}/pago/resumen`, { token });
}

export function obtenerPagoSolicitudApi(token, idSolicitud) {
  return peticionApi(`/api/solicitudes/${idSolicitud}/pago`, { token });
}

export function pagarSolicitudApi(token, idSolicitud, idMetodoPago) {
  return peticionApi(`/api/solicitudes/${idSolicitud}/pagar`, {
    metodo: 'POST',
    token,
    cuerpo: { idMetodoPago }
  });
}

export function iniciarPagoExternoApi(token, idSolicitud, appRedirectUrl = null) {
  return peticionApi(`/api/solicitudes/${idSolicitud}/pagar/iniciar`, {
    metodo: 'POST',
    token,
    cuerpo: { appRedirectUrl }
  });
}

export function confirmarPagoExternoApi(token, idSolicitud, idOrdenExterna = null) {
  return peticionApi(`/api/solicitudes/${idSolicitud}/pagar/confirmar`, {
    metodo: 'POST',
    token,
    cuerpo: { idOrdenExterna }
  });
}

export function cancelarPagoExternoApi(token, idSolicitud) {
  return peticionApi(`/api/solicitudes/${idSolicitud}/pagar/cancelar`, {
    metodo: 'POST',
    token
  });
}

export function liberarCustodiaApi(token, idSolicitud) {
  return peticionApi(`/api/solicitudes/${idSolicitud}/pago/liberar`, {
    metodo: 'POST',
    token
  });
}
