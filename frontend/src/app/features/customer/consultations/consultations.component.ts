import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../../services/auth.service';

const API = 'http://localhost:8080/api/v1';

@Component({
  selector: 'app-consultations',
  templateUrl: './consultations.component.html',
  styleUrls: ['./consultations.component.scss']
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

  constructor(private http: HttpClient, private auth: AuthService, private fb: FormBuilder) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      professionalId: [''],
      topic: ['GENERAL', Validators.required],
      question: ['', [Validators.required, Validators.minLength(3)]]
    });
    this.load();
    this.loadProfessionalsByTopic('GENERAL');

    // Reload professionals whenever topic changes
    this.form.get('topic')!.valueChanges.subscribe(topic => {
      this.form.patchValue({ professionalId: '' });
      this.loadProfessionalsByTopic(topic);
    });
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

  loadProfessionalsByTopic(topic: string): void {
    this.http.get<any[]>(`http://localhost:8080/api/v1/consultations/professionals?topic=${topic}`).subscribe({
      next: data => { this.professionals = Array.isArray(data) ? data : []; },
      error: () => { this.professionals = []; }
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
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      Object.values(this.form.controls).forEach(c => c.markAsDirty());
      return;
    }
    const id = this.auth.getUserId();
    if (!id) return;
    this.submitting = true;
    this.error = '';
    const v = this.form.value;

    const data: any = {
      customerId: id,
      topic: v.topic,
      question: v.question
    };
    if (v.professionalId) data.professionalId = Number(v.professionalId);

    // Send as multipart — same pattern as reviews
    const fd = new FormData();
    fd.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    if (this.selectedPhoto) {
      fd.append('photo', this.selectedPhoto, this.selectedPhoto.name);
    }

    this.http.post<any>(`${API}/customers/${id}/consultations`, fd).subscribe({
      next: () => { this.onSubmitSuccess(); },
      error: (e) => { this.error = e?.error?.message || 'Failed to submit.'; this.submitting = false; }
    });
  }

  private onSubmitSuccess(): void {
    this.success = 'Consultation submitted! A professional will respond shortly.';
    this.showForm = false;
    this.form.reset({ topic: 'GENERAL' });
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
    if (!url) return '';
    // If already a full URL, return as-is
    if (url.startsWith('http')) return url;
    // Path is like /api/v1/files/consultations/uuid.jpg
    return `http://localhost:8080${url}`;
  }
}
