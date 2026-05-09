import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Professional, Service } from '../../../models';
import { AppointmentService } from '../../../services/appointment.service';
import { AuthService } from '../../../services/auth.service';
import { ProfessionalService } from '../../../services/professional.service';
const BASE = 'http://localhost:8080';

interface Slot {
  id: number;
  startTime: string;
  endTime: string;
  isBooked: boolean;
  slotType: 'WORKING' | 'LUNCH_BREAK' | 'BREAK' | 'BLOCKED';
  slotStatus: 'AVAILABLE' | 'UNAVAILABLE' | 'COMPLETED';
  past: boolean;
  label: string;
  typeLabel: string;
}

@Component({
  selector: 'app-appointment-booking',
  templateUrl: './appointment-booking.component.html',
  styleUrls: ['./appointment-booking.component.scss']
})
export class AppointmentBookingComponent implements OnInit {
  form!: FormGroup;
  professional: Professional | null = null;
  services: Service[] = [];
  loading = false;
  submitting = false;
  error = '';
  success = '';

  selectedDate = '';
  slots: Slot[] = [];
  slotsLoading = false;
  selectedSlot: Slot | null = null;
  slotError = '';

  // Portfolio modal
  showPortfolio = false;

  // Professional reviews
  professionalReviews: any[] = [];
  reviewsLoading = false;
  averageRating = 0;
  averageQuality = 0;
  averageTimeliness = 0;
  averageProfessionalism = 0;
  previewPhoto: string | null = null;

  get minDate(): string { return new Date().toISOString().split('T')[0]; }

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private professionalService: ProfessionalService,
    private appointmentService: AppointmentService,
    private authService: AuthService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      professionalId: [null, Validators.required],
      serviceId:      [null, Validators.required]
    });
    const id = this.route.snapshot.paramMap.get('professionalId');
    if (id) this.loadProfessional(Number(id));
  }

  loadProfessional(id: number): void {
    this.loading = true;
    this.professionalService.getById(id).subscribe({
      next: pro => {
        this.professional = pro;
        this.services = pro.services || [];
        this.form.patchValue({ professionalId: pro.id });
        if (!this.services.length) {
          this.professionalService.getServices(id).subscribe({
            next: svcs => { this.services = svcs; },
            error: () => {}
          });
        }
        this.loading = false;
        this.loadProfessionalReviews(id);
      },
      error: () => { this.error = 'Failed to load professional details.'; this.loading = false; }
    });
  }

  loadProfessionalReviews(professionalId: number): void {
    this.reviewsLoading = true;
    this.http.get<any>(`${BASE}/api/reviews?professionalId=${professionalId}`).subscribe({
      next: data => {
        this.professionalReviews = data.reviews || [];
        this.averageRating = data.averageRating || 0;
        this.averageQuality = data.averageQualityRating || 0;
        this.averageTimeliness = data.averageTimelinessRating || 0;
        this.averageProfessionalism = data.averageProfessionalismRating || 0;
        this.reviewsLoading = false;
      },
      error: () => { this.reviewsLoading = false; }
    });
  }

  stars(n: number): number[] { return [1, 2, 3, 4, 5]; }

  openPhotoPreview(url: string): void { this.previewPhoto = url; }

  get f() { return this.form.controls; }

  onDateChange(): void {
    this.selectedSlot = null;
    this.slotError = '';
    if (!this.selectedDate || !this.form.value.professionalId) return;
    this.loadSlots();
  }

  onServiceChange(): void {
    this.selectedSlot = null;
    if (this.selectedDate) this.loadSlots();
  }

  loadSlots(): void {
    const profId = this.form.value.professionalId;
    if (!profId || !this.selectedDate) return;

    // Get duration from selected service
    const serviceId = this.form.value.serviceId;
    const selectedSvc = this.services.find(s => s.id == serviceId);
    const duration = selectedSvc?.durationMins || 30;

    this.slotsLoading = true;
    this.slots = [];

    this.http.get<any[]>(
      `${BASE}/api/v1/professionals/${profId}/availability/time-slots?date=${this.selectedDate}&durationMins=${duration}`
    ).subscribe({
      next: data => {
        this.slots = data
          .map((s: any) => ({
            id: s.id || 0,
            startTime: s.startTime,
            endTime: s.endTime,
            isBooked: s.booked,
            slotType: s.slotType || 'WORKING',
            slotStatus: s.slotStatus || 'AVAILABLE',
            past: !!s.past,
            label: this.formatTime(s.startTime) + ' – ' + this.formatTime(s.endTime),
            typeLabel: this.getTypeLabel(s.slotType)
          }));
        this.slotsLoading = false;
      },
      error: () => { this.slotsLoading = false; }
    });
  }

  isBookable(slot: Slot): boolean {
    return slot.slotStatus === 'AVAILABLE';
  }

  selectSlot(slot: Slot): void {
    if (!this.isBookable(slot)) {
      if (slot.slotStatus === 'COMPLETED') this.slotError = 'This slot has a completed appointment.';
      else if (slot.past) this.slotError = 'This time slot has already passed.';
      else if (slot.isBooked) this.slotError = 'This slot is already booked. Please choose another.';
      else this.slotError = `This is a ${slot.typeLabel} — not available for booking.`;
      return;
    }
    this.selectedSlot = slot;
    this.slotError = '';
  }

  slotClass(slot: Slot): string {
    if (slot === this.selectedSlot) return 'slot-selected';
    if (slot.slotStatus === 'COMPLETED')   return 'slot-completed';
    if (slot.slotStatus === 'UNAVAILABLE') return slot.past ? 'slot-past' : 'slot-booked';
    return 'slot-available';
  }

  getTypeLabel(type: string): string {
    const map: Record<string, string> = {
      WORKING:     '🕐 Working Hours',
      LUNCH_BREAK: '🍽️ Lunch Break',
      BREAK:       '☕ Short Break',
      BLOCKED:     '🚫 Blocked'
    };
    return map[type] ?? type;
  }

  formatTime(t: string): string {
    if (!t) return '';
    const [h, m] = t.split(':');
    const hour = parseInt(h);
    return `${hour % 12 || 12}:${m} ${hour >= 12 ? 'PM' : 'AM'}`;
  }

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    if (!this.selectedDate) { this.error = 'Please select a date.'; return; }
    if (!this.selectedSlot) { this.error = 'Please select a time slot.'; return; }

    const customerId = this.authService.getUserId();
    if (!customerId) { this.error = 'You must be logged in to book.'; return; }

    this.submitting = true;
    this.error = '';
    this.success = '';

    const { professionalId, serviceId } = this.form.value;
    const dateTime = `${this.selectedDate}T${this.selectedSlot.startTime}:00`;

    this.appointmentService.book({ customerId, professionalId, serviceId, dateTime }).subscribe({
      next: () => {
        this.success = 'Appointment booked successfully!';
        this.submitting = false;
        if (this.selectedSlot) this.selectedSlot.isBooked = true;
        setTimeout(() => this.router.navigate(['/dashboard/customer/appointments']), 1500);
      },
      error: (err: any) => {
        console.error('Booking error:', err);
        const msg = err?.error?.message || `Error ${err?.status}: Failed to book appointment`;
        // Show overlap errors as slot error so it appears near the slot grid
        if (msg.toLowerCase().includes('overlap') || msg.toLowerCase().includes('break')) {
          this.slotError = msg;
          this.selectedSlot = null;
        } else {
          this.error = msg;
        }
        this.submitting = false;
      }
    });
  }
}
