import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { VALID_CITIES, cityValidator } from '../../../validators/city.validator';
import { emailDomainValidator } from '../../../validators/email-domain.validator';
import { fullNameValidator } from '../../../validators/full-name.validator';
import { passwordStrengthValidator } from '../../../validators/password-strength.validator';

@Component({
  selector: 'app-customer-register',
  templateUrl: './customer-register.component.html'
})
export class CustomerRegisterComponent {
  form: FormGroup;
  cities = VALID_CITIES;
  errorMessage = '';
  loading = false;

  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router) {
    this.form = this.fb.group({
      name: ['', [Validators.required, fullNameValidator()]],
      email: ['', [Validators.required, Validators.email, emailDomainValidator()]],
      password: ['', [Validators.required, Validators.minLength(8), passwordStrengthValidator()]],
      city: ['', [Validators.required, cityValidator()]]
    });
  }

  get f() { return this.form.controls; }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.errorMessage = '';
    this.auth.registerCustomer(this.form.value).subscribe({
      next: () => this.router.navigate(['/dashboard/customer']),
      error: err => {
        this.errorMessage = err?.error?.message || 'Registration failed. Please try again.';
        this.loading = false;
      }
    });
  }
}
