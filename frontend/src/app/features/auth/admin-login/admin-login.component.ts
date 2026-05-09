import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-admin-login',
  templateUrl: './admin-login.component.html'
})
export class AdminLoginComponent {
  form: FormGroup;
  errorMessage = '';
  loading = false;

  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router) {
    this.form = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    });
  }

  get f() { return this.form.controls; }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.errorMessage = '';
    // Admin login sends username + password directly
    const credentials = { username: this.form.value.username, password: this.form.value.password };
    this.auth.loginAdmin(credentials).subscribe({
      next: () => this.router.navigate(['/dashboard/admin']),
      error: err => {
        this.errorMessage = err?.error?.message || 'Invalid credentials.';
        this.loading = false;
      }
    });
  }
}
