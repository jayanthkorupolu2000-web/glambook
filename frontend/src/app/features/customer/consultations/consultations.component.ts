import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../../services/auth.service';

const API = 'http://localhost:8080/api/v1';

@Component({
  selector: 'app-consultations',
  templateUrl: './consultations.component.html'
})
export class ConsultationsComponent implements OnInit {
  consultations: any[] = [];
  professionals: any[] = [];
  loading = false;
  showForm = false;
  submitting = false;
  form!: FormGroup;
  error = '';
  success = '';

  // Photo upload state
  selectedPhoto: File | null = null;
  photoPreview: string | null = null;
  uploadingPhoto = false;

  topics = ['HAIR', 'SKIN', 'MAKEUP', 'GENERAL'];
  types = ['VIRTUAL', 'IN_PERSON', 'GENERAL'];

  constructor(private http: HttpClient, private auth: AuthService, private fb: FormBuilder) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      professionalId: [''],
      type: ['GENERAL', Validators.required],
      topic: ['GENERAL', Validators.required],
      question: ['', [Validators.required, Validators.minLength(10)]]
    });
    this.load();
    this.loadProfessionals();
  }

  load(): void {
    const id = this.auth.getUserId();
    if (!id) return;
    this.loading = true;
    this.http.get<any[]>(`${API}/customers/${id}/consultations`).subscribe({
      next: data => { this.consultations = Array.isArray(data) ? data : []; this.loading = false; },
      error: () => { this.consultations = []; this.loading = false; }
    });
  }

  loadProfessionals(): void {
    this.http.get<any[]>('http://localhost:8080/api/professionals/all').subscribe({
      next: data => { this.professionals = Array.isArray(data) ? data : []; },
      error: () => {}
    });
  }

  onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const file = input.files[0];
    const allowed = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
    if (!allowed.includes(file.type)) {
      this.error = 'Only JPG, PNG, or WEBP images are allowed.';
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.error = 'Photo must be under 5 MB.';
      return;
    }
    this.selectedPhoto = file;
    this.error = '';
    const reader = new FileReader();
    reader.onload = e => { this.photoPreview = e.target?.result as string; };
    reader.readAsDataURL(file);
  }

  removePhoto(): void {
    this.selectedPhoto = null;
    this.photoPreview = null;
  }

  submit(): void {
    if (this.form.invalid) return;
    const id = this.auth.getUserId();
    if (!id) return;
    this.submitting = true;
    this.error = '';
    const v = this.form.value;
    const payload: any = {
      customerId: id,
      type: v.type,
      topic: v.topic,
      question: v.question
    };
    if (v.professionalId) payload.professionalId = Number(v.professionalId);

    this.http.post<any>(`${API}/customers/${id}/consultations`, payload).subscribe({
      next: (created) => {
        if (this.selectedPhoto && created?.id) {
          this.uploadPhoto(created.id);
        } else {
          this.onSubmitSuccess();
        }
      },
      error: (e) => { this.error = e?.error?.message || 'Failed to submit.'; this.submitting = false; }
    });
  }

  private uploadPhoto(consultationId: number): void {
    this.uploadingPhoto = true;
    const fd = new FormData();
    fd.append('file', this.selectedPhoto!);
    this.http.post(`${API}/consultations/${consultationId}/photo`, fd).subscribe({
      next: () => { this.uploadingPhoto = false; this.onSubmitSuccess(); },
      error: () => {
        // Consultation was created; photo upload failed — still show success
        this.uploadingPhoto = false;
        this.onSubmitSuccess();
      }
    });
  }

  private onSubmitSuccess(): void {
    this.success = 'Consultation submitted! A professional will respond shortly.';
    this.showForm = false;
    this.form.reset({ type: 'GENERAL', topic: 'GENERAL' });
    this.selectedPhoto = null;
    this.photoPreview = null;
    this.submitting = false;
    this.load();
  }

  statusBadge(s: string): string {
    const m: Record<string, string> = {
      PENDING: 'warning text-dark', RESPONDED: 'success', CLOSED: 'secondary'
    };
    return `badge bg-${m[s] ?? 'secondary'}`;
  }

  topicIcon(t: string): string {
    const m: Record<string, string> = { HAIR: '💇', SKIN: '✨', MAKEUP: '💄', GENERAL: '💬' };
    return m[t] ?? '💬';
  }

  photoUrl(url: string): string {
    return `http://localhost:8080/api/v1/files${url}`;
  }
}
