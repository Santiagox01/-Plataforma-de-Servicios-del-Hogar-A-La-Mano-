import React, { useEffect, useState } from 'react';
import { ActivityIndicator, ScrollView, StyleSheet, Text, View } from 'react-native';
import { consultarPerfilProfesionalApi } from '../../api/profesionalesApi';
import { useAuth } from '../../context/AuthContext';
import { BotonPrimario, TarjetaAviso } from '../../components/ComponentesComunes';
import { COLORES, ESPACIADO } from '../../theme/tema';

export default function PerfilProfesionalScreen({ route, navigation }) {
  const { token } = useAuth();
  const { idProfesional } = route.params;
  const [perfil, setPerfil] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    consultarPerfilProfesionalApi(token, idProfesional)
      .then(setPerfil)
      .catch((e) => setError(e.mensaje || 'No se pudo cargar el perfil.'))
      .finally(() => setCargando(false));
  }, [idProfesional, token]);

  if (cargando) {
    return (
      <View style={estilos.centrado}>
        <ActivityIndicator size="large" color={COLORES.primario} />
      </View>
    );
  }

  if (error || !perfil) {
    return (
      <View style={estilos.contenedor}>
        <TarjetaAviso tipo="error" mensaje={error || 'Perfil no disponible.'} />
      </View>
    );
  }

  return (
    <ScrollView contentContainerStyle={estilos.contenedor}>
      <View style={estilos.tarjeta}>
        <View style={estilos.fila}>
          <Text style={estilos.nombre}>{perfil.nombre}</Text>
          <View style={estilos.sello}>
            <Text style={estilos.textoSello}>{perfil.selloVerificacion}</Text>
          </View>
        </View>
        <Text style={estilos.subtitulo}>{perfil.ubicacion}</Text>
        <Text style={estilos.rating}>
          ★ {perfil.calificacionPromedio.toFixed(1)} ({perfil.totalResenas} reseñas)
        </Text>
      </View>

      <View style={estilos.tarjeta}>
        <Text style={estilos.tituloSeccion}>Servicios ofrecidos</Text>
        {perfil.servicios.map((s) => (
          <View key={s.idServicio} style={estilos.filaServicio}>
            <Text style={estilos.categoria}>{s.nombreCategoria}</Text>
            <Text style={estilos.tarifa}>
              {s.tarifaBase ? `Tarifa base: $${Number(s.tarifaBase).toLocaleString('es-CO')}` : 'Tarifa por configurar'}
            </Text>
          </View>
        ))}
      </View>

      {perfil.servicios.length > 0 ? (
        <BotonPrimario
          titulo="Solicitar servicio"
          onPress={() =>
            navigation.navigate('CrearSolicitud', {
              idProfesional: perfil.idProfesional,
              nombreProfesional: perfil.nombre,
              servicios: perfil.servicios
            })
          }
        />
      ) : null}

      <View style={estilos.tarjeta}>
        <Text style={estilos.tituloSeccion}>Últimos comentarios</Text>
        {perfil.ultimosComentarios?.length ? (
          perfil.ultimosComentarios.map((c, idx) => (
            <View key={idx} style={estilos.comentario}>
              <Text style={estilos.autorComentario}>{c.autor} • ★ {c.estrellas}</Text>
              <Text style={estilos.textoComentario}>{c.comentario}</Text>
            </View>
          ))
        ) : (
          <Text style={estilos.subtitulo}>Aún no hay reseñas registradas para este profesional.</Text>
        )}
      </View>
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
  fila: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center'
  },
  nombre: {
    fontSize: 20,
    fontWeight: '800',
    color: COLORES.texto
  },
  sello: {
    backgroundColor: COLORES.exitoFondo,
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 14
  },
  textoSello: {
    color: COLORES.exito,
    fontWeight: '700',
    fontSize: 12
  },
  subtitulo: {
    color: COLORES.textoSecundario,
    fontSize: 13,
    marginTop: 4
  },
  rating: {
    fontSize: 15,
    fontWeight: '700',
    color: COLORES.texto,
    marginTop: 8
  },
  tituloSeccion: {
    fontSize: 15,
    fontWeight: '700',
    color: COLORES.texto,
    marginBottom: 8
  },
  filaServicio: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 6,
    borderBottomWidth: 1,
    borderBottomColor: COLORES.borde
  },
  categoria: {
    fontWeight: '600',
    color: COLORES.texto
  },
  tarifa: {
    color: COLORES.primario,
    fontWeight: '700'
  },
  comentario: {
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: COLORES.borde
  },
  autorComentario: {
    fontWeight: '700',
    fontSize: 13,
    color: COLORES.texto
  },
  textoComentario: {
    fontSize: 13,
    color: COLORES.textoSecundario,
    marginTop: 2
  }
});
