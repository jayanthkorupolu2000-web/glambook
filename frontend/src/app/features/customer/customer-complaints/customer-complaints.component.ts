import { HttpClient } from '@angular/common/http';
import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ComplaintResponse } from '../../../models/complaint.model';
import { AppointmentResponse } from '../../../models/customer.model';
import { AuthService } from '../../../services/auth.service';
import { CustomerAppointmentService } from '../../../services/customer-appointment.service';

const BASE = 'http://localhost:8080/api/v1';

@Component({
  selector: 'app-customer-complaints',
  templateUrl: './customer-complaints.component.html',
  styleUrls: ['./customer-complaints.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class CustomerComplaintsComponent implements OnInit {
  form: FormGroup;
  complaints: ComplaintResponse[] = [];
  completedAppointments: AppointmentResponse[] = [];
  loading = false;
  submitting = false;
  error = '';
  success = '';
  customerId = 0;
  showForm = false;

  feedbackOptions = [
    { value: 'POOR',    label: '😞 Poor' },
    { value: 'AVERAGE', label: '😐 Average' },
    { value: 'GOOD',    label: '🙂 Good' },
    { value: 'BETTER',  label: '😊 Better' }
  ];

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private apptService: CustomerAppointmentService,
    private http: HttpClient
  ) {
    this.form = this.fb.group({
      professionalId: [null, Validators.required],
      description:    ['', [Validators.required, Validators.minLength(20), Validators.maxLength(1000)]],
      feedback:       ['POOR', Validators.required],
      rating:         [1, [Validators.required, Validators.min(1), Validators.max(5)]]
    });
  }

  ngOnInit(): void {
    this.customerId = this.auth.getUserId() || 0;
    this.loadComplaints();
    this.loadCompletedAppointments();
  }

  loadComplaints(): void {
    this.loading = true;
    this.http.get<ComplaintResponse[]>(`${BASE}/customers/${this.customerId}/complaints`).subscribe({
      next: data => { this.complaints = data; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  loadCompletedAppointments(): void {
    this.apptService.getHistory(this.customerId).subscribe({
      next: data => {
        // Only completed appointments can be complained about
        const completed = data.filter(a => a.status === 'COMPLETED');
        // Deduplicate by professionalId
        const seen = new Set<number>();
        this.completedAppointments = completed.filter(a => {
          if (seen.has(a.professionalId)) return false;
          seen.add(a.professionalId);
          return true;
        });
      },
      error: () => {}
    });
  }

  setRating(r: number): void {
    this.form.patchValue({ rating: r });
  }

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.submitting = true;
    this.error = '';

    const body = {
      customerId:     this.customerId,
      professionalId: Number(this.form.value.professionalId),
      description:    this.form.value.description,
      feedback:       this.form.value.feedback,
      rating:         this.form.value.rating
    };

    this.http.post<ComplaintResponse>(`${BASE}/complaints`, body).subscribe({
      next: () => {
        this.success = 'Your complaint has been submitted. Our admin team will review it shortly.';
        this.form.reset({ feedback: 'POOR', rating: 1 });
        this.showForm = false;
        this.loadComplaints();
        setTimeout(() => this.success = '', 5000);
        this.submitting = false;
      },
      error: err => {
        this.error = err?.error?.message || 'Failed to submit complaint. Please try again.';
        this.submitting = false;
      }
    });
  }

  statusColor(status: string): string {
    const m: Record<string, string> = {
      OPEN: '#c77c00', FORWARDED: '#1565c0', RESOLVED: '#16a34a'
    };
    return m[status] ?? '#64748b';
  }

  statusBg(status: string): string {
    const m: Record<string, string> = {
      OPEN: '#fff8e1', FORWARDED: '#e3f2fd', RESOLVED: '#dcfce7'
    };
    return m[status] ?? '#f1f5f9';
  }

  stars(n: number): number[] { return [1, 2, 3, 4, 5]; }

  get f() { return this.form.controls; }
}
