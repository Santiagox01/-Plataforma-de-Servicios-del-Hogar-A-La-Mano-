import React, { useEffect, useState } from 'react';
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { crearReporteApi, listarMotivosReporteApi } from '../../api/moderacionApi';
import { useAuth } from '../../context/AuthContext';
import { BotonPrimario, CampoTexto, TarjetaAviso } from '../../components/ComponentesComunes';
import { COLORES, ESPACIADO } from '../../theme/tema';

export default function ReportarUsuarioScreen({ route, navigation }) {
  const { token } = useAuth();
  const { idSolicitud, nombreContraparte } = route.params;
  const [motivos, setMotivos] = useState([]);
  const [motivoSeleccionado, setMotivoSeleccionado] = useState(null);
  const [descripcion, setDescripcion] = useState('');
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);
  const [creado, setCreado] = useState(null);

  useEffect(() => {
    listarMotivosReporteApi(token)
      .then((lista) => {
        setMotivos(lista || []);
        if (lista?.length) {
          setMotivoSeleccionado(lista[0].codigo);
        }
      })
      .catch((e) => setError(e.mensaje || 'No se pudieron cargar los motivos de reporte.'));
  }, [token]);

  async function enviar() {
    setError(null);
    setCargando(true);
    try {
      const resp = await crearReporteApi(token, {
        idSolicitud,
        motivo: motivoSeleccionado,
        descripcion
      });
      setCreado(resp);
    } catch (e) {
      setError(e.mensaje || 'No se pudo enviar el reporte.');
    } finally {
      setCargando(false);
    }
  }

  return (
    <ScrollView contentContainerStyle={estilos.contenedor}>
      {error ? <TarjetaAviso tipo="error" mensaje={error} /> : null}

      {creado ? (
        <View style={estilos.tarjeta}>
          <TarjetaAviso
            tipo="exito"
            titulo={`Reporte #${creado.idReporte} enviado`}
            mensaje="Tu identidad se mantiene confidencial para el reportado. El caso ingresó a la cola de moderación."
          />
          <BotonPrimario titulo="Volver a la solicitud" onPress={() => navigation.goBack()} />
        </View>
      ) : (
        <View style={estilos.tarjeta}>
          <Text style={estilos.titulo}>
            Reportar sobre solicitud #{idSolicitud}
            {nombreContraparte ? ` (${nombreContraparte})` : ''}
          </Text>
          <Text style={estilos.etiqueta}>Selecciona el motivo</Text>
          <View style={estilos.filaChips}>
            {motivos.map((m) => {
              const activo = motivoSeleccionado === m.codigo;
              return (
                <TouchableOpacity
                  key={m.codigo}
                  style={[estilos.chip, activo && estilos.chipActivo]}
                  onPress={() => setMotivoSeleccionado(m.codigo)}
                >
                  <Text style={[estilos.textoChip, activo && estilos.textoChipActivo]}>{m.etiqueta}</Text>
                </TouchableOpacity>
              );
            })}
          </View>

          <CampoTexto
            etiqueta="Describe lo ocurrido"
            valor={descripcion}
            onChangeText={setDescripcion}
            placeholder="Explica los hechos con el mayor detalle posible..."
            multiline
          />

          <BotonPrimario titulo="Enviar reporte a moderación" variante="peligro" onPress={enviar} cargando={cargando} />
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
    borderColor: COLORES.borde
  },
  titulo: {
    fontSize: 16,
    fontWeight: '800',
    color: COLORES.texto,
    marginBottom: 12
  },
  etiqueta: {
    fontSize: 13,
    fontWeight: '700',
    color: COLORES.texto,
    marginBottom: 8
  },
  filaChips: {
    gap: 8,
    marginBottom: 12
  },
  chip: {
    borderWidth: 1,
    borderColor: COLORES.borde,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 9
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
  }
});
