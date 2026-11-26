import React, { useEffect, useState } from 'react';
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { listarColaReportesApi } from '../../api/adminApi';
import { useAuth } from '../../context/AuthContext';
import { BotonPrimario, TarjetaAviso } from '../../components/ComponentesComunes';
import { COLORES, ESPACIADO } from '../../theme/tema';

const FILTROS = [
  { codigo: 'PENDIENTE', etiqueta: 'Pendientes' },
  { codigo: 'RESUELTO', etiqueta: 'Resueltos' },
  { codigo: 'DESCARTADO', etiqueta: 'Descartados' },
  { codigo: null, etiqueta: 'Todos' }
];

export default function ColaReportesScreen({ navigation }) {
  const { token } = useAuth();
  const [filtro, setFiltro] = useState('PENDIENTE');
  const [reportes, setReportes] = useState([]);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);

  async function cargar(estado = filtro) {
    setError(null);
    setCargando(true);
    try {
      const lista = await listarColaReportesApi(token, estado);
      setReportes(lista || []);
    } catch (e) {
      setError(e.mensaje || 'No se pudo cargar la cola de reportes.');
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    const unsubscribe = navigation.addListener('focus', () => cargar(filtro));
    return unsubscribe;
  }, [navigation, filtro]);

  return (
    <ScrollView contentContainerStyle={estilos.contenedor}>
      {error ? <TarjetaAviso tipo="error" mensaje={error} /> : null}

      <View style={estilos.filaChips}>
        {FILTROS.map((f) => {
          const activo = filtro === f.codigo;
          return (
            <TouchableOpacity
              key={f.etiqueta}
              style={[estilos.chip, activo && estilos.chipActivo]}
              onPress={() => {
                setFiltro(f.codigo);
                cargar(f.codigo);
              }}
            >
              <Text style={[estilos.textoChip, activo && estilos.textoChipActivo]}>{f.etiqueta}</Text>
            </TouchableOpacity>
          );
        })}
      </View>

      <BotonPrimario titulo="Recargar cola" variante="contorno" onPress={() => cargar(filtro)} cargando={cargando} />

      {reportes.length === 0 ? (
        <TarjetaAviso tipo="exito" mensaje="No hay reportes para mostrar en este filtro." />
      ) : (
        reportes.map((r) => (
          <TouchableOpacity
            key={r.idReporte}
            style={[estilos.tarjeta, r.esPrioritario && estilos.tarjetaPrioritaria]}
            onPress={() => navigation.navigate('DetalleReporteAdmin', { reporte: r })}
          >
            <View style={estilos.filaEspacio}>
              <Text style={estilos.tituloCard}>
                Reporte #{r.idReporte} • Sol. #{r.idSolicitud}
              </Text>
              {r.esPrioritario ? (
                <Text style={estilos.badgePrioridad}>PRIORITARIO</Text>
              ) : (
                <Text style={estilos.estado}>{r.estadoReporte}</Text>
              )}
            </View>
            <Text style={estilos.motivo}>{r.etiquetaMotivo}</Text>
            <Text style={estilos.detalle}>
              {r.nombreReportante} ({r.rolReportante}) → {r.nombreReportado} ({r.rolReportado})
            </Text>
            <Text style={estilos.detalle}>
              Historial reportado: {r.reportesPreviosReportado} previo(s) • Pago: {r.estadoPagoSolicitud || 'N/A'}
            </Text>
          </TouchableOpacity>
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
  filaChips: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginBottom: 10
  },
  chip: {
    borderWidth: 1,
    borderColor: COLORES.borde,
    borderRadius: 20,
    paddingHorizontal: 12,
    paddingVertical: 6,
    backgroundColor: COLORES.tarjeta
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
  },
  tarjeta: {
    backgroundColor: COLORES.tarjeta,
    borderRadius: 12,
    padding: ESPACIADO.md,
    borderWidth: 1,
    borderColor: COLORES.borde,
    marginBottom: 10
  },
  tarjetaPrioritaria: {
    borderColor: COLORES.peligro,
    borderWidth: 1.5
  },
  filaEspacio: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center'
  },
  tituloCard: {
    fontWeight: '800',
    color: COLORES.texto
  },
  badgePrioridad: {
    backgroundColor: COLORES.peligroFondo,
    color: COLORES.peligro,
    fontWeight: '800',
    fontSize: 11,
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 8
  },
  estado: {
    fontWeight: '700',
    color: COLORES.primario,
    fontSize: 12
  },
  motivo: {
    fontWeight: '700',
    color: COLORES.texto,
    marginTop: 4
  },
  detalle: {
    fontSize: 12,
    color: COLORES.textoSecundario,
    marginTop: 2
  }
});
