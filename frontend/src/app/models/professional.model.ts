export interface ProfessionalProfileResponse {
  id: number;
  name: string;
  email: string;
  city: string;
  specialization: string;
  experienceYears: number;
  certifications: string;
  trainingDetails: string;
  serviceAreas: string;
  travelRadiusKm: number;
  bio: string;
  instagramHandle: string;
  isAvailableHome: boolean;
  isAvailableSalon: boolean;
  responseTimeHrs: number;
  profilePhotoUrl: string;
  status: string;
  salonOwnerName: string;
  approvedAt: string;
  averageRating: number;
  totalReviews: number;
  featuredPortfolioItems: PortfolioResponse[];
}

export interface PortfolioResponse {
  id: number;
  professionalId: number;
  serviceId: number;
  serviceTag: string;
  mediaType: 'BEFORE_AFTER_PHOTO' | 'VIDEO_CLIP' | 'SINGLE_PHOTO';
  beforePhotoUrl: string;
  afterPhotoUrl: string;
  photoUrl: string;
  videoUrl: string;
  videoThumbnail: string;
  caption: string;
  testimonial: string;
  isFeatured: boolean;
  createdAt: string;
}

export interface ReviewWithResponse {
  id: number;
  customerId: number;
  customerName: string;
  professionalId: number;
  appointmentId: number;
  rating: number;
  comment: string;
  photos: string[];
  status: string;
  createdAt: string;
  updatedAt: string;
  professionalResponse: string;
  professionalResponseAt: string;
}
export interface AvailabilityResponse {
  id: number;
  professionalId: number;
  availDate: string;
  startTime: string;
  endTime: string;
  isBooked: boolean;
}

export interface ProfessionalNotificationResponse {
  id: number;
  type: string;
  referenceId: number;
  message: string;
  isRead: boolean;
  createdAt: string;
}

export interface ServicePopularityResponse {
  serviceId: number;
  serviceName: string;
  bookingCount: number;
  totalRevenue: number;
}

export interface ProfessionalAnalyticsResponse {
  professionalId: number;
  totalAppointments: number;
  completedAppointments: number;
  cancelledAppointments: number;
  totalEarnings: number;
  averageRating: number;
  totalReviews: number;
  clientRetentionRate: number;
  popularServices: ServicePopularityResponse[];
  peakBookingDay: string;
  peakBookingHour: number;
  monthlyEarnings: Record<string, number>;
  reportGeneratedAt: string;
}
