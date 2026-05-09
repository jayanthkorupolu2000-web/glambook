import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../../services/auth.service';

const API = 'http://localhost:8080/api';

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
    const id = this.auth.getUserId();
    if (!id) return;
    this.loading = true;
    this.http.get<any[]>(`${API}/customers/${id}/orders`).subscribe({
      next: data => { this.orders = Array.isArray(data) ? data : []; this.loading = false; },
      error: () => { this.orders = []; this.loading = false; }
    });
  }

  statusBadge(s: string): string {
    const m: Record<string, string> = {
      PROCESSING: 'warning text-dark', SHIPPED: 'info text-dark',
      DELIVERED: 'success', CANCELLED: 'danger'
    };
    return `badge bg-${m[s] ?? 'secondary'}`;
  }
}
