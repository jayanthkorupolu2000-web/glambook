import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from './guards/auth.guard';
import { NoAuthGuard } from './guards/no-auth.guard';

const routes: Routes = [
  { path: '', redirectTo: 'auth', pathMatch: 'full' },

  {
    path: 'auth',
    canActivate: [NoAuthGuard],
    loadChildren: () =>
      import('./features/auth/auth.module').then(m => m.AuthModule)
  },

  {
    path: 'dashboard/customer',
    loadChildren: () =>
      import('./features/customer/customer.module').then(m => m.CustomerModule),
    canActivate: [AuthGuard],
    data: { roles: ['CUSTOMER'] }
  },

  {
    path: 'dashboard/owner',
    loadChildren: () =>
      import('./features/owner/owner.module').then(m => m.OwnerModule),
    canActivate: [AuthGuard],
    data: { roles: ['SALON_OWNER'] }
  },

  {
    path: 'dashboard/admin',
    loadChildren: () =>
      import('./features/admin/admin.module').then(m => m.AdminModule),
    canActivate: [AuthGuard],
    data: { roles: ['ADMIN'] }
  },

  {
    path: 'dashboard/professional',
    loadChildren: () =>
      import('./features/professional/professional.module').then(m => m.ProfessionalModule),
    canActivate: [AuthGuard],
    data: { roles: ['PROFESSIONAL'] }
  },

  { path: '**', redirectTo: 'auth' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
