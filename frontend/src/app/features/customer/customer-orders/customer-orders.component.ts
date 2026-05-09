import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../../services/auth.service';

const BASE = 'http://localhost:8080';

@Component({
  selector: 'app-customer-orders',
  templateUrl: './customer-orders.component.html'
})
export class CustomerOrdersComponent implements OnInit {
  orders: any[] = [];
  loading = false;

  constructor(private http: HttpClient, private auth: AuthService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.http.get<any[]>(`${BASE}/api/products/orders`).subscribe({
      next: data => { this.orders = Array.isArray(data) ? data : []; this.loading = false; },
      error: () => { this.orders = []; this.loading = false; }
    });
  }

  statusBadge(s: string): string {
    const m: Record<string, string> = {
      PLACED:     'primary',
      CONFIRMED:  'warning text-dark',
      SHIPPED:    'info text-dark',
      DELIVERED:  'success',
      CANCELLED:  'danger'
    };
    return `badge bg-${m[s] ?? 'secondary'}`;
  }
}
