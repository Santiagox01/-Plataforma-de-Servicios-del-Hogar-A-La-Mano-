import React, { useEffect, useState } from 'react';
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { configurarTarifaApi, listarMisServiciosApi } from '../../api/tarifasApi';
import { useAuth } from '../../context/AuthContext';
import { BotonPrimario, CampoTexto, TarjetaAviso } from '../../components/ComponentesComunes';
import { COLORES, ESPACIADO } from '../../theme/tema';

export default function ConfigurarTarifasScreen() {
  const { token } = useAuth();
  const [servicios, setServicios] = useState([]);
  const [seleccionado, setSeleccionado] = useState(null);
  const [tarifaBase, setTarifaBase] = useState('');
  const [precioMin, setPrecioMin] = useState('');
  const [precioMax, setPrecioMax] = useState('');
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);
  const [exito, setExito] = useState(null);

  async function cargar() {
    setError(null);
    try {
      const lista = await listarMisServiciosApi(token);
      setServicios(lista || []);
      if (lista?.length) {
        seleccionar(lista[0]);
      }
    } catch (e) {
      setError(e.mensaje || 'No se pudieron cargar tus categorías.');
    }
  }

  useEffect(() => {
    cargar();
  }, []);

  function seleccionar(srv) {
    setSeleccionado(srv);
    setTarifaBase(srv.tarifaBase ? String(srv.tarifaBase) : '');
    setPrecioMin(srv.precioMin ? String(srv.precioMin) : '');
    setPrecioMax(srv.precioMax ? String(srv.precioMax) : '');
  }

  async function guardar() {
    if (!seleccionado) return;
    setError(null);
    setExito(null);
    setCargando(true);
    try {
      const actualizado = await configurarTarifaApi(token, {
        idCategoria: seleccionado.idCategoria,
        tarifaBase: Number(tarifaBase),
        precioMin: Number(precioMin),
        precioMax: Number(precioMax)
      });
      setServicios((prev) => prev.map((s) => (s.idServicio === actualizado.idServicio ? actualizado : s)));
      setSeleccionado(actualizado);
      setExito(`Tarifa guardada para ${actualizado.nombreCategoria}.`);
    } catch (e) {
      setError(e.mensaje || 'No se pudo guardar la tarifa.');
    } finally {
      setCargando(false);
    }
  }

  return (
    <ScrollView contentContainerStyle={estilos.contenedor}>
      {error ? <TarjetaAviso tipo="error" mensaje={error} /> : null}
      {exito ? <TarjetaAviso tipo="exito" mensaje={exito} /> : null}

      <View style={estilos.tarjeta}>
        <Text style={estilos.titulo}>Selecciona una categoría</Text>
        <View style={estilos.filaChips}>
          {servicios.map((srv) => {
            const activo = seleccionado?.idServicio === srv.idServicio;
            return (
              <TouchableOpacity
                key={srv.idServicio}
                style={[estilos.chip, activo && estilos.chipActivo]}
                onPress={() => seleccionar(srv)}
              >
                <Text style={[estilos.textoChip, activo && estilos.textoChipActivo]}>
                  {srv.nombreCategoria} {srv.tarifaConfigurada ? '✓' : ''}
                </Text>
              </TouchableOpacity>
            );
          })}
        </View>

        <CampoTexto etiqueta="Tarifa base (COP)" valor={tarifaBase} onChangeText={setTarifaBase} keyboardType="numeric" />
        <CampoTexto etiqueta="Precio mínimo (COP)" valor={precioMin} onChangeText={setPrecioMin} keyboardType="numeric" />
        <CampoTexto etiqueta="Precio máximo (COP)" valor={precioMax} onChangeText={setPrecioMax} keyboardType="numeric" />

        <BotonPrimario titulo="Guardar tarifa" onPress={guardar} cargando={cargando} />
      </View>
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
    fontSize: 15,
    fontWeight: '700',
    color: COLORES.texto,
    marginBottom: 10
  },
  filaChips: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginBottom: 14
  },
  chip: {
    borderWidth: 1,
    borderColor: COLORES.borde,
    borderRadius: 20,
    paddingHorizontal: 12,
    paddingVertical: 7
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
