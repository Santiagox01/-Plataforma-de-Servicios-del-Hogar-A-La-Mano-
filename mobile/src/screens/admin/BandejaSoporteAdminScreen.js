import React, { useEffect, useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { listarCasosSoporteAdminApi, responderCasoSoporteApi } from '../../api/adminApi';
import { useAuth } from '../../context/AuthContext';
import { BotonPrimario, CampoTexto, TarjetaAviso } from '../../components/ComponentesComunes';
import { COLORES, ESPACIADO } from '../../theme/tema';

export default function BandejaSoporteAdminScreen({ navigation }) {
  const { token } = useAuth();
  const [casos, setCasos] = useState([]);
  const [respuestas, setRespuestas] = useState({});
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);

  async function cargar() {
    setError(null);
    setCargando(true);
    try {
      const lista = await listarCasosSoporteAdminApi(token);
      setCasos(lista || []);
    } catch (e) {
      setError(e.mensaje || 'No se pudo cargar la bandeja de soporte.');
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    const unsubscribe = navigation.addListener('focus', cargar);
    return unsubscribe;
  }, [navigation]);

  async function responder(idCaso, cerrar) {
    setError(null);
    try {
      const actualizado = await responderCasoSoporteApi(token, idCaso, {
        respuesta: respuestas[idCaso] || '',
        cerrar
      });
      setCasos((prev) => prev.map((c) => (c.idCaso === idCaso ? actualizado : c)));
    } catch (e) {
      setError(e.mensaje || 'No se pudo registrar la respuesta.');
    }
  }

  return (
    <ScrollView contentContainerStyle={estilos.contenedor}>
      {error ? <TarjetaAviso tipo="error" mensaje={error} /> : null}
      <BotonPrimario titulo="Recargar bandeja de soporte" variante="contorno" onPress={cargar} cargando={cargando} />

      {casos.length === 0 ? (
        <TarjetaAviso tipo="exito" mensaje="No hay casos de soporte registrados." />
      ) : (
        casos.map((c) => (
          <View key={c.idCaso} style={estilos.tarjeta}>
            <View style={estilos.fila}>
              <Text style={estilos.numero}>{c.numeroCaso}</Text>
              <Text style={estilos.estado}>{c.estadoCaso}</Text>
            </View>
            <Text style={estilos.asunto}>{c.asunto}</Text>
            <Text style={estilos.texto}>Usuario: {c.nombreUsuario} {c.idSolicitud ? `• Solicitud #${c.idSolicitud}` : ''}</Text>
            <Text style={estilos.texto}>{c.descripcion}</Text>

            {c.respuestaAdmin ? (
              <TarjetaAviso tipo="info" titulo="Respuesta registrada" mensaje={c.respuestaAdmin} />
            ) : null}

            {c.estadoCaso !== 'CERRADO' ? (
              <View style={estilos.bloqueRespuesta}>
                <CampoTexto
                  etiqueta="Respuesta al usuario"
                  valor={respuestas[c.idCaso] || ''}
                  onChangeText={(txt) => setRespuestas((prev) => ({ ...prev, [c.idCaso]: txt }))}
                  placeholder="Escribe la solución o seguimiento..."
                  multiline
                />
                <BotonPrimario
                  titulo="Responder y cerrar caso"
                  variante="exito"
                  onPress={() => responder(c.idCaso, true)}
                />
              </View>
            ) : null}
          </View>
        ))
      )}
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
  fila: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 4
  },
  numero: {
    fontWeight: '800',
    color: COLORES.primario
  },
  estado: {
    fontWeight: '700',
    color: COLORES.textoSecundario,
    fontSize: 12
  },
  asunto: {
    fontSize: 15,
    fontWeight: '700',
    color: COLORES.texto
  },
  texto: {
    fontSize: 13,
    color: COLORES.textoSecundario,
    marginTop: 4,
    marginBottom: 6
  },
  bloqueRespuesta: {
    marginTop: 8
  }
});
