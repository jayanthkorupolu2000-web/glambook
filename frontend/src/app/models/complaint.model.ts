export interface ComplaintRequest {
  customerId: number;
  professionalId: number;
  description: string;
  feedback: 'POOR' | 'AVERAGE' | 'GOOD' | 'BETTER';
  rating: number;
}

export interface ComplaintResponse {
  id: number;
  customerId: number;
  customerName: string;
  professionalId: number;
  professionalName: string;
  description: string;
  feedback: string;
  rating: number;
  status: 'OPEN' | 'FORWARDED' | 'RESOLVED';
  resolutionNotes: string;
  createdAt: string;
}

export interface PolicyRequest {
  title: string;
  content: string;
}

export interface PolicyResponse {
  id: number;
  title: string;
  content: string;
  publishedBy: string;
  createdAt: string;
}

export interface UserStatusResponse {
  id: number;
  name: string;
  userType: string;
  status: string;
}

export interface CancellationStats {
  total: number;
  sameDay: number;
  suspendedCustomers: number;
}

export interface PaymentStats {
  total: number;
  paid: number;
  refunded: number;
  successRatio: number;
}
