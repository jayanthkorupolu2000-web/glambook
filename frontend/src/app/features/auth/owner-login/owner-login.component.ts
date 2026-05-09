import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-owner-login',
  templateUrl: './owner-login.component.html'
})
export class OwnerLoginComponent {
  form: FormGroup;
  errorMessage = '';
  loading = false;

  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });
  }

  get f() { return this.form.controls; }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.errorMessage = '';
    this.auth.loginOwner(this.form.value).subscribe({
      next: () => this.router.navigate(['/dashboard/owner']),
      error: err => {
        this.errorMessage = err?.error?.message || 'Invalid email or password.';
        this.loading = false;
      }
    });
  }
}
