import { peticionApi } from './clienteHttp';

export function listarNotificacionesApi(token) {
  return peticionApi('/api/notificaciones', { token });
}

export function marcarNotificacionLeidaApi(token, idNotificacion) {
  return peticionApi(`/api/notificaciones/${idNotificacion}/leer`, {
    metodo: 'PATCH',
    token
  });
}
