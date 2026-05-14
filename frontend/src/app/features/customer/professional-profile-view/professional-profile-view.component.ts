import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Professional } from '../../../models';
import { ProfessionalService } from '../../../services/professional.service';

const BASE = 'http://localhost:8080';

interface Review {
  id: number;
  customerName: string;
  rating: number;
  qualityRating: number;
  timelinessRating: number;
  professionalismRating: number;
  comment: string;
  createdAt: string;
}

interface Certificate {
  name: string;
  fileUrl: string;
  isPdf: boolean;
}

@Component({
  selector: 'app-professional-profile-view',
  templateUrl: './professional-profile-view.component.html',
  styleUrls: ['./professional-profile-view.component.scss']
})
export class ProfessionalProfileViewComponent implements OnInit {
  professional: Professional | null = null;
  reviews: Review[] = [];
  certificates: Certificate[] = [];
  loading = true;
  error = '';

  averageRating = 0;
  averageQuality = 0;
  averageTimeliness = 0;
  averageProfessionalism = 0;
  totalReviews = 0;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private professionalService: ProfessionalService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('professionalId'));
    if (id) this.load(id);
  }

  load(id: number): void {
    this.loading = true;
    this.professionalService.getById(id).subscribe({
      next: pro => {
        this.professional = pro;
        this.loading = false;
        this.loadReviews(id);
        this.loadCertificates(id);
      },
      error: () => { this.error = 'Failed to load professional details.'; this.loading = false; }
    });
  }

  loadReviews(id: number): void {
    this.http.get<any>(`${BASE}/api/reviews?professionalId=${id}`).subscribe({
      next: data => {
        this.reviews = data.reviews || [];
        this.averageRating = data.averageRating || 0;
        this.averageQuality = data.averageQualityRating || 0;
        this.averageTimeliness = data.averageTimelinessRating || 0;
        this.averageProfessionalism = data.averageProfessionalismRating || 0;
        this.totalReviews = this.reviews.length;
      },
      error: () => {}
    });
  }

  loadCertificates(id: number): void {
    this.http.get<any[]>(`${BASE}/api/v1/professionals/${id}/certificates`).subscribe({
      next: data => { this.certificates = data || []; },
      error: () => {}
    });
  }

  stars(rating: number): boolean[] {
    return Array.from({ length: 5 }, (_, i) => i < Math.round(rating));
  }

  bookNow(): void {
    if (this.professional) {
      this.router.navigate(['/dashboard/customer/book', this.professional.id]);
    }
  }

  goBack(): void {
    this.router.navigate(['/dashboard/customer/search']);
  }
}
