import React, { useEffect, useState } from 'react';
import { ScrollView, StyleSheet, Switch, Text, TouchableOpacity, View } from 'react-native';
import * as DocumentPicker from 'expo-document-picker';
import {
  actualizarDisponibilidadApi,
  recargarDocumentosProfesionalApi
} from '../../api/profesionalesApi';
import { listarNotificacionesApi, marcarNotificacionLeidaApi } from '../../api/notificacionesApi';
import { useAuth } from '../../context/AuthContext';
import { BotonPrimario, EncabezadoRol, TarjetaAviso } from '../../components/ComponentesComunes';
import { COLORES, ESPACIADO } from '../../theme/tema';

export default function PanelProfesionalScreen({ navigation }) {
  const { token, usuario, aviso, recargarPerfil, actualizarUsuarioLocal, cerrarSesion } = useAuth();
  const [notificaciones, setNotificaciones] = useState([]);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);

  async function cargarTodo() {
    setCargando(true);
    setError(null);
    try {
      await recargarPerfil();
      const lista = await listarNotificacionesApi(token);
      setNotificaciones(lista || []);
    } catch (e) {
      setError(e.mensaje || 'No se pudo actualizar la información.');
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    cargarTodo();
  }, []);

  async function cambiarDisponibilidad(nuevoValor) {
    setError(null);
    try {
      const actualizado = await actualizarDisponibilidadApi(token, nuevoValor);
      actualizarUsuarioLocal(actualizado);
    } catch (e) {
      setError(e.mensaje || 'No se pudo cambiar la disponibilidad.');
    }
  }

  async function reenviarDocumento() {
    setError(null);
    const res = await DocumentPicker.getDocumentAsync({
      type: ['image/jpeg', 'image/png', 'application/pdf'],
      copyToCacheDirectory: true
    });
    if (res.canceled || !res.assets?.length) return;

    setCargando(true);
    try {
      const actualizado = await recargarDocumentosProfesionalApi(token, res.assets[0]);
      actualizarUsuarioLocal(actualizado);
    } catch (e) {
      setError(e.mensaje || 'No se pudo cargar el documento.');
    } finally {
      setCargando(false);
    }
  }

  async function leerNotificacion(id) {
    await marcarNotificacionLeidaApi(token, id);
    setNotificaciones((prev) => prev.map((n) => (n.idNotificacion === id ? { ...n, leida: true } : n)));
  }

  const estado = usuario?.estadoVerificacion;

  return (
    <ScrollView contentContainerStyle={estilos.contenedor}>
      <EncabezadoRol usuario={usuario} onCerrarSesion={cerrarSesion} />

      {error ? <TarjetaAviso tipo="error" mensaje={error} /> : null}
      {aviso ? <TarjetaAviso tipo="alerta" titulo="Aviso de cuenta" mensaje={aviso} /> : null}

      {estado === 'PENDIENTE_VERIFICACION' ? (
        <TarjetaAviso
          tipo="alerta"
          titulo="Pendiente de verificación"
          mensaje="Tu documentación está siendo revisada por el administrador. Aún no puedes recibir solicitudes ni activarte como disponible."
        />
      ) : null}

      {estado === 'RECHAZADO' ? (
        <View>
          <TarjetaAviso
            tipo="error"
            titulo="Verificación rechazada"
            mensaje={`Motivo: ${usuario?.motivoRechazo || 'Documento inválido'}`}
          />
          <BotonPrimario titulo="Volver a cargar documento" onPress={reenviarDocumento} cargando={cargando} />
        </View>
      ) : null}

      {estado === 'VERIFICADO' ? (
        <View style={estilos.tarjeta}>
          <TarjetaAviso tipo="exito" titulo="Cuenta Verificada" mensaje="Ya puedes recibir solicitudes cuando estés disponible." />
          <View style={estilos.filaSwitch}>
            <Text style={estilos.etiquetaSwitch}>Disponible para recibir solicitudes</Text>
            <Switch value={Boolean(usuario?.disponible)} onValueChange={cambiarDisponibilidad} />
          </View>
        </View>
      ) : null}

      <BotonPrimario titulo="Bandeja de solicitudes" onPress={() => navigation.navigate('MisSolicitudes')} />
      <BotonPrimario titulo="Configurar tarifas por categoría" variante="secundario" onPress={() => navigation.navigate('ConfigurarTarifas')} />
      <BotonPrimario titulo="Ayuda y soporte" variante="contorno" onPress={() => navigation.navigate('Soporte')} />
      <BotonPrimario titulo="Actualizar estado y notificaciones" variante="contorno" onPress={cargarTodo} cargando={cargando} />

      <View style={estilos.tarjeta}>
        <Text style={estilos.tituloSeccion}>Notificaciones ({notificaciones.length})</Text>
        {notificaciones.length === 0 ? (
          <Text style={estilos.vacio}>No tienes notificaciones aún.</Text>
        ) : (
          notificaciones.map((n) => (
            <TouchableOpacity
              key={n.idNotificacion}
              style={[estilos.notif, !n.leida && estilos.notifNoLeida]}
              onPress={() => leerNotificacion(n.idNotificacion)}
            >
              <Text style={estilos.notifTitulo}>{n.titulo}</Text>
              <Text style={estilos.notifMensaje}>{n.mensaje}</Text>
            </TouchableOpacity>
          ))
        )}
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
    borderColor: COLORES.borde,
    marginBottom: 12
  },
  filaSwitch: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center'
  },
  etiquetaSwitch: {
    fontSize: 14,
    fontWeight: '700',
    color: COLORES.texto
  },
  tituloSeccion: {
    fontSize: 15,
    fontWeight: '700',
    color: COLORES.texto,
    marginBottom: 8
  },
  vacio: {
    color: COLORES.textoSecundario,
    fontSize: 13
  },
  notif: {
    paddingVertical: 10,
    borderBottomWidth: 1,
    borderBottomColor: COLORES.borde
  },
  notifNoLeida: {
    backgroundColor: COLORES.primarioSuave,
    paddingHorizontal: 8,
    borderRadius: 6
  },
  notifTitulo: {
    fontWeight: '700',
    fontSize: 13,
    color: COLORES.texto
  },
  notifMensaje: {
    fontSize: 12,
    color: COLORES.textoSecundario,
    marginTop: 2
  }
});
