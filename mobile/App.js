import React from 'react';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { AuthProvider } from './src/context/AuthContext';
import NavegacionApp from './src/navigation/NavegacionApp';

export default function App() {
  return (
    <SafeAreaProvider>
      <AuthProvider>
        <StatusBar style="light" />
        <NavegacionApp />
      </AuthProvider>
    </SafeAreaProvider>
  );
}
