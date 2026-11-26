import React, { useEffect, useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import {
  crearCasoSoporteApi,
  listarMisCasosSoporteApi,
  listarReportesRecibidosApi
} from '../../api/moderacionApi';
import { useAuth } from '../../context/AuthContext';
import { BotonPrimario, CampoTexto, TarjetaAviso } from '../../components/ComponentesComunes';
import { COLORES, ESPACIADO } from '../../theme/tema';

export default function SoporteScreen({ route }) {
  const { token } = useAuth();
  const idSolicitudInicial = route?.params?.idSolicitud ? String(route.params.idSolicitud) : '';
  const [casos, setCasos] = useState([]);
  const [reportesRecibidos, setReportesRecibidos] = useState([]);
  const [asunto, setAsunto] = useState('');
  const [descripcion, setDescripcion] = useState('');
  const [idSolicitudTexto, setIdSolicitudTexto] = useState(idSolicitudInicial);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);
  const [confirmacion, setConfirmacion] = useState(null);

  async function cargar() {
    setError(null);
    try {
      const [listaCasos, listaReportes] = await Promise.all([
        listarMisCasosSoporteApi(token),
        listarReportesRecibidosApi(token)
      ]);
      setCasos(listaCasos || []);
      setReportesRecibidos(listaReportes || []);
    } catch (e) {
      setError(e.mensaje || 'No se pudieron cargar tus tickets de soporte.');
    }
  }

  useEffect(() => {
    cargar();
  }, []);

  async function enviar() {
    setError(null);
    setConfirmacion(null);
    setCargando(true);
    try {
      const creado = await crearCasoSoporteApi(token, {
        asunto,
        descripcion,
        idSolicitud: idSolicitudTexto.trim() ? Number(idSolicitudTexto) : null
      });
      setConfirmacion(`Ticket registrado con número de caso único: ${creado.numeroCaso}`);
      setAsunto('');
      setDescripcion('');
      setCasos((prev) => [creado, ...prev]);
    } catch (e) {
      setError(e.mensaje || 'No se pudo enviar el caso a soporte.');
    } finally {
      setCargando(false);
    }
  }

  return (
    <ScrollView contentContainerStyle={estilos.contenedor}>
      {error ? <TarjetaAviso tipo="error" mensaje={error} /> : null}
      {confirmacion ? <TarjetaAviso tipo="exito" titulo="Caso recibido" mensaje={confirmacion} /> : null}

      <View style={estilos.tarjeta}>
        <Text style={estilos.titulo}>Contactar soporte de A La Mano</Text>
        <CampoTexto
          etiqueta="Asunto"
          valor={asunto}
          onChangeText={setAsunto}
          placeholder="Ej. Duda con pago o estado de mi cuenta"
        />
        <CampoTexto
          etiqueta="ID de solicitud relacionada (opcional)"
          valor={idSolicitudTexto}
          onChangeText={setIdSolicitudTexto}
          placeholder="Ej. 3"
          keyboardType="numeric"
        />
        <CampoTexto
          etiqueta="Descripción del problema"
          valor={descripcion}
          onChangeText={setDescripcion}
          placeholder="Cuéntanos en qué podemos ayudarte..."
          multiline
        />
        <BotonPrimario titulo="Enviar caso a soporte" onPress={enviar} cargando={cargando} />
      </View>

      <BotonPrimario titulo="Actualizar mis casos" variante="contorno" onPress={cargar} />

      <Text style={estilos.tituloLista}>Mis tickets de soporte ({casos.length})</Text>
      {casos.length === 0 ? (
        <TarjetaAviso tipo="info" mensaje="No tienes casos de soporte creados." />
      ) : (
        casos.map((c) => (
          <View key={c.idCaso} style={estilos.tarjeta}>
            <View style={estilos.fila}>
              <Text style={estilos.numero}>{c.numeroCaso}</Text>
              <Text style={estilos.estado}>{c.estadoCaso}</Text>
            </View>
            <Text style={estilos.asunto}>{c.asunto}</Text>
            <Text style={estilos.texto}>{c.descripcion}</Text>
            {c.respuestaAdmin ? (
              <TarjetaAviso
                tipo="exito"
                titulo="Respuesta de soporte"
                mensaje={c.respuestaAdmin}
              />
            ) : null}
          </View>
        ))
      )}

      <Text style={estilos.tituloLista}>Reportes recibidos ({reportesRecibidos.length})</Text>
      {reportesRecibidos.length === 0 ? (
        <TarjetaAviso tipo="info" mensaje="No registras reportes en tu contra." />
      ) : (
        reportesRecibidos.map((r) => (
          <View key={r.idReporte} style={estilos.tarjeta}>
            <View style={estilos.fila}>
              <Text style={estilos.numero}>Solicitud #{r.idSolicitud} • {r.nombreCategoria}</Text>
              <Text style={estilos.estado}>{r.estadoReporte}</Text>
            </View>
            <Text style={estilos.asunto}>{r.etiquetaMotivo}</Text>
            {r.accionTomada ? (
              <Text style={estilos.texto}>
                Resolución: {r.accionTomada} — {r.justificacionResolucion}
              </Text>
            ) : (
              <Text style={estilos.texto}>En revisión por moderación (reportante confidencial).</Text>
            )}
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
  titulo: {
    fontSize: 16,
    fontWeight: '800',
    color: COLORES.texto,
    marginBottom: 10
  },
  tituloLista: {
    fontSize: 15,
    fontWeight: '700',
    color: COLORES.texto,
    marginVertical: 8
  },
  fila: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 4
  },
  numero: {
    fontWeight: '800',
    color: COLORES.primario,
    fontSize: 13
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
    marginBottom: 8
  }
});
