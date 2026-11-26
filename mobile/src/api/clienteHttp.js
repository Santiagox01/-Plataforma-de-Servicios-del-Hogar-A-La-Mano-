import { API_BASE_URL } from '../config/apiConfig';

export class ErrorApi extends Error {
  constructor(mensaje, estado, detalles = null) {
    super(mensaje);
    this.estado = estado;
    this.detalles = detalles;
  }
}

export async function peticionApi(ruta, { metodo = 'GET', cuerpo = null, token = null, esMultipart = false } = {}) {
  const headers = { Accept: 'application/json' };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  if (!esMultipart && cuerpo !== null) {
    headers['Content-Type'] = 'application/json';
  }

  const respuesta = await fetch(`${API_BASE_URL}${ruta}`, {
    method: metodo,
    headers,
    body: cuerpo === null ? undefined : (esMultipart ? cuerpo : JSON.stringify(cuerpo))
  });

  const texto = await respuesta.text();
  let datos = null;
  if (texto) {
    try {
      datos = JSON.parse(texto);
    } catch {
      datos = { mensaje: texto };
    }
  }

  if (!respuesta.ok) {
    const mensaje = datos?.mensaje || 'No fue posible completar la solicitud.';
    throw new ErrorApi(mensaje, respuesta.status, datos?.detalles || null);
  }

  return datos;
}
