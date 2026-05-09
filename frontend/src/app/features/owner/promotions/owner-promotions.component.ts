import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { PromotionResponse } from '../../../models/owner.model';
import { AuthService } from '../../../services/auth.service';
import { OwnerIdService } from '../../../services/owner-id.service';
import { OwnerPromotionService } from '../../../services/owner-promotion.service';

@Component({
  selector: 'app-owner-promotions',
  templateUrl: './owner-promotions.component.html'
})
export class OwnerPromotionsComponent implements OnInit {
  form: FormGroup;
  promotions: PromotionResponse[] = [];
  loading = false;
  submitting = false;
  error = '';
  success = '';
  ownerId = 0;

  constructor(
    private fb: FormBuilder,
    private promotionService: OwnerPromotionService,
    private auth: AuthService,
    private ownerIdService: OwnerIdService
  ) {
    this.form = this.fb.group({
      title: ['', Validators.required],
      description: [''],
      discountPct: [null, [Validators.required, Validators.min(1), Validators.max(100)]],
      startDate: ['', Validators.required],
      endDate: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.ownerIdService.getOwnerId().subscribe(id => {
      this.ownerId = id;
      this.load();
    });
  }

  load(): void {
    this.loading = true;
    this.promotionService.getPromotions(this.ownerId).subscribe({
      next: data => { this.promotions = data; this.loading = false; },
      error: () => { this.error = 'Failed to load promotions.'; this.loading = false; }
    });
  }

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const v = this.form.value;
    if (new Date(v.endDate) < new Date(v.startDate)) {
      this.error = 'End date must be on or after start date.';
      return;
    }
    this.submitting = true;
    this.error = '';
    // Ensure discountPct is a number
    const payload = { ...v, discountPct: Number(v.discountPct) };
    this.promotionService.createPromotion(this.ownerId, payload).subscribe({
      next: () => {
        this.success = 'Promotion created!';
        this.form.reset();
        this.load();
        this.submitting = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to create promotion.';
        this.submitting = false;
      }
    });
  }

  isExpired(endDate: string): boolean {
    return new Date(endDate) < new Date();
  }

  get f() { return this.form.controls; }
}
