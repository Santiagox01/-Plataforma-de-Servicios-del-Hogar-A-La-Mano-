import React, { useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useAuth } from '../../context/AuthContext';
import { BotonPrimario, CampoTexto, TarjetaAviso } from '../../components/ComponentesComunes';
import { COLORES, ESPACIADO } from '../../theme/tema';

export default function LoginScreen({ navigation }) {
  const { iniciarSesion } = useAuth();
  const [correo, setCorreo] = useState('');
  const [contrasena, setContrasena] = useState('');
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);
  const [esRestriccion, setEsRestriccion] = useState(false);

  async function manejarLogin() {
    setError(null);
    setEsRestriccion(false);
    setCargando(true);
    try {
      await iniciarSesion(correo, contrasena);
    } catch (e) {
      setEsRestriccion(e.estado === 403 || e.estado === 423);
      setError(e.mensaje || 'No fue posible iniciar sesión.');
    } finally {
      setCargando(false);
    }
  }

  return (
    <ScrollView contentContainerStyle={estilos.contenedor}>
      <View style={estilos.marca}>
        <Text style={estilos.tituloMarca}>A La Mano</Text>
        <Text style={estilos.subtituloMarca}>Servicios confiables para tu hogar en Medellín</Text>
      </View>

      {error ? (
        <TarjetaAviso
          tipo={esRestriccion ? 'alerta' : 'error'}
          titulo={esRestriccion ? 'Acceso restringido' : undefined}
          mensaje={error}
        />
      ) : null}

      <View style={estilos.tarjeta}>
        <Text style={estilos.tituloSeccion}>Iniciar sesión</Text>
        <CampoTexto
          etiqueta="Correo electrónico"
          valor={correo}
          onChangeText={setCorreo}
          placeholder="usuario@correo.com"
          keyboardType="email-address"
        />
        <CampoTexto
          etiqueta="Contraseña"
          valor={contrasena}
          onChangeText={setContrasena}
          placeholder="Tu contraseña"
          secureTextEntry
        />
        <BotonPrimario titulo="Ingresar" onPress={manejarLogin} cargando={cargando} />
      </View>

      <View style={estilos.enlacesRegistro}>
        <Text style={estilos.textoRegistro}>¿No tienes cuenta?</Text>
        <BotonPrimario
          titulo="Registrarme como Cliente"
          variante="contorno"
          onPress={() => navigation.navigate('RegistroCliente')}
        />
        <BotonPrimario
          titulo="Registrarme como Profesional"
          variante="secundario"
          onPress={() => navigation.navigate('RegistroProfesional')}
        />
      </View>
    </ScrollView>
  );
}

const estilos = StyleSheet.create({
  contenedor: {
    flexGrow: 1,
    backgroundColor: COLORES.fondo,
    padding: ESPACIADO.lg,
    justifyContent: 'center'
  },
  marca: {
    alignItems: 'center',
    marginBottom: 24
  },
  tituloMarca: {
    fontSize: 30,
    fontWeight: '800',
    color: COLORES.primario
  },
  subtituloMarca: {
    fontSize: 13,
    color: COLORES.textoSecundario,
    marginTop: 4,
    textAlign: 'center'
  },
  tarjeta: {
    backgroundColor: COLORES.tarjeta,
    borderRadius: 14,
    padding: ESPACIADO.md,
    borderWidth: 1,
    borderColor: COLORES.borde
  },
  tituloSeccion: {
    fontSize: 18,
    fontWeight: '700',
    color: COLORES.texto,
    marginBottom: 12
  },
  enlacesRegistro: {
    marginTop: 18
  },
  textoRegistro: {
    textAlign: 'center',
    color: COLORES.textoSecundario,
    marginBottom: 8,
    fontWeight: '600'
  }
});
