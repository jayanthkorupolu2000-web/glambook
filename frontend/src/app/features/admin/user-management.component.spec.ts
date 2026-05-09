import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { AdminService } from '../../services/admin.service';
import { UserManagementComponent } from './user-management.component';

describe('UserManagementComponent', () => {
  let component: UserManagementComponent;
  let fixture: ComponentFixture<UserManagementComponent>;
  let adminService: jasmine.SpyObj<AdminService>;

  const mockUsersResponse = {
    customers: [
      { id: 1, name: 'John Doe', email: 'john@example.com', phone: '1234567890', city: 'Hyderabad' }
    ],
    owners: [
      { id: 1, name: 'Jane Smith', salonName: 'Jane Salon', email: 'jane@salon.com', phone: '0987654321', city: 'Visakhapatnam' }
    ],
    professionals: [
      { id: 1, name: 'Bob Wilson', email: 'bob@example.com', city: 'Vijayawada', specialization: 'Hair Stylist' }
    ],
    totalCount: 3
  };

  beforeEach(async () => {
    const spy = jasmine.createSpyObj('AdminService', ['getAllUsers']);

    await TestBed.configureTestingModule({
      declarations: [UserManagementComponent],
      imports: [HttpClientTestingModule],
      providers: [
        { provide: AdminService, useValue: spy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(UserManagementComponent);
    component = fixture.componentInstance;
    adminService = TestBed.inject(AdminService) as jasmine.SpyObj<AdminService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load users on init', () => {
    adminService.getAllUsers.and.returnValue(of(mockUsersResponse));

    component.ngOnInit();

    expect(adminService.getAllUsers).toHaveBeenCalled();
    expect(component.users.length).toBe(3);
    expect(component.loading).toBeFalse();
  });

  it('should transform users data correctly', () => {
    adminService.getAllUsers.and.returnValue(of(mockUsersResponse));

    component.ngOnInit();

    const customerUser = component.users.find(u => u.role === 'CUSTOMER');
    const ownerUser = component.users.find(u => u.role === 'SALON_OWNER');
    const professionalUser = component.users.find(u => u.role === 'PROFESSIONAL');

    expect(customerUser).toBeDefined();
    expect(customerUser?.name).toBe('John Doe');
    expect(customerUser?.additionalInfo).toBeUndefined();

    expect(ownerUser).toBeDefined();
    expect(ownerUser?.name).toBe('Jane Smith');
    expect(ownerUser?.additionalInfo).toBe('Jane Salon');

    expect(professionalUser).toBeDefined();
    expect(professionalUser?.name).toBe('Bob Wilson');
    expect(professionalUser?.additionalInfo).toBe('Hair Stylist');
  });

  it('should return correct badge classes for roles', () => {
    expect(component.getRoleBadgeClass('CUSTOMER')).toBe('badge bg-primary');
    expect(component.getRoleBadgeClass('SALON_OWNER')).toBe('badge bg-success');
    expect(component.getRoleBadgeClass('PROFESSIONAL')).toBe('badge bg-info');
    expect(component.getRoleBadgeClass('UNKNOWN')).toBe('badge bg-secondary');
  });

  it('should return correct display names for roles', () => {
    expect(component.getRoleDisplayName('CUSTOMER')).toBe('Customer');
    expect(component.getRoleDisplayName('SALON_OWNER')).toBe('Salon Owner');
    expect(component.getRoleDisplayName('PROFESSIONAL')).toBe('Professional');
    expect(component.getRoleDisplayName('UNKNOWN')).toBe('UNKNOWN');
  });

  it('should handle error when loading users fails', () => {
    const errorMessage = 'Network error';
    adminService.getAllUsers.and.throwError(errorMessage);

    component.ngOnInit();

    expect(component.error).toBe('Failed to load users. Please try again.');
    expect(component.loading).toBeFalse();
  });
});