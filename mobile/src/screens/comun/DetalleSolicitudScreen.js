import React, { useEffect, useState } from 'react';
import { ActivityIndicator, Linking, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import {
  aceptarSolicitudApi,
  confirmarPrecioFinalApi,
  obtenerDetalleSolicitudApi,
  rechazarPrecioFinalApi,
  rechazarSolicitudApi,
  registrarPrecioFinalApi
} from '../../api/solicitudesApi';
import { liberarCustodiaApi } from '../../api/pagosApi';
import { useAuth } from '../../context/AuthContext';
import { BotonPrimario, CampoTexto, TarjetaAviso } from '../../components/ComponentesComunes';
import { COLORES, ESPACIADO } from '../../theme/tema';

export default function DetalleSolicitudScreen({ route, navigation }) {
  const { token, usuario } = useAuth();
  const { idSolicitud } = route.params;
  const [solicitud, setSolicitud] = useState(null);
  const [sugerencias, setSugerencias] = useState([]);
  const [motivo, setMotivo] = useState('');
  const [precioFinalInput, setPrecioFinalInput] = useState('');
  const [cargando, setCargando] = useState(true);
  const [procesando, setProcesando] = useState(false);
  const [error, setError] = useState(null);

  async function cargar() {
    setError(null);
    setCargando(true);
    try {
      const detalle = await obtenerDetalleSolicitudApi(token, idSolicitud);
      setSolicitud(detalle);
    } catch (e) {
      setError(e.mensaje || 'No se pudo cargar la solicitud.');
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    const unsubscribe = navigation.addListener('focus', cargar);
    return unsubscribe;
  }, [navigation, idSolicitud]);

  async function ejecutarAccion(fn) {
    setError(null);
    setProcesando(true);
    try {
      const resultado = await fn();
      setSolicitud(resultado);
    } catch (e) {
      setError(e.mensaje || 'No se pudo completar la acción.');
    } finally {
      setProcesando(false);
    }
  }

  async function manejarLiberacion() {
    setError(null);
    setProcesando(true);
    try {
      await liberarCustodiaApi(token, idSolicitud);
      await cargar();
    } catch (e) {
      setError(e.mensaje || 'No fue posible liberar la custodia.');
    } finally {
      setProcesando(false);
    }
  }

  async function manejarRechazo() {
    setError(null);
    setProcesando(true);
    try {
      const resp = await rechazarSolicitudApi(token, idSolicitud, motivo);
      setSolicitud(resp.solicitud);
      setSugerencias(resp.profesionalesSugeridos || []);
    } catch (e) {
      setError(e.mensaje || 'No se pudo rechazar la solicitud.');
    } finally {
      setProcesando(false);
    }
  }

  if (cargando) {
    return (
      <View style={estilos.centrado}>
        <ActivityIndicator size="large" color={COLORES.primario} />
      </View>
    );
  }

  if (!solicitud) {
    return (
      <View style={estilos.contenedor}>
        <TarjetaAviso tipo="error" mensaje={error || 'Solicitud no encontrada.'} />
      </View>
    );
  }

  const esProfesional = usuario?.rol === 'PROFESIONAL';
  const esCliente = usuario?.rol === 'CLIENTE';

  return (
    <ScrollView contentContainerStyle={estilos.contenedor}>
      {error ? <TarjetaAviso tipo="error" mensaje={error} /> : null}

      <View style={estilos.tarjeta}>
        <Text style={estilos.titulo}>Solicitud #{solicitud.idSolicitud} • {solicitud.nombreCategoria}</Text>
        <Text style={estilos.estado}>Estado: {solicitud.estadoSolicitud}</Text>
        <Text style={estilos.texto}>Cliente: {solicitud.nombreCliente}</Text>
        <Text style={estilos.texto}>Profesional: {solicitud.nombreProfesional}</Text>
        <Text style={estilos.texto}>Modalidad: {solicitud.esUrgente ? 'Urgente' : 'Estándar'}</Text>
        <Text style={estilos.texto}>Descripción: {solicitud.descripcion}</Text>
        <Text style={estilos.precio}>
          Precio estimado: ${Number(solicitud.precioEstimado).toLocaleString('es-CO')} COP
        </Text>
        {solicitud.precioFinal ? (
          <Text style={estilos.precioFinal}>
            Precio final: ${Number(solicitud.precioFinal).toLocaleString('es-CO')} COP (
            {solicitud.precioFinalConfirmado ? 'Confirmado' : 'Pendiente de confirmación'})
          </Text>
        ) : null}
        {solicitud.motivoRechazo ? (
          <TarjetaAviso tipo="alerta" titulo="Motivo del rechazo" mensaje={solicitud.motivoRechazo} />
        ) : null}
      </View>

      {solicitud.urlsFotos?.length > 0 ? (
        <View style={estilos.tarjeta}>
          <Text style={estilos.subtitulo}>Fotografías adjuntas ({solicitud.urlsFotos.length})</Text>
          {solicitud.urlsFotos.map((url, idx) => (
            <BotonPrimario
              key={idx}
              titulo={`Ver foto #${idx + 1}`}
              variante="contorno"
              onPress={() => Linking.openURL(url)}
            />
          ))}
        </View>
      ) : null}

      {solicitud.chatHabilitado ? (
        <BotonPrimario
          titulo="Abrir chat de coordinación"
          variante="secundario"
          onPress={() => navigation.navigate('Chat', { idSolicitud: solicitud.idSolicitud })}
        />
      ) : null}

      {esProfesional && solicitud.estadoSolicitud === 'PENDIENTE' ? (
        <View style={estilos.tarjeta}>
          <BotonPrimario
            titulo="Aceptar solicitud"
            variante="exito"
            onPress={() => ejecutarAccion(() => aceptarSolicitudApi(token, idSolicitud))}
            cargando={procesando}
          />
          <CampoTexto
            etiqueta="Motivo de rechazo (opcional)"
            valor={motivo}
            onChangeText={setMotivo}
            placeholder="Ej. Sin disponibilidad en ese horario"
          />
          <BotonPrimario
            titulo="Rechazar solicitud"
            variante="peligro"
            onPress={manejarRechazo}
            cargando={procesando}
          />
        </View>
      ) : null}

      {esProfesional && ['ACEPTADA', 'EN_CAMINO', 'EN_PROGRESO'].includes(solicitud.estadoSolicitud) && !solicitud.precioFinal ? (
        <View style={estilos.tarjeta}>
          <CampoTexto
            etiqueta="Registrar precio final acordado (COP)"
            valor={precioFinalInput}
            onChangeText={setPrecioFinalInput}
            keyboardType="numeric"
            placeholder="Ej. 60000"
          />
          <BotonPrimario
            titulo="Registrar precio final"
            onPress={() => ejecutarAccion(() => registrarPrecioFinalApi(token, idSolicitud, precioFinalInput))}
            cargando={procesando}
          />
        </View>
      ) : null}

      {esCliente && solicitud.precioFinal && !solicitud.precioFinalConfirmado && solicitud.estadoSolicitud !== 'EN_DISPUTA' ? (
        <View style={estilos.tarjeta}>
          <TarjetaAviso
            tipo="info"
            titulo="Confirmación de precio final"
            mensaje={`El profesional registró $${Number(solicitud.precioFinal).toLocaleString('es-CO')} COP como precio final.`}
          />
          <BotonPrimario
            titulo="Confirmar precio final"
            variante="exito"
            onPress={() => ejecutarAccion(() => confirmarPrecioFinalApi(token, idSolicitud))}
            cargando={procesando}
          />
          <CampoTexto
            etiqueta="Motivo si rechazas el precio"
            valor={motivo}
            onChangeText={setMotivo}
            placeholder="Describe la inconformidad para soporte"
          />
          <BotonPrimario
            titulo="Rechazar precio final (disputa)"
            variante="peligro"
            onPress={() => ejecutarAccion(() => rechazarPrecioFinalApi(token, idSolicitud, motivo))}
            cargando={procesando}
          />
        </View>
      ) : null}

      {esCliente && solicitud.puedeResenarse && !solicitud.yaResenada ? (
        <BotonPrimario
          titulo="Calificar servicio (Reseña)"
          onPress={() => navigation.navigate('Resena', { idSolicitud: solicitud.idSolicitud })}
        />
      ) : null}

      {solicitud.yaResenada ? (
        <TarjetaAviso tipo="exito" mensaje="Ya enviaste tu calificación para este servicio." />
      ) : null}

      {solicitud.estadoPago ? (
        <TarjetaAviso
          tipo={solicitud.tieneReporteAbierto ? 'alerta' : 'info'}
          titulo={`Pago: ${solicitud.estadoPago}`}
          mensaje={
            solicitud.tieneReporteAbierto
              ? 'Existe un reporte abierto asociado a esta solicitud; la custodia sigue retenida hasta su resolución.'
              : 'Consulta el comprobante estructurado desde la pantalla de pago.'
          }
        />
      ) : null}

      {esCliente && solicitud.puedePagar ? (
        <BotonPrimario
          titulo="Pagar servicio en custodia"
          variante="exito"
          onPress={() => navigation.navigate('PagoServicio', { idSolicitud: solicitud.idSolicitud })}
        />
      ) : null}

      {solicitud.idPago ? (
        <BotonPrimario
          titulo="Ver comprobante / estado del pago"
          variante="contorno"
          onPress={() => navigation.navigate('PagoServicio', { idSolicitud: solicitud.idSolicitud })}
        />
      ) : null}

      {esCliente && solicitud.estadoPago === 'RETENIDO' ? (
        <BotonPrimario
          titulo="Confirmar servicio recibido"
          variante="exito"
          onPress={manejarLiberacion}
          cargando={procesando}
        />
      ) : null}

      <BotonPrimario
        titulo={esCliente ? 'Reportar profesional' : 'Reportar cliente'}
        variante="peligro"
        onPress={() =>
          navigation.navigate('ReportarUsuario', {
            idSolicitud: solicitud.idSolicitud,
            nombreContraparte: esCliente ? solicitud.nombreProfesional : solicitud.nombreCliente
          })
        }
      />

      <BotonPrimario
        titulo="Contactar soporte sobre esta solicitud"
        variante="contorno"
        onPress={() => navigation.navigate('Soporte', { idSolicitud: solicitud.idSolicitud })}
      />

      {sugerencias.length > 0 ? (
        <View style={estilos.tarjeta}>
          <Text style={estilos.subtitulo}>Otros profesionales disponibles</Text>
          {sugerencias.map((sug) => (
            <TouchableOpacity
              key={sug.idProfesional}
              style={estilos.sugerencia}
              onPress={() => navigation.navigate('PerfilProfesional', { idProfesional: sug.idProfesional })}
            >
              <Text style={estilos.nombreSug}>{sug.nombre} • {sug.distanciaKm} km</Text>
              <Text style={estilos.texto}>{sug.ubicacion}</Text>
            </TouchableOpacity>
          ))}
        </View>
      ) : null}
    </ScrollView>
  );
}

const estilos = StyleSheet.create({
  centrado: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: COLORES.fondo
  },
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
  titulo: {
    fontSize: 17,
    fontWeight: '800',
    color: COLORES.texto
  },
  subtitulo: {
    fontSize: 15,
    fontWeight: '700',
    color: COLORES.texto,
    marginBottom: 8
  },
  estado: {
    fontSize: 14,
    fontWeight: '700',
    color: COLORES.primario,
    marginVertical: 4
  },
  texto: {
    fontSize: 13,
    color: COLORES.textoSecundario,
    marginTop: 3
  },
  precio: {
    fontSize: 15,
    fontWeight: '700',
    color: COLORES.texto,
    marginTop: 8
  },
  precioFinal: {
    fontSize: 15,
    fontWeight: '800',
    color: COLORES.exito,
    marginTop: 4
  },
  sugerencia: {
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: COLORES.borde
  },
  nombreSug: {
    fontWeight: '700',
    color: COLORES.primario
  }
});
