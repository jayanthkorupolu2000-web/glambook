import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { Role } from '../models';
import { AuthService } from '../services/auth.service';

const ROLE_DASHBOARD: Record<Role, string> = {
  CUSTOMER: '/dashboard/customer/browse',
  SALON_OWNER: '/dashboard/owner',
  ADMIN: '/dashboard/admin',
  PROFESSIONAL: '/dashboard/professional'
};

@Injectable({ providedIn: 'root' })
export class NoAuthGuard implements CanActivate {
  constructor(private auth: AuthService, private router: Router) {}

  canActivate(): boolean | UrlTree {
    if (this.auth.isLoggedIn()) {
      const role = this.auth.getRole();
      if (role && ROLE_DASHBOARD[role]) {
        return this.router.createUrlTree([ROLE_DASHBOARD[role]]);
      }
    }
    return true;
  }
}
