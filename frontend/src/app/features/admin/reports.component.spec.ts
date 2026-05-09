import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { AdminService } from '../../services/admin.service';
import { ReportsComponent } from './reports.component';

describe('ReportsComponent', () => {
  let component: ReportsComponent;
  let fixture: ComponentFixture<ReportsComponent>;
  let adminService: jasmine.SpyObj<AdminService>;

  const mockReportsData = {
    totalAppointments: 150,
    totalPayments: 125
  };

  beforeEach(async () => {
    const adminServiceSpy = jasmine.createSpyObj('AdminService', ['getReports']);

    await TestBed.configureTestingModule({
      declarations: [ReportsComponent],
      imports: [HttpClientTestingModule],
      providers: [
        { provide: AdminService, useValue: adminServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ReportsComponent);
    component = fixture.componentInstance;
    adminService = TestBed.inject(AdminService) as jasmine.SpyObj<AdminService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load reports on init', () => {
    adminService.getReports.and.returnValue(of(mockReportsData));

    component.ngOnInit();

    expect(adminService.getReports).toHaveBeenCalled();
    expect(component.reportsData).toEqual(mockReportsData);
    expect(component.loading).toBeFalse();
    expect(component.error).toBeNull();
  });

  it('should handle error when loading reports fails', () => {
    const errorMessage = 'Network error';
    adminService.getReports.and.returnValue(throwError(() => new Error(errorMessage)));

    component.ngOnInit();

    expect(component.reportsData).toBeNull();
    expect(component.loading).toBeFalse();
    expect(component.error).toBe('Failed to load reports. Please try again.');
  });

  it('should refresh reports when refreshReports is called', () => {
    adminService.getReports.and.returnValue(of(mockReportsData));

    component.refreshReports();

    expect(adminService.getReports).toHaveBeenCalled();
    expect(component.reportsData).toEqual(mockReportsData);
  });

  it('should show loading state initially', () => {
    adminService.getReports.and.returnValue(of(mockReportsData));
    
    component.loadReports();
    
    expect(component.loading).toBeTrue();
  });

  it('should display reports data in template', () => {
    adminService.getReports.and.returnValue(of(mockReportsData));
    component.ngOnInit();
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('150');
    expect(compiled.textContent).toContain('125');
    expect(compiled.textContent).toContain('Total Appointments');
    expect(compiled.textContent).toContain('Total Payments');
  });

  it('should display error message when error occurs', () => {
    adminService.getReports.and.returnValue(throwError(() => new Error('Network error')));
    component.ngOnInit();
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('Failed to load reports. Please try again.');
  });
});