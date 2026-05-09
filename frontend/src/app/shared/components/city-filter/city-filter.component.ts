import { Component, EventEmitter, Input, Output } from '@angular/core';
import { City } from '../../../models';

@Component({
  selector: 'app-city-filter',
  templateUrl: './city-filter.component.html',
  styleUrls: ['./city-filter.component.scss']
})
export class CityFilterComponent {
  readonly cities: City[] = ['Visakhapatnam', 'Vijayawada', 'Hyderabad', 'Ananthapur', 'Khammam'];

  @Input() selectedCity: City | null = null;
  @Output() cityChange = new EventEmitter<City | null>();

  onCitySelect(city: City | null): void {
    this.selectedCity = city;
    this.cityChange.emit(city);
  }
}
