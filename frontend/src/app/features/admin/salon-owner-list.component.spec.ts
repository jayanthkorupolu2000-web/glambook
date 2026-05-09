import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { SalonOwner } from '../../models';
import { AdminService } from '../../services/admin.service';
import { SalonOwnerListComponent } from './salon-owner-list.component';

describe('SalonOwnerListComponent', () => {
  let component: SalonOwnerListComponent;
  let fixture: ComponentFixture<SalonOwnerListComponent>;
  let adminService: jasmine.SpyObj<AdminService>;

  const mockOwners: SalonOwner[] = [
    {
      id: 1,
      name: 'Ravi Kumar',
      salonName: 'Ravi Salon',
      city: 'Visakhapatnam',
      email: 'ravi@salon.com',
      phone: '9000000001'
    },
    {
      id: 2,
      name: 'Priya Reddy',
      salonName: 'Priya Salon',
      city: 'Vijayawada',
      email: 'priya@salon.com',
      phone: '9000000002'
    }
  ];

  beforeEach(async () => {
    const adminServiceSpy = jasmine.createSpyObj('AdminService', ['getAllOwners']);

    await TestBed.configureTestingModule({
      declarations: [SalonOwnerListComponent],
      imports: [HttpClientTestingModule],
      providers: [
        { provide: AdminService, useValue: adminServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SalonOwnerListComponent);
    component = fixture.componentInstance;
    adminService = TestBed.inject(AdminService) as jasmine.SpyObj<AdminService>;
  });

  it('should create', () => {
    adminService.getAllOwners.and.returnValue(of(mockOwners));
    expect(component).toBeTruthy();
  });

  it('should load owners on init', () => {
    adminService.getAllOwners.and.returnValue(of(mockOwners));
    
    component.ngOnInit();
    
    expect(adminService.getAllOwners).toHaveBeenCalled();
    expect(component.owners).toEqual(mockOwners);
    expect(component.loading).toBeFalse();
  });

  it('should handle error when loading owners fails', () => {
    const errorResponse = new Error('Network error');
    adminService.getAllOwners.and.returnValue(throwError(() => errorResponse));
    
    component.ngOnInit();
    
    expect(component.error).toBe('Failed to load salon owners. Please try again.');
    expect(component.loading).toBeFalse();
  });

  it('should sort owners by city', () => {
    const unsortedOwners = [mockOwners[1], mockOwners[0]]; // Vijayawada first, then Visakhapatnam
    adminService.getAllOwners.and.returnValue(of(unsortedOwners));
    
    component.loadOwners();
    
    expect(component.owners[0].city).toBe('Vijayawada');
    expect(component.owners[1].city).toBe('Visakhapatnam');
  });

  it('should return correct badge class for cities', () => {
    expect(component.getCityBadgeClass('Visakhapatnam')).toBe('badge bg-primary');
    expect(component.getCityBadgeClass('Vijayawada')).toBe('badge bg-success');
    expect(component.getCityBadgeClass('Hyderabad')).toBe('badge bg-info');
    expect(component.getCityBadgeClass('Ananthapur')).toBe('badge bg-warning text-dark');
    expect(component.getCityBadgeClass('Khammam')).toBe('badge bg-secondary');
    expect(component.getCityBadgeClass('Unknown')).toBe('badge bg-light text-dark');
  });

  it('should track owners by id', () => {
    const owner = mockOwners[0];
    const result = component.trackByOwnerId(0, owner);
    expect(result).toBe(owner.id);
  });

  it('should refresh owners when loadOwners is called', () => {
    adminService.getAllOwners.and.returnValue(of(mockOwners));
    
    component.loadOwners();
    
    expect(adminService.getAllOwners).toHaveBeenCalled();
    expect(component.owners).toEqual(mockOwners);
  });
});