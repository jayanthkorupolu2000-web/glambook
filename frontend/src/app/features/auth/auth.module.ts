import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { AdminLoginComponent } from './admin-login/admin-login.component';
import { AuthLoginComponent } from './auth-login.component';
import { CustomerLoginComponent } from './customer-login/customer-login.component';
import { CustomerRegisterComponent } from './customer-register/customer-register.component';
import { OwnerLoginComponent } from './owner-login/owner-login.component';
import { ProfessionalLoginComponent } from './professional-login/professional-login.component';
import { ProfessionalRegisterComponent } from './professional-register/professional-register.component';

const routes: Routes = [
  { path: '', component: AuthLoginComponent },
  { path: 'login', component: AuthLoginComponent },
  { path: 'customer/login', component: CustomerLoginComponent },
  { path: 'customer/register', component: CustomerRegisterComponent },
  { path: 'owner/login', component: OwnerLoginComponent },
  { path: 'admin/login', component: AdminLoginComponent },
  { path: 'professional/login', component: ProfessionalLoginComponent },
  { path: 'professional/register', component: ProfessionalRegisterComponent }
];

@NgModule({
  declarations: [
    AuthLoginComponent,
    CustomerLoginComponent,
    CustomerRegisterComponent,
    OwnerLoginComponent,
    AdminLoginComponent,
    ProfessionalLoginComponent,
    ProfessionalRegisterComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule.forChild(routes)
  ]
})
export class AuthModule {}
