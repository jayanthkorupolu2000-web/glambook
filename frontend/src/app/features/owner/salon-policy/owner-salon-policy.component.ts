import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { SalonPolicyResponse } from '../../../models/owner.model';
import { AuthService } from '../../../services/auth.service';
import { OwnerIdService } from '../../../services/owner-id.service';
import { OwnerSalonPolicyService } from '../../../services/owner-salon-policy.service';

@Component({
  selector: 'app-owner-salon-policy',
  templateUrl: './owner-salon-policy.component.html'
})
export class OwnerSalonPolicyComponent implements OnInit {
  form: FormGroup;
  policies: SalonPolicyResponse[] = [];
  latestPolicy: SalonPolicyResponse | null = null;
  loading = false;
  submitting = false;
  success = '';
  error = '';
  ownerId = 0;

  constructor(
    private fb: FormBuilder,
    private policyService: OwnerSalonPolicyService,
    private auth: AuthService,
    private ownerIdService: OwnerIdService
  ) {
    this.form = this.fb.group({
      title: ['', Validators.required],
      content: ['', [Validators.required, Validators.minLength(20)]]
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
    this.policyService.getPolicies(this.ownerId).subscribe({
      next: data => {
        this.policies = data.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        this.latestPolicy = this.policies[0] || null;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  publish(): void {
    if (this.form.invalid) return;
    this.submitting = true;
    this.error = '';
    this.success = '';
    this.policyService.publishPolicy(this.ownerId, this.form.value).subscribe({
      next: () => {
        this.success = '✅ Policy published! All mapped professionals have been notified.';
        this.form.reset();
        this.load();
        this.submitting = false;
        setTimeout(() => this.success = '', 5000);
      },
      error: (e) => {
        this.error = e?.error?.message || 'Failed to publish policy. Please try again.';
        this.submitting = false;
      }
    });
  }

  get f() { return this.form.controls; }
}
