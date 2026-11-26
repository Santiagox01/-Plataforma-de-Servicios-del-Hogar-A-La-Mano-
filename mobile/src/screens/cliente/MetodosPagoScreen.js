import React, { useEffect, useState } from 'react';
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import {
  eliminarMetodoPagoApi,
  listarMetodosPagoApi,
  obtenerConfigPagosApi,
  registrarMetodoPagoApi,
  registrarMetodoPayPalApi
} from '../../api/pagosApi';
import { useAuth } from '../../context/AuthContext';
import { BotonPrimario, CampoTexto, TarjetaAviso } from '../../components/ComponentesComunes';
import { COLORES, ESPACIADO } from '../../theme/tema';

const TIPOS_TARJETA = [
  { codigo: 'TARJETA_CREDITO', etiqueta: 'Tarjeta Crédito' },
  { codigo: 'TARJETA_DEBITO', etiqueta: 'Tarjeta Débito' }
];

export default function MetodosPagoScreen() {
  const { token, usuario } = useAuth();
  const [config, setConfig] = useState({ pasarelaActiva: 'simulada', requiereAprobacionExterna: false });
  const [metodos, setMetodos] = useState([]);
  const [tipo, setTipo] = useState('TARJETA_CREDITO');
  const [numeroTarjeta, setNumeroTarjeta] = useState('');
  const [cvv, setCvv] = useState('');
  const [mesVencimiento, setMesVencimiento] = useState('12');
  const [anioVencimiento, setAnioVencimiento] = useState('2028');
  const [titular, setTitular] = useState('');
  const [aliasPayPal, setAliasPayPal] = useState(usuario?.correo || '');
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);
  const [exito, setExito] = useState(null);

  async function cargar() {
    setError(null);
    try {
      const [cfg, lista] = await Promise.all([
        obtenerConfigPagosApi(token),
        listarMetodosPagoApi(token)
      ]);
      if (cfg) {
        setConfig(cfg);
      }
      setMetodos(lista || []);
    } catch (e) {
      setError(e.mensaje || 'No se pudieron cargar tus métodos de pago.');
    }
  }

  useEffect(() => {
    cargar();
  }, []);

  async function agregarTarjeta() {
    setError(null);
    setExito(null);
    setCargando(true);
    try {
      const creado = await registrarMetodoPagoApi(token, {
        numeroTarjeta,
        cvv,
        mesVencimiento: Number(mesVencimiento),
        anioVencimiento: Number(anioVencimiento),
        tipo,
        titular
      });
      setMetodos((prev) => [creado, ...prev]);
      setNumeroTarjeta('');
      setCvv('');
      setTitular('');
      setExito(`Tarjeta ${creado.resumen} guardada de forma tokenizada.`);
    } catch (e) {
      setError(e.mensaje || 'La pasarela rechazó el método de pago.');
    } finally {
      setCargando(false);
    }
  }

  async function vincularPayPal() {
    setError(null);
    setExito(null);
    setCargando(true);
    try {
      const creado = await registrarMetodoPayPalApi(token, aliasPayPal);
      setExito(`Método ${creado.resumen} disponible para tus pagos.`);
      await cargar();
    } catch (e) {
      setError(e.mensaje || 'No fue posible vincular el método PayPal.');
    } finally {
      setCargando(false);
    }
  }

  async function eliminar(idMetodoPago) {
    setError(null);
    try {
      await eliminarMetodoPagoApi(token, idMetodoPago);
      setMetodos((prev) => prev.filter((m) => m.idMetodoPago !== idMetodoPago));
    } catch (e) {
      setError(e.mensaje || 'No fue posible eliminar el método de pago.');
    }
  }

  const esPayPal = Boolean(config?.requiereAprobacionExterna);
  const tienePayPal = metodos.some((m) => m.tipo === 'PAYPAL');

  return (
    <ScrollView contentContainerStyle={estilos.contenedor}>
      {error ? <TarjetaAviso tipo="error" mensaje={error} /> : null}
      {exito ? <TarjetaAviso tipo="exito" mensaje={exito} /> : null}

      {esPayPal ? (
        <View style={estilos.tarjeta}>
          <TarjetaAviso
            tipo="info"
            titulo="Pasarela activa: PayPal Sandbox"
            mensaje={
              'Con PayPal no necesitas registrar ni almacenar datos de tarjetas en A La Mano.\n' +
              'Al pagar una solicitud te redirigiremos a PayPal Sandbox para aprobar el pago en USD.'
            }
          />
          {!tienePayPal ? (
            <>
              <CampoTexto
                etiqueta="Correo o alias PayPal (referencia visual)"
                valor={aliasPayPal}
                onChangeText={setAliasPayPal}
                placeholder="comprador@personal.example.com"
                keyboardType="email-address"
              />
              <BotonPrimario titulo="Registrar método PayPal" onPress={vincularPayPal} cargando={cargando} />
            </>
          ) : (
            <TarjetaAviso tipo="exito" mensaje="Ya tienes tu método PayPal listo en la cuenta." />
          )}
        </View>
      ) : (
        <>
          <TarjetaAviso
            tipo="info"
            titulo="Tarjetas de prueba (Pasarela Simulada)"
            mensaje={
              '• Terminada en 4242: aprueba registro y cobro.\n' +
              '• Terminada en 0002: registra, rechaza cobro por fondos insuficientes.\n' +
              '• Terminada en 0001: rechazada al registrar. Nunca guardamos PAN ni CVV.'
            }
          />

          <View style={estilos.tarjeta}>
            <Text style={estilos.titulo}>Agregar nueva tarjeta</Text>
            <View style={estilos.filaChips}>
              {TIPOS_TARJETA.map((t) => {
                const activo = tipo === t.codigo;
                return (
                  <TouchableOpacity
                    key={t.codigo}
                    style={[estilos.chip, activo && estilos.chipActivo]}
                    onPress={() => setTipo(t.codigo)}
                  >
                    <Text style={[estilos.textoChip, activo && estilos.textoChipActivo]}>{t.etiqueta}</Text>
                  </TouchableOpacity>
                );
              })}
            </View>

            <CampoTexto
              etiqueta="Nombre del titular"
              valor={titular}
              onChangeText={setTitular}
              placeholder="Como aparece en la tarjeta"
            />
            <CampoTexto
              etiqueta="Número de tarjeta"
              valor={numeroTarjeta}
              onChangeText={setNumeroTarjeta}
              placeholder="4242424242424242"
              keyboardType="numeric"
            />
            <View style={estilos.fila}>
              <View style={estilos.col}>
                <CampoTexto
                  etiqueta="Mes (MM)"
                  valor={mesVencimiento}
                  onChangeText={setMesVencimiento}
                  keyboardType="numeric"
                />
              </View>
              <View style={estilos.col}>
                <CampoTexto
                  etiqueta="Año (AAAA)"
                  valor={anioVencimiento}
                  onChangeText={setAnioVencimiento}
                  keyboardType="numeric"
                />
              </View>
              <View style={estilos.col}>
                <CampoTexto
                  etiqueta="CVV"
                  valor={cvv}
                  onChangeText={setCvv}
                  keyboardType="numeric"
                  secureTextEntry
                />
              </View>
            </View>

            <BotonPrimario titulo="Validar y guardar tarjeta" onPress={agregarTarjeta} cargando={cargando} />
          </View>
        </>
      )}

      <Text style={estilos.tituloLista}>Mis métodos guardados ({metodos.length})</Text>
      {metodos.length === 0 ? (
        <TarjetaAviso tipo="alerta" mensaje="Aún no tienes métodos de pago registrados." />
      ) : (
        metodos.map((m) => (
          <View key={m.idMetodoPago} style={estilos.tarjeta}>
            <View style={estilos.filaEspacio}>
              <View>
                <Text style={estilos.resumen}>{m.resumen}</Text>
                <Text style={estilos.detalle}>
                  {m.tipo}
                  {m.mesVencimiento && m.anioVencimiento ? ` • Vence ${m.mesVencimiento}/${m.anioVencimiento}` : ''}
                </Text>
                <Text style={estilos.detalle}>Titular/Alias: {m.titular}</Text>
              </View>
              <TouchableOpacity onPress={() => eliminar(m.idMetodoPago)}>
                <Text style={estilos.eliminar}>Eliminar</Text>
              </TouchableOpacity>
            </View>
          </View>
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
    color: COLORES.texto,
    marginBottom: 10
  },
  tituloLista: {
    fontSize: 15,
    fontWeight: '700',
    color: COLORES.texto,
    marginVertical: 8
  },
  filaChips: {
    flexDirection: 'row',
    gap: 8,
    marginBottom: 12
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
  },
  fila: {
    flexDirection: 'row',
    gap: 8
  },
  col: {
    flex: 1
  },
  filaEspacio: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center'
  },
  resumen: {
    fontSize: 15,
    fontWeight: '800',
    color: COLORES.texto
  },
  detalle: {
    fontSize: 12,
    color: COLORES.textoSecundario,
    marginTop: 2
  },
  eliminar: {
    color: COLORES.peligro,
    fontWeight: '700',
    fontSize: 13
  }
});
