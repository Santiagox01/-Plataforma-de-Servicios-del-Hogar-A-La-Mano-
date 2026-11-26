import React, { useEffect, useState } from 'react';
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import * as Location from 'expo-location';
import { buscarProfesionalesApi, listarCategoriasApi } from '../../api/profesionalesApi';
import { useAuth } from '../../context/AuthContext';
import { BotonPrimario, CampoTexto, EncabezadoRol, TarjetaAviso } from '../../components/ComponentesComunes';
import { COLORES, ESPACIADO } from '../../theme/tema';

const ZONAS_MANUALES = [
  { nombre: 'El Poblado', lat: 6.2088, lng: -75.5695 },
  { nombre: 'Laureles', lat: 6.2447, lng: -75.5950 },
  { nombre: 'Envigado', lat: 6.1719, lng: -75.5863 }
];

export default function BusquedaScreen({ navigation }) {
  const { token, usuario, cerrarSesion } = useAuth();
  const [categorias, setCategorias] = useState([]);
  const [categoriaSeleccionada, setCategoriaSeleccionada] = useState(null);
  const [latitud, setLatitud] = useState(usuario?.latitud || 6.2088);
  const [longitud, setLongitud] = useState(usuario?.longitud || -75.5695);
  const [etiquetaUbicacion, setEtiquetaUbicacion] = useState(usuario?.ubicacion || 'El Poblado');
  const [radioKm, setRadioKm] = useState('10');
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    listarCategoriasApi()
      .then((lista) => {
        setCategorias(lista || []);
        if (lista?.length) {
          setCategoriaSeleccionada(lista[0]);
        }
      })
      .catch(() => setError('No se pudieron cargar las categorías.'));
  }, []);

  async function usarGps() {
    setError(null);
    const permiso = await Location.requestForegroundPermissionsAsync();
    if (permiso.status !== 'granted') {
      setError('Permiso de ubicación denegado. Elige una zona manual.');
      return;
    }
    const pos = await Location.getCurrentPositionAsync({});
    setLatitud(pos.coords.latitude);
    setLongitud(pos.coords.longitude);
    setEtiquetaUbicacion('GPS actual');
  }

  async function buscar() {
    if (!categoriaSeleccionada) {
      setError('Selecciona una categoría.');
      return;
    }
    setError(null);
    setCargando(true);
    try {
      const resultado = await buscarProfesionalesApi(token, {
        categoriaId: categoriaSeleccionada.idCategoria,
        latitud,
        longitud,
        radioKm: Number(radioKm) || 10
      });
      navigation.navigate('Resultados', {
        resultado,
        categoria: categoriaSeleccionada,
        latitud,
        longitud
      });
    } catch (e) {
      setError(e.mensaje || 'Error al buscar profesionales.');
    } finally {
      setCargando(false);
    }
  }

  return (
    <ScrollView contentContainerStyle={estilos.contenedor}>
      <EncabezadoRol usuario={usuario} onCerrarSesion={cerrarSesion} />

      {error ? <TarjetaAviso tipo="error" mensaje={error} /> : null}

      <View style={estilos.tarjeta}>
        <Text style={estilos.titulo}>1. Elige el servicio que necesitas</Text>
        <View style={estilos.filaChips}>
          {categorias.map((cat) => {
            const activa = categoriaSeleccionada?.idCategoria === cat.idCategoria;
            return (
              <TouchableOpacity
                key={cat.idCategoria}
                style={[estilos.chip, activa && estilos.chipActiva]}
                onPress={() => setCategoriaSeleccionada(cat)}
              >
                <Text style={[estilos.textoChip, activa && estilos.textoChipActiva]}>{cat.nombre}</Text>
              </TouchableOpacity>
            );
          })}
        </View>

        <Text style={estilos.titulo}>2. Tu ubicación: {etiquetaUbicacion}</Text>
        <View style={estilos.filaChips}>
          {ZONAS_MANUALES.map((z) => (
            <TouchableOpacity
              key={z.nombre}
              style={estilos.chip}
              onPress={() => {
                setLatitud(z.lat);
                setLongitud(z.lng);
                setEtiquetaUbicacion(z.nombre);
              }}
            >
              <Text style={estilos.textoChip}>{z.nombre}</Text>
            </TouchableOpacity>
          ))}
        </View>
        <BotonPrimario titulo="Usar mi ubicación GPS" variante="contorno" onPress={usarGps} />

        <CampoTexto
          etiqueta="Radio máximo de búsqueda (km)"
          valor={radioKm}
          onChangeText={setRadioKm}
          keyboardType="numeric"
        />

        <BotonPrimario titulo="Buscar profesionales verificados" onPress={buscar} cargando={cargando} />
      </View>

      <BotonPrimario
        titulo="Ver mis solicitudes"
        variante="secundario"
        onPress={() => navigation.navigate('MisSolicitudes')}
      />
      <BotonPrimario
        titulo="Mis métodos de pago"
        variante="contorno"
        onPress={() => navigation.navigate('MetodosPago')}
      />
      <BotonPrimario
        titulo="Ayuda y soporte"
        variante="contorno"
        onPress={() => navigation.navigate('Soporte')}
      />
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
    borderRadius: 14,
    padding: ESPACIADO.md,
    borderWidth: 1,
    borderColor: COLORES.borde
  },
  titulo: {
    fontSize: 15,
    fontWeight: '700',
    color: COLORES.texto,
    marginBottom: 10,
    marginTop: 6
  },
  filaChips: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginBottom: 12
  },
  chip: {
    borderWidth: 1,
    borderColor: COLORES.borde,
    borderRadius: 20,
    paddingHorizontal: 12,
    paddingVertical: 7,
    backgroundColor: COLORES.fondo
  },
  chipActiva: {
    backgroundColor: COLORES.primario,
    borderColor: COLORES.primario
  },
  textoChip: {
    color: COLORES.texto,
    fontSize: 13
  },
  textoChipActiva: {
    color: COLORES.blanco,
    fontWeight: '700'
  }
});
