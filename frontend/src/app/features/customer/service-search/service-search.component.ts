import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { ProfessionalSearchResult, SERVICE_CATEGORIES } from '../../../models/customer.model';
import { AuthService } from '../../../services/auth.service';

const BASE = 'http://localhost:8080/api/v1';

@Component({
  selector: 'app-service-search',
  templateUrl: './service-search.component.html'
})
export class ServiceSearchComponent implements OnInit {
  form: FormGroup;
  results: ProfessionalSearchResult[] = [];
  loading = false;
  cities = ['Visakhapatnam', 'Vijayawada', 'Hyderabad', 'Ananthapur', 'Khammam'];

  serviceTree = SERVICE_CATEGORIES;
  selectedGroup = '';
  selectedCategory = '';
  availableCategories: string[] = [];

  services: any[] = [];
  togglingFav: Record<number, boolean> = {};
  favSuccess = '';

  // Portfolio modal state
  portfolioProfId: number | null = null;
  portfolioProfName = '';

  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router, private http: HttpClient) {
    this.form = this.fb.group({
      city: [''],
      serviceType: [null],
      targetGroup: [''],
      category: [''],
      minPrice: [''],
      maxPrice: [''],
      minRating: [''],
      date: [''],
      keyword: ['']
    });
  }

  ngOnInit(): void {
    this.form.get('keyword')?.valueChanges
      .pipe(debounceTime(400), distinctUntilChanged())
      .subscribe(() => this.search());

    this.form.get('targetGroup')?.valueChanges.subscribe(group => {
      this.selectedGroup = group;
      this.availableCategories = group ? (this.serviceTree as any)[group] || [] : [];
      this.form.patchValue({ category: '' });
    });

    this.search();
    this.loadServices();
  }

  loadServices(): void {
    const id = this.auth.getUserId();
    const url = id ? `/api/services/list?customerId=${id}` : '/api/services/list';
    this.http.get<any[]>(`http://localhost:8080${url}`).subscribe({
      next: data => { this.services = Array.isArray(data) ? data : []; },
      error: () => {}
    });
  }

  toggleServiceFavorite(service: any): void {
    const id = this.auth.getUserId();
    if (!id) return;
    this.togglingFav[service.id] = true;
    this.http.post<any>(`${BASE}/customers/${id}/favorites/services/${service.id}`, {}).subscribe({
      next: (res) => {
        service.favorited = res.favorited;
        this.favSuccess = res.favorited ? `${service.name} added to favorites!` : `${service.name} removed from favorites.`;
        this.togglingFav[service.id] = false;
        setTimeout(() => this.favSuccess = '', 2500);
      },
      error: () => { this.togglingFav[service.id] = false; }
    });
  }

  search(): void {
    this.loading = true;
    const v = this.form.value;

    // Clamp: min can't be negative, max must be >= min
    const minP = v.minPrice !== '' && v.minPrice !== null ? Math.max(0, +v.minPrice) : null;
    const maxP = v.maxPrice !== '' && v.maxPrice !== null ? +v.maxPrice : null;

    const params: Record<string, string> = {};
    if (v.city)        params['city'] = v.city;
    if (v.targetGroup) params['targetGroup'] = v.targetGroup;
    if (v.category)    params['category'] = v.category;
    if (v.serviceType === 'HOME') params['homeAvailable'] = 'true';
    if (v.keyword)     params['keyword'] = v.keyword;
    if (minP !== null && minP >= 0) params['minPrice'] = minP.toString();
    if (maxP !== null && maxP > 0)  params['maxPrice'] = maxP.toString();
    if (v.minRating && +v.minRating > 0) params['minRating'] = v.minRating;

    const query = new URLSearchParams(params).toString();
    const url = `${BASE}/professionals/search${query ? '?' + query : ''}`;

    const token = this.auth.getToken();
    const headers: Record<string, string> = token ? { Authorization: `Bearer ${token}` } : {};

    fetch(url, { headers }).then(r => r.json()).then(data => {
      this.results = Array.isArray(data) ? data : [];
      this.loading = false;
    }).catch(() => { this.results = []; this.loading = false; });
  }

  /** Called on Min input — clamp to 0, clear Max if it's now less than Min */
  onMinPriceChange(): void {
    const raw = +this.form.value.minPrice;
    if (isNaN(raw)) return;
    const clamped = Math.max(0, raw);
    if (clamped !== raw) this.form.patchValue({ minPrice: clamped }, { emitEvent: false });

    const maxVal = +this.form.value.maxPrice;
    if (this.form.value.maxPrice !== '' && !isNaN(maxVal) && maxVal < clamped) {
      this.form.patchValue({ maxPrice: clamped }, { emitEvent: false });
    }
    this.search();
  }

  /** Called on Max input — ensure max >= min */
  onMaxPriceChange(): void {
    const minVal = +this.form.value.minPrice || 0;
    const raw    = +this.form.value.maxPrice;
    if (!isNaN(raw) && raw < minVal) {
      this.form.patchValue({ maxPrice: minVal }, { emitEvent: false });
    }
    this.search();
  }

  get minPriceValue(): number { return Math.max(0, +this.form.value.minPrice || 0); }

  viewProfile(profId: number): void {
    this.router.navigate(['/dashboard/customer/book', profId]);
  }

  bookNow(profId: number): void {
    this.router.navigate(['/dashboard/customer/book', profId]);
  }

  openPortfolio(pro: ProfessionalSearchResult): void {
    this.portfolioProfId = pro.professionalId;
    this.portfolioProfName = pro.name;
  }

  closePortfolio(): void {
    this.portfolioProfId = null;
    this.portfolioProfName = '';
  }

  stars(n: number): number[] { return Array.from({ length: 5 }, (_, i) => i + 1); }
}
