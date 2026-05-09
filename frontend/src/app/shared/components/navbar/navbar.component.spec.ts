import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { AuthService } from '../../../services/auth.service';
import { NavbarComponent } from './navbar.component';

describe('NavbarComponent', () => {
  let component: NavbarComponent;
  let fixture: ComponentFixture<NavbarComponent>;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    const authServiceSpy = jasmine.createSpyObj('AuthService', [
      'getRole',
      'isLoggedIn',
      'logout'
    ]);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      declarations: [NavbarComponent],
      imports: [RouterTestingModule],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(NavbarComponent);
    component = fixture.componentInstance;
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with current role and login status', () => {
    authService.getRole.and.returnValue('CUSTOMER');
    authService.isLoggedIn.and.returnValue(true);

    component.ngOnInit();

    expect(component.currentRole).toBe('CUSTOMER');
    expect(component.isLoggedIn).toBe(true);
  });

  it('should return correct navigation links for CUSTOMER role', () => {
    component.currentRole = 'CUSTOMER';
    component.isLoggedIn = true;

    const links = component.getNavigationLinks();

    expect(links).toEqual([
      { label: 'Browse Professionals', route: '/dashboard/customer/browse' },
      { label: 'My Appointments', route: '/dashboard/customer/appointments' },
      { label: 'My Profile', route: '/dashboard/customer/profile' }
    ]);
  });

  it('should return correct navigation links for SALON_OWNER role', () => {
    component.currentRole = 'SALON_OWNER';
    component.isLoggedIn = true;

    const links = component.getNavigationLinks();

    expect(links).toEqual([
      { label: 'Dashboard', route: '/dashboard/owner' },
      { label: 'Staff Management', route: '/dashboard/owner/staff' },
      { label: 'Services', route: '/dashboard/owner/services' },
      { label: 'Appointments', route: '/dashboard/owner/appointments' }
    ]);
  });

  it('should return correct navigation links for ADMIN role', () => {
    component.currentRole = 'ADMIN';
    component.isLoggedIn = true;

    const links = component.getNavigationLinks();

    expect(links).toEqual([
      { label: 'Dashboard', route: '/dashboard/admin' },
      { label: 'User Management', route: '/dashboard/admin/users' },
      { label: 'Salon Owners', route: '/dashboard/admin/owners' },
      { label: 'Reports', route: '/dashboard/admin/reports' }
    ]);
  });

  it('should return empty array when not logged in', () => {
    component.isLoggedIn = false;
    component.currentRole = null;

    const links = component.getNavigationLinks();

    expect(links).toEqual([]);
  });

  it('should return correct role display name', () => {
    expect(component.getRoleDisplayName()).toBe('');

    component.currentRole = 'CUSTOMER';
    expect(component.getRoleDisplayName()).toBe('Customer');

    component.currentRole = 'SALON_OWNER';
    expect(component.getRoleDisplayName()).toBe('Salon Owner');

    component.currentRole = 'PROFESSIONAL';
    expect(component.getRoleDisplayName()).toBe('Professional');

    component.currentRole = 'ADMIN';
    expect(component.getRoleDisplayName()).toBe('Admin');
  });

  it('should logout and navigate to login page', () => {
    component.logout();

    expect(authService.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/auth/login']);
  });
});