export const INITIAL_CATEGORIES = [
  { id: 'all', name: 'Todos', icon: 'apps' },
  { id: 'electricidad', name: 'Electricidad', icon: 'flash' },
  { id: 'plomeria', name: 'Plomería', icon: 'water' },
  { id: 'gas', name: 'Redes Gas', icon: 'flame' },
  { id: 'cerrajeria', name: 'Cerrajería', icon: 'key' }
];

export const INITIAL_PROFESSIONALS = [
  {
    id: 'usr_pro_1',
    name: 'Carlos Mario Restrepo',
    specialtyName: 'Electricista Residencial e Industrial',
    categories: ['electricidad'],
    rate: 45000,
    rating: 4.9,
    reviewCount: 88,
    distanceKm: 1.8,
    zone: 'Laureles',
    status: 'verified',
    ccNumber: '71.394.205',
    conteReg: 'CONTE TE1-49201',
    bio: 'Técnico certificado con más de 12 años atendiendo hogares en Medellín. Especialista en cortocircuitos, cuadros eléctricos y certificación RETIE.',
    avatar: 'https://images.unsplash.com/photo-1540569014015-19a7be504e3a?w=180&auto=format&fit=crop&q=80'
  },
  {
    id: 'usr_pro_2',
    name: 'Diego Alejandro Ospina',
    specialtyName: 'Maestro Plomero e Hidráulico',
    categories: ['plomeria'],
    rate: 40000,
    rating: 4.8,
    reviewCount: 95,
    distanceKm: 2.5,
    zone: 'Belén',
    status: 'verified',
    ccNumber: '70.812.903',
    conteReg: null,
    bio: 'Detección no invasiva de fugas de agua, cambio de griferías, destape de cañerías con sonda eléctrica y mantenimiento preventivo en todo el Valle de Aburrá.',
    avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=180&auto=format&fit=crop&q=80'
  },
  {
    id: 'usr_pro_3',
    name: 'Andrés Felipe Morales',
    specialtyName: 'Técnico Gas, Redes y Calentadores',
    categories: ['gas'],
    rate: 50000,
    rating: 5.0,
    reviewCount: 47,
    distanceKm: 1.2,
    zone: 'El Poblado',
    status: 'verified',
    ccNumber: '1.020.849.201',
    conteReg: 'Certificado EPM Gas #G-9921',
    bio: 'Reparación de calentadores de paso Haceb y Bosch, detección de fugas de gas y acometidas certificadas.',
    avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=180&auto=format&fit=crop&q=80'
  },
  {
    id: 'usr_pro_4',
    name: 'Pedro Pablo Jaramillo',
    specialtyName: 'Cerrajería 24/7 y Acabados',
    categories: ['cerrajeria'],
    rate: 35000,
    rating: 0,
    reviewCount: 0,
    distanceKm: 3.4,
    zone: 'Envigado',
    status: 'pending',
    ccNumber: '1.036.711.592',
    conteReg: null,
    bio: 'Apertura de puertas residenciales y de vehículos sin rotura de cerradura, candados y cerraduras inteligentes.',
    avatar: 'https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=180&auto=format&fit=crop&q=80'
  }
];

export const INITIAL_SERVICES = [
  {
    id: 'SRV-2026-01',
    proId: 'usr_pro_1',
    proName: 'Carlos Mario Restrepo',
    proSpecialty: 'Electricista Residencial',
    description: 'Corto en la cocina al encender el microondas y olor a quemado en la caja de tacos.',
    address: 'Calle 10 # 43E-20, Apt 502, El Poblado',
    status: 'EN CAMINO',
    totalAmount: 61500,
    escrowStatus: 'En Custodia',
    isUrgent: true,
    scheduledAt: 'Hoy, Inmediato'
  }
];
