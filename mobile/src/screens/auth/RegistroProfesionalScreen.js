import React, { useEffect, useState } from 'react';
import { ScrollView, StyleSheet, Switch, Text, TouchableOpacity, View } from 'react-native';
import * as DocumentPicker from 'expo-document-picker';
import * as Location from 'expo-location';
import { listarCategoriasApi } from '../../api/profesionalesApi';
import { useAuth } from '../../context/AuthContext';
import { BotonPrimario, CampoTexto, TarjetaAviso } from '../../components/ComponentesComunes';
import { COLORES, ESPACIADO } from '../../theme/tema';

export default function RegistroProfesionalScreen() {
  const { registrarProfesional } = useAuth();
  const [nombre, setNombre] = useState('');
  const [correo, setCorreo] = useState('');
  const [celular, setCelular] = useState('');
  const [contrasena, setContrasena] = useState('');
  const [ubicacion, setUbicacion] = useState('Laureles, Medellín');
  const [latitud, setLatitud] = useState(6.2447);
  const [longitud, setLongitud] = useState(-75.5950);
  const [categorias, setCategorias] = useState([]);
  const [seleccionadas, setSeleccionadas] = useState([]);
  const [documento, setDocumento] = useState(null);
  const [certificaciones, setCertificaciones] = useState([]);
  const [aceptaTerminos, setAceptaTerminos] = useState(false);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    listarCategoriasApi()
      .then((lista) => setCategorias(lista || []))
      .catch(() => setError('No se pudieron cargar las categorías de oficio.'));
  }, []);

  function alternarCategoria(id) {
    setSeleccionadas((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  }

  async function elegirDocumentoIdentidad() {
    const res = await DocumentPicker.getDocumentAsync({
      type: ['image/jpeg', 'image/png', 'application/pdf'],
      copyToCacheDirectory: true
    });
    if (!res.canceled && res.assets?.length) {
      setDocumento(res.assets[0]);
    }
  }

  async function elegirCertificacion() {
    const res = await DocumentPicker.getDocumentAsync({
      type: ['image/jpeg', 'image/png', 'application/pdf'],
      copyToCacheDirectory: true
    });
    if (!res.canceled && res.assets?.length) {
      setCertificaciones((prev) => [...prev, res.assets[0]]);
    }
  }

  async function usarGps() {
    const permiso = await Location.requestForegroundPermissionsAsync();
    if (permiso.status !== 'granted') {
      setError('Permiso de ubicación denegado.');
      return;
    }
    const pos = await Location.getCurrentPositionAsync({});
    setLatitud(pos.coords.latitude);
    setLongitud(pos.coords.longitude);
    setUbicacion('Ubicación GPS actual');
  }

  async function enviar() {
    setError(null);
    setCargando(true);
    try {
      await registrarProfesional({
        nombre,
        correo,
        celular,
        contrasena,
        ubicacion,
        latitud,
        longitud,
        categoriasIds: seleccionadas,
        aceptaTerminos,
        documentoIdentidad: documento,
        certificaciones
      });
    } catch (e) {
      setError(e.mensaje || 'Error al registrar profesional.');
    } finally {
      setCargando(false);
    }
  }

  return (
    <ScrollView contentContainerStyle={estilos.contenedor}>
      {error ? <TarjetaAviso tipo="error" mensaje={error} /> : null}

      <CampoTexto etiqueta="Nombre completo" valor={nombre} onChangeText={setNombre} />
      <CampoTexto etiqueta="Correo electrónico" valor={correo} onChangeText={setCorreo} keyboardType="email-address" />
      <CampoTexto etiqueta="Celular (10 dígitos)" valor={celular} onChangeText={setCelular} keyboardType="phone-pad" />
      <CampoTexto etiqueta="Contraseña" valor={contrasena} onChangeText={setContrasena} secureTextEntry />
      <CampoTexto etiqueta="Zona o dirección base" valor={ubicacion} onChangeText={setUbicacion} />

      <BotonPrimario titulo="Obtener ubicación GPS" variante="contorno" onPress={usarGps} />

      <Text style={estilos.etiqueta}>Categorías de oficio</Text>
      <View style={estilos.filaChips}>
        {categorias.map((cat) => {
          const activa = seleccionadas.includes(cat.idCategoria);
          return (
            <TouchableOpacity
              key={cat.idCategoria}
              style={[estilos.chip, activa && estilos.chipActiva]}
              onPress={() => alternarCategoria(cat.idCategoria)}
            >
              <Text style={[estilos.textoChip, activa && estilos.textoChipActiva]}>{cat.nombre}</Text>
            </TouchableOpacity>
          );
        })}
      </View>

      <Text style={estilos.etiqueta}>Documento de identidad (JPG, PNG o PDF, máx. 10 MB)</Text>
      <BotonPrimario
        titulo={documento ? `Adjunto: ${documento.name}` : 'Adjuntar documento de identidad'}
        variante="secundario"
        onPress={elegirDocumentoIdentidad}
      />

      <BotonPrimario
        titulo={`Agregar certificación opcional (${certificaciones.length})`}
        variante="contorno"
        onPress={elegirCertificacion}
      />

      <View style={estilos.filaSwitch}>
        <Switch value={aceptaTerminos} onValueChange={setAceptaTerminos} />
        <Text style={estilos.textoTerminos}>Acepto términos y verificación documental</Text>
      </View>

      <BotonPrimario titulo="Enviar registro profesional" onPress={enviar} cargando={cargando} />
    </ScrollView>
  );
}

const estilos = StyleSheet.create({
  contenedor: {
    padding: ESPACIADO.md,
    backgroundColor: COLORES.fondo
  },
  etiqueta: {
    fontSize: 13,
    fontWeight: '700',
    color: COLORES.texto,
    marginTop: 8,
    marginBottom: 8
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
    backgroundColor: COLORES.tarjeta,
    borderRadius: 20,
    paddingHorizontal: 12,
    paddingVertical: 7
  },
  chipActiva: {
    backgroundColor: COLORES.primario,
    borderColor: COLORES.primario
  },
  textoChip: {
    fontSize: 13,
    color: COLORES.texto
  },
  textoChipActiva: {
    color: COLORES.blanco,
    fontWeight: '700'
  },
  filaSwitch: {
    flexDirection: 'row',
    alignItems: 'center',
    marginVertical: 12,
    gap: 10
  },
  textoTerminos: {
    flex: 1,
    fontSize: 13,
    color: COLORES.textoSecundario
  }
});
