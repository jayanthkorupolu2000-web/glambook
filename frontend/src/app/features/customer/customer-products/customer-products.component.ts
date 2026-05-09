import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../../services/auth.service';

const API = 'http://localhost:8080/api';
const API_V1 = 'http://localhost:8080/api/v1';

@Component({
  selector: 'app-customer-products',
  templateUrl: './customer-products.component.html'
})
export class CustomerProductsComponent implements OnInit {
  products: any[] = [];
  loading = false;
  addingToCart: Record<number, boolean> = {};
  togglingFav: Record<number, boolean> = {};
  success = '';

  constructor(private http: HttpClient, private auth: AuthService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    const id = this.auth.getUserId();
    this.loading = true;
    const url = id ? `${API_V1}/products?customerId=${id}` : `${API_V1}/products`;
    this.http.get<any[]>(url).subscribe({
      next: data => { this.products = Array.isArray(data) ? data : []; this.loading = false; },
      error: () => { this.products = []; this.loading = false; }
    });
  }

  toggleFavorite(product: any): void {
    const id = this.auth.getUserId();
    if (!id) return;
    this.togglingFav[product.id] = true;
    this.http.post<any>(`${API_V1}/customers/${id}/favorites/products/${product.id}`, {}).subscribe({
      next: (res) => {
        product.favorited = res.favorited;
        this.togglingFav[product.id] = false;
      },
      error: () => { this.togglingFav[product.id] = false; }
    });
  }

  order(product: any): void {
    const id = this.auth.getUserId();
    if (!id) return;
    this.addingToCart[product.id] = true;
    this.http.post(`${API}/customers/${id}/orders`, {
      items: [{ productId: product.id, quantity: 1 }]
    }).subscribe({
      next: () => {
        this.success = `${product.name} ordered successfully!`;
        this.addingToCart[product.id] = false;
        setTimeout(() => this.success = '', 3000);
      },
      error: () => { this.addingToCart[product.id] = false; }
    });
  }
}
