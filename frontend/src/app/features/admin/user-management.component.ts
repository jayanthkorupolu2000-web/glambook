import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AdminService, AdminUsersResponse, SalonOwnerEditResponse } from '../../services/admin.service';

interface UserTableRow {
  id: number;
  name: string;
  email: string;
  phone?: string;
  city: string;
  role: 'CUSTOMER' | 'SALON_OWNER' | 'PROFESSIONAL';
  additionalInfo?: string;
}

@Component({
  selector: 'app-user-management',
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class UserManagementComponent implements OnInit {
  users: UserTableRow[] = [];
  loading = false;
  error: string | null = null;

  // Edit modal state
  showEditModal = false;
  editTarget: UserTableRow | null = null;
  editForm!: FormGroup;
  editLoading = false;
  editError: string | null = null;
  editSuccess: string | null = null;

  constructor(
    private adminService: AdminService,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.loadUsers();
    this.editForm = this.fb.group({
      name:      ['', [Validators.required, Validators.minLength(2)]],
      phone:     ['', [Validators.required, Validators.pattern(/^[6-9]\d{9}$/)]],
      salonName: ['', [Validators.required, Validators.minLength(2)]]
    });
  }

  loadUsers(): void {
    this.loading = true;
    this.error = null;
    this.adminService.getAllUsers().subscribe({
      next: (response: AdminUsersResponse) => {
        this.users = this.transformUsersData(response);
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load users. Please try again.';
        this.loading = false;
      }
    });
  }

  // ── Edit modal ──────────────────────────────────────────────────────────────

  openEditModal(user: UserTableRow): void {
    this.editTarget = user;
    this.editError = null;
    this.editSuccess = null;
    this.editForm.reset({ name: user.name, phone: user.phone ?? '', salonName: user.additionalInfo ?? '' });
    this.showEditModal = true;
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.editTarget = null;
    this.editForm.reset();
  }

  saveEdit(): void {
    if (this.editForm.invalid || !this.editTarget) return;

    this.editLoading = true;
    this.editError = null;
    this.editSuccess = null;

    const { name, phone, salonName } = this.editForm.value;

    this.adminService.editSalonOwner(this.editTarget.id, { name, phone, salonName }).subscribe({
      next: (updated: SalonOwnerEditResponse) => {
        const idx = this.users.findIndex(u => u.id === updated.id);
        if (idx !== -1) {
          this.users[idx] = { ...this.users[idx], name: updated.name, phone: updated.phone };
        }
        this.editLoading = false;
        this.editSuccess = 'Salon Owner updated successfully.';
        setTimeout(() => this.closeEditModal(), 1200);
      },
      error: (err) => {
        console.error('Edit salon owner error:', err);
        const msg = err?.error?.message || err?.message || `Error ${err?.status}: ${err?.statusText}`;
        this.editError = msg;
        this.editLoading = false;
      }
    });
  }

  // ── Helpers ─────────────────────────────────────────────────────────────────

  private transformUsersData(response: AdminUsersResponse): UserTableRow[] {
    const users: UserTableRow[] = [];

    response.customers.forEach(c => users.push({
      id: c.id, name: c.name, email: c.email,
      phone: c.phone, city: c.city, role: 'CUSTOMER'
    }));

    response.owners.forEach(o => users.push({
      id: o.id, name: o.name, email: o.email,
      phone: o.phone, city: o.city, role: 'SALON_OWNER',
      additionalInfo: o.salonName
    }));

    response.professionals.forEach(p => users.push({
      id: p.id, name: p.name, email: p.email,
      city: p.city, role: 'PROFESSIONAL',
      additionalInfo: p.specialization
    }));

    return users.sort((a, b) => a.name.localeCompare(b.name));
  }

  getRoleBadgeClass(role: string): string {
    const map: Record<string, string> = {
      CUSTOMER: 'badge bg-primary',
      SALON_OWNER: 'badge bg-success',
      PROFESSIONAL: 'badge bg-info'
    };
    return map[role] ?? 'badge bg-secondary';
  }

  getRoleDisplayName(role: string): string {
    const map: Record<string, string> = {
      CUSTOMER: 'Customer',
      SALON_OWNER: 'Salon Owner',
      PROFESSIONAL: 'Professional'
    };
    return map[role] ?? role;
  }

  trackByUserId(_: number, user: UserTableRow): number {
    return user.id;
  }

  countByRole(role: string): number {
    return this.users.filter(u => u.role === role).length;
  }

  // Convenience getters for template
  get nameCtrl()      { return this.editForm.get('name')!; }
  get phoneCtrl()     { return this.editForm.get('phone')!; }
  get salonNameCtrl() { return this.editForm.get('salonName')!; }
}
