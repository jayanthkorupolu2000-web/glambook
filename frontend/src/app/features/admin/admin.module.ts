import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { AdminDashboardComponent } from './admin-dashboard.component';
import { AdminComplaintsComponent } from './complaints/admin-complaints.component';
import { AdminPolicyComponent } from './policy/admin-policy.component';
import { AdminReportsComponent } from './reports-v2/admin-reports.component';
import { ReportsComponent } from './reports.component';
import { SalonOwnerListComponent } from './salon-owner-list.component';
import { AdminUserManagementComponent } from './user-management-v2/admin-user-management.component';
import { UserManagementComponent } from './user-management.component';

const routes: Routes = [
  { path: '', component: AdminDashboardComponent },
  { path: 'users', component: UserManagementComponent },
  { path: 'owners', component: SalonOwnerListComponent },
  { path: 'reports', component: ReportsComponent },
  { path: 'complaints', component: AdminComplaintsComponent },
  { path: 'user-management', component: AdminUserManagementComponent },
  { path: 'analytics', component: AdminReportsComponent },
  { path: 'policy', component: AdminPolicyComponent }
];

@NgModule({
  declarations: [
    AdminDashboardComponent,
    UserManagementComponent,
    SalonOwnerListComponent,
    ReportsComponent,
    AdminComplaintsComponent,
    AdminUserManagementComponent,
    AdminReportsComponent,
    AdminPolicyComponent
  ],
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule.forChild(routes)]
})
export class AdminModule {}
