import { Component, OnInit } from '@angular/core';
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
  templateUrl: './admin-user-management.component.html'
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
  }

  executeAction(): void {
    this.adminService.updateUserStatus(this.pendingId, this.pendingUserType, this.pendingNewStatus).subscribe({
      next: res => {
        if (this.pendingUserType === 'CUSTOMER') {
          const c = this.customers.find(x => x.id === this.pendingId);
          if (c) c.status = res.status;
        } else {
          const p = this.professionals.find(x => x.id === this.pendingId);
          if (p) p.status = res.status;
        }
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
}
