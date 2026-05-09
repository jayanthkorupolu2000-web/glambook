import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-customer-dashboard',
  template: `
    <router-outlet></router-outlet>
  `
})
export class CustomerDashboardComponent {
  constructor(private auth: AuthService, private router: Router) {}
}
