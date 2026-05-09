import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { AppointmentOverviewComponent } from './appointment-overview/appointment-overview.component';
import { OwnerLoyaltyComponent } from './loyalty/owner-loyalty.component';
import { OwnerComplaintsComponent } from './owner-complaints/owner-complaints.component';
import { OwnerConsultationsComponent } from './owner-consultations/owner-consultations.component';
import { OwnerDashboardComponent } from './owner-dashboard.component';
import { OwnerGroupBookingsComponent } from './owner-group-bookings/owner-group-bookings.component';
import { OwnerNotificationsComponent } from './owner-notifications/owner-notifications.component';
import { OwnerProfileComponent } from './owner-profile/owner-profile.component';
import { OwnerReportsComponent } from './owner-reports/owner-reports.component';
import { OwnerPromotionsComponent } from './promotions/owner-promotions.component';
import { OwnerSalonPolicyComponent } from './salon-policy/owner-salon-policy.component';
import { ServiceManagementComponent } from './service-management/service-management.component';
import { StaffListComponent } from './staff-list/staff-list.component';

const routes: Routes = [
  {
    path: '',
    component: OwnerDashboardComponent,
    children: [
      { path: '', redirectTo: 'staff', pathMatch: 'full' },
      { path: 'profile', component: OwnerProfileComponent },
      { path: 'staff', component: StaffListComponent },
      { path: 'services', component: ServiceManagementComponent },
      { path: 'appointments', component: AppointmentOverviewComponent },
      { path: 'promotions', component: OwnerPromotionsComponent },
      { path: 'loyalty', component: OwnerLoyaltyComponent },
      { path: 'policies', component: OwnerSalonPolicyComponent },
      { path: 'complaints', component: OwnerComplaintsComponent },
      { path: 'reports', component: OwnerReportsComponent },
      { path: 'notifications', component: OwnerNotificationsComponent },
      { path: 'group-bookings', component: OwnerGroupBookingsComponent },
      { path: 'consultations', component: OwnerConsultationsComponent }
    ]
  }
];

@NgModule({
  declarations: [
    OwnerDashboardComponent,
    OwnerProfileComponent,
    StaffListComponent,
    ServiceManagementComponent,
    AppointmentOverviewComponent,
    OwnerPromotionsComponent,
    OwnerLoyaltyComponent,
    OwnerSalonPolicyComponent,
    OwnerComplaintsComponent,
    OwnerReportsComponent,
    OwnerNotificationsComponent,
    OwnerGroupBookingsComponent,
    OwnerConsultationsComponent
  ],
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule.forChild(routes)]
})
export class OwnerModule {}
