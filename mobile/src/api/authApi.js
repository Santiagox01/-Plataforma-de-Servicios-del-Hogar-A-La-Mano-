import { peticionApi } from './clienteHttp';

export function iniciarSesionApi(correo, contrasena) {
  return peticionApi('/api/auth/login', {
    metodo: 'POST',
    cuerpo: { correo, contrasena }
  });
}

export function registrarClienteApi(datos) {
  return peticionApi('/api/auth/registro/cliente', {
    metodo: 'POST',
    cuerpo: datos
  });
}

export function registrarProfesionalApi({
  nombre,
  correo,
  celular,
  contrasena,
  ubicacion,
  latitud,
  longitud,
  categoriasIds,
  aceptaTerminos,
  documentoIdentidad,
  certificaciones = []
}) {
  const formData = new FormData();
  formData.append('nombre', nombre);
  formData.append('correo', correo);
  formData.append('celular', celular);
  formData.append('contrasena', contrasena);
  formData.append('ubicacion', ubicacion);
  formData.append('latitud', String(latitud));
  formData.append('longitud', String(longitud));
  formData.append('aceptaTerminos', String(Boolean(aceptaTerminos)));

  categoriasIds.forEach((id) => {
    formData.append('categoriasIds', String(id));
  });

  if (documentoIdentidad) {
    formData.append('documentoIdentidad', {
      uri: documentoIdentidad.uri,
      name: documentoIdentidad.name || 'documento.pdf',
      type: documentoIdentidad.mimeType || 'application/pdf'
    });
  }

  certificaciones.forEach((cert, idx) => {
    formData.append('certificaciones', {
      uri: cert.uri,
      name: cert.name || `certificacion_${idx + 1}.pdf`,
      type: cert.mimeType || 'application/pdf'
    });
  });

  return peticionApi('/api/auth/registro/profesional', {
    metodo: 'POST',
    cuerpo: formData,
    esMultipart: true
  });
}

export function cerrarSesionApi(token) {
  return peticionApi('/api/auth/logout', {
    metodo: 'POST',
    token
  });
}

export function obtenerMiPerfilApi(token) {
  return peticionApi('/api/auth/me', { token });
}
