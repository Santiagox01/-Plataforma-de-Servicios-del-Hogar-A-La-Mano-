import React, { useState } from 'react';
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { buscarProfesionalesApi } from '../../api/profesionalesApi';
import { useAuth } from '../../context/AuthContext';
import { BotonPrimario, TarjetaAviso } from '../../components/ComponentesComunes';
import { COLORES, ESPACIADO } from '../../theme/tema';

export default function ResultadosScreen({ route, navigation }) {
  const { token } = useAuth();
  const { resultado: resultadoInicial, categoria, latitud, longitud } = route.params;
  const [datos, setDatos] = useState(resultadoInicial);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);

  async function ampliarRadio() {
    setError(null);
    setCargando(true);
    try {
      const nuevoRadio = Math.round(datos.radioAplicadoKm * 2);
      const nuevo = await buscarProfesionalesApi(token, {
        categoriaId: categoria.idCategoria,
        latitud,
        longitud,
        radioKm: nuevoRadio
      });
      setDatos(nuevo);
    } catch (e) {
      setError(e.mensaje || 'No fue posible ampliar la búsqueda.');
    } finally {
      setCargando(false);
    }
  }

  return (
    <ScrollView contentContainerStyle={estilos.contenedor}>
      <Text style={estilos.encabezado}>
        {categoria.nombre} • {datos.totalResultados} resultado(s) en {datos.radioAplicadoKm} km
      </Text>

      {error ? <TarjetaAviso tipo="error" mensaje={error} /> : null}

      {datos.resultados.length === 0 ? (
        <View>
          <TarjetaAviso tipo="alerta" titulo="Sin resultados cercanos" mensaje={datos.mensajeSugerencia} />
          <BotonPrimario
            titulo={`Ampliar búsqueda a ${Math.round(datos.radioAplicadoKm * 2)} km`}
            onPress={ampliarRadio}
            cargando={cargando}
          />
        </View>
      ) : (
        datos.resultados.map((pro) => (
          <TouchableOpacity
            key={pro.idProfesional}
            style={estilos.tarjeta}
            onPress={() => navigation.navigate('PerfilProfesional', { idProfesional: pro.idProfesional })}
          >
            <View style={estilos.filaSuperior}>
              <Text style={estilos.nombre}>{pro.nombre}</Text>
              <View style={estilos.sello}>
                <Text style={estilos.textoSello}>{pro.selloVerificacion}</Text>
              </View>
            </View>
            <Text style={estilos.ubicacion}>{pro.ubicacion} • {pro.distanciaKm} km</Text>
            <View style={estilos.filaInferior}>
              <Text style={estilos.calificacion}>★ {pro.calificacionPromedio.toFixed(1)} ({pro.totalResenas} reseñas)</Text>
              {pro.tarifaBase ? (
                <Text style={estilos.tarifa}>Base: ${Number(pro.tarifaBase).toLocaleString('es-CO')}</Text>
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
  encabezado: {
    fontSize: 14,
    fontWeight: '700',
    color: COLORES.textoSecundario,
    marginBottom: 12
  },
  tarjeta: {
    backgroundColor: COLORES.tarjeta,
    borderRadius: 12,
    padding: ESPACIADO.md,
    borderWidth: 1,
    borderColor: COLORES.borde,
    marginBottom: 12
  },
  filaSuperior: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center'
  },
  nombre: {
    fontSize: 16,
    fontWeight: '700',
    color: COLORES.texto
  },
  sello: {
    backgroundColor: COLORES.exitoFondo,
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 12
  },
  textoSello: {
    color: COLORES.exito,
    fontSize: 12,
    fontWeight: '700'
  },
  ubicacion: {
    color: COLORES.textoSecundario,
    fontSize: 13,
    marginTop: 4
  },
  filaInferior: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 10
  },
  calificacion: {
    fontSize: 13,
    fontWeight: '600',
    color: COLORES.texto
  },
  tarifa: {
    fontSize: 13,
    fontWeight: '700',
    color: COLORES.primario
  }
});
