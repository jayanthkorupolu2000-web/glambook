import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { AdminService } from '../../../services/admin.service';

// Extended interfaces with status field
interface CustomerWithStatus {
  id: number;
  name: string;
  email: string;
  city: string;
  status: string;
  cancelCount: number;
}

interface ProfessionalWithStatus {
  id: number;
  name: string;
  email: string;
  city: string;
  specialization: string;
  status: string;
}

@Component({
  selector: 'app-admin-user-management',
  templateUrl: './admin-user-management.component.html',
  styleUrls: ['./admin-user-management.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class AdminUserManagementComponent implements OnInit {
  activeTab: 'customers' | 'professionals' = 'customers';
  customers: CustomerWithStatus[] = [];
  professionals: ProfessionalWithStatus[] = [];
  loading = false;
  error = '';

  // Confirm modal state
  pendingId = 0;
  pendingUserType = '';
  pendingNewStatus = '';
  pendingName = '';
  suspendReason = '';
  reasonSubmitted = false;

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.adminService.getAllUsers().subscribe({
      next: data => {
        this.customers = data.customers.map(c => ({
          id: c.id,
          name: c.name,
          email: c.email,
          city: c.city,
          status: (c as any).status || 'ACTIVE',
          cancelCount: (c as any).cancelCount || 0
        }));
        this.professionals = data.professionals.map(p => ({
          id: p.id,
          name: p.name,
          email: p.email,
          city: p.city,
          specialization: p.specialization,
          status: (p as any).status || 'ACTIVE'
        }));
        this.loading = false;
      },
      error: () => { this.error = 'Failed to load users.'; this.loading = false; }
    });
  }

  openConfirm(id: number, userType: string, currentStatus: string, name: string): void {
    this.pendingId = id;
    this.pendingUserType = userType;
    this.pendingNewStatus = currentStatus === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    this.pendingName = name;
    this.suspendReason = '';
    this.reasonSubmitted = false;
  }

  executeAction(): void {
    if (this.pendingNewStatus === 'SUSPENDED') {
      this.reasonSubmitted = true;
      if (!this.suspendReason.trim()) return;
    }
    this.adminService.updateUserStatus(this.pendingId, this.pendingUserType, this.pendingNewStatus).subscribe({
      next: () => {
        // Reload from server to get the persisted status
        this.loadUsers();
      },
      error: () => alert('Failed to update status.')
    });
  }

  statusBadge(status: string): string {
    return status === 'ACTIVE' ? 'badge bg-success' : 'badge bg-danger';
  }

  actionLabel(status: string): string {
    return status === 'ACTIVE' ? 'Suspend' : 'Reactivate';
  }

  actionClass(status: string): string {
    return status === 'ACTIVE' ? 'btn btn-sm btn-danger' : 'btn btn-sm btn-success';
  }

  get activeCount(): number {
    return [...this.customers, ...this.professionals].filter(u => u.status === 'ACTIVE').length;
  }

  get suspendedCount(): number {
    return [...this.customers, ...this.professionals].filter(u => u.status !== 'ACTIVE').length;
  }
}
