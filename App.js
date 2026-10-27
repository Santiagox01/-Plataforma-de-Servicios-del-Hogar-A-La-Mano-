import React, { useState, useEffect } from 'react';
import {
  StyleSheet,
  Text,
  View,
  ScrollView,
  TextInput,
  TouchableOpacity,
  Alert,
  StatusBar,
  Image
} from 'react-native';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { THEME } from './src/constants/theme';
import { INITIAL_CATEGORIES, INITIAL_SERVICES } from './src/data/mockData';
import Header from './src/components/Header';
import ProfessionalCard from './src/components/ProfessionalCard';
import BookingModal from './src/components/BookingModal';
import ProDetailModal from './src/components/ProDetailModal';
import AdminAuditCard from './src/components/AdminAuditCard';
import AuthModal from './src/components/AuthModal';
import VerificationModal from './src/components/VerificationModal';
import SupabaseConfigModal from './src/components/SupabaseConfigModal';
import ChatModal from './src/components/ChatModal';

import { getLocalUser, setLocalUser, logoutUser, DEMO_USERS } from './src/services/authService';
import {
  fetchProfessionals,
  createServiceOrder,
  fetchVerificationDossier,
  updateProfessionalStatus
} from './src/services/databaseService';
import {
  testSupabaseConnection,
  initializeSupabaseClient,
  SUPABASE_PROJECT_REF
} from './src/services/supabase';

export default function App() {
  const [currentUser, setCurrentUser] = useState(DEMO_USERS.client);
  const [activeTab, setActiveTab] = useState('explore'); // 'explore' | 'services' | 'messages' | 'profile'
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [selectedZone, setSelectedZone] = useState('Todas');
  const [searchQuery, setSearchQuery] = useState('');

  const [professionals, setProfessionals] = useState([]);
  const [services, setServices] = useState(INITIAL_SERVICES);

  // Modals state
  const [authModalVisible, setAuthModalVisible] = useState(false);
  const [supabaseModalVisible, setSupabaseModalVisible] = useState(false);
  const [selectedProForDetail, setSelectedProForDetail] = useState(null);
  const [selectedProForBooking, setSelectedProForBooking] = useState(null);

  // Chat state
  const [chatModalVisible, setChatModalVisible] = useState(false);
  const [selectedProForChat, setSelectedProForChat] = useState(null);
  const [selectedServiceForChat, setSelectedServiceForChat] = useState('SRV-2026-01');

  // Identity Verification state (Validación Documental)
  const [verificationModalVisible, setVerificationModalVisible] = useState(false);
  const [activeDossier, setActiveDossier] = useState(null);
  const [dossierProName, setDossierProName] = useState('');
  const [isAuditorModeForDossier, setIsAuditorModeForDossier] = useState(false);

  // Supabase connection state
  const [isSupabaseConnected, setIsSupabaseConnected] = useState(false);

  useEffect(() => {
    // Inicializar sesión y cargar datos de base de datos
    (async () => {
      try {
        // Inicializar Supabase y probar conexión en segundo plano
        const init = await initializeSupabaseClient();
        const testRes = await testSupabaseConnection(init.key);
        setIsSupabaseConnected(testRes.connected);

        const user = await getLocalUser();
        if (user) setCurrentUser(user);

        const pros = await fetchProfessionals();
        setProfessionals(pros);
      } catch (err) {
        console.warn('Initialization error:', err);
      }
    })();
  }, []);

  // Abrir chat interactivo con un profesional
  const handleOpenChat = (pro, serviceId = 'SRV-2026-01') => {
    setSelectedProForChat(pro || {
      id: 'usr_pro_1',
      name: 'Carlos Mario Restrepo',
      specialtyName: 'Electricista Residencial e Industrial',
      avatar: 'https://images.unsplash.com/photo-1540569014015-19a7be504e3a?w=180&auto=format&fit=crop&q=80'
    });
    setSelectedServiceForChat(serviceId);
    setChatModalVisible(true);
  };

  // Filtrado de técnicos
  const filteredProfessionals = professionals.filter(pro => {
    if (currentUser.role === 'client' && pro.status !== 'verified') return false;

    if (selectedCategory !== 'all' && !pro.categories.includes(selectedCategory)) {
      return false;
    }
    if (selectedZone !== 'Todas' && pro.zone !== selectedZone) {
      return false;
    }
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      return (
        pro.name.toLowerCase().includes(q) ||
        pro.specialtyName.toLowerCase().includes(q) ||
        pro.zone.toLowerCase().includes(q)
      );
    }
    return true;
  });

  // Contratar y solicitar servicio técnico con pago protegido
  const handleConfirmBooking = async (bookingData) => {
    try {
      const newService = {
        id: `SRV-2026-${(services.length + 1).toString().padStart(2, '0')}`,
        clientId: currentUser.id,
        proId: bookingData.proId,
        proName: bookingData.proName,
        proSpecialty: bookingData.proSpecialty,
        description: bookingData.description,
        address: bookingData.address,
        status: 'EN CAMINO',
        totalAmount: bookingData.pricing.totalAmount,
        escrowStatus: 'Protegido',
        isUrgent: bookingData.isUrgent,
        scheduledAt: 'Hoy, Inmediato'
      };

      setServices(prev => [newService, ...prev]);
      setSelectedProForBooking(null);
      setActiveTab('services');

      Alert.alert(
        '¡Servicio Solicitado con Éxito!',
        `Tu solicitud a ${bookingData.proName} ha sido confirmada por ${bookingData.pricing.formattedTotal}. Tu pago permanece 100% protegido hasta que verifiques que el servicio quedó a satisfacción.`
      );

      createServiceOrder({
        ...bookingData,
        clientId: currentUser.id
      }).catch(err => console.log('DB sync:', err));
    } catch (err) {
      console.warn('Booking confirmed locally:', err);
    }
  };

  // Administrador aprueba técnico (HU03)
  const handleApprovePro = async (proId) => {
    await updateProfessionalStatus(proId, 'verified', null, currentUser.name);
    setProfessionals(prev =>
      prev.map(p => (p.id === proId ? { ...p, status: 'verified', rating: 5.0, reviewCount: 1 } : p))
    );
    if (currentUser.id === proId) {
      const updated = { ...currentUser, status: 'verified' };
      setCurrentUser(updated);
      setLocalUser(updated);
    }
    Alert.alert(
      'Técnico Acreditado con Éxito',
      'Matrícula y antecedentes penales validados. El técnico ahora está 100% visible en el catálogo de clientes.'
    );
  };

  // Administrador rechaza técnico con observaciones (HU03)
  const handleRejectPro = async (proId, reason) => {
    await updateProfessionalStatus(proId, 'rejected', reason, currentUser.name);
    setProfessionals(prev =>
      prev.map(p => (p.id === proId ? { ...p, status: 'rejected' } : p))
    );
    if (currentUser.id === proId) {
      const updated = { ...currentUser, status: 'rejected' };
      setCurrentUser(updated);
      setLocalUser(updated);
    }
    Alert.alert(
      'Expediente Notificado con Observaciones',
      `Se notificó al técnico para subsanar documentos:\n"${reason || 'Documento no legible o no vigente'}"`
    );
  };

  // Abrir expediente de verificación
  const handleOpenVerificationDossier = async (pro, isAuditor = false) => {
    const dossier = await fetchVerificationDossier(pro.id);
    setActiveDossier(dossier);
    setDossierProName(pro.name);
    setIsAuditorModeForDossier(isAuditor);
    setVerificationModalVisible(true);
  };

  // Vistas de pestañas
  const renderExploreTab = () => (
    <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.tabContent}>
      {/* Search Bar */}
      <View style={styles.searchWrapper}>
        <Ionicons name="search" size={18} color={THEME.colors.textLight} />
        <TextInput
          style={styles.searchInput}
          placeholder="¿Qué necesitas reparar? (Ej. corto, fuga, gas...)"
          placeholderTextColor={THEME.colors.textLight}
          value={searchQuery}
          onChangeText={setSearchQuery}
        />
        {searchQuery.length > 0 && (
          <TouchableOpacity onPress={() => setSearchQuery('')}>
            <Ionicons name="close-circle" size={18} color={THEME.colors.textLight} />
          </TouchableOpacity>
        )}
      </View>

      {/* Trust Banner */}
      <View style={styles.trustBanner}>
        <View style={styles.trustIcon}>
          <Ionicons name="shield-checkmark" size={20} color={THEME.colors.verified} />
        </View>
        <View style={{ flex: 1 }}>
          <Text style={styles.trustTitle}>Profesionales 100% Verificados</Text>
          <Text style={styles.trustSub}>Cédula colombiana, antecedentes PONAL y matrículas CONTE/EPM.</Text>
        </View>
      </View>

      {/* Comunas Filter */}
      <View style={styles.sectionHeaderRow}>
        <Text style={styles.sectionTitle}>Comunas de Medellín</Text>
        <Text style={styles.zoneActiveText}>{selectedZone}</Text>
      </View>
      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.zonesScroll}>
        {['Todas', 'El Poblado', 'Laureles', 'Belén', 'Envigado', 'Robledo'].map(zone => (
          <TouchableOpacity
            key={zone}
            style={[styles.zonePill, selectedZone === zone && styles.zonePillActive]}
            onPress={() => setSelectedZone(zone)}
          >
            <Text style={[styles.zonePillText, selectedZone === zone && styles.zonePillTextActive]}>{zone}</Text>
          </TouchableOpacity>
        ))}
      </ScrollView>

      {/* Categories */}
      <Text style={styles.sectionTitle}>Categorías de Servicio</Text>
      <View style={styles.categoriesGrid}>
        {INITIAL_CATEGORIES.map(cat => {
          const isActive = selectedCategory === cat.id;
          return (
            <TouchableOpacity
              key={cat.id}
              style={styles.categoryItem}
              onPress={() => setSelectedCategory(cat.id)}
              activeOpacity={0.7}
            >
              <View style={[styles.categoryCircle, isActive && styles.categoryCircleActive]}>
                <Ionicons
                  name={cat.icon}
                  size={20}
                  color={isActive ? '#ffffff' : THEME.colors.primary}
                />
              </View>
              <Text style={[styles.categoryName, isActive && styles.categoryNameActive]}>
                {cat.name}
              </Text>
            </TouchableOpacity>
          );
        })}
      </View>

      {/* Technicians List */}
      <View style={styles.sectionHeaderRow}>
        <Text style={styles.sectionTitle}>Técnicos Disponibles en Medellín</Text>
        <Text style={styles.countBadge}>{filteredProfessionals.length} verificados</Text>
      </View>

      {filteredProfessionals.length === 0 ? (
        <View style={styles.emptyCard}>
          <Ionicons name="search-outline" size={36} color={THEME.colors.textLight} />
          <Text style={styles.emptyTitle}>No hay técnicos disponibles con estos filtros</Text>
          <Text style={styles.emptySub}>Prueba seleccionando otra comuna o categoría de servicio.</Text>
        </View>
      ) : (
        filteredProfessionals.map(pro => (
          <ProfessionalCard
            key={pro.id}
            professional={pro}
            onOpenDetail={setSelectedProForDetail}
            onOpenBooking={setSelectedProForBooking}
            onSelect={setSelectedProForDetail}
            onBook={setSelectedProForBooking}
          />
        ))
      )}
    </ScrollView>
  );

  const renderServicesTab = () => (
    <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.tabContent}>
      <Text style={styles.sectionTitle}>Mis Solicitudes y Servicios</Text>
      <Text style={styles.sectionSub}>Garantía A La Mano: tu dinero permanece protegido hasta tu visto bueno.</Text>

      {services.map(srv => (
        <View key={srv.id} style={styles.serviceCard}>
          <View style={styles.srvTopRow}>
            <View>
              <Text style={styles.srvCode}>{srv.id}</Text>
              <Text style={styles.srvProName}>{srv.proName}</Text>
              <Text style={styles.srvSpecialty}>{srv.proSpecialty}</Text>
            </View>
            <View style={styles.statusBadge}>
              <Text style={styles.statusBadgeText}>{srv.status}</Text>
            </View>
          </View>

          <View style={styles.srvDetailBox}>
            <Text style={styles.srvDesc}>{srv.description}</Text>
            <View style={styles.srvMetaRow}>
              <Ionicons name="location" size={13} color={THEME.colors.textMuted} />
              <Text style={styles.srvAddress}>{srv.address}</Text>
            </View>
          </View>

          <View style={styles.srvFooterRow}>
            <View>
              <Text style={styles.escrowLabel}>Protección de Pago:</Text>
              <View style={styles.escrowBadge}>
                <Ionicons name="shield-checkmark" size={12} color="#0284c7" />
                <Text style={styles.escrowVal}>{srv.escrowStatus || 'Protegido'}</Text>
              </View>
            </View>

            <View style={{ alignItems: 'flex-end' }}>
              <Text style={styles.totalLabel}>Monto Protegido:</Text>
              <Text style={styles.totalVal}>${Number(srv.totalAmount).toLocaleString('es-CO')} COP</Text>
            </View>
          </View>

          {/* Botón Chatear con el Técnico */}
          <TouchableOpacity
            style={styles.chatActionBtn}
            onPress={() => {
              const matchedPro = professionals.find(p => p.id === srv.proId || p.name === srv.proName);
              handleOpenChat(matchedPro || { name: srv.proName, specialtyName: srv.proSpecialty }, srv.id);
            }}
            activeOpacity={0.8}
          >
            <Ionicons name="chatbubbles" size={15} color={THEME.colors.primary} />
            <Text style={styles.chatActionBtnText}>Chatear con el Técnico</Text>
          </TouchableOpacity>

          {srv.status === 'EN CAMINO' && (
            <TouchableOpacity
              style={styles.confirmReleaseBtn}
              onPress={() => {
                Alert.alert(
                  'Confirmar y Entregar Pago',
                  `¿Confirmas que el servicio fue realizado a satisfacción? Se transferirán $${Number(srv.totalAmount).toLocaleString('es-CO')} COP a la cuenta del técnico.`,
                  [
                    { text: 'Cancelar', style: 'cancel' },
                    {
                      text: 'Sí, Entregar Pago',
                      onPress: () => {
                        setServices(services.map(s => s.id === srv.id ? { ...s, status: 'FINALIZADO', escrowStatus: 'Liberado' } : s));
                        Alert.alert('Pago Entregado', '¡Gracias por confiar en A La Mano! El dinero fue transferido.');
                      }
                    }
                  ]
                );
              }}
              activeOpacity={0.85}
            >
              <Ionicons name="checkmark-done-circle" size={16} color="#ffffff" />
              <Text style={styles.confirmReleaseText}>Confirmar Trabajo y Liberar Pago</Text>
            </TouchableOpacity>
          )}
        </View>
      ))}
    </ScrollView>
  );

  const renderMessagesTab = () => {
    const conversations = [
      {
        id: 'conv_1',
        pro: professionals[0] || {
          id: 'usr_pro_1',
          name: 'Carlos Mario Restrepo',
          specialtyName: 'Electricista Residencial',
          avatar: 'https://images.unsplash.com/photo-1540569014015-19a7be504e3a?w=150'
        },
        serviceCode: 'SRV-2026-01',
        lastMessage: 'Estoy cruzando la 80 hacia su sector, calculo unos 12 a 15 minutos máximo.',
        time: '12:50 PM',
        unread: true
      },
      {
        id: 'conv_2',
        pro: professionals[1] || {
          id: 'usr_pro_2',
          name: 'Diego Alejandro Ospina',
          specialtyName: 'Maestro Plomero',
          avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150'
        },
        serviceCode: 'SRV-2026-02',
        lastMessage: 'Perfecto doña Juanita, llevo la tubería de repuesto y el soplete.',
        time: 'Ayer',
        unread: false
      }
    ];

    return (
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.tabContent}>
        <Text style={styles.sectionTitle}>Mensajes y Coordinación</Text>
        <Text style={styles.sectionSub}>Chat directo y seguro con técnicos asignados a tus servicios.</Text>

        {conversations.map(conv => (
          <TouchableOpacity
            key={conv.id}
            style={styles.chatCard}
            onPress={() => handleOpenChat(conv.pro, conv.serviceCode)}
            activeOpacity={0.75}
          >
            <View style={{ position: 'relative' }}>
              <Image
                source={{ uri: conv.pro.avatar }}
                style={styles.chatAvatar}
              />
              <View style={styles.chatOnlineDot} />
            </View>

            <View style={{ flex: 1 }}>
              <View style={styles.chatHeaderRow}>
                <Text style={styles.chatName}>{conv.pro.name}</Text>
                <Text style={styles.chatTime}>{conv.time}</Text>
              </View>
              <Text style={styles.chatProSpecialty}>{conv.pro.specialtyName} • {conv.serviceCode}</Text>
              <Text style={styles.chatLastMsg} numberOfLines={2}>
                "{conv.lastMessage}"
              </Text>
            </View>

            <View style={styles.chatChevronCol}>
              {conv.unread && <View style={styles.unreadBadge} />}
              <Ionicons name="chevron-forward" size={18} color={THEME.colors.textLight} />
            </View>
          </TouchableOpacity>
        ))}
      </ScrollView>
    );
  };

  const renderProfileTab = () => (
    <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.tabContent}>
      {currentUser.role === 'admin' ? (
        <View>
          {/* Header Admin */}
          <View style={styles.adminHeaderBox}>
            <Ionicons name="shield-checkmark" size={26} color="#ffffff" />
            <View style={{ flex: 1 }}>
              <Text style={styles.adminTitle}>Consola de Auditoría (Admin UdeA)</Text>
              <Text style={styles.adminSub}>Aprobación de Técnicos, Validación de Cédulas y Matrículas CONTE</Text>
            </View>
          </View>

          {/* Lista de Técnicos para Auditoría */}
          <View style={styles.sectionHeaderRow}>
            <Text style={styles.sectionTitle}>Solicitudes de Ingreso Técnico</Text>
            <Text style={styles.countBadge}>
              {professionals.filter(p => p.status === 'pending').length} pendientes
            </Text>
          </View>

          {professionals
            .filter(p => p.status === 'pending')
            .map(p => (
              <AdminAuditCard
                key={p.id}
                professional={p}
                onApprove={() => handleApprovePro(p.id)}
                onReject={() => handleRejectPro(p.id, 'Requiere soporte de antecedentes con vigencia menor a 30 días.')}
                onInspect={() => handleOpenVerificationDossier(p, true)}
              />
            ))}

          {professionals.filter(p => p.status === 'pending').length === 0 && (
            <View style={styles.emptyCard}>
              <Ionicons name="checkmark-done-circle" size={36} color={THEME.colors.verified} />
              <Text style={styles.emptyTitle}>¡Todo al día! No hay técnicos pendientes</Text>
              <Text style={styles.emptySub}>Todos los expedientes de matrícula y cédulas han sido validados.</Text>
            </View>
          )}

          {/* Técnicos Ya Verificados */}
          <Text style={[styles.sectionTitle, { marginTop: 16 }]}>Técnicos Ya Acreditados</Text>
          {professionals
            .filter(p => p.status === 'verified')
            .map(p => (
              <AdminAuditCard
                key={p.id}
                professional={p}
                onApprove={() => {}}
                onReject={() => {}}
                onInspect={() => handleOpenVerificationDossier(p, true)}
              />
            ))}
        </View>
      ) : (
        <View style={styles.profileBox}>
          {/* Avatar y Datos Personales */}
          <View style={styles.profileAvatar}>
            <Text style={styles.profileAvatarText}>
              {currentUser.name ? currentUser.name.charAt(0) : 'U'}
            </Text>
          </View>
          <Text style={styles.profileName}>{currentUser.name}</Text>
          <Text style={styles.profileRole}>
            {currentUser.role === 'professional' ? 'Técnico Especialista' : 'Cliente Verificado'} • {currentUser.zone}, Medellín
          </Text>
          <Text style={styles.profileEmail}>{currentUser.email} • {currentUser.phone}</Text>

          {/* BANNER DE VERIFICACIÓN DE IDENTIDAD SI ES TÉCNICO (HU02) */}
          {currentUser.role === 'professional' && (
            <View style={[
              styles.verificationCard,
              currentUser.status === 'verified' ? styles.verificationCardVerified : styles.verificationCardPending
            ]}>
              <View style={styles.verifHeaderRow}>
                <Ionicons
                  name={currentUser.status === 'verified' ? 'shield-checkmark' : 'alert-circle'}
                  size={22}
                  color={currentUser.status === 'verified' ? '#15803d' : '#b45309'}
                />
                <View style={{ flex: 1 }}>
                  <Text style={[
                    styles.verifTitle,
                    currentUser.status === 'verified' ? styles.verifTitleVerified : styles.verifTitlePending
                  ]}>
                    {currentUser.status === 'verified'
                      ? '✓ Técnico Acreditado y Verificado'
                      : '⏳ Expediente en Proceso de Verificación'}
                  </Text>
                  <Text style={styles.verifSub}>
                    {currentUser.status === 'verified'
                      ? 'Cédula, Antecedentes PONAL y Matrícula CONTE validados exitosamente.'
                      : 'Tus documentos están radicados y en proceso de validación por auditoría UdeA.'}
                  </Text>
                </View>
              </View>

              <TouchableOpacity
                style={styles.verifActionBtn}
                onPress={() => handleOpenVerificationDossier(currentUser, false)}
                activeOpacity={0.8}
              >
                <Ionicons name="document-text" size={15} color={THEME.colors.primary} />
                <Text style={styles.verifActionText}>
                  {currentUser.status === 'verified' ? 'Ver Mi Expediente de Acreditación' : 'Gestionar y Subir Documentos'}
                </Text>
              </TouchableOpacity>
            </View>
          )}

          {/* Opciones del Menú */}
          <View style={styles.profileMenu}>
            <TouchableOpacity style={styles.menuRow} onPress={() => setAuthModalVisible(true)}>
              <Ionicons name="person-add" size={18} color={THEME.colors.primary} />
              <Text style={styles.menuText}>Iniciar Sesión o Crear Nueva Cuenta</Text>
              <Ionicons name="chevron-forward" size={16} color={THEME.colors.textLight} />
            </TouchableOpacity>

            <TouchableOpacity style={styles.menuRow} onPress={() => setActiveTab('services')}>
              <Ionicons name="receipt" size={18} color={THEME.colors.primary} />
              <Text style={styles.menuText}>Historial de Servicios Contratados</Text>
              <Ionicons name="chevron-forward" size={16} color={THEME.colors.textLight} />
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.menuRow}
              onPress={() => Alert.alert('Garantía A La Mano', 'Pago Protegido: Tu dinero se retiene de forma 100% segura y solo se entrega al técnico una vez que confirmes que el servicio fue realizado a entera satisfacción.')}
            >
              <Ionicons name="shield-half" size={18} color={THEME.colors.primary} />
              <Text style={styles.menuText}>Póliza de Garantía del Hogar</Text>
              <Ionicons name="chevron-forward" size={16} color={THEME.colors.textLight} />
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.menuRow}
              onPress={() => Alert.alert('Habeas Data', 'Datos protegidos bajo la Ley 1581 de 2012 de Colombia.')}
            >
              <Ionicons name="document-text" size={18} color={THEME.colors.primary} />
              <Text style={styles.menuText}>Términos y Privacidad (Ley 1581)</Text>
              <Ionicons name="chevron-forward" size={16} color={THEME.colors.textLight} />
            </TouchableOpacity>
          </View>

          {/* Cerrar Sesión */}
          <TouchableOpacity
            style={styles.logoutBtn}
            onPress={async () => {
              await logoutUser();
              setCurrentUser(DEMO_USERS.client);
              Alert.alert('Sesión Cerrada', 'Has vuelto al usuario de demostración.');
            }}
          >
            <Ionicons name="log-out-outline" size={16} color={THEME.colors.urgent} />
            <Text style={styles.logoutBtnText}>Cerrar Sesión Activa</Text>
          </TouchableOpacity>
        </View>
      )}
    </ScrollView>
  );

  return (
    <SafeAreaProvider>
      <SafeAreaView style={styles.safeContainer} edges={['top', 'left', 'right']}>
        <StatusBar barStyle="dark-content" backgroundColor="#ffffff" />
        <Header
          currentUser={currentUser}
          onPressProfile={() => setActiveTab('profile')}
        />

        <View style={styles.mainContainer}>
          {activeTab === 'explore' && renderExploreTab()}
          {activeTab === 'services' && renderServicesTab()}
          {activeTab === 'messages' && renderMessagesTab()}
          {activeTab === 'profile' && renderProfileTab()}
        </View>

        {/* Bottom Tabs */}
        <View style={styles.bottomNav}>
          <TouchableOpacity style={styles.navItem} onPress={() => setActiveTab('explore')}>
            <Ionicons
              name={activeTab === 'explore' ? 'home' : 'home-outline'}
              size={22}
              color={activeTab === 'explore' ? THEME.colors.primary : THEME.colors.textMuted}
            />
            <Text style={[styles.navLabel, activeTab === 'explore' && styles.navLabelActive]}>Explorar</Text>
          </TouchableOpacity>

          <TouchableOpacity style={styles.navItem} onPress={() => setActiveTab('services')}>
            <Ionicons
              name={activeTab === 'services' ? 'receipt' : 'receipt-outline'}
              size={22}
              color={activeTab === 'services' ? THEME.colors.primary : THEME.colors.textMuted}
            />
            <Text style={[styles.navLabel, activeTab === 'services' && styles.navLabelActive]}>Solicitudes</Text>
          </TouchableOpacity>

          <TouchableOpacity style={styles.navItem} onPress={() => setActiveTab('messages')}>
            <Ionicons
              name={activeTab === 'messages' ? 'chatbubbles' : 'chatbubbles-outline'}
              size={22}
              color={activeTab === 'messages' ? THEME.colors.primary : THEME.colors.textMuted}
            />
            <Text style={[styles.navLabel, activeTab === 'messages' && styles.navLabelActive]}>Mensajes</Text>
          </TouchableOpacity>

          <TouchableOpacity style={styles.navItem} onPress={() => setActiveTab('profile')}>
            <Ionicons
              name={activeTab === 'profile' ? 'person' : 'person-outline'}
              size={22}
              color={activeTab === 'profile' ? THEME.colors.primary : THEME.colors.textMuted}
            />
            <Text style={[styles.navLabel, activeTab === 'profile' && styles.navLabelActive]}>Perfil</Text>
          </TouchableOpacity>
        </View>

        {/* Modals */}
        <BookingModal
          visible={!!selectedProForBooking}
          professional={selectedProForBooking}
          onClose={() => setSelectedProForBooking(null)}
          onConfirmBooking={handleConfirmBooking}
        />

        <ProDetailModal
          visible={!!selectedProForDetail}
          professional={selectedProForDetail}
          onClose={() => setSelectedProForDetail(null)}
          onOpenBooking={setSelectedProForBooking}
        />

        <AuthModal
          visible={authModalVisible}
          onClose={() => setAuthModalVisible(false)}
          onAuthSuccess={(user) => {
            setCurrentUser(user);
            if (user.role === 'admin') setActiveTab('profile');
            else setActiveTab('explore');
          }}
        />

        <VerificationModal
          visible={verificationModalVisible}
          onClose={() => setVerificationModalVisible(false)}
          dossier={activeDossier}
          professionalName={dossierProName}
          isAuditor={isAuditorModeForDossier}
          onApproveDossier={(proId) => handleApprovePro(proId)}
          onRejectDossier={(proId, reason) => handleRejectPro(proId, reason)}
        />

        <ChatModal
          visible={chatModalVisible}
          pro={selectedProForChat}
          serviceId={selectedServiceForChat}
          onClose={() => setChatModalVisible(false)}
        />
      </SafeAreaView>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  safeContainer: {
    flex: 1,
    backgroundColor: '#ffffff'
  },
  mainContainer: {
    flex: 1,
    backgroundColor: '#f8fafc'
  },
  tabContent: {
    padding: 16,
    paddingBottom: 30
  },
  searchWrapper: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: '#ffffff',
    borderWidth: 1,
    borderColor: THEME.colors.border,
    borderRadius: 14,
    paddingHorizontal: 12,
    height: 44,
    marginBottom: 12
  },
  searchInput: {
    flex: 1,
    fontSize: 12.5,
    color: THEME.colors.textMain
  },
  trustBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    backgroundColor: THEME.colors.verifiedBg,
    borderWidth: 1,
    borderColor: '#a7f3d0',
    padding: 12,
    borderRadius: 14,
    marginBottom: 14
  },
  trustIcon: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: '#ffffff',
    alignItems: 'center',
    justifyContent: 'center'
  },
  trustTitle: {
    fontSize: 12.5,
    fontWeight: '800',
    color: '#065f46'
  },
  trustSub: {
    fontSize: 11,
    color: '#047857'
  },
  sectionHeaderRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 8,
    marginTop: 4
  },
  sectionTitle: {
    fontSize: 13.5,
    fontWeight: '800',
    color: THEME.colors.textMain
  },
  sectionSub: {
    fontSize: 11.5,
    color: THEME.colors.textMuted,
    marginBottom: 12
  },
  zoneActiveText: {
    fontSize: 11.5,
    fontWeight: '700',
    color: THEME.colors.primary
  },
  zonesScroll: {
    gap: 8,
    paddingBottom: 12
  },
  zonePill: {
    paddingVertical: 5,
    paddingHorizontal: 12,
    borderRadius: 20,
    backgroundColor: '#ffffff',
    borderWidth: 1,
    borderColor: THEME.colors.border
  },
  zonePillActive: {
    backgroundColor: THEME.colors.primary,
    borderColor: THEME.colors.primary
  },
  zonePillText: {
    fontSize: 11.5,
    fontWeight: '600',
    color: THEME.colors.textMuted
  },
  zonePillTextActive: {
    color: '#ffffff'
  },
  categoriesGrid: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 16
  },
  categoryItem: {
    alignItems: 'center',
    gap: 4
  },
  categoryCircle: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: THEME.colors.primaryLight,
    alignItems: 'center',
    justifyContent: 'center'
  },
  categoryCircleActive: {
    backgroundColor: THEME.colors.primary
  },
  categoryName: {
    fontSize: 10.5,
    fontWeight: '600',
    color: THEME.colors.textMuted
  },
  categoryNameActive: {
    color: THEME.colors.primary,
    fontWeight: '800'
  },
  countBadge: {
    fontSize: 11,
    fontWeight: '700',
    color: THEME.colors.primary,
    backgroundColor: THEME.colors.primaryLight,
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 8
  },
  emptyCard: {
    backgroundColor: '#ffffff',
    borderRadius: 16,
    padding: 24,
    alignItems: 'center',
    gap: 6,
    borderWidth: 1,
    borderColor: THEME.colors.border,
    marginVertical: 10
  },
  emptyTitle: {
    fontSize: 13,
    fontWeight: '700',
    color: THEME.colors.textMain
  },
  emptySub: {
    fontSize: 11,
    color: THEME.colors.textMuted,
    textAlign: 'center'
  },
  serviceCard: {
    backgroundColor: '#ffffff',
    borderRadius: 16,
    padding: 14,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: THEME.colors.border,
    gap: 8
  },
  srvTopRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start'
  },
  srvCode: {
    fontSize: 10,
    fontWeight: '700',
    color: THEME.colors.textMuted
  },
  srvProName: {
    fontSize: 14,
    fontWeight: '800',
    color: THEME.colors.textMain
  },
  srvSpecialty: {
    fontSize: 11.5,
    color: THEME.colors.primary,
    fontWeight: '600'
  },
  statusBadge: {
    backgroundColor: '#e0f2fe',
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 8
  },
  statusBadgeText: {
    fontSize: 10,
    fontWeight: '800',
    color: '#0284c7'
  },
  srvDetailBox: {
    backgroundColor: '#f8fafc',
    padding: 10,
    borderRadius: 10,
    gap: 4
  },
  srvDesc: {
    fontSize: 12,
    color: THEME.colors.textMain,
    lineHeight: 16
  },
  srvMetaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4
  },
  srvAddress: {
    fontSize: 11,
    color: THEME.colors.textMuted
  },
  srvFooterRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-end',
    borderTopWidth: 1,
    borderTopColor: '#f1f5f9',
    paddingTop: 8
  },
  escrowLabel: {
    fontSize: 10,
    color: THEME.colors.textMuted
  },
  escrowBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    marginTop: 2
  },
  escrowVal: {
    fontSize: 11,
    fontWeight: '700',
    color: '#0284c7'
  },
  totalLabel: {
    fontSize: 10,
    color: THEME.colors.textMuted
  },
  totalVal: {
    fontSize: 14,
    fontWeight: '800',
    color: THEME.colors.primary
  },
  confirmReleaseBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    backgroundColor: '#15803d',
    paddingVertical: 10,
    borderRadius: 12,
    marginTop: 4
  },
  confirmReleaseText: {
    fontSize: 12,
    fontWeight: '700',
    color: '#ffffff'
  },
  chatActionBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    backgroundColor: THEME.colors.primaryLight,
    borderWidth: 1,
    borderColor: THEME.colors.primary,
    paddingVertical: 9,
    borderRadius: 12,
    marginTop: 8
  },
  chatActionBtnText: {
    fontSize: 12,
    fontWeight: '700',
    color: THEME.colors.primary
  },
  chatCard: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    backgroundColor: '#ffffff',
    padding: 14,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: THEME.colors.border,
    marginBottom: 10,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.04,
    shadowRadius: 3,
    elevation: 1
  },
  chatAvatar: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: '#e2e8f0'
  },
  chatOnlineDot: {
    position: 'absolute',
    bottom: 0,
    right: 0,
    width: 12,
    height: 12,
    borderRadius: 6,
    backgroundColor: THEME.colors.verified,
    borderWidth: 2,
    borderColor: '#ffffff'
  },
  chatHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center'
  },
  chatName: {
    fontSize: 13.5,
    fontWeight: '700',
    color: THEME.colors.textMain
  },
  chatTime: {
    fontSize: 10.5,
    color: THEME.colors.textMuted
  },
  chatProSpecialty: {
    fontSize: 11,
    color: THEME.colors.primary,
    fontWeight: '600',
    marginTop: 1
  },
  chatLastMsg: {
    fontSize: 11.5,
    color: THEME.colors.textMuted,
    marginTop: 2,
    lineHeight: 16
  },
  chatChevronCol: {
    alignItems: 'center',
    gap: 6
  },
  unreadBadge: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: THEME.colors.accent
  },
  adminHeaderBox: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    backgroundColor: '#0f172a',
    padding: 16,
    borderRadius: 16,
    marginBottom: 14
  },
  adminTitle: {
    fontSize: 14,
    fontWeight: '800',
    color: '#ffffff'
  },
  adminSub: {
    fontSize: 10.5,
    color: '#94a3b8',
    marginTop: 2,
    lineHeight: 14
  },
  profileBox: {
    backgroundColor: '#ffffff',
    borderRadius: 16,
    padding: 20,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: THEME.colors.border
  },
  profileAvatar: {
    width: 64,
    height: 64,
    borderRadius: 32,
    backgroundColor: THEME.colors.primaryLight,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 8
  },
  profileAvatarText: {
    fontSize: 20,
    fontWeight: '800',
    color: THEME.colors.primary
  },
  profileName: {
    fontSize: 16,
    fontWeight: '800',
    color: THEME.colors.textMain
  },
  profileRole: {
    fontSize: 11.5,
    fontWeight: '600',
    color: THEME.colors.primary,
    marginTop: 2
  },
  profileEmail: {
    fontSize: 11,
    color: THEME.colors.textMuted,
    marginTop: 2
  },
  verificationCard: {
    width: '100%',
    borderRadius: 14,
    padding: 14,
    marginTop: 14,
    borderWidth: 1,
    gap: 10
  },
  verificationCardVerified: {
    backgroundColor: '#f0fdf4',
    borderColor: '#86efac'
  },
  verificationCardPending: {
    backgroundColor: '#fffbeb',
    borderColor: '#fde68a'
  },
  verifHeaderRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10
  },
  verifTitle: {
    fontSize: 12,
    fontWeight: '800'
  },
  verifTitleVerified: {
    color: '#15803d'
  },
  verifTitlePending: {
    color: '#b45309'
  },
  verifSub: {
    fontSize: 10.5,
    color: THEME.colors.textMuted,
    marginTop: 2,
    lineHeight: 14
  },
  verifActionBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    backgroundColor: '#ffffff',
    paddingVertical: 8,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: THEME.colors.primary
  },
  verifActionText: {
    fontSize: 11.5,
    fontWeight: '700',
    color: THEME.colors.primary
  },
  dbStatusCard: {
    width: '100%',
    backgroundColor: '#f8fafc',
    borderRadius: 14,
    padding: 12,
    marginTop: 12,
    borderWidth: 1,
    borderColor: THEME.colors.border,
    gap: 8
  },
  dbStatusHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8
  },
  dbDotLarge: {
    width: 10,
    height: 10,
    borderRadius: 5,
    backgroundColor: '#f59e0b'
  },
  dbDotLargeConnected: {
    backgroundColor: '#16a34a'
  },
  dbStatusTitle: {
    fontSize: 12,
    fontWeight: '700',
    color: THEME.colors.textMain
  },
  dbStatusSub: {
    fontSize: 10.5,
    color: THEME.colors.textMuted
  },
  dbManageBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    backgroundColor: '#ffffff',
    paddingVertical: 7,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: THEME.colors.border
  },
  dbManageBtnText: {
    fontSize: 11,
    fontWeight: '700',
    color: THEME.colors.primary
  },
  profileMenu: {
    width: '100%',
    marginTop: 16,
    borderTopWidth: 1,
    borderTopColor: '#f1f5f9'
  },
  menuRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 14,
    borderBottomWidth: 1,
    borderBottomColor: '#f1f5f9'
  },
  menuText: {
    flex: 1,
    marginLeft: 10,
    fontSize: 12.5,
    fontWeight: '600',
    color: THEME.colors.textMain
  },
  logoutBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginTop: 16,
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: 10,
    backgroundColor: '#fff1f2'
  },
  logoutBtnText: {
    fontSize: 12,
    fontWeight: '700',
    color: THEME.colors.urgent
  },
  bottomNav: {
    flexDirection: 'row',
    backgroundColor: '#ffffff',
    borderTopWidth: 1,
    borderTopColor: THEME.colors.border,
    paddingVertical: 8,
    paddingHorizontal: 6
  },
  navItem: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 3
  },
  navLabel: {
    fontSize: 10,
    fontWeight: '600',
    color: THEME.colors.textMuted
  },
  navLabelActive: {
    color: THEME.colors.primary,
    fontWeight: '700'
  }
});
