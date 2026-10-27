import React, { useState, useRef, useEffect } from 'react';
import {
  Modal,
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  TextInput,
  ScrollView,
  Image,
  KeyboardAvoidingView,
  Platform,
  SafeAreaView
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { THEME } from '../constants/theme';

export default function ChatModal({ visible, pro, onClose, serviceId = 'SRV-2026-01' }) {
  if (!pro) return null;

  const [inputMessage, setInputMessage] = useState('');
  const [messages, setMessages] = useState([
    {
      id: 'm1',
      sender: 'system',
      text: '🔒 Canal protegido por A La Mano. Los datos de contacto se mantienen privados por seguridad.',
      time: '12:45 PM'
    },
    {
      id: 'm2',
      sender: 'pro',
      text: `Buenas tardes, le habla ${pro.name}. Voy en camino a su dirección con los materiales necesarios para el trabajo.`,
      time: '12:46 PM'
    },
    {
      id: 'm3',
      sender: 'client',
      text: 'Buenas tardes maestro. ¿Aproximadamente en cuántos minutos llega?',
      time: '12:48 PM'
    },
    {
      id: 'm4',
      sender: 'pro',
      text: 'Estoy cruzando la 80 hacia su sector, calculo unos 12 a 15 minutos máximo.',
      time: '12:50 PM'
    }
  ]);

  const scrollViewRef = useRef(null);

  useEffect(() => {
    if (visible) {
      setTimeout(() => {
        scrollViewRef.current?.scrollToEnd({ animated: true });
      }, 250);
    }
  }, [visible, messages]);

  const handleSend = (textToSend) => {
    const text = (textToSend || inputMessage).trim();
    if (!text) return;

    const newMsg = {
      id: `msg-${Date.now()}`,
      sender: 'client',
      text: text,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };

    setMessages(prev => [...prev, newMsg]);
    setInputMessage('');

    // Respuesta simulada del técnico después de 1.5s para enriquecer la demo
    setTimeout(() => {
      const responses = [
        '¡Enterado! Ya estoy muy cerca, nos vemos en un momento.',
        'Entendido, llevo las herramientas listas.',
        'Perfecto, cualquier detalle le aviso por aquí.',
        'Listo, al llegar le timbro o le escribo.'
      ];
      const randomResponse = responses[Math.floor(Math.random() * responses.length)];
      setMessages(prev => [
        ...prev,
        {
          id: `msg-pro-${Date.now()}`,
          sender: 'pro',
          text: randomResponse,
          time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        }
      ]);
    }, 1200);
  };

  const QUICK_REPLIES = [
    '¡Hola! Ya estoy en la casa.',
    '¿Cuánto tardas en llegar?',
    'El timbre no suena, porfa avísame al llegar.',
    '¡Listo, aquí te espero!'
  ];

  return (
    <Modal visible={visible} animationType="slide" transparent={false} onRequestClose={onClose}>
      <SafeAreaView style={styles.safeArea}>
        <KeyboardAvoidingView
          behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
          style={styles.keyboardContainer}
          keyboardVerticalOffset={Platform.OS === 'ios' ? 10 : 0}
        >
          {/* Header del Chat */}
          <View style={styles.header}>
            <TouchableOpacity onPress={onClose} style={styles.backBtn} activeOpacity={0.7}>
              <Ionicons name="arrow-back" size={24} color={THEME.colors.textMain} />
            </TouchableOpacity>

            <View style={styles.proInfoContainer}>
              <View style={styles.avatarWrapper}>
                <Image
                  source={{ uri: pro.avatar || 'https://images.unsplash.com/photo-1540569014015-19a7be504e3a?w=150' }}
                  style={styles.avatar}
                />
                <View style={styles.onlineDot} />
              </View>

              <View style={{ flex: 1 }}>
                <Text style={styles.proName} numberOfLines={1}>{pro.name}</Text>
                <View style={styles.subRow}>
                  <Text style={styles.proSpecialty} numberOfLines={1}>{pro.specialtyName}</Text>
                  <Text style={styles.serviceRef}>• {serviceId}</Text>
                </View>
              </View>
            </View>

            <TouchableOpacity
              style={styles.infoBtn}
              onPress={() => alert(`Técnico: ${pro.name}\nEspecialidad: ${pro.specialtyName}\nTeléfono protegido por la plataforma A La Mano.`)}
            >
              <Ionicons name="shield-checkmark" size={20} color={THEME.colors.verified} />
            </TouchableOpacity>
          </View>

          {/* Lista de Mensajes */}
          <ScrollView
            ref={scrollViewRef}
            style={styles.messagesList}
            contentContainerStyle={styles.messagesContent}
            keyboardShouldPersistTaps="handled"
            showsVerticalScrollIndicator={false}
          >
            {messages.map(item => {
              if (item.sender === 'system') {
                return (
                  <View key={item.id} style={styles.systemBox}>
                    <Ionicons name="lock-closed" size={13} color="#0284c7" />
                    <Text style={styles.systemText}>{item.text}</Text>
                  </View>
                );
              }

              const isClient = item.sender === 'client';
              return (
                <View
                  key={item.id}
                  style={[
                    styles.msgWrapper,
                    isClient ? styles.msgWrapperClient : styles.msgWrapperPro
                  ]}
                >
                  <View
                    style={[
                      styles.bubble,
                      isClient ? styles.bubbleClient : styles.bubblePro
                    ]}
                  >
                    <Text style={[styles.msgText, isClient ? styles.msgTextClient : styles.msgTextPro]}>
                      {item.text}
                    </Text>
                    <Text style={[styles.msgTime, isClient ? styles.msgTimeClient : styles.msgTimePro]}>
                      {item.time}
                    </Text>
                  </View>
                </View>
              );
            })}
          </ScrollView>

          {/* Respuestas Rápidas */}
          <View style={styles.quickRepliesContainer}>
            <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.quickRepliesScroll}>
              {QUICK_REPLIES.map((reply, idx) => (
                <TouchableOpacity
                  key={idx}
                  style={styles.quickChip}
                  onPress={() => handleSend(reply)}
                  activeOpacity={0.7}
                >
                  <Text style={styles.quickChipText}>{reply}</Text>
                </TouchableOpacity>
              ))}
            </ScrollView>
          </View>

          {/* Input de Mensaje */}
          <View style={styles.inputContainer}>
            <TextInput
              style={styles.textInput}
              placeholder="Escribe un mensaje al técnico..."
              placeholderTextColor={THEME.colors.textLight}
              value={inputMessage}
              onChangeText={setInputMessage}
              multiline
              maxLength={400}
            />
            <TouchableOpacity
              style={[
                styles.sendBtn,
                inputMessage.trim().length > 0 ? styles.sendBtnActive : styles.sendBtnDisabled
              ]}
              onPress={() => handleSend()}
              disabled={inputMessage.trim().length === 0}
              activeOpacity={0.8}
            >
              <Ionicons
                name="send"
                size={18}
                color={inputMessage.trim().length > 0 ? '#ffffff' : THEME.colors.textLight}
              />
            </TouchableOpacity>
          </View>
        </KeyboardAvoidingView>
      </SafeAreaView>
    </Modal>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#ffffff'
  },
  keyboardContainer: {
    flex: 1,
    backgroundColor: '#f8fafc'
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: '#ffffff',
    borderBottomWidth: 1,
    borderBottomColor: THEME.colors.border
  },
  backBtn: {
    padding: 6
  },
  proInfoContainer: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10
  },
  avatarWrapper: {
    position: 'relative'
  },
  avatar: {
    width: 42,
    height: 42,
    borderRadius: 21,
    backgroundColor: '#e2e8f0'
  },
  onlineDot: {
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
  proName: {
    fontSize: 14.5,
    fontWeight: '700',
    color: THEME.colors.textMain
  },
  subRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4
  },
  proSpecialty: {
    fontSize: 11.5,
    color: THEME.colors.textMuted
  },
  serviceRef: {
    fontSize: 11,
    color: THEME.colors.primary,
    fontWeight: '600'
  },
  infoBtn: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: '#ecfdf5',
    alignItems: 'center',
    justifyContent: 'center'
  },
  messagesList: {
    flex: 1
  },
  messagesContent: {
    padding: 16,
    paddingBottom: 8,
    gap: 10
  },
  systemBox: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: '#f0f9ff',
    borderWidth: 1,
    borderColor: '#bae6fd',
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderRadius: 12,
    marginVertical: 6,
    alignSelf: 'center',
    maxWidth: '92%'
  },
  systemText: {
    fontSize: 11.5,
    color: '#0369a1',
    fontWeight: '500',
    lineHeight: 16
  },
  msgWrapper: {
    flexDirection: 'row',
    marginVertical: 2
  },
  msgWrapperClient: {
    justifyContent: 'flex-end'
  },
  msgWrapperPro: {
    justifyContent: 'flex-start'
  },
  bubble: {
    maxWidth: '80%',
    paddingHorizontal: 14,
    paddingVertical: 10,
    borderRadius: 18
  },
  bubbleClient: {
    backgroundColor: THEME.colors.primary,
    borderBottomRightRadius: 4
  },
  bubblePro: {
    backgroundColor: '#ffffff',
    borderWidth: 1,
    borderColor: THEME.colors.border,
    borderBottomLeftRadius: 4
  },
  msgText: {
    fontSize: 13.5,
    lineHeight: 19
  },
  msgTextClient: {
    color: '#ffffff'
  },
  msgTextPro: {
    color: THEME.colors.textMain
  },
  msgTime: {
    fontSize: 10,
    marginTop: 4,
    alignSelf: 'flex-end'
  },
  msgTimeClient: {
    color: 'rgba(255, 255, 255, 0.75)'
  },
  msgTimePro: {
    color: THEME.colors.textLight
  },
  quickRepliesContainer: {
    backgroundColor: '#ffffff',
    borderTopWidth: 1,
    borderTopColor: '#f1f5f9',
    paddingVertical: 8
  },
  quickRepliesScroll: {
    paddingHorizontal: 16,
    gap: 8
  },
  quickChip: {
    backgroundColor: '#f1f5f9',
    paddingVertical: 6,
    paddingHorizontal: 12,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: THEME.colors.border
  },
  quickChipText: {
    fontSize: 11.5,
    color: THEME.colors.textMain,
    fontWeight: '600'
  },
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    paddingHorizontal: 16,
    paddingVertical: 10,
    backgroundColor: '#ffffff',
    borderTopWidth: 1,
    borderTopColor: THEME.colors.border
  },
  textInput: {
    flex: 1,
    minHeight: 40,
    maxHeight: 100,
    backgroundColor: '#f8fafc',
    borderWidth: 1,
    borderColor: THEME.colors.border,
    borderRadius: 20,
    paddingHorizontal: 14,
    paddingVertical: 8,
    fontSize: 13,
    color: THEME.colors.textMain
  },
  sendBtn: {
    width: 40,
    height: 40,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center'
  },
  sendBtnActive: {
    backgroundColor: THEME.colors.primary
  },
  sendBtnDisabled: {
    backgroundColor: '#e2e8f0'
  }
});
