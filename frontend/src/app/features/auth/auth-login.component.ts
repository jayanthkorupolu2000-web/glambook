import { Component } from '@angular/core';

@Component({
  selector: 'app-auth-login',
  template: `
    <div class="gb-auth-page min-vh-100 d-flex align-items-center justify-content-center">

      <div class="gb-card gb-card--wide gb-card--opaque">

        <!-- Brand header -->
        <div class="gb-card__header">
          <div class="gb-brand-logo">
            <i class="fa-solid fa-scissors"></i>
          </div>
          <h1 class="gb-card__title">Login to GlamBook</h1>
          <p class="gb-card__subtitle">Your all-in-one salon management platform</p>
        </div>

        <!-- Role grid -->
        <div class="gb-role-grid">

          <a routerLink="/auth/customer/login" class="gb-role-tile gb-role-tile--customer">
            <div class="gb-role-tile__icon"><i class="fa-solid fa-user"></i></div>
            <div class="gb-role-tile__label">Customer</div>
            <div class="gb-role-tile__hint">Book appointments</div>
          </a>

          <a routerLink="/auth/owner/login" class="gb-role-tile gb-role-tile--owner">
            <div class="gb-role-tile__icon"><i class="fa-solid fa-store"></i></div>
            <div class="gb-role-tile__label">Salon Owner</div>
            <div class="gb-role-tile__hint">Manage your salon</div>
          </a>

          <a routerLink="/auth/professional/login" class="gb-role-tile gb-role-tile--professional">
            <div class="gb-role-tile__icon"><i class="fa-solid fa-cut"></i></div>
            <div class="gb-role-tile__label">Professional</div>
            <div class="gb-role-tile__hint">View your schedule</div>
          </a>

          <a routerLink="/auth/admin/login" class="gb-role-tile gb-role-tile--admin">
            <div class="gb-role-tile__icon"><i class="fa-solid fa-shield-halved"></i></div>
            <div class="gb-role-tile__label">Admin</div>
            <div class="gb-role-tile__hint">System management</div>
          </a>

        </div>

        <!-- Divider -->
        <div class="gb-divider">
          <span>New to GlamBook?</span>
        </div>

        <!-- Register links -->
        <div class="gb-register-row">
          <a routerLink="/auth/customer/register" class="gb-outline-btn gb-outline-btn--customer">
            <i class="fa-solid fa-user-plus me-2"></i>Register as Customer
          </a>
          <a routerLink="/auth/professional/register" class="gb-outline-btn gb-outline-btn--professional">
            <i class="fa-solid fa-id-badge me-2"></i>Register as Professional
          </a>
        </div>

      </div>
    </div>
  `
})
export class AuthLoginComponent {}
