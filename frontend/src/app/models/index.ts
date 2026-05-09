export type Role = 'ADMIN' | 'SALON_OWNER' | 'PROFESSIONAL' | 'CUSTOMER';

export type City = 'Visakhapatnam' | 'Vijayawada' | 'Hyderabad' | 'Ananthapur' | 'Khammam';

export type ServiceCategory = 'Hair' | 'Beard' | 'Skin' | 'Nails' | 'Makeup' | 'Body' | 'Grooming' | 'Packages' | 'Special';

export interface AuthResponse {
  token: string;
  role: Role;
  userId: number;
  name: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface CustomerRegisterRequest {
  name: string;
  email: string;
  password: string;
  city: City;
}

export interface ProfessionalRegisterRequest {
  name: string;
  email: string;
  password: string;
  city: City;
  specialization: string;
}

export interface Customer {
  id: number;
  name: string;
  email: string;
  phone: string;
  city: City;
  profilePicture?: string;
  emergencyContact?: string;
  medicalNotes?: string;
}

export interface SalonOwner {
  id: number;
  name: string;
  salonName: string;
  city: City;
  email: string;
  phone: string;
}

export interface Service {
  id: number;
  name: string;
  category: ServiceCategory;
  gender: 'MEN' | 'WOMEN' | 'KIDS';
  price: number;
  durationMins: number;
}

export interface Professional {
  id: number;
  name: string;
  email: string;
  city: City;
  specialization: string;
  salonOwner: SalonOwner;
  services: Service[];
  rating?: number;
}

export interface AppointmentRequest {
  customerId: number;
  professionalId: number;
  serviceId: number;
  dateTime: string; // ISO 8601
}

export interface Payment {
  id: number;
  appointmentId: number;
  amount: number;
  method: 'CASH' | 'CARD' | 'UPI';
  status: 'PENDING' | 'PAID' | 'REFUNDED';
  paidAt?: string;
}

export interface PaymentRequest {
  appointmentId: number;
  amount: number;
  method: 'CASH' | 'CARD' | 'UPI';
}

export interface Appointment {
  id: number;
  customer: Customer;
  professional: Professional;
  service: Service;
  dateTime: string; // ISO 8601
  status: 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED';
  payment?: Payment;
}

export interface Review {
  id: number;
  customerId: number;
  professionalId: number;
  rating: number; // 1–5
  comment: string;
  createdAt: string;
}

export interface ReviewRequest {
  professionalId: number;
  rating: number;
  comment?: string;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
