import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { SalonOwner } from '../../models';
import { AdminService, SalonOwnerManagementResponse } from '../../services/admin.service';

@Component({
  selector: 'app-salon-owner-list',
  templateUrl: './salon-owner-list.component.html',
  styleUrls: ['./salon-owner-list.component.scss']
})
export class SalonOwnerListComponent implements OnInit {
  owners: SalonOwner[] = [];
  loading = false;
  error: string | null = null;

  // Edit modal state
  showEditModal = false;
  editTarget: SalonOwner | null = null;
  editForm!: FormGroup;
  editLoading = false;
  editError: string | null = null;
  editSuccess: string | null = null;

  constructor(
    private adminService: AdminService,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.loadOwners();
    this.editForm = this.fb.group({
      ownerName: ['', [Validators.required, Validators.minLength(2)]],
      salonName: ['', [Validators.required, Validators.minLength(2)]],
      phone:     ['', [Validators.required, Validators.pattern(/^[6-9]\d{9}$/)]]
    });
  }

  loadOwners(): void {
    this.loading = true;
    this.error = null;
    this.adminService.getAllOwners().subscribe({
      next: (owners: SalonOwner[]) => {
        this.owners = owners.sort((a, b) => a.city.localeCompare(b.city));
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load salon owners. Please try again.';
        this.loading = false;
      }
    });
  }

  // ── Edit modal ──────────────────────────────────────────────────────────────

  openEditModal(owner: SalonOwner): void {
    this.editTarget = owner;
    this.editError = null;
    this.editSuccess = null;
    this.editForm.reset({
      ownerName: owner.name,
      salonName: owner.salonName,
      phone:     owner.phone ?? ''
    });
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

    const { ownerName, salonName, phone } = this.editForm.value;

    this.adminService.editSalonOwnerDetails(this.editTarget.id, { ownerName, salonName, phone })
      .subscribe({
        next: (updated: SalonOwnerManagementResponse) => {
          // Update the row in-place — no full reload needed
          const idx = this.owners.findIndex(o => o.id === updated.id);
          if (idx !== -1) {
            this.owners[idx] = {
              ...this.owners[idx],
              name:      updated.ownerName,
              salonName: updated.salonName,
              phone:     updated.phone
            };
          }
          this.editLoading = false;
          this.editSuccess = 'Salon Owner updated successfully.';
          setTimeout(() => this.closeEditModal(), 1200);
        },
        error: (err) => {
          const msg = err?.error?.message || err?.message || `Error ${err?.status}`;
          this.editError = msg;
          this.editLoading = false;
        }
      });
  }

  // ── Helpers ─────────────────────────────────────────────────────────────────

  getCityBadgeClass(city: string): string {
    const map: Record<string, string> = {
      'Visakhapatnam': 'badge bg-primary',
      'Vijayawada':    'badge bg-success',
      'Hyderabad':     'badge bg-info',
      'Ananthapur':    'badge bg-warning text-dark',
      'Khammam':       'badge bg-secondary'
    };
    return map[city] ?? 'badge bg-light text-dark';
  }

  /** New: CSS class for styled city badge matching dashboard palette */
  getCityClass(city: string): string {
    const map: Record<string, string> = {
      'Visakhapatnam': 'so-city-badge--blue',
      'Vijayawada':    'so-city-badge--green',
      'Hyderabad':     'so-city-badge--teal',
      'Ananthapur':    'so-city-badge--amber',
      'Khammam':       'so-city-badge--navy'
    };
    return map[city] ?? 'so-city-badge--slate';
  }

  get uniqueCityCount(): number {
    return new Set(this.owners.map(o => o.city)).size;
  }

  trackByOwnerId(_: number, owner: SalonOwner): number {
    return owner.id;
  }

  get ownerNameCtrl() { return this.editForm.get('ownerName')!; }
  get salonNameCtrl() { return this.editForm.get('salonName')!; }
  get phoneCtrl()     { return this.editForm.get('phone')!; }
}
