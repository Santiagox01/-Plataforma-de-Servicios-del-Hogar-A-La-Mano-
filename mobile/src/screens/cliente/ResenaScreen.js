import React, { useState } from 'react';
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { crearResenaApi } from '../../api/solicitudesApi';
import { useAuth } from '../../context/AuthContext';
import { BotonPrimario, CampoTexto, TarjetaAviso } from '../../components/ComponentesComunes';
import { COLORES, ESPACIADO } from '../../theme/tema';

export default function ResenaScreen({ route, navigation }) {
  const { token } = useAuth();
  const { idSolicitud } = route.params;
  const [estrellas, setEstrellas] = useState(5);
  const [comentario, setComentario] = useState('');
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);

  async function enviar() {
    setError(null);
    setCargando(true);
    try {
      await crearResenaApi(token, idSolicitud, { estrellas, comentario });
      navigation.goBack();
    } catch (e) {
      setError(e.mensaje || 'No se pudo registrar la reseña.');
    } finally {
      setCargando(false);
    }
  }

  return (
    <ScrollView contentContainerStyle={estilos.contenedor}>
      {error ? <TarjetaAviso tipo="error" mensaje={error} /> : null}

      <View style={estilos.tarjeta}>
        <Text style={estilos.titulo}>Califica tu experiencia</Text>
        <View style={estilos.filaEstrellas}>
          {[1, 2, 3, 4, 5].map((num) => (
            <TouchableOpacity
              key={num}
              style={[estilos.estrella, estrellas === num && estilos.estrellaActiva]}
              onPress={() => setEstrellas(num)}
            >
              <Text style={[estilos.textoEstrella, estrellas === num && estilos.textoEstrellaActiva]}>
                ★ {num}
              </Text>
            </TouchableOpacity>
          ))}
        </View>

        <CampoTexto
          etiqueta="Comentario (opcional)"
          valor={comentario}
          onChangeText={setComentario}
          placeholder="Cuéntanos cómo fue el servicio..."
          multiline
        />

        <BotonPrimario titulo="Enviar calificación" onPress={enviar} cargando={cargando} />
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
    borderColor: COLORES.borde
  },
  titulo: {
    fontSize: 17,
    fontWeight: '800',
    color: COLORES.texto,
    marginBottom: 12
  },
  filaEstrellas: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 14
  },
  estrella: {
    borderWidth: 1,
    borderColor: COLORES.borde,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 8
  },
  estrellaActiva: {
    backgroundColor: COLORES.primario,
    borderColor: COLORES.primario
  },
  textoEstrella: {
    fontWeight: '700',
    color: COLORES.texto
  },
  textoEstrellaActiva: {
    color: COLORES.blanco
  }
});
