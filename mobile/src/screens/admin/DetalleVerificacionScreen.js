import React, { useEffect, useState } from 'react';
import { Linking, ScrollView, StyleSheet, Text, View } from 'react-native';
import {
  aprobarProfesionalApi,
  obtenerUrlsDocumentosApi,
  rechazarProfesionalApi
} from '../../api/adminApi';
import { useAuth } from '../../context/AuthContext';
import { BotonPrimario, CampoTexto, TarjetaAviso } from '../../components/ComponentesComunes';
import { COLORES, ESPACIADO } from '../../theme/tema';

export default function DetalleVerificacionScreen({ route, navigation }) {
  const { token } = useAuth();
  const { profesional } = route.params;
  const [urls, setUrls] = useState(null);
  const [motivoRechazo, setMotivoRechazo] = useState('');
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    obtenerUrlsDocumentosApi(token, profesional.idProfesional)
      .then(setUrls)
      .catch((e) => setError(e.mensaje || 'No se pudieron generar las URLs temporales.'));
  }, [profesional.idProfesional, token]);

  async function aprobar() {
    setError(null);
    setCargando(true);
    try {
      await aprobarProfesionalApi(token, profesional.idProfesional);
      navigation.goBack();
    } catch (e) {
      setError(e.mensaje || 'No fue posible aprobar el profesional.');
    } finally {
      setCargando(false);
    }
  }

  async function rechazar() {
    setError(null);
    setCargando(true);
    try {
      await rechazarProfesionalApi(token, profesional.idProfesional, motivoRechazo);
      navigation.goBack();
    } catch (e) {
      setError(e.mensaje || 'Debes indicar un motivo para rechazar.');
    } finally {
      setCargando(false);
    }
  }

  return (
    <ScrollView contentContainerStyle={estilos.contenedor}>
      {error ? <TarjetaAviso tipo="error" mensaje={error} /> : null}

      <View style={estilos.tarjeta}>
        <Text style={estilos.nombre}>{profesional.nombre}</Text>
        <Text style={estilos.detalle}>{profesional.correo} • {profesional.celular}</Text>
        <Text style={estilos.detalle}>Ubicación: {profesional.ubicacion}</Text>
        <Text style={estilos.detalle}>Oficios: {(profesional.categorias || []).join(', ')}</Text>
      </View>

      <View style={estilos.tarjeta}>
        <Text style={estilos.titulo}>Revisión documental (enlace temporal seguro)</Text>
        {urls ? (
          <View>
            <BotonPrimario
              titulo="Ver documento de identidad (inline)"
              variante="contorno"
              onPress={() => Linking.openURL(urls.urlDocumentoIdentidad)}
            />
            {urls.urlsCertificaciones?.map((u, idx) => (
              <BotonPrimario
                key={idx}
                titulo={`Ver certificación #${idx + 1}`}
                variante="contorno"
                onPress={() => Linking.openURL(u)}
              />
            ))}
          </View>
        ) : (
          <Text style={estilos.detalle}>Cargando enlace temporal...</Text>
        )}
      </View>

      <View style={estilos.tarjeta}>
        <BotonPrimario titulo="Aprobar profesional" variante="exito" onPress={aprobar} cargando={cargando} />
        <CampoTexto
          etiqueta="Motivo (obligatorio solo si rechazas)"
          valor={motivoRechazo}
          onChangeText={setMotivoRechazo}
          placeholder="Ej. Documento ilegible o incompleto"
          multiline
        />
        <BotonPrimario titulo="Rechazar verificación" variante="peligro" onPress={rechazar} cargando={cargando} />
      </View>
    </ScrollView>
  );
}

const estilos = StyleSheet.create({
  contenedor: {
    padding: ESPACIADO.md,
    backgroundColor: COLORES.fondo
  },
  tarjeta: {
    backgroundColor: COLORES.tarjeta,
    borderRadius: 12,
    padding: ESPACIADO.md,
    borderWidth: 1,
    borderColor: COLORES.borde,
    marginBottom: 12
  },
  nombre: {
    fontSize: 18,
    fontWeight: '800',
    color: COLORES.texto
  },
  titulo: {
    fontSize: 14,
    fontWeight: '700',
    color: COLORES.texto,
    marginBottom: 8
  },
  detalle: {
    fontSize: 13,
    color: COLORES.textoSecundario,
    marginTop: 4
  }
});
