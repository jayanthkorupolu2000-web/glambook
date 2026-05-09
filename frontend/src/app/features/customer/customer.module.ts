import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';

import { AppointmentBookingComponent } from './appointment-booking/appointment-booking.component';
import { AppointmentHistoryComponent } from './appointment-history/appointment-history.component';
import { BeautyProfileComponent } from './beauty-profile/beauty-profile.component';
import { ConsultationsComponent } from './consultations/consultations.component';
import { CustomerAppointmentsComponent } from './customer-appointments/customer-appointments.component';
import { CustomerDashboardComponent } from './customer-dashboard.component';
import { CustomerDashboardHomeComponent } from './customer-dashboard/customer-dashboard.component';
import { CustomerFavoritesComponent } from './customer-favorites/customer-favorites.component';
import { CustomerLoyaltyComponent } from './customer-loyalty/customer-loyalty.component';
import { CustomerNotificationsComponent } from './customer-notifications/customer-notifications.component';
import { CustomerOrdersComponent } from './customer-orders/customer-orders.component';
import { CustomerProductsComponent } from './customer-products/customer-products.component';
import { CustomerProfileComponent } from './customer-profile/customer-profile.component';
import { ProfessionalBrowseComponent } from './professional-browse/professional-browse.component';
import { ServiceSearchComponent } from './service-search/service-search.component';

const routes: Routes = [
  {
    path: '',
    component: CustomerDashboardComponent,
    children: [
      { path: '', component: CustomerDashboardHomeComponent },
      { path: 'home', component: CustomerDashboardHomeComponent },
      { path: 'search', component: ServiceSearchComponent },
      { path: 'browse', component: ProfessionalBrowseComponent },
      { path: 'book/:professionalId', component: AppointmentBookingComponent },
      { path: 'appointments', component: CustomerAppointmentsComponent },
      { path: 'profile', component: CustomerProfileComponent },
      { path: 'notifications', component: CustomerNotificationsComponent },
      { path: 'appointment-history', component: AppointmentHistoryComponent },
      { path: 'beauty-profile', component: BeautyProfileComponent },
      { path: 'consultations', component: ConsultationsComponent },
      { path: 'products', component: CustomerProductsComponent },
      { path: 'orders', component: CustomerOrdersComponent },
      { path: 'loyalty', component: CustomerLoyaltyComponent },
      { path: 'favorites', component: CustomerFavoritesComponent }
    ]
  }
];

@NgModule({
  declarations: [
    CustomerDashboardComponent,
    CustomerDashboardHomeComponent,
    ServiceSearchComponent,
    CustomerAppointmentsComponent,
    CustomerNotificationsComponent,
    ProfessionalBrowseComponent,
    AppointmentBookingComponent,
    AppointmentHistoryComponent,
    CustomerProfileComponent,
    BeautyProfileComponent,
    ConsultationsComponent,
    CustomerProductsComponent,
    CustomerOrdersComponent,
    CustomerLoyaltyComponent,
    CustomerFavoritesComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    SharedModule,
    RouterModule.forChild(routes)
  ]
})
export class CustomerModule {}
