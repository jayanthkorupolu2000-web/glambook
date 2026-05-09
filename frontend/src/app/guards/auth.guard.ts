import { Injectable } from '@angular/core';
import {
    ActivatedRouteSnapshot,
    CanActivate,
    Router,
    RouterStateSnapshot,
    UrlTree
} from '@angular/router';
import { Role } from '../models';
import { AuthService } from '../services/auth.service';

const ROLE_DASHBOARD: Record<Role, string> = {
  CUSTOMER: '/dashboard/customer/browse',
  SALON_OWNER: '/dashboard/owner',
  ADMIN: '/dashboard/admin',
  PROFESSIONAL: '/dashboard/professional'
};

@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  constructor(private authService: AuthService, private router: Router) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): boolean | UrlTree {
    if (!this.authService.isLoggedIn()) {
      return this.router.createUrlTree(['/auth']);
    }

    const requiredRoles: Role[] | undefined = route.data['roles'];
    if (!requiredRoles || requiredRoles.length === 0) {
      return true;
    }

    const userRole = this.authService.getRole();
    if (userRole && requiredRoles.includes(userRole)) {
      return true;
    }

    // Authenticated but wrong role — redirect to their own dashboard
    if (userRole && ROLE_DASHBOARD[userRole]) {
      const target = ROLE_DASHBOARD[userRole];
      if (state.url.startsWith(target)) {
        return true;
      }
      return this.router.createUrlTree([target]);
    }

    return this.router.createUrlTree(['/auth']);
  }
}
