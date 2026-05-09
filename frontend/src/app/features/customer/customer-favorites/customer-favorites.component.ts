import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

const API = 'http://localhost:8080/api/v1';

@Component({
  selector: 'app-customer-favorites',
  templateUrl: './customer-favorites.component.html'
})
export class CustomerFavoritesComponent implements OnInit {
  activeTab: 'products' | 'services' = 'products';

  products: any[] = [];
  services: any[] = [];
  loading = false;
  togglingProduct: Record<number, boolean> = {};
  togglingService: Record<number, boolean> = {};

  success = '';
  error = '';

  constructor(private http: HttpClient, private auth: AuthService, private router: Router) {}

  ngOnInit(): void { this.loadAll(); }

  loadAll(): void { this.loadProducts(); this.loadServices(); }

  loadProducts(): void {
    const id = this.auth.getUserId();
    if (!id) return;
    this.loading = true;
    this.http.get<any[]>(`${API}/customers/${id}/favorites/products`).subscribe({
      next: data => { this.products = Array.isArray(data) ? data : []; this.loading = false; },
      error: () => { this.products = []; this.loading = false; }
    });
  }

  loadServices(): void {
    const id = this.auth.getUserId();
    if (!id) return;
    this.http.get<any[]>(`${API}/customers/${id}/favorites/services`).subscribe({
      next: data => { this.services = Array.isArray(data) ? data : []; },
      error: () => { this.services = []; }
    });
  }

  removeFavoriteProduct(product: any): void {
    const id = this.auth.getUserId();
    if (!id) return;
    this.togglingProduct[product.id] = true;
    this.http.post<any>(`${API}/customers/${id}/favorites/products/${product.id}`, {}).subscribe({
      next: () => { this.products = this.products.filter(p => p.id !== product.id); this.togglingProduct[product.id] = false; },
      error: () => { this.togglingProduct[product.id] = false; }
    });
  }

  removeFavoriteService(service: any): void {
    const id = this.auth.getUserId();
    if (!id) return;
    this.togglingService[service.id] = true;
    this.http.post<any>(`${API}/customers/${id}/favorites/services/${service.id}`, {}).subscribe({
      next: () => { this.services = this.services.filter(s => s.id !== service.id); this.togglingService[service.id] = false; },
      error: () => { this.togglingService[service.id] = false; }
    });
  }

  /** Navigate to products page and auto-open the order modal for this product */
  orderProduct(product: any): void {
    this.router.navigate(['/dashboard/customer/products'], {
      queryParams: { order: product.id }
    });
  }

  /** Navigate to the booking page for the professional who offers this service */
  bookService(service: any): void {
    if (service.professionalId) {
      this.router.navigate(['/dashboard/customer/book', service.professionalId]);
    } else {
      this.router.navigate(['/dashboard/customer/search'], { queryParams: { q: service.name } });
    }
  }

  stars(n: number): number[] { return [1, 2, 3, 4, 5]; }
}
