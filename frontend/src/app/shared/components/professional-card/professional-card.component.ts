import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { Professional } from '../../../models';

@Component({
  selector: 'app-professional-card',
  templateUrl: './professional-card.component.html',
  styleUrls: ['./professional-card.component.scss']
})
export class ProfessionalCardComponent implements OnChanges {
  @Input() professional!: Professional;
  @Output() bookNow = new EventEmitter<Professional>();

  stars: boolean[] = [];

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['professional']) {
      const rating = Math.round(this.professional?.rating ?? 0);
      this.stars = Array.from({ length: 5 }, (_, i) => i < rating);
    }
  }

  onBookNow(): void {
    this.bookNow.emit(this.professional);
  }
}
