import React, { useState } from 'react';
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { resolverReporteApi } from '../../api/adminApi';
import { useAuth } from '../../context/AuthContext';
import { BotonPrimario, CampoTexto, TarjetaAviso } from '../../components/ComponentesComunes';
import { COLORES, ESPACIADO } from '../../theme/tema';

const ACCIONES = [
  { codigo: 'ADVERTENCIA', etiqueta: 'Advertencia' },
  { codigo: 'SUSPENSION_TEMPORAL', etiqueta: 'Suspensión temporal' },
  { codigo: 'DESACTIVACION', etiqueta: 'Desactivar cuenta' },
  { codigo: 'DESCARTAR', etiqueta: 'Descartar (Sin mérito)' }
];

const DECISIONES_PAGO = [
  { codigo: 'SIN_CAMBIO', etiqueta: 'Mantener igual' },
  { codigo: 'REEMBOLSAR', etiqueta: 'Reembolsar al cliente' },
  { codigo: 'LIBERAR', etiqueta: 'Liberar al profesional' }
];

export default function DetalleReporteAdminScreen({ route }) {
  const { token } = useAuth();
  const [reporte, setReporte] = useState(route.params.reporte);
  const [accion, setAccion] = useState('ADVERTENCIA');
  const [diasSuspension, setDiasSuspension] = useState('7');
  const [decisionPago, setDecisionPago] = useState('SIN_CAMBIO');
  const [justificacion, setJustificacion] = useState('');
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);
  const [exito, setExito] = useState(null);

  async function resolver() {
    setError(null);
    setExito(null);
    setCargando(true);
    try {
      const actualizado = await resolverReporteApi(token, reporte.idReporte, {
        accion,
        justificacion,
        diasSuspension: accion === 'SUSPENSION_TEMPORAL' ? Number(diasSuspension) : null,
        decisionPago: (reporte.tienePagoRetenido || reporte.estadoPagoSolicitud === 'LIBERADO')
          ? decisionPago
          : 'SIN_CAMBIO'
      });
      setReporte(actualizado);
      setExito('Resolución inmutable guardada en auditoría administrativa.');
    } catch (e) {
      setError(e.mensaje || 'No se pudo resolver el reporte.');
    } finally {
      setCargando(false);
    }
  }

  return (
    <ScrollView contentContainerStyle={estilos.contenedor}>
      {error ? <TarjetaAviso tipo="error" mensaje={error} /> : null}
      {exito ? <TarjetaAviso tipo="exito" mensaje={exito} /> : null}

      <View style={estilos.tarjeta}>
        <Text style={estilos.titulo}>
          Reporte #{reporte.idReporte} • Solicitud #{reporte.idSolicitud} ({reporte.nombreCategoria})
        </Text>
        <Text style={estilos.estado}>
          Estado: {reporte.estadoReporte} {reporte.esPrioritario ? '• PRIORITARIO (Reincidencia)' : ''}
        </Text>
        <Text style={estilos.texto}>Motivo: {reporte.etiquetaMotivo}</Text>
        <Text style={estilos.texto}>Descripción: {reporte.descripcion}</Text>
      </View>

      <View style={estilos.tarjeta}>
        <Text style={estilos.subtitulo}>Historial de ambas partes</Text>
        <Text style={estilos.texto}>
          Reportante: {reporte.nombreReportante} ({reporte.rolReportante}) — {reporte.reportesPreviosReportante} reporte(s) previos enviados
        </Text>
        <Text style={estilos.texto}>
          Reportado: {reporte.nombreReportado} ({reporte.rolReportado}) — {reporte.reportesPreviosReportado} reporte(s) previos recibidos
        </Text>
        <Text style={estilos.texto}>
          Pago en solicitud: {reporte.estadoPagoSolicitud || 'Sin pago'} {reporte.tienePagoRetenido ? '(RETENIDO en custodia)' : ''}
        </Text>
      </View>

      {reporte.estadoReporte === 'PENDIENTE' ? (
        <View style={estilos.tarjeta}>
          <Text style={estilos.subtitulo}>Decisión de moderación</Text>
          <View style={estilos.filaChips}>
            {ACCIONES.map((a) => {
              const activo = accion === a.codigo;
              return (
                <TouchableOpacity
                  key={a.codigo}
                  style={[estilos.chip, activo && estilos.chipActivo]}
                  onPress={() => setAccion(a.codigo)}
                >
                  <Text style={[estilos.textoChip, activo && estilos.textoChipActivo]}>{a.etiqueta}</Text>
                </TouchableOpacity>
              );
            })}
          </View>

          {accion === 'SUSPENSION_TEMPORAL' ? (
            <CampoTexto
              etiqueta="Días de suspensión temporal"
              valor={diasSuspension}
              onChangeText={setDiasSuspension}
              keyboardType="numeric"
            />
          ) : null}

          {(reporte.tienePagoRetenido || reporte.estadoPagoSolicitud === 'LIBERADO') ? (
            <>
              <Text style={estilos.subtitulo}>Acción sobre pago ({reporte.estadoPagoSolicitud})</Text>
              <View style={estilos.filaChips}>
                {DECISIONES_PAGO.filter((d) => reporte.tienePagoRetenido || d.codigo !== 'LIBERAR').map((d) => {
                  const activo = decisionPago === d.codigo;
                  return (
                    <TouchableOpacity
                      key={d.codigo}
                      style={[estilos.chip, activo && estilos.chipActivo]}
                      onPress={() => setDecisionPago(d.codigo)}
                    >
                      <Text style={[estilos.textoChip, activo && estilos.textoChipActivo]}>{d.etiqueta}</Text>
                    </TouchableOpacity>
                  );
                })}
              </View>
            </>
          ) : null}

          <CampoTexto
            etiqueta="Justificación obligatoria (trazabilidad inmutable)"
            valor={justificacion}
            onChangeText={setJustificacion}
            placeholder="Argumenta la resolución tomada..."
            multiline
          />

          <BotonPrimario titulo="Confirmar resolución" onPress={resolver} cargando={cargando} />
        </View>
      ) : (
        <View style={estilos.tarjeta}>
          <Text style={estilos.subtitulo}>Resolución cerrada (inmutable)</Text>
          <Text style={estilos.texto}>Acción aplicada: {reporte.accionTomada}</Text>
          <Text style={estilos.texto}>Decisión de pago: {reporte.decisionPago}</Text>
          <Text style={estilos.texto}>Justificación: {reporte.justificacionResolucion}</Text>
        </View>
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
    color: COLORES.texto
  },
  subtitulo: {
    fontSize: 14,
    fontWeight: '700',
    color: COLORES.texto,
    marginBottom: 8
  },
  estado: {
    color: COLORES.primario,
    fontWeight: '700',
    marginVertical: 4
  },
  texto: {
    fontSize: 13,
    color: COLORES.textoSecundario,
    marginTop: 4
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
    borderRadius: 10,
    paddingHorizontal: 10,
    paddingVertical: 7
  },
  chipActivo: {
    backgroundColor: COLORES.primario,
    borderColor: COLORES.primario
  },
  textoChip: {
    color: COLORES.texto,
    fontSize: 12
  },
  textoChipActivo: {
    color: COLORES.blanco,
    fontWeight: '700'
  }
});
