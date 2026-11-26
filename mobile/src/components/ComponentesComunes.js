import React from 'react';
import {
  ActivityIndicator,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View
} from 'react-native';
import { COLORES, ESPACIADO } from '../theme/tema';

export function BotonPrimario({ titulo, onPress, cargando = false, variante = 'primario', deshabilitado = false }) {
  const fondo = {
    primario: COLORES.primario,
    secundario: COLORES.texto,
    exito: COLORES.exito,
    peligro: COLORES.peligro,
    contorno: 'transparent'
  }[variante] || COLORES.primario;

  const esContorno = variante === 'contorno';

  return (
    <TouchableOpacity
      style={[
        estilos.boton,
        { backgroundColor: fondo },
        esContorno && estilos.botonContorno,
        (cargando || deshabilitado) && estilos.botonDeshabilitado
      ]}
      onPress={onPress}
      disabled={cargando || deshabilitado}
    >
      {cargando ? (
        <ActivityIndicator color={esContorno ? COLORES.primario : COLORES.blanco} />
      ) : (
        <Text style={[estilos.textoBoton, esContorno && { color: COLORES.primario }]}>{titulo}</Text>
      )}
    </TouchableOpacity>
  );
}

export function CampoTexto({
  etiqueta,
  valor,
  onChangeText,
  placeholder,
  secureTextEntry = false,
  keyboardType = 'default',
  error = null,
  multiline = false
}) {
  return (
    <View style={estilos.grupoCampo}>
      {etiqueta ? <Text style={estilos.etiqueta}>{etiqueta}</Text> : null}
      <TextInput
        style={[
          estilos.input,
          multiline && estilos.inputMultilinea,
          error && estilos.inputError
        ]}
        value={valor}
        onChangeText={onChangeText}
        placeholder={placeholder}
        placeholderTextColor={COLORES.textoTenue}
        secureTextEntry={secureTextEntry}
        keyboardType={keyboardType}
        autoCapitalize={keyboardType === 'email-address' ? 'none' : 'sentences'}
        multiline={multiline}
      />
      {error ? <Text style={estilos.textoError}>{error}</Text> : null}
    </View>
  );
}

export function TarjetaAviso({ tipo = 'info', titulo, mensaje }) {
  const mapaEstilos = {
    alerta: { bg: COLORES.alertaFondo, borde: COLORES.alerta, color: '#92400e' },
    exito: { bg: COLORES.exitoFondo, borde: COLORES.exito, color: '#065f46' },
    error: { bg: COLORES.peligroFondo, borde: COLORES.peligro, color: '#991b1b' },
    info: { bg: COLORES.primarioSuave, borde: COLORES.primario, color: COLORES.primario }
  };
  const esquema = mapaEstilos[tipo] || mapaEstilos.info;

  return (
    <View style={[estilos.aviso, { backgroundColor: esquema.bg, borderColor: esquema.borde }]}>
      {titulo ? <Text style={[estilos.avisoTitulo, { color: esquema.color }]}>{titulo}</Text> : null}
      <Text style={[estilos.avisoMensaje, { color: esquema.color }]}>{mensaje}</Text>
    </View>
  );
}

export function EncabezadoRol({ usuario, onCerrarSesion }) {
  return (
    <View style={estilos.encabezado}>
      <View style={{ flex: 1 }}>
        <Text style={estilos.nombreEncabezado}>{usuario?.nombre}</Text>
        <Text style={estilos.rolEncabezado}>{usuario?.rol} • {usuario?.correo}</Text>
      </View>
      <TouchableOpacity style={estilos.botonSalir} onPress={onCerrarSesion}>
        <Text style={estilos.textoSalir}>Salir</Text>
      </TouchableOpacity>
    </View>
  );
}

const estilos = StyleSheet.create({
  boton: {
    borderRadius: 10,
    paddingVertical: 13,
    paddingHorizontal: 16,
    alignItems: 'center',
    justifyContent: 'center',
    marginVertical: 6
  },
  botonContorno: {
    borderWidth: 1.5,
    borderColor: COLORES.primario
  },
  botonDeshabilitado: {
    opacity: 0.6
  },
  textoBoton: {
    color: COLORES.blanco,
    fontSize: 15,
    fontWeight: '700'
  },
  grupoCampo: {
    marginBottom: 12
  },
  etiqueta: {
    fontSize: 13,
    fontWeight: '600',
    color: COLORES.texto,
    marginBottom: 5
  },
  input: {
    backgroundColor: COLORES.tarjeta,
    borderWidth: 1,
    borderColor: COLORES.borde,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 15,
    color: COLORES.texto
  },
  inputMultilinea: {
    minHeight: 80,
    textAlignVertical: 'top'
  },
  inputError: {
    borderColor: COLORES.peligro
  },
  textoError: {
    color: COLORES.peligro,
    fontSize: 12,
    marginTop: 4
  },
  aviso: {
    borderWidth: 1,
    borderRadius: 10,
    padding: 12,
    marginBottom: 12
  },
  avisoTitulo: {
    fontWeight: '700',
    fontSize: 14,
    marginBottom: 3
  },
  avisoMensaje: {
    fontSize: 13,
    lineHeight: 18
  },
  encabezado: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: COLORES.tarjeta,
    padding: ESPACIADO.md,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: COLORES.borde,
    marginBottom: ESPACIADO.md
  },
  nombreEncabezado: {
    fontSize: 16,
    fontWeight: '700',
    color: COLORES.texto
  },
  rolEncabezado: {
    fontSize: 12,
    color: COLORES.textoSecundario,
    marginTop: 2
  },
  botonSalir: {
    backgroundColor: COLORES.primarioSuave,
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 8
  },
  textoSalir: {
    color: COLORES.primario,
    fontWeight: '700',
    fontSize: 13
  }
});
