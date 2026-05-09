import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { ProfAnalyticsComponent } from './prof-analytics/prof-analytics.component';
import { ProfAppointmentsComponent } from './prof-appointments/prof-appointments.component';
import { ProfAvailabilityComponent } from './prof-availability/prof-availability.component';
import { ProfConsultationsComponent } from './prof-consultations/prof-consultations.component';
import { ProfDashboardComponent } from './prof-dashboard/prof-dashboard.component';
import { ProfGroupBookingsComponent } from './prof-group-bookings/prof-group-bookings.component';
import { ProfMessagesComponent } from './prof-messages/prof-messages.component';
import { ProfNotificationsComponent } from './prof-notifications/prof-notifications.component';
import { ProfPoliciesComponent } from './prof-policies/prof-policies.component';
import { ProfPortfolioComponent } from './prof-portfolio/prof-portfolio.component';
import { ProfProfileComponent } from './prof-profile/prof-profile.component';
import { ProfReviewsComponent } from './prof-reviews/prof-reviews.component';
import { ProfScheduleComponent } from './prof-schedule/prof-schedule.component';
import { ProfServicesComponent } from './prof-services/prof-services.component';
import { ProfessionalDashboardComponent } from './professional-dashboard.component';

const routes: Routes = [
  {
    path: '',
    component: ProfessionalDashboardComponent,
    children: [
      { path: '', component: ProfDashboardComponent },
      { path: 'profile', component: ProfProfileComponent },
      { path: 'services', component: ProfServicesComponent },
      { path: 'portfolio', component: ProfPortfolioComponent },
      { path: 'availability', component: ProfAvailabilityComponent },
      { path: 'schedule', component: ProfScheduleComponent },
      { path: 'appointments', component: ProfAppointmentsComponent },
      { path: 'communications', component: ProfMessagesComponent },
      { path: 'reviews', component: ProfReviewsComponent },
      { path: 'analytics', component: ProfAnalyticsComponent },
      { path: 'notifications', component: ProfNotificationsComponent },
      { path: 'consultations', component: ProfConsultationsComponent },
      { path: 'group-bookings', component: ProfGroupBookingsComponent },
      { path: 'policies', component: ProfPoliciesComponent }
    ]
  }
];

@NgModule({
  declarations: [
    ProfessionalDashboardComponent,
    ProfDashboardComponent,
    ProfProfileComponent,
    ProfServicesComponent,
    ProfPortfolioComponent,
    ProfAvailabilityComponent,
    ProfScheduleComponent,
    ProfAppointmentsComponent,
    ProfMessagesComponent,
    ProfReviewsComponent,
    ProfAnalyticsComponent,
    ProfNotificationsComponent,
    ProfConsultationsComponent,
    ProfGroupBookingsComponent,
    ProfPoliciesComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    SharedModule,
    RouterModule.forChild(routes)
  ]
})
export class ProfessionalModule {}
