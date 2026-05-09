import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ProfessionalProfileResponse } from '../../../models/professional.model';
import { AuthService } from '../../../services/auth.service';
import { ProfessionalProfileService } from '../../../services/professional-profile.service';

@Component({
  selector: 'app-prof-profile',
  templateUrl: './prof-profile.component.html'
})
export class ProfProfileComponent implements OnInit {
  profile: ProfessionalProfileResponse | null = null;
  form: FormGroup;
  editMode = false;
  loading = false;
  saving = false;
  success = '';
  error = '';
  profId = 0;
  photoPreview: string | null = null;
  selectedPhoto: File | null = null;

  constructor(
    private fb: FormBuilder,
    private profileService: ProfessionalProfileService,
    private auth: AuthService
  ) {
    this.form = this.fb.group({
      specialization: ['', Validators.required],
      experienceYears: [0, [Validators.required, Validators.min(0)]],
      bio: ['', Validators.maxLength(1000)],
      certifications: [''],
      trainingDetails: [''],
      serviceAreas: [''],
      travelRadiusKm: [0, Validators.min(0)],
      instagramHandle: [''],
      isAvailableHome: [false],
      isAvailableSalon: [true],
      responseTimeHrs: [24, Validators.min(1)]
    });
  }

  ngOnInit(): void {
    this.profId = this.auth.getUserId() || 0;
    this.loadProfile();
  }

  loadProfile(): void {
    this.loading = true;
    this.error = '';
    this.profileService.getProfile(this.profId).subscribe({
      next: data => {
        this.profile = data;
        // Update stored userId if it was wrong
        if (data.id && data.id !== this.profId) {
          localStorage.setItem('auth_user_id', String(data.id));
          this.profId = data.id;
        }
        this.loading = false;
      },
      error: (err) => {
        console.error('Profile load error:', err);
        this.error = `Failed to load profile (${err?.status || 'network error'}). Please restart the backend and try again.`;
        this.loading = false;
        // Create a minimal profile from auth data so the page isn't blank
        this.profile = {
          id: this.profId,
          name: this.auth.getUserName() || 'Professional',
          email: this.auth.getUserEmail() || '',
          city: '', specialization: '', experienceYears: 0,
          certifications: '', trainingDetails: '', serviceAreas: '',
          travelRadiusKm: 0, bio: '', instagramHandle: '',
          isAvailableHome: false, isAvailableSalon: true,
          responseTimeHrs: 24, profilePhotoUrl: '', status: 'ACTIVE',
          salonOwnerName: '', approvedAt: '', averageRating: 0,
          totalReviews: 0, featuredPortfolioItems: []
        } as any;
      }
    });
  }

  startEdit(): void {
    if (this.profile) {
      this.form.patchValue({
        specialization: this.profile.specialization,
        experienceYears: this.profile.experienceYears,
        bio: this.profile.bio,
        certifications: this.profile.certifications,
        trainingDetails: this.profile.trainingDetails,
        serviceAreas: this.profile.serviceAreas,
        travelRadiusKm: this.profile.travelRadiusKm,
        instagramHandle: this.profile.instagramHandle,
        isAvailableHome: this.profile.isAvailableHome,
        isAvailableSalon: this.profile.isAvailableSalon,
        responseTimeHrs: this.profile.responseTimeHrs
      });
    }
    this.editMode = true;
  }

  save(): void {
    if (this.form.invalid) return;
    this.saving = true;
    this.profileService.updateProfile(this.profId, this.form.value).subscribe({
      next: data => {
        this.profile = data;
        this.editMode = false;
        this.success = 'Profile updated!';
        this.saving = false;
        setTimeout(() => this.success = '', 3000);
      },
      error: () => { this.error = 'Failed to save.'; this.saving = false; }
    });
  }

  onPhotoSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.selectedPhoto = file;
    const reader = new FileReader();
    reader.onload = e => this.photoPreview = e.target?.result as string;
    reader.readAsDataURL(file);
  }

  uploadPhoto(): void {
    if (!this.selectedPhoto) return;
    this.profileService.uploadProfilePhoto(this.profId, this.selectedPhoto).subscribe({
      next: res => {
        if (this.profile) this.profile.profilePhotoUrl = res.photoUrl;
        this.photoPreview = null;
        this.selectedPhoto = null;
        this.success = 'Photo uploaded!';
        setTimeout(() => this.success = '', 3000);
      },
      error: () => this.error = 'Photo upload failed.'
    });
  }

  stars(n: number): number[] { return Array.from({ length: 5 }, (_, i) => i + 1); }

  onHomeVisitToggle(event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    // Auto-set travel radius to 10km when enabling home visits if currently 0
    if (checked && !this.form.value.travelRadiusKm) {
      this.form.patchValue({ travelRadiusKm: 10 });
    }
    // Clear travel radius when disabling home visits
    if (!checked) {
      this.form.patchValue({ travelRadiusKm: 0 });
    }
  }
}
