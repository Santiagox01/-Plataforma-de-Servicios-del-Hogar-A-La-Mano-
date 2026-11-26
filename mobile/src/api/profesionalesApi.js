import { peticionApi } from './clienteHttp';

export function listarCategoriasApi() {
  return peticionApi('/api/categorias');
}

export function buscarProfesionalesApi(token, { categoriaId, latitud, longitud, radioKm }) {
  const params = new URLSearchParams({
    categoriaId: String(categoriaId),
    latitud: String(latitud),
    longitud: String(longitud)
  });
  if (radioKm) {
    params.append('radioKm', String(radioKm));
  }
  return peticionApi(`/api/profesionales/buscar?${params.toString()}`, { token });
}

export function consultarPerfilProfesionalApi(token, idProfesional) {
  return peticionApi(`/api/profesionales/${idProfesional}`, { token });
}

export function actualizarDisponibilidadApi(token, disponible) {
  return peticionApi('/api/profesionales/me/disponibilidad', {
    metodo: 'PATCH',
    token,
    cuerpo: { disponible }
  });
}

export function recargarDocumentosProfesionalApi(token, documentoIdentidad) {
  const formData = new FormData();
  formData.append('documentoIdentidad', {
    uri: documentoIdentidad.uri,
    name: documentoIdentidad.name || 'documento.pdf',
    type: documentoIdentidad.mimeType || 'application/pdf'
  });

  return peticionApi('/api/profesionales/me/documentos', {
    metodo: 'POST',
    token,
    cuerpo: formData,
    esMultipart: true
  });
}
