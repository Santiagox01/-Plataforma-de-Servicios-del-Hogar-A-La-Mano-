import React, { createContext, useContext, useEffect, useState } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import {
  cerrarSesionApi,
  iniciarSesionApi,
  obtenerMiPerfilApi,
  registrarClienteApi,
  registrarProfesionalApi
} from '../api/authApi';

const CLAVE_TOKEN = 'alamano_jwt_token';
const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(null);
  const [usuario, setUsuario] = useState(null);
  const [aviso, setAviso] = useState(null);
  const [cargandoInicial, setCargandoInicial] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const tokenGuardado = await AsyncStorage.getItem(CLAVE_TOKEN);
        if (tokenGuardado) {
          const perfil = await obtenerMiPerfilApi(tokenGuardado);
          setToken(tokenGuardado);
          setUsuario(perfil);
        }
      } catch {
        await AsyncStorage.removeItem(CLAVE_TOKEN);
      } finally {
        setCargandoInicial(false);
      }
    })();
  }, []);

  async function guardarSesion(respuesta) {
    await AsyncStorage.setItem(CLAVE_TOKEN, respuesta.token);
    setToken(respuesta.token);
    setUsuario(respuesta.usuario);
    setAviso(respuesta.aviso || null);
  }

  async function iniciarSesion(correo, contrasena) {
    const respuesta = await iniciarSesionApi(correo, contrasena);
    await guardarSesion(respuesta);
    return respuesta;
  }

  async function registrarCliente(datos) {
    const respuesta = await registrarClienteApi(datos);
    await guardarSesion(respuesta);
    return respuesta;
  }

  async function registrarProfesional(datos) {
    const respuesta = await registrarProfesionalApi(datos);
    await guardarSesion(respuesta);
    return respuesta;
  }

  async function recargarPerfil() {
    if (!token) return null;
    const perfil = await obtenerMiPerfilApi(token);
    setUsuario(perfil);
    return perfil;
  }

  async function cerrarSesion() {
    try {
      if (token) {
        await cerrarSesionApi(token);
      }
    } catch {
      // Ignora error de red al cerrar sesion local
    } finally {
      await AsyncStorage.removeItem(CLAVE_TOKEN);
      setToken(null);
      setUsuario(null);
      setAviso(null);
    }
  }

  return (
    <AuthContext.Provider
      value={{
        token,
        usuario,
        aviso,
        cargandoInicial,
        iniciarSesion,
        registrarCliente,
        registrarProfesional,
        recargarPerfil,
        actualizarUsuarioLocal: setUsuario,
        cerrarSesion
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
