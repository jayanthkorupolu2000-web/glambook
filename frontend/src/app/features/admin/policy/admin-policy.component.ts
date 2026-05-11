import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { PolicyResponse } from '../../../models/complaint.model';
import { AdminService } from '../../../services/admin.service';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-admin-policy',
  templateUrl: './admin-policy.component.html',
  styleUrls: ['./admin-policy.component.scss']
})
export class AdminPolicyComponent implements OnInit {
  form: FormGroup;
  policies: PolicyResponse[] = [];
  latestPolicy: PolicyResponse | null = null;
  loading = false;
  submitting = false;
  error = '';
  success = '';

  constructor(
    private fb: FormBuilder,
    private adminService: AdminService,
    private auth: AuthService
  ) {
    this.form = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(255)]],
      content: ['', [Validators.required, Validators.minLength(50)]]
    });
  }

  ngOnInit(): void {
    this.loadPolicies();
  }

  loadPolicies(): void {
    this.loading = true;
    this.adminService.getAllPolicies().subscribe({
      next: data => {
        this.policies = data.sort((a, b) =>
          new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        this.latestPolicy = this.policies[0] || null;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  publish(): void {
    if (this.form.invalid) return;
    const adminId = this.auth.getUserId();
    if (!adminId) return;

    this.submitting = true;
    this.error = '';
    this.success = '';

    this.adminService.publishPolicy(adminId, this.form.value).subscribe({
      next: () => {
        this.success = 'Policy published successfully!';
        this.form.reset();
        this.loadPolicies();
        this.submitting = false;
      },
      error: () => {
        this.error = 'Failed to publish policy.';
        this.submitting = false;
      }
    });
  }

  get f() { return this.form.controls; }
}
