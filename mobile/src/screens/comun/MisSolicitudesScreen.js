import React, { useEffect, useState } from 'react';
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { listarMisSolicitudesApi } from '../../api/solicitudesApi';
import { useAuth } from '../../context/AuthContext';
import { BotonPrimario, TarjetaAviso } from '../../components/ComponentesComunes';
import { COLORES, ESPACIADO } from '../../theme/tema';

export default function MisSolicitudesScreen({ navigation }) {
  const { token, usuario } = useAuth();
  const [solicitudes, setSolicitudes] = useState([]);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);

  async function cargar() {
    setCargando(true);
    setError(null);
    try {
      const lista = await listarMisSolicitudesApi(token);
      setSolicitudes(lista || []);
    } catch (e) {
      setError(e.mensaje || 'No se pudieron cargar las solicitudes.');
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    const unsubscribe = navigation.addListener('focus', cargar);
    return unsubscribe;
  }, [navigation]);

  return (
    <ScrollView contentContainerStyle={estilos.contenedor}>
      {error ? <TarjetaAviso tipo="error" mensaje={error} /> : null}
      <BotonPrimario titulo="Actualizar bandeja" variante="contorno" onPress={cargar} cargando={cargando} />

      {solicitudes.length === 0 ? (
        <TarjetaAviso tipo="info" mensaje="No tienes solicitudes registradas todavía." />
      ) : (
        solicitudes.map((s) => (
          <TouchableOpacity
            key={s.idSolicitud}
            style={estilos.tarjeta}
            onPress={() => navigation.navigate('DetalleSolicitud', { idSolicitud: s.idSolicitud })}
          >
            <View style={estilos.fila}>
              <Text style={estilos.categoria}>{s.nombreCategoria}</Text>
              <View style={estilos.badge}>
                <Text style={estilos.textoBadge}>{s.estadoSolicitud}</Text>
              </View>
            </View>
            <Text style={estilos.persona}>
              {usuario?.rol === 'PROFESIONAL' ? `Cliente: ${s.nombreCliente}` : `Profesional: ${s.nombreProfesional}`}
            </Text>
            <Text style={estilos.desc} numberOfLines={2}>{s.descripcion}</Text>
            <View style={estilos.fila}>
              <Text style={estilos.precio}>
                Estimado: ${Number(s.precioEstimado).toLocaleString('es-CO')}
              </Text>
              {s.precioFinal ? (
                <Text style={estilos.precioFinal}>
                  Final: ${Number(s.precioFinal).toLocaleString('es-CO')}
                </Text>
              ) : null}
            </View>
          </TouchableOpacity>
        ))
      )}
    </ScrollView>
  );
}

const estilos = StyleSheet.create({
  contenedor: {
    padding: ESPACIADO.md,
    backgroundColor: COLORES.fondo,
    flexGrow: 1
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
    alignItems: 'center'
  },
  categoria: {
    fontSize: 16,
    fontWeight: '800',
    color: COLORES.texto
  },
  badge: {
    backgroundColor: COLORES.primarioSuave,
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 12
  },
  textoBadge: {
    color: COLORES.primario,
    fontWeight: '700',
    fontSize: 11
  },
  persona: {
    fontSize: 13,
    fontWeight: '600',
    color: COLORES.textoSecundario,
    marginTop: 4
  },
  desc: {
    fontSize: 13,
    color: COLORES.texto,
    marginVertical: 6
  },
  precio: {
    fontSize: 13,
    fontWeight: '700',
    color: COLORES.textoSecundario
  },
  precioFinal: {
    fontSize: 13,
    fontWeight: '800',
    color: COLORES.exito
  }
});
