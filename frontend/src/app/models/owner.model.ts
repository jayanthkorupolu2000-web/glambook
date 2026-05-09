export interface ResourceAvailabilityResponse {
  id: number;
  resourceId: number;
  availDate: string;
  startTime: string;
  endTime: string;
  isBooked: boolean;
}

export interface ResourceResponse {
  id: number;
  ownerId: number;
  type: string;
  name: string;
  description: string;
  isAvailable: boolean;
  availabilitySlots: ResourceAvailabilityResponse[];
}

export interface PromotionResponse {
  id: number;
  ownerId: number;
  title: string;
  description: string;
  discountPct: number;
  startDate: string;
  endDate: string;
  isActive: boolean;
}

export interface LoyaltyResponse {
  id: number;
  customerId: number;
  customerName: string;
  ownerId: number;
  points: number;
  tier: 'BRONZE' | 'SILVER' | 'GOLD';
}

export interface SalonPolicyResponse {
  id: number;
  ownerId: number;
  ownerName: string;
  title: string;
  content: string;
  createdAt: string;
}

export interface OwnerNotificationResponse {
  id: number;
  type: string;
  referenceId: number;
  message: string;
  isRead: boolean;
  createdAt: string;
}

export interface OwnerReportResponse {
  ownerId: number;
  totalAppointments: number;
  completedAppointments: number;
  cancelledAppointments: number;
  totalRevenue: number;
  averageRating: number;
  totalComplaints: number;
  openComplaints: number;
  forwardedComplaints: number;
  resolvedComplaints: number;
  professionalsCount: number;
  pendingApprovals: number;
  reportGeneratedAt: string;
}
