import React, { useEffect, useRef, useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { enviarMensajeApi, listarMensajesApi, marcarMensajeLeidoApi } from '../../api/chatApi';
import { useAuth } from '../../context/AuthContext';
import { BotonPrimario, CampoTexto, TarjetaAviso } from '../../components/ComponentesComunes';
import { COLORES, ESPACIADO } from '../../theme/tema';

const INTERVALO_POLLING_MS = 2000;

export default function ChatScreen({ route }) {
  const { token, usuario } = useAuth();
  const { idSolicitud } = route.params;
  const [mensajes, setMensajes] = useState([]);
  const [texto, setTexto] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState(null);
  const scrollRef = useRef(null);

  async function sincronizarMensajes() {
    try {
      const lista = await listarMensajesApi(token, idSolicitud);
      setMensajes(lista || []);
      for (const m of lista || []) {
        if (m.idRemitente !== usuario?.idUsuario && m.estadoMensaje === 'ENVIADO') {
          await marcarMensajeLeidoApi(token, idSolicitud, m.idMensaje).catch(() => {});
        }
      }
    } catch (e) {
      setError(e.mensaje || 'No se pudieron cargar los mensajes.');
    }
  }

  useEffect(() => {
    sincronizarMensajes();
    const timer = setInterval(sincronizarMensajes, INTERVALO_POLLING_MS);
    return () => clearInterval(timer);
  }, [idSolicitud, token]);

  async function enviar() {
    if (!texto.trim()) return;
    setError(null);
    setEnviando(true);
    try {
      const nuevo = await enviarMensajeApi(token, idSolicitud, texto);
      setMensajes((prev) => [...prev, nuevo]);
      setTexto('');
    } catch (e) {
      setError(e.mensaje || 'No se pudo enviar el mensaje.');
    } finally {
      setEnviando(false);
    }
  }

  return (
    <View style={estilos.contenedor}>
      {error ? <TarjetaAviso tipo="error" mensaje={error} /> : null}

      <ScrollView
        ref={scrollRef}
        style={estilos.lista}
        onContentSizeChange={() => scrollRef.current?.scrollToEnd({ animated: true })}
      >
        {mensajes.length === 0 ? (
          <Text style={estilos.vacio}>Aún no hay mensajes. Inicia la conversación.</Text>
        ) : (
          mensajes.map((m) => {
            const esMio = m.idRemitente === usuario?.idUsuario;
            return (
              <View key={m.idMensaje} style={[estilos.burbuja, esMio ? estilos.burbujaMia : estilos.burbujaOtro]}>
                <Text style={[estilos.autor, esMio && estilos.textoBlanco]}>{m.nombreRemitente}</Text>
                <Text style={[estilos.contenido, esMio && estilos.textoBlanco]}>{m.contenido}</Text>
                <Text style={[estilos.estado, esMio && estilos.textoBlanco]}>{m.estadoMensaje}</Text>
              </View>
            );
          })
        )}
      </ScrollView>

      <View style={estilos.barraEnvio}>
        <CampoTexto
          valor={texto}
          onChangeText={setTexto}
          placeholder="Escribe un mensaje..."
        />
        <BotonPrimario titulo="Enviar" onPress={enviar} cargando={enviando} />
      </View>
    </View>
  );
}

const estilos = StyleSheet.create({
  contenedor: {
    flex: 1,
    backgroundColor: COLORES.fondo,
    padding: ESPACIADO.md
  },
  lista: {
    flex: 1,
    marginBottom: 10
  },
  vacio: {
    textAlign: 'center',
    color: COLORES.textoSecundario,
    marginTop: 20
  },
  burbuja: {
    maxWidth: '82%',
    padding: 10,
    borderRadius: 12,
    marginBottom: 8
  },
  burbujaMia: {
    alignSelf: 'flex-end',
    backgroundColor: COLORES.primario
  },
  burbujaOtro: {
    alignSelf: 'flex-start',
    backgroundColor: COLORES.tarjeta,
    borderWidth: 1,
    borderColor: COLORES.borde
  },
  autor: {
    fontSize: 11,
    fontWeight: '700',
    color: COLORES.textoSecundario
  },
  contenido: {
    fontSize: 14,
    color: COLORES.texto,
    marginTop: 2
  },
  estado: {
    fontSize: 10,
    color: COLORES.textoTenue,
    alignSelf: 'flex-end',
    marginTop: 3
  },
  textoBlanco: {
    color: COLORES.blanco
  },
  barraEnvio: {
    backgroundColor: COLORES.tarjeta,
    borderRadius: 12,
    padding: 10,
    borderWidth: 1,
    borderColor: COLORES.borde
  }
});
