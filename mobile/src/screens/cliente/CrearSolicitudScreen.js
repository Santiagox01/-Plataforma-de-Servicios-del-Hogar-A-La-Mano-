import React, { useEffect, useState } from 'react';
import { ScrollView, StyleSheet, Switch, Text, TouchableOpacity, View } from 'react-native';
import * as DocumentPicker from 'expo-document-picker';
import { obtenerCotizacionApi } from '../../api/tarifasApi';
import { crearSolicitudApi } from '../../api/solicitudesApi';
import { useAuth } from '../../context/AuthContext';
import { BotonPrimario, CampoTexto, TarjetaAviso } from '../../components/ComponentesComunes';
import { COLORES, ESPACIADO } from '../../theme/tema';

export default function CrearSolicitudScreen({ route, navigation }) {
  const { token } = useAuth();
  const { nombreProfesional, servicios = [] } = route.params;
  const [servicioSeleccionado, setServicioSeleccionado] = useState(servicios[0] || null);
  const [descripcion, setDescripcion] = useState('');
  const [esUrgente, setEsUrgente] = useState(false);
  const [fotos, setFotos] = useState([]);
  const [cotizacion, setCotizacion] = useState(null);
  const [errorCotizacion, setErrorCotizacion] = useState(null);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!servicioSeleccionado) return;
    setErrorCotizacion(null);
    obtenerCotizacionApi(token, servicioSeleccionado.idServicio, esUrgente)
      .then(setCotizacion)
      .catch((e) => {
        setCotizacion(null);
        setErrorCotizacion(e.mensaje || 'No se pudo calcular el precio estimado.');
      });
  }, [servicioSeleccionado, esUrgente, token]);

  async function agregarFoto() {
    const res = await DocumentPicker.getDocumentAsync({
      type: ['image/jpeg', 'image/png'],
      copyToCacheDirectory: true
    });
    if (!res.canceled && res.assets?.length) {
      setFotos((prev) => [...prev, res.assets[0]]);
    }
  }

  function quitarFoto(indice) {
    setFotos((prev) => prev.filter((_, idx) => idx !== indice));
  }

  async function enviarSolicitud() {
    if (!servicioSeleccionado) return;
    setError(null);
    setCargando(true);
    try {
      const creada = await crearSolicitudApi(token, {
        idServicio: servicioSeleccionado.idServicio,
        descripcion,
        esUrgente,
        fotos
      });
      navigation.replace('DetalleSolicitud', { idSolicitud: creada.idSolicitud });
    } catch (e) {
      setError(e.mensaje || 'No fue posible enviar la solicitud.');
    } finally {
      setCargando(false);
    }
  }

  return (
    <ScrollView contentContainerStyle={estilos.contenedor}>
      <Text style={estilos.tituloPro}>Solicitar servicio a {nombreProfesional}</Text>

      {error ? <TarjetaAviso tipo="error" mensaje={error} /> : null}

      <View style={estilos.tarjeta}>
        <Text style={estilos.etiqueta}>Categoría del servicio</Text>
        <View style={estilos.filaChips}>
          {servicios.map((srv) => {
            const activo = servicioSeleccionado?.idServicio === srv.idServicio;
            return (
              <TouchableOpacity
                key={srv.idServicio}
                style={[estilos.chip, activo && estilos.chipActivo]}
                onPress={() => setServicioSeleccionado(srv)}
              >
                <Text style={[estilos.textoChip, activo && estilos.textoChipActivo]}>{srv.nombreCategoria}</Text>
              </TouchableOpacity>
            );
          })}
        </View>

        <CampoTexto
          etiqueta="Descripción del problema"
          valor={descripcion}
          onChangeText={setDescripcion}
          placeholder="Describe qué necesitas reparar o revisar..."
          multiline
        />

        <View style={estilos.filaSwitch}>
          <View style={{ flex: 1 }}>
            <Text style={estilos.etiquetaSwitch}>¿Requiere atención urgente?</Text>
            <Text style={estilos.subSwitch}>Se recalcula el precio estimado al instante en el servidor</Text>
          </View>
          <Switch value={esUrgente} onValueChange={setEsUrgente} />
        </View>
      </View>

      <View style={estilos.tarjeta}>
        <Text style={estilos.etiqueta}>Cotización en vivo</Text>
        {errorCotizacion ? (
          <TarjetaAviso tipo="alerta" mensaje={errorCotizacion} />
        ) : cotizacion ? (
          <View>
            <Text style={estilos.precioEstimado}>
              Precio estimado: ${Number(cotizacion.precioEstimado).toLocaleString('es-CO')} COP
            </Text>
            <Text style={estilos.detalle}>{cotizacion.detalleCalculo}</Text>
            <Text style={estilos.detalle}>
              Rango configurado: ${Number(cotizacion.precioMin).toLocaleString('es-CO')} - ${Number(cotizacion.precioMax).toLocaleString('es-CO')}
            </Text>
            <Text style={estilos.detalle}>
              Tasa de garantía (al pagar): ${Number(cotizacion.tasaGarantiaInformativa).toLocaleString('es-CO')} COP
            </Text>
          </View>
        ) : (
          <Text style={estilos.detalle}>Calculando cotización...</Text>
        )}
      </View>

      <View style={estilos.tarjeta}>
        <Text style={estilos.etiqueta}>Fotografías del problema (opcional, máx. 3)</Text>
        <BotonPrimario titulo={`Adjuntar foto (${fotos.length}/3)`} variante="contorno" onPress={agregarFoto} />
        {fotos.map((f, idx) => (
          <View key={idx} style={estilos.filaFoto}>
            <Text style={estilos.nombreFoto} numberOfLines={1}>{f.name || `Foto ${idx + 1}`}</Text>
            <TouchableOpacity onPress={() => quitarFoto(idx)}>
              <Text style={estilos.quitar}>Quitar</Text>
            </TouchableOpacity>
          </View>
        ))}
      </View>

      <BotonPrimario titulo="Enviar solicitud" onPress={enviarSolicitud} cargando={cargando} />
    </ScrollView>
  );
}

const estilos = StyleSheet.create({
  contenedor: {
    padding: ESPACIADO.md,
    backgroundColor: COLORES.fondo
  },
  tituloPro: {
    fontSize: 18,
    fontWeight: '800',
    color: COLORES.texto,
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
  etiqueta: {
    fontSize: 13,
    fontWeight: '700',
    color: COLORES.texto,
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
    borderRadius: 20,
    paddingHorizontal: 12,
    paddingVertical: 6
  },
  chipActivo: {
    backgroundColor: COLORES.primario,
    borderColor: COLORES.primario
  },
  textoChip: {
    color: COLORES.texto,
    fontSize: 13
  },
  textoChipActivo: {
    color: COLORES.blanco,
    fontWeight: '700'
  },
  filaSwitch: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: 6
  },
  etiquetaSwitch: {
    fontSize: 14,
    fontWeight: '700',
    color: COLORES.texto
  },
  subSwitch: {
    fontSize: 12,
    color: COLORES.textoSecundario
  },
  precioEstimado: {
    fontSize: 20,
    fontWeight: '800',
    color: COLORES.primario,
    marginBottom: 6
  },
  detalle: {
    fontSize: 13,
    color: COLORES.textoSecundario,
    marginTop: 2
  },
  filaFoto: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 6,
    borderBottomWidth: 1,
    borderBottomColor: COLORES.borde
  },
  nombreFoto: {
    flex: 1,
    fontSize: 13,
    color: COLORES.texto
  },
  quitar: {
    color: COLORES.peligro,
    fontWeight: '700',
    marginLeft: 10
  }
});
