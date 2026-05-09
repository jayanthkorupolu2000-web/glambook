import { Component, OnInit } from '@angular/core';
import { City, PagedResponse, Professional } from '../../../models';
import { ProfessionalService } from '../../../services/professional.service';

const CITIES: City[] = ['Visakhapatnam', 'Vijayawada', 'Hyderabad', 'Ananthapur', 'Khammam'];

@Component({
  selector: 'app-professional-browse',
  templateUrl: './professional-browse.component.html'
})
export class ProfessionalBrowseComponent implements OnInit {
  cities = CITIES;
  selectedCity: City = 'Hyderabad';

  professionals: Professional[] = [];
  loading = false;
  error = '';

  page = 0;
  size = 9;
  totalPages = 0;
  totalElements = 0;
  pageNumbers: number[] = [];

  constructor(private professionalService: ProfessionalService) {}

  ngOnInit(): void {
    this.load();
  }

  onCityChange(city: City): void {
    this.selectedCity = city;
    this.page = 0;
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.professionalService.getByCity(this.selectedCity, this.page, this.size).subscribe({
      next: (res: PagedResponse<Professional>) => {
        this.professionals = res.content;
        this.totalPages = res.totalPages;
        this.totalElements = res.totalElements;
        this.pageNumbers = Array.from({ length: this.totalPages }, (_, i) => i);
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load professionals. Please try again.';
        this.loading = false;
      }
    });
  }

  prevPage(): void {
    if (this.page > 0) {
      this.page--;
      this.load();
    }
  }

  nextPage(): void {
    if (this.page < this.totalPages - 1) {
      this.page++;
      this.load();
    }
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }
}
