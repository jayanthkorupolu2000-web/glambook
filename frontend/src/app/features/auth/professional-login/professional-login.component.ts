import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-professional-login',
  templateUrl: './professional-login.component.html'
})
export class ProfessionalLoginComponent {
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
    this.auth.loginProfessional(this.form.value).subscribe({
      next: () => this.router.navigate(['/dashboard/professional']),
      error: err => {
        this.errorMessage = err?.error?.message || 'Invalid email or password.';
        this.loading = false;
      }
    });
  }
}
