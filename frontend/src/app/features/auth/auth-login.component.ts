import { Component } from '@angular/core';

@Component({
  selector: 'app-auth-login',
  template: `
    <div class="min-vh-100 d-flex align-items-center justify-content-center" style="background:#f8f9fa;">
      <div class="card border-0 shadow-sm p-4" style="width:100%;max-width:420px;">
        <div class="text-center mb-4">
          <h2 class="fw-bold" style="color:#244AFD;">✂ Glambook</h2>
          <p class="text-muted">Salon Management Platform</p>
        </div>
        <div class="d-grid gap-3">
          <a routerLink="/auth/customer/login" class="btn btn-lg text-white fw-semibold" style="background:#244AFD;">
            Login as Customer
          </a>
          <a routerLink="/auth/owner/login" class="btn btn-lg btn-outline-success fw-semibold">
            Login as Salon Owner
          </a>
          <a routerLink="/auth/admin/login" class="btn btn-lg btn-outline-secondary fw-semibold">
            Login as Admin
          </a>
          <a routerLink="/auth/professional/login" class="btn btn-lg btn-outline-warning fw-semibold">
            Login as Professional
          </a>
          <a routerLink="/auth/professional/register" class="btn btn-lg btn-outline-info fw-semibold">
            Register as Professional
          </a>
        </div>
        <hr class="my-3">
        <p class="text-center text-muted mb-0">
          New customer?
          <a routerLink="/auth/customer/register" style="color:#244AFD;">Register here</a>
        </p>
      </div>
    </div>
  `
})
export class AuthLoginComponent {}
