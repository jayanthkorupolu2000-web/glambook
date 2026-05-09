import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { AlertComponent } from './components/alert/alert.component';
import { CityFilterComponent } from './components/city-filter/city-filter.component';
import { LoadingSpinnerComponent } from './components/loading-spinner/loading-spinner.component';
import { NavbarComponent } from './components/navbar/navbar.component';
import { PortfolioModalComponent } from './components/portfolio-modal/portfolio-modal.component';
import { ProfessionalCardComponent } from './components/professional-card/professional-card.component';

@NgModule({
  declarations: [
    NavbarComponent,
    CityFilterComponent,
    ProfessionalCardComponent,
    LoadingSpinnerComponent,
    AlertComponent,
    PortfolioModalComponent
  ],
  imports: [
    CommonModule,
    RouterModule
  ],
  exports: [
    NavbarComponent,
    CityFilterComponent,
    ProfessionalCardComponent,
    LoadingSpinnerComponent,
    AlertComponent,
    PortfolioModalComponent,
    RouterModule
  ]
})
export class SharedModule { }