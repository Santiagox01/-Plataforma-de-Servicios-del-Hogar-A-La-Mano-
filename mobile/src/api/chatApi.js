import { peticionApi } from './clienteHttp';

export function listarMensajesApi(token, idSolicitud) {
  return peticionApi(`/api/solicitudes/${idSolicitud}/chat/mensajes`, { token });
}

export function enviarMensajeApi(token, idSolicitud, contenido) {
  return peticionApi(`/api/solicitudes/${idSolicitud}/chat/mensajes`, {
    metodo: 'POST',
    token,
    cuerpo: { contenido }
  });
}

export function marcarMensajeLeidoApi(token, idSolicitud, idMensaje) {
  return peticionApi(`/api/solicitudes/${idSolicitud}/chat/mensajes/${idMensaje}/leer`, {
    metodo: 'PATCH',
    token
  });
}
