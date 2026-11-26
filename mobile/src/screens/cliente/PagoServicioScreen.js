import React, { useEffect, useState } from 'react';
import { ActivityIndicator, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import * as WebBrowser from 'expo-web-browser';
import * as Linking from 'expo-linking';
import {
  cancelarPagoExternoApi,
  confirmarPagoExternoApi,
  iniciarPagoExternoApi,
  liberarCustodiaApi,
  listarMetodosPagoApi,
  obtenerResumenPagoApi,
  pagarSolicitudApi
} from '../../api/pagosApi';
import { useAuth } from '../../context/AuthContext';
import { BotonPrimario, TarjetaAviso } from '../../components/ComponentesComunes';
import { COLORES, ESPACIADO } from '../../theme/tema';

WebBrowser.maybeCompleteAuthSession();

export default function PagoServicioScreen({ route, navigation }) {
  const { token, usuario } = useAuth();
  const { idSolicitud } = route.params;
  const [resumen, setResumen] = useState(null);
  const [metodos, setMetodos] = useState([]);
  const [idMetodoSeleccionado, setIdMetodoSeleccionado] = useState(null);
  const [pagoRespuesta, setPagoRespuesta] = useState(null);
  const [avisoInfo, setAvisoInfo] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [procesando, setProcesando] = useState(false);
  const [error, setError] = useState(null);

  async function cargar() {
    setError(null);
    setCargando(true);
    try {
      const res = await obtenerResumenPagoApi(token, idSolicitud);
      setResumen(res);
      setPagoRespuesta(res?.pagoActual || null);
      if (usuario?.rol === 'CLIENTE' && !res?.requiereAprobacionExterna) {
        const lista = await listarMetodosPagoApi(token);
        const soloTarjetas = (lista || []).filter((m) => m.tipo !== 'PAYPAL');
        setMetodos(soloTarjetas);
        if (soloTarjetas.length && !idMetodoSeleccionado) {
          setIdMetodoSeleccionado(soloTarjetas[0].idMetodoPago);
        }
      }
    } catch (e) {
      setError(e.mensaje || 'No se pudo cargar el detalle del pago.');
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    const unsubscribe = navigation.addListener('focus', cargar);
    return unsubscribe;
  }, [navigation, idSolicitud]);

  async function confirmarPagoSimulado() {
    if (!idMetodoSeleccionado) {
      setError('Selecciona o registra un método de pago primero.');
      return;
    }
    setError(null);
    setAvisoInfo(null);
    setProcesando(true);
    try {
      const pago = await pagarSolicitudApi(token, idSolicitud, idMetodoSeleccionado);
      setPagoRespuesta(pago);
      await cargar();
    } catch (e) {
      setError(e.mensaje || 'No se pudo procesar el pago.');
    } finally {
      setProcesando(false);
    }
  }

  async function pagarConPayPal() {
    setError(null);
    setAvisoInfo(null);
    setProcesando(true);
    try {
      const redirectUrl = Linking.createURL('paypal-retorno');
      const inicio = await iniciarPagoExternoApi(token, idSolicitud, redirectUrl);
      const sesion = await WebBrowser.openAuthSessionAsync(inicio.urlAprobacion, redirectUrl);

      if (sesion.type === 'success' && sesion.url) {
        const parsed = Linking.parse(sesion.url);
        const estado = parsed?.queryParams?.estado;
        const orderId = parsed?.queryParams?.orderId || inicio.idOrdenExterna;
        if (estado === 'cancelado') {
          await cancelarPagoExternoApi(token, idSolicitud);
          setAvisoInfo('Cancelaste la aprobación en PayPal. Puedes volver a intentarlo cuando desees.');
          await cargar();
          return;
        }
        const confirmado = await confirmarPagoExternoApi(token, idSolicitud, orderId);
        setPagoRespuesta(confirmado);
        await cargar();
        return;
      }

      // Si el usuario cerro el navegador manualmente, consultamos contra el backend/PayPal antes de asumir estado
      try {
        const confirmado = await confirmarPagoExternoApi(token, idSolicitud, inicio.idOrdenExterna);
        setPagoRespuesta(confirmado);
      } catch (errConfirm) {
        setAvisoInfo(
          (errConfirm?.mensaje || 'Aprobación aún pendiente en PayPal.') +
          ' Si ya aprobaste en el navegador, presiona "Ya aprobé en PayPal — Confirmar".'
        );
      }
      await cargar();
    } catch (e) {
      setError(e.mensaje || 'No se pudo completar el pago con PayPal.');
    } finally {
      setProcesando(false);
    }
  }

  async function confirmarPendientePayPal() {
    setError(null);
    setAvisoInfo(null);
    setProcesando(true);
    try {
      const confirmado = await confirmarPagoExternoApi(
        token,
        idSolicitud,
        pagoRespuesta?.referenciaOrdenExterna || null
      );
      setPagoRespuesta(confirmado);
      await cargar();
    } catch (e) {
      setError(e.mensaje || 'PayPal aún no reporta la orden como aprobada.');
    } finally {
      setProcesando(false);
    }
  }

  async function cancelarPendientePayPal() {
    setError(null);
    setAvisoInfo(null);
    setProcesando(true);
    try {
      await cancelarPagoExternoApi(token, idSolicitud);
      setAvisoInfo('Intento de pago en PayPal cancelado. Puedes iniciar uno nuevo.');
      await cargar();
    } catch (e) {
      setError(e.mensaje || 'No se pudo cancelar el intento de pago.');
    } finally {
      setProcesando(false);
    }
  }

  async function liberarCustodia() {
    setError(null);
    setAvisoInfo(null);
    setProcesando(true);
    try {
      const liberado = await liberarCustodiaApi(token, idSolicitud);
      setPagoRespuesta(liberado);
      await cargar();
    } catch (e) {
      setError(e.mensaje || 'No fue posible liberar la custodia.');
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

  const comprobante = pagoRespuesta?.comprobante;

  return (
    <ScrollView contentContainerStyle={estilos.contenedor}>
      {error ? <TarjetaAviso tipo="error" mensaje={error} /> : null}
      {avisoInfo ? <TarjetaAviso tipo="alerta" mensaje={avisoInfo} /> : null}

      {resumen ? (
        <View style={estilos.tarjeta}>
          <Text style={estilos.titulo}>Resumen de cobro • Solicitud #{resumen.idSolicitud}</Text>
          <Text style={estilos.texto}>
            Estado de solicitud: {resumen.estadoSolicitud} • Pasarela: {(resumen.pasarelaActiva || 'simulada').toUpperCase()}
          </Text>
          {resumen.montoServicio ? (
            <>
              <View style={estilos.linea}>
                <Text style={estilos.texto}>Precio final del servicio</Text>
                <Text style={estilos.valor}>${Number(resumen.montoServicio).toLocaleString('es-CO')} COP</Text>
              </View>
              <View style={estilos.linea}>
                <Text style={estilos.texto}>Garantía fija A La Mano</Text>
                <Text style={estilos.valor}>${Number(resumen.tasaGarantia).toLocaleString('es-CO')} COP</Text>
              </View>
              <View style={[estilos.linea, estilos.lineaTotal]}>
                <Text style={estilos.totalLabel}>Total a pagar en custodia</Text>
                <Text style={estilos.totalValor}>${Number(resumen.totalAPagar).toLocaleString('es-CO')} COP</Text>
              </View>
              {resumen.montoUsd ? (
                <View style={estilos.cajaUsd}>
                  <Text style={estilos.textoUsd}>
                    Equivalente en PayPal (USD): ${Number(resumen.montoUsd).toFixed(2)} USD
                  </Text>
                  <Text style={estilos.detalleUsd}>
                    Tasa configurada: 1 USD = ${Number(resumen.tasaCambioUsd).toLocaleString('es-CO')} COP
                  </Text>
                </View>
              ) : null}
            </>
          ) : (
            <TarjetaAviso tipo="alerta" mensaje="Aún no hay un precio final confirmado para esta solicitud." />
          )}
        </View>
      ) : null}

      {pagoRespuesta?.estadoPago === 'RECHAZADO' ? (
        <TarjetaAviso
          tipo="error"
          titulo="Pago rechazado por la pasarela"
          mensaje={`${pagoRespuesta.motivoRechazo} Puedes intentar nuevamente.`}
        />
      ) : null}

      {pagoRespuesta?.estadoPago === 'CANCELADO' ? (
        <TarjetaAviso
          tipo="alerta"
          titulo="Intento anterior cancelado"
          mensaje={pagoRespuesta.motivoRechazo || 'Cancelaste el intento anterior en PayPal. Puedes iniciar uno nuevo.'}
        />
      ) : null}

      {comprobante ? (
        <View style={estilos.tarjeta}>
          <Text style={estilos.titulo}>Comprobante de pago</Text>
          <Text style={estilos.estadoBadge}>Estado del pago: {comprobante.estadoPago}</Text>
          <Text style={estilos.texto}>N° Comprobante: {comprobante.numeroComprobante}</Text>
          <Text style={estilos.texto}>Pasarela: {(comprobante.pasarela || 'simulada').toUpperCase()}</Text>
          <Text style={estilos.texto}>Ref. Pasarela: {comprobante.referenciaPasarela}</Text>
          <Text style={estilos.texto}>Categoría: {comprobante.nombreCategoria}</Text>
          <Text style={estilos.texto}>Cliente: {comprobante.nombreCliente}</Text>
          <Text style={estilos.texto}>Profesional: {comprobante.nombreProfesional}</Text>
          <Text style={estilos.texto}>Método: {comprobante.metodoPagoResumen}</Text>
          <Text style={estilos.texto}>
            Servicio: ${Number(comprobante.montoServicio).toLocaleString('es-CO')} COP + Garantía: $
            {Number(comprobante.tasaGarantia).toLocaleString('es-CO')} COP
          </Text>
          <Text style={estilos.totalValor}>
            Total pagado: ${Number(comprobante.totalPagado).toLocaleString('es-CO')} COP
          </Text>
          {comprobante.montoUsd ? (
            <Text style={estilos.texto}>
              Cobrado en PayPal: ${Number(comprobante.montoUsd).toFixed(2)} USD (tasa ${Number(comprobante.tasaCambioUsd).toLocaleString('es-CO')} COP/USD)
            </Text>
          ) : null}
        </View>
      ) : null}

      {usuario?.rol === 'CLIENTE' && resumen?.puedePagar && resumen?.requiereAprobacionExterna ? (
        <View style={estilos.tarjeta}>
          <Text style={estilos.titulo}>Pago seguro con PayPal Sandbox</Text>
          <TarjetaAviso
            tipo="info"
            mensaje="Al pulsar 'Pagar con PayPal' se abrirá PayPal Sandbox para aprobar el monto en USD. El dinero quedará autorizado en custodia hasta confirmar el servicio recibido."
          />
          <BotonPrimario
            titulo="Pagar con PayPal"
            variante="primario"
            onPress={pagarConPayPal}
            cargando={procesando}
          />
          {pagoRespuesta?.estadoPago === 'PENDIENTE_APROBACION' ? (
            <>
              <BotonPrimario
                titulo="Ya aprobé en PayPal — Confirmar"
                variante="exito"
                onPress={confirmarPendientePayPal}
                cargando={procesando}
              />
              <BotonPrimario
                titulo="Cancelar intento en PayPal"
                variante="contorno"
                onPress={cancelarPendientePayPal}
                cargando={procesando}
              />
            </>
          ) : null}
        </View>
      ) : null}

      {usuario?.rol === 'CLIENTE' && resumen?.puedePagar && !resumen?.requiereAprobacionExterna ? (
        <View style={estilos.tarjeta}>
          <Text style={estilos.titulo}>Selecciona tu método de pago</Text>
          {metodos.length === 0 ? (
            <TarjetaAviso tipo="alerta" mensaje="No tienes tarjetas registradas todavía." />
          ) : (
            metodos.map((m) => {
              const activo = idMetodoSeleccionado === m.idMetodoPago;
              return (
                <TouchableOpacity
                  key={m.idMetodoPago}
                  style={[estilos.opcionMetodo, activo && estilos.opcionActiva]}
                  onPress={() => setIdMetodoSeleccionado(m.idMetodoPago)}
                >
                  <Text style={estilos.resumenMetodo}>{m.resumen}</Text>
                  <Text style={estilos.texto}>{m.titular} • Vence {m.mesVencimiento}/{m.anioVencimiento}</Text>
                </TouchableOpacity>
              );
            })
          )}
          <BotonPrimario
            titulo="Administrar / agregar tarjetas"
            variante="contorno"
            onPress={() => navigation.navigate('MetodosPago')}
          />
          {metodos.length > 0 ? (
            <BotonPrimario
              titulo="Pagar con custodia"
              variante="exito"
              onPress={confirmarPagoSimulado}
              cargando={procesando}
            />
          ) : null}
        </View>
      ) : null}

      {usuario?.rol === 'CLIENTE' && pagoRespuesta?.estadoPago === 'RETENIDO' ? (
        <View style={estilos.tarjeta}>
          {resumen?.tieneReporteAbierto ? (
            <TarjetaAviso
              tipo="alerta"
              titulo="Fondos retenidos por moderación"
              mensaje="Existe un reporte o disputa abierta sobre esta solicitud. El dinero seguirá en custodia hasta su resolución."
            />
          ) : null}
          <BotonPrimario
            titulo="Confirmar servicio recibido"
            variante="exito"
            onPress={liberarCustodia}
            cargando={procesando}
          />
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
    fontSize: 16,
    fontWeight: '800',
    color: COLORES.texto,
    marginBottom: 8
  },
  texto: {
    fontSize: 13,
    color: COLORES.textoSecundario,
    marginTop: 3
  },
  valor: {
    fontSize: 14,
    fontWeight: '700',
    color: COLORES.texto
  },
  linea: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 6
  },
  lineaTotal: {
    borderTopWidth: 1,
    borderTopColor: COLORES.borde,
    paddingTop: 8,
    marginTop: 10
  },
  totalLabel: {
    fontSize: 15,
    fontWeight: '800',
    color: COLORES.texto
  },
  totalValor: {
    fontSize: 16,
    fontWeight: '800',
    color: COLORES.primario,
    marginTop: 4
  },
  cajaUsd: {
    marginTop: 10,
    backgroundColor: COLORES.primarioSuave,
    borderRadius: 8,
    padding: 10
  },
  textoUsd: {
    fontSize: 14,
    fontWeight: '800',
    color: COLORES.primario
  },
  detalleUsd: {
    fontSize: 12,
    color: COLORES.textoSecundario,
    marginTop: 2
  },
  estadoBadge: {
    fontWeight: '800',
    color: COLORES.exito,
    marginBottom: 4
  },
  opcionMetodo: {
    borderWidth: 1,
    borderColor: COLORES.borde,
    borderRadius: 10,
    padding: 10,
    marginBottom: 8
  },
  opcionActiva: {
    borderColor: COLORES.primario,
    backgroundColor: COLORES.primarioSuave
  },
  resumenMetodo: {
    fontWeight: '700',
    color: COLORES.texto
  }
});
