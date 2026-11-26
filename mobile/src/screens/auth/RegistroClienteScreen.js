import React, { useState } from 'react';
import { ScrollView, StyleSheet, Switch, Text, TouchableOpacity, View } from 'react-native';
import * as Location from 'expo-location';
import { useAuth } from '../../context/AuthContext';
import { BotonPrimario, CampoTexto, TarjetaAviso } from '../../components/ComponentesComunes';
import { COLORES, ESPACIADO } from '../../theme/tema';

const ZONAS_MEDELLIN = [
  { nombre: 'El Poblado, Medellín', lat: 6.2088, lng: -75.5695 },
  { nombre: 'Laureles, Medellín', lat: 6.2447, lng: -75.5950 },
  { nombre: 'Envigado, Antioquia', lat: 6.1719, lng: -75.5863 }
];

export default function RegistroClienteScreen() {
  const { registrarCliente } = useAuth();
  const [nombre, setNombre] = useState('');
  const [correo, setCorreo] = useState('');
  const [celular, setCelular] = useState('');
  const [contrasena, setContrasena] = useState('');
  const [ubicacion, setUbicacion] = useState(ZONAS_MEDELLIN[0].nombre);
  const [latitud, setLatitud] = useState(ZONAS_MEDELLIN[0].lat);
  const [longitud, setLongitud] = useState(ZONAS_MEDELLIN[0].lng);
  const [aceptaTerminos, setAceptaTerminos] = useState(false);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);
  const [detalles, setDetalles] = useState({});

  async function usarGps() {
    const permiso = await Location.requestForegroundPermissionsAsync();
    if (permiso.status !== 'granted') {
      setError('Permiso de ubicación denegado. Selecciona una zona manualmente.');
      return;
    }
    const pos = await Location.getCurrentPositionAsync({});
    setLatitud(pos.coords.latitude);
    setLongitud(pos.coords.longitude);
    setUbicacion('Ubicación GPS actual');
  }

  async function enviar() {
    setError(null);
    setDetalles({});
    setCargando(true);
    try {
      await registrarCliente({
        nombre,
        correo,
        celular,
        contrasena,
        ubicacion,
        latitud,
        longitud,
        aceptaTerminos
      });
    } catch (e) {
      setError(e.mensaje || 'Error al registrar cliente.');
      setDetalles(e.detalles || {});
    } finally {
      setCargando(false);
    }
  }

  return (
    <ScrollView contentContainerStyle={estilos.contenedor}>
      {error ? <TarjetaAviso tipo="error" mensaje={error} /> : null}

      <CampoTexto etiqueta="Nombre completo" valor={nombre} onChangeText={setNombre} error={detalles.nombre} />
      <CampoTexto
        etiqueta="Correo electrónico"
        valor={correo}
        onChangeText={setCorreo}
        keyboardType="email-address"
        error={detalles.correo}
      />
      <CampoTexto
        etiqueta="Celular (10 dígitos)"
        valor={celular}
        onChangeText={setCelular}
        keyboardType="phone-pad"
        placeholder="3001112233"
        error={detalles.celular}
      />
      <CampoTexto
        etiqueta="Contraseña (mín. 8 caracteres, 1 mayúscula y 1 número)"
        valor={contrasena}
        onChangeText={setContrasena}
        secureTextEntry
        error={detalles.contrasena}
      />
      <CampoTexto
        etiqueta="Dirección o zona"
        valor={ubicacion}
        onChangeText={setUbicacion}
        error={detalles.ubicacion}
      />

      <View style={estilos.filaZonas}>
        {ZONAS_MEDELLIN.map((z) => (
          <TouchableOpacity
            key={z.nombre}
            style={estilos.chip}
            onPress={() => {
              setUbicacion(z.nombre);
              setLatitud(z.lat);
              setLongitud(z.lng);
            }}
          >
            <Text style={estilos.textoChip}>{z.nombre.split(',')[0]}</Text>
          </TouchableOpacity>
        ))}
        <TouchableOpacity style={[estilos.chip, estilos.chipGps]} onPress={usarGps}>
          <Text style={estilos.textoChipGps}>Usar GPS</Text>
        </TouchableOpacity>
      </View>

      <View style={estilos.filaSwitch}>
        <Switch value={aceptaTerminos} onValueChange={setAceptaTerminos} />
        <Text style={estilos.textoTerminos}>Acepto los términos y condiciones de A La Mano</Text>
      </View>

      <BotonPrimario titulo="Crear cuenta" onPress={enviar} cargando={cargando} />
    </ScrollView>
  );
}

const estilos = StyleSheet.create({
  contenedor: {
    padding: ESPACIADO.md,
    backgroundColor: COLORES.fondo
  },
  filaZonas: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginBottom: 14
  },
  chip: {
    backgroundColor: COLORES.tarjeta,
    borderWidth: 1,
    borderColor: COLORES.borde,
    borderRadius: 20,
    paddingHorizontal: 10,
    paddingVertical: 6
  },
  chipGps: {
    backgroundColor: COLORES.primarioSuave,
    borderColor: COLORES.primario
  },
  textoChip: {
    fontSize: 12,
    color: COLORES.texto
  },
  textoChipGps: {
    fontSize: 12,
    color: COLORES.primario,
    fontWeight: '700'
  },
  filaSwitch: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 14,
    gap: 10
  },
  textoTerminos: {
    flex: 1,
    fontSize: 13,
    color: COLORES.textoSecundario
  }
});
