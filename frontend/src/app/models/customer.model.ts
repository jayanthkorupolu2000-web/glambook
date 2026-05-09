export interface AppointmentResponse {
  id: number;
  customerId: number;
  customerName: string;
  serviceId: number;
  serviceName: string;
  serviceCategory: string;
  servicePrice: number;
  professionalId: number;
  professionalName: string;
  professionalPhotoUrl: string;
  salonOwnerName: string;
  salonCity: string;
  scheduledAt: string;
  type: string;
  status: 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED';
  travelFee: number;
  homeAddress: string;
  totalAmount: number;
  groupBookingId: number;
  rebookedFromId: number;
  payments: PaymentResponse[];
  canReview: boolean;
  canRebook: boolean;
  canCancel: boolean;
}

export interface PaymentResponse {
  id: number;
  appointmentId: number;
  amount: number;
  method: string;
  paymentType: string;
  status: string;
  transactionId: string;
  receiptUrl: string;
  paidAt: string;
}

export interface ConsultationResponse {
  id: number;
  customerId: number;
  professionalId: number;
  professionalName: string;
  type: string;
  status: string;
  customerNotes: string;
  professionalNotes: string;
  treatmentPlan: string;
  aftercareInstructions: string;
  followUpDate: string;
  createdAt: string;
}

export interface ProductResponse {
  id: number;
  name: string;
  brand: string;
  category: string;
  description: string;
  ingredients: string;
  usageTips: string;
  price: number;
  stock: number;
  imageUrl: string;
  recommendedFor: string;
}

export interface OrderResponse {
  id: number;
  customerId: number;
  items: OrderItemResponse[];
  totalAmount: number;
  paymentMethod: string;
  paymentStatus: string;
  deliveryStatus: string;
  deliveryAddress: string;
  estimatedDelivery: string;
  createdAt: string;
}

export interface OrderItemResponse {
  id: number;
  productId: number;
  productName: string;
  brand: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface CustomerNotificationResponse {
  id: number;
  type: string;
  referenceId: number;
  message: string;
  isRead: boolean;
  createdAt: string;
}

export interface CustomerDashboardDTO {
  customerName: string;
  profilePhotoUrl: string;
  cityName: string;
  memberSince: string;
  upcomingAppointments: AppointmentResponse[];
  pendingAppointments: AppointmentResponse[];
  pendingReviews: AppointmentResponse[];
  totalLoyaltyPoints: number;
  unreadNotificationCount: number;
  pendingOrderCount: number;
  beautyProfileComplete: boolean;
  latestGlobalPolicy: { title: string; content: string; createdAt: string } | null;
}

export interface ProfessionalSearchResult {
  professionalId: number;
  name: string;
  cityName: string;
  specialization: string;
  profilePhotoUrl: string;
  averageRating: number;
  totalReviews: number;
  isAvailableSalon: boolean;
  isAvailableHome: boolean;
  travelRadiusKm: number;
  activeServices: any[];
  featuredPortfolio: any[];
  availableSlots: any[];
  activePromotions: any[];
}

// Service category tree for filtering
export const SERVICE_CATEGORIES = {
  MEN: ['Hair', 'Beard', 'Skin', 'Packages'],
  WOMEN: ['Hair', 'Skin', 'Nails', 'Makeup', 'Body'],
  KIDS: ['Hair', 'Grooming', 'Special']
};
