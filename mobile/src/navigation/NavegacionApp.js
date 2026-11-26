import React from 'react';
import { ActivityIndicator, View } from 'react-native';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { useAuth } from '../context/AuthContext';
import { COLORES } from '../theme/tema';

import LoginScreen from '../screens/auth/LoginScreen';
import RegistroClienteScreen from '../screens/auth/RegistroClienteScreen';
import RegistroProfesionalScreen from '../screens/auth/RegistroProfesionalScreen';
import BusquedaScreen from '../screens/cliente/BusquedaScreen';
import ResultadosScreen from '../screens/cliente/ResultadosScreen';
import PerfilProfesionalScreen from '../screens/cliente/PerfilProfesionalScreen';
import CrearSolicitudScreen from '../screens/cliente/CrearSolicitudScreen';
import ResenaScreen from '../screens/cliente/ResenaScreen';
import MetodosPagoScreen from '../screens/cliente/MetodosPagoScreen';
import PagoServicioScreen from '../screens/cliente/PagoServicioScreen';
import PanelProfesionalScreen from '../screens/profesional/PanelProfesionalScreen';
import ConfigurarTarifasScreen from '../screens/profesional/ConfigurarTarifasScreen';
import MisSolicitudesScreen from '../screens/comun/MisSolicitudesScreen';
import DetalleSolicitudScreen from '../screens/comun/DetalleSolicitudScreen';
import ChatScreen from '../screens/comun/ChatScreen';
import ReportarUsuarioScreen from '../screens/comun/ReportarUsuarioScreen';
import SoporteScreen from '../screens/comun/SoporteScreen';
import PendientesVerificacionScreen from '../screens/admin/PendientesVerificacionScreen';
import DetalleVerificacionScreen from '../screens/admin/DetalleVerificacionScreen';
import ColaReportesScreen from '../screens/admin/ColaReportesScreen';
import DetalleReporteAdminScreen from '../screens/admin/DetalleReporteAdminScreen';
import BandejaSoporteAdminScreen from '../screens/admin/BandejaSoporteAdminScreen';

const Stack = createNativeStackNavigator();

export default function NavegacionApp() {
  const { token, usuario, cargandoInicial } = useAuth();

  if (cargandoInicial) {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: COLORES.fondo }}>
        <ActivityIndicator size="large" color={COLORES.primario} />
      </View>
    );
  }

  return (
    <NavigationContainer>
      <Stack.Navigator
        screenOptions={{
          headerStyle: { backgroundColor: COLORES.primario },
          headerTintColor: COLORES.blanco,
          headerTitleStyle: { fontWeight: '700' }
        }}
      >
        {!token || !usuario ? (
          <>
            <Stack.Screen name="Login" component={LoginScreen} options={{ headerShown: false }} />
            <Stack.Screen name="RegistroCliente" component={RegistroClienteScreen} options={{ title: 'Registro Cliente' }} />
            <Stack.Screen name="RegistroProfesional" component={RegistroProfesionalScreen} options={{ title: 'Registro Profesional' }} />
          </>
        ) : usuario.rol === 'CLIENTE' ? (
          <>
            <Stack.Screen name="Busqueda" component={BusquedaScreen} options={{ title: 'Buscar Profesionales' }} />
            <Stack.Screen name="Resultados" component={ResultadosScreen} options={{ title: 'Resultados' }} />
            <Stack.Screen name="PerfilProfesional" component={PerfilProfesionalScreen} options={{ title: 'Perfil Profesional' }} />
            <Stack.Screen name="CrearSolicitud" component={CrearSolicitudScreen} options={{ title: 'Solicitar Servicio' }} />
            <Stack.Screen name="MisSolicitudes" component={MisSolicitudesScreen} options={{ title: 'Mis Solicitudes' }} />
            <Stack.Screen name="DetalleSolicitud" component={DetalleSolicitudScreen} options={{ title: 'Detalle de Solicitud' }} />
            <Stack.Screen name="Chat" component={ChatScreen} options={{ title: 'Chat del Servicio' }} />
            <Stack.Screen name="Resena" component={ResenaScreen} options={{ title: 'Calificar Servicio' }} />
            <Stack.Screen name="MetodosPago" component={MetodosPagoScreen} options={{ title: 'Mis Métodos de Pago' }} />
            <Stack.Screen name="PagoServicio" component={PagoServicioScreen} options={{ title: 'Pago y Comprobante' }} />
            <Stack.Screen name="ReportarUsuario" component={ReportarUsuarioScreen} options={{ title: 'Reportar Problema' }} />
            <Stack.Screen name="Soporte" component={SoporteScreen} options={{ title: 'Ayuda y Soporte' }} />
          </>
        ) : usuario.rol === 'PROFESIONAL' ? (
          <>
            <Stack.Screen name="PanelProfesional" component={PanelProfesionalScreen} options={{ title: 'Mi Cuenta Profesional' }} />
            <Stack.Screen name="MisSolicitudes" component={MisSolicitudesScreen} options={{ title: 'Bandeja de Solicitudes' }} />
            <Stack.Screen name="DetalleSolicitud" component={DetalleSolicitudScreen} options={{ title: 'Detalle de Solicitud' }} />
            <Stack.Screen name="Chat" component={ChatScreen} options={{ title: 'Chat del Servicio' }} />
            <Stack.Screen name="ConfigurarTarifas" component={ConfigurarTarifasScreen} options={{ title: 'Configurar Tarifas' }} />
            <Stack.Screen name="PerfilProfesional" component={PerfilProfesionalScreen} options={{ title: 'Perfil Profesional' }} />
            <Stack.Screen name="PagoServicio" component={PagoServicioScreen} options={{ title: 'Comprobante de Pago' }} />
            <Stack.Screen name="ReportarUsuario" component={ReportarUsuarioScreen} options={{ title: 'Reportar Cliente' }} />
            <Stack.Screen name="Soporte" component={SoporteScreen} options={{ title: 'Ayuda y Soporte' }} />
          </>
        ) : (
          <>
            <Stack.Screen name="PendientesVerificacion" component={PendientesVerificacionScreen} options={{ title: 'Verificación de Identidad' }} />
            <Stack.Screen name="DetalleVerificacion" component={DetalleVerificacionScreen} options={{ title: 'Revisar Expediente' }} />
            <Stack.Screen name="ColaReportes" component={ColaReportesScreen} options={{ title: 'Cola de Reportes' }} />
            <Stack.Screen name="DetalleReporteAdmin" component={DetalleReporteAdminScreen} options={{ title: 'Resolver Reporte' }} />
            <Stack.Screen name="BandejaSoporteAdmin" component={BandejaSoporteAdminScreen} options={{ title: 'Bandeja de Soporte' }} />
          </>
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
}
