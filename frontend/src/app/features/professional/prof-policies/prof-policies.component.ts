import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../../services/auth.service';
import { ProfessionalProfileService } from '../../../services/professional-profile.service';
import { SalonPolicyResponse, SalonPolicyService } from '../../../services/salon-policy.service';

@Component({
  selector: 'app-prof-policies',
  templateUrl: './prof-policies.component.html',
  styleUrls: ['./prof-policies.component.css']
})
export class ProfPoliciesComponent implements OnInit {
  policies: SalonPolicyResponse[] = [];
  loading = false;
  error = '';
  city = '';
  profId = 0;

  constructor(
    private policyService: SalonPolicyService,
    private profileService: ProfessionalProfileService,
    private auth: AuthService
  ) {}

  ngOnInit(): void {
    this.profId = this.auth.getUserId() || 0;
    // Load profile to get city, then fetch policies
    this.profileService.getProfile(this.profId).subscribe({
      next: profile => {
        this.city = profile.city || '';
        if (this.city) this.loadPolicies();
        else this.error = 'City not set in your profile. Please update your profile.';
      },
      error: () => { this.error = 'Failed to load profile.'; }
    });
  }

  loadPolicies(): void {
    this.loading = true;
    this.policyService.getPoliciesByCity(this.city).subscribe({
      next: data => { this.policies = Array.isArray(data) ? data : []; this.loading = false; },
      error: () => { this.error = 'Failed to load policies.'; this.loading = false; }
    });
  }
}
