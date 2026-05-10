import { HttpClient } from '@angular/common/http';
import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { SalonOwner } from '../../../models';
import { AuthService } from '../../../services/auth.service';
import { OwnerIdService } from '../../../services/owner-id.service';

const API_BASE = 'http://localhost:8080';

@Component({
  selector: 'app-owner-profile',
  templateUrl: './owner-profile.component.html',
  styleUrls: ['./owner-profile.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class OwnerProfileComponent implements OnInit {
  profile: SalonOwner | null = null;
  loading = false;
  error: string | null = null;
  successMsg = '';
  ownerId = 0;

  // Edit state
  editMode = false;
  editName = '';
  editSalonName = '';
  editPhone = '';
  editError = '';
  saving = false;
  submitted = false;

  constructor(
    private http: HttpClient,
    private ownerIdService: OwnerIdService,
    private auth: AuthService
  ) {}

  ngOnInit(): void {
    this.ownerIdService.getOwnerId().subscribe(id => {
      if (!id) { this.error = 'Could not determine owner ID.'; return; }
      this.ownerId = id;
      this.loadProfile(id);
    });
  }

  loadProfile(id: number): void {
    this.loading = true;
    this.http.get<SalonOwner>(`${API_BASE}/api/owners/${id}/profile`).subscribe({
      next: data => { this.profile = data; this.loading = false; },
      error: () => { this.error = 'Failed to load profile.'; this.loading = false; }
    });
  }

  startEdit(): void {
    if (!this.profile) return;
    this.editName      = this.profile.name      || '';
    this.editSalonName = this.profile.salonName  || '';
    this.editPhone     = this.profile.phone      || '';
    this.editError     = '';
    this.submitted     = false;
    this.editMode      = true;
  }

  cancelEdit(): void {
    this.editMode  = false;
    this.editError = '';
    this.submitted = false;
  }

  isValidPhone(phone: string): boolean {
    return /^[6-9]\d{9}$/.test(phone);
  }

  saveProfile(): void {
    this.submitted = true;
    this.editError = '';

    if (!this.editName.trim())      { this.editError = 'Name is required.'; return; }
    if (!this.editSalonName.trim()) { this.editError = 'Salon name is required.'; return; }
    if (!this.isValidPhone(this.editPhone)) {
      this.editError = 'Enter a valid 10-digit Indian mobile number.';
      return;
    }

    this.saving = true;
    const body = {
      name:      this.editName.trim(),
      salonName: this.editSalonName.trim(),
      phone:     this.editPhone.trim()
    };

    this.http.patch<SalonOwner>(`${API_BASE}/api/owners/${this.ownerId}/profile`, body).subscribe({
      next: updated => {
        this.profile   = updated;
        this.saving    = false;
        this.editMode  = false;
        this.submitted = false;
        // Update the cached name in auth service so sidebar reflects new name
        localStorage.setItem('auth_user_name', updated.name);
        this.showSuccess('Profile updated successfully.');
      },
      error: err => {
        this.editError = err?.error?.message || 'Failed to save profile. Please try again.';
        this.saving    = false;
      }
    });
  }

  private showSuccess(msg: string): void {
    this.successMsg = msg;
    setTimeout(() => this.successMsg = '', 4000);
  }
}
