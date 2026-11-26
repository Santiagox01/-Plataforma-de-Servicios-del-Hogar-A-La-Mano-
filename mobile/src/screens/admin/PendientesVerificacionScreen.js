import React, { useEffect, useState } from 'react';
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { listarProfesionalesPendientesApi } from '../../api/adminApi';
import { useAuth } from '../../context/AuthContext';
import { BotonPrimario, EncabezadoRol, TarjetaAviso } from '../../components/ComponentesComunes';
import { COLORES, ESPACIADO } from '../../theme/tema';

export default function PendientesVerificacionScreen({ navigation }) {
  const { token, usuario, cerrarSesion } = useAuth();
  const [pendientes, setPendientes] = useState([]);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);

  async function cargarPendientes() {
    setCargando(true);
    setError(null);
    try {
      const lista = await listarProfesionalesPendientesApi(token);
      setPendientes(lista || []);
    } catch (e) {
      setError(e.mensaje || 'No se pudo cargar la lista de pendientes.');
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    const unsubscribe = navigation.addListener('focus', cargarPendientes);
    return unsubscribe;
  }, [navigation]);

  return (
    <ScrollView contentContainerStyle={estilos.contenedor}>
      <EncabezadoRol usuario={usuario} onCerrarSesion={cerrarSesion} />

      {error ? <TarjetaAviso tipo="error" mensaje={error} /> : null}

      <BotonPrimario titulo="Cola de reportes (Moderación)" onPress={() => navigation.navigate('ColaReportes')} />
      <BotonPrimario titulo="Bandeja de soporte" variante="secundario" onPress={() => navigation.navigate('BandejaSoporteAdmin')} />
      <BotonPrimario titulo="Recargar solicitudes pendientes" variante="contorno" onPress={cargarPendientes} cargando={cargando} />

      <Text style={estilos.titulo}>Profesionales pendientes ({pendientes.length})</Text>

      {pendientes.length === 0 ? (
        <TarjetaAviso tipo="exito" mensaje="No hay profesionales pendientes de verificación." />
      ) : (
        pendientes.map((pro) => (
          <TouchableOpacity
            key={pro.idProfesional}
            style={estilos.tarjeta}
            onPress={() => navigation.navigate('DetalleVerificacion', { profesional: pro })}
          >
            <Text style={estilos.nombre}>{pro.nombre}</Text>
            <Text style={estilos.detalle}>{pro.correo} • {pro.celular}</Text>
            <Text style={estilos.detalle}>Zona: {pro.ubicacion}</Text>
            <Text style={estilos.categorias}>Categorías: {(pro.categorias || []).join(', ')}</Text>
          </TouchableOpacity>
        ))
      )}
    </ScrollView>
  );
}

const estilos = StyleSheet.create({
  contenedor: {
    padding: ESPACIADO.md,
    backgroundColor: COLORES.fondo,
    flexGrow: 1
  },
  titulo: {
    fontSize: 16,
    fontWeight: '700',
    color: COLORES.texto,
    marginVertical: 10
  },
  tarjeta: {
    backgroundColor: COLORES.tarjeta,
    borderRadius: 12,
    padding: ESPACIADO.md,
    borderWidth: 1,
    borderColor: COLORES.borde,
    marginBottom: 10
  },
  nombre: {
    fontSize: 16,
    fontWeight: '700',
    color: COLORES.texto
  },
  detalle: {
    fontSize: 13,
    color: COLORES.textoSecundario,
    marginTop: 2
  },
  categorias: {
    fontSize: 12,
    fontWeight: '700',
    color: COLORES.primario,
    marginTop: 6
  }
});
