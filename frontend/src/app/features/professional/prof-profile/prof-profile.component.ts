import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ProfessionalProfileResponse } from '../../../models/professional.model';
import { AuthService } from '../../../services/auth.service';
import { ProfessionalProfileService } from '../../../services/professional-profile.service';

// ── Specialization options ──────────────────────────────────────────────────
export const SPECIALIZATIONS = [
  'Hair Styling',
  'Hair Coloring',
  'Kids Haircut',
  'Beard & Grooming',
  'Skin Care',
  'Facial',
  'Nail Art',
  'Manicure & Pedicure',
  'Makeup',
  'Bridal Makeup',
  'Body Massage',
  'Waxing & Threading',
  'Packages',
  'Special Treatments',
] as const;

// ── Service-area options keyed by specialization ────────────────────────────
export const SERVICE_AREAS_BY_SPEC: Record<string, string[]> = {
  'Hair Styling':        ['Blow Dry', 'Straightening', 'Keratin Treatment', 'Hair Spa', 'Trim & Style'],
  'Hair Coloring':       ['Global Color', 'Highlights', 'Balayage', 'Ombre', 'Root Touch-Up'],
  'Kids Haircut':        ['Boys Haircut', 'Girls Haircut', 'Baby Haircut', 'School Style'],
  'Beard & Grooming':    ['Beard Trim', 'Clean Shave', 'Beard Styling', 'Mustache Trim'],
  'Skin Care':           ['Acne Treatment', 'Anti-Aging', 'Brightening', 'Hydration Therapy'],
  'Facial':              ['Basic Facial', 'Gold Facial', 'Fruit Facial', 'D-Tan Facial', 'Cleanup'],
  'Nail Art':            ['Gel Nails', 'Acrylic Nails', 'Nail Extensions', 'Nail Art Design'],
  'Manicure & Pedicure': ['Basic Manicure', 'Spa Manicure', 'Basic Pedicure', 'Spa Pedicure'],
  'Makeup':              ['Party Makeup', 'Engagement Makeup', 'Photoshoot Makeup', 'Natural Makeup'],
  'Bridal Makeup':       ['Bridal Package', 'Pre-Bridal', 'Engagement Look', 'Reception Look'],
  'Body Massage':        ['Swedish Massage', 'Deep Tissue', 'Aromatherapy', 'Hot Stone'],
  'Waxing & Threading':  ['Full Body Wax', 'Eyebrow Threading', 'Upper Lip', 'Full Arms/Legs Wax'],
  'Packages':            ['Bridal Package', 'Party Package', 'Grooming Package', 'Spa Package'],
  'Special Treatments':  ['Hair Loss Treatment', 'Dandruff Treatment', 'Scalp Treatment', 'Chemical Peel'],
};

const MAX_CERT_SIZE_MB = 3;

// ── Certificate entry ───────────────────────────────────────────────────────
export interface CertEntry {
  /** User-given display name */
  name: string;
  /** data-URL (for images) or null (for PDFs) — used for in-page preview */
  previewUrl: string | null;
  /** true = PDF, false = image */
  isPdf: boolean;
  /** original File object (kept so we can read it again if needed) */
  file: File;
  /** asset path that will be stored in the backend field */
  assetPath: string;
}

@Component({
  selector: 'app-prof-profile',
  templateUrl: './prof-profile.component.html',
  styleUrls: ['./prof-profile.component.css']
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

  // Specialization dropdown options
  specializationOptions = SPECIALIZATIONS;

  // Service-area checkboxes
  availableServiceAreas: string[] = [];
  selectedServiceAreas: Set<string> = new Set();

  // ── Multi-certificate state ──────────────────────────────────────────────
  /** Saved certificates (shown in both view & edit mode) */
  certificates: CertEntry[] = [];

  /** Pending file waiting for a name before being added */
  pendingCertFile: File | null = null;
  pendingCertPreview: string | null = null;
  pendingCertIsPdf = false;
  pendingCertName = '';
  certError = '';

  /** Certificate currently shown in the viewer overlay */
  viewingCert: CertEntry | null = null;

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
      travelRadiusKm: [0, [Validators.min(0), Validators.max(10)]],
      instagramHandle: [''],
      isAvailableHome: [false],
      isAvailableSalon: [true],
      responseTimeHrs: [24, Validators.min(1)]
    });
  }

  ngOnInit(): void {
    this.profId = this.auth.getUserId() || 0;
    this.loadProfile();

    // React to specialization changes → update service-area checkboxes
    this.form.get('specialization')!.valueChanges.subscribe(spec => {
      this.availableServiceAreas = SERVICE_AREAS_BY_SPEC[spec] ?? [];
      this.selectedServiceAreas = new Set(
        [...this.selectedServiceAreas].filter(a => this.availableServiceAreas.includes(a))
      );
      this.syncServiceAreasToForm();
    });
  }

  loadProfile(): void {
    this.loading = true;
    this.error = '';
    this.profileService.getProfile(this.profId).subscribe({
      next: data => {
        this.profile = data;
        if (data.id && data.id !== this.profId) {
          localStorage.setItem('auth_user_id', String(data.id));
          this.profId = data.id;
        }
        // Restore certificates from the stored JSON string
        this.certificates = this.parseCertificates(data.certifications);
        this.loading = false;
      },
      error: (err) => {
        console.error('Profile load error:', err);
        this.error = `Failed to load profile (${err?.status || 'network error'}). Please restart the backend and try again.`;
        this.loading = false;
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

  // ── localStorage helpers for certificate previews ────────────────────────
  // The DB column only stores name/assetPath/isPdf (no base64).
  // The base64 preview is persisted in localStorage so it survives save/reload.

  private certPreviewKey(assetPath: string): string {
    return `cert_preview_${assetPath}`;
  }

  private savePreviewToStorage(assetPath: string, dataUrl: string): void {
    try {
      localStorage.setItem(this.certPreviewKey(assetPath), dataUrl);
    } catch {
      // localStorage quota exceeded — silently ignore; viewer will show fallback
    }
  }

  private loadPreviewFromStorage(assetPath: string): string | null {
    return localStorage.getItem(this.certPreviewKey(assetPath));
  }

  private removePreviewFromStorage(assetPath: string): void {
    localStorage.removeItem(this.certPreviewKey(assetPath));
  }

  // ── Serialise / deserialise certificates ─────────────────────────────────
  /**
   * Stored in DB as: [{"name":"...", "assetPath":"...", "isPdf":false}]
   * Preview (base64) is kept in localStorage keyed by assetPath.
   */
  private parseCertificates(raw: string): CertEntry[] {
    if (!raw) return [];
    try {
      const arr = JSON.parse(raw);
      if (!Array.isArray(arr)) return [];
      return arr.map((item: any) => {
        const assetPath = item.assetPath || '';
        const isPdf = !!item.isPdf;
        // Restore preview from localStorage (null for PDFs or if not found)
        const previewUrl = isPdf ? null : this.loadPreviewFromStorage(assetPath);
        return {
          name: item.name || 'Certificate',
          assetPath,
          isPdf,
          previewUrl,
          file: null as any
        };
      });
    } catch {
      // Legacy: plain string path from old single-cert format
      if (raw.startsWith('assets/')) {
        const assetPath = raw;
        return [{
          name: 'Certificate',
          assetPath,
          isPdf: raw.endsWith('.pdf'),
          previewUrl: this.loadPreviewFromStorage(assetPath),
          file: null as any
        }];
      }
      return [];
    }
  }

  private serialiseCertificates(): string {
    // Only name/assetPath/isPdf go to the DB — no base64 to avoid column overflow.
    return JSON.stringify(this.certificates.map(c => ({
      name: c.name,
      assetPath: c.assetPath,
      isPdf: c.isPdf
    })));
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

      // Restore service-area checkboxes
      const spec = this.profile.specialization;
      this.availableServiceAreas = SERVICE_AREAS_BY_SPEC[spec] ?? [];
      const saved = (this.profile.serviceAreas || '').split(',').map(s => s.trim()).filter(Boolean);
      this.selectedServiceAreas = new Set(saved.filter(a => this.availableServiceAreas.includes(a)));

      // Reset pending cert state
      this.pendingCertFile = null;
      this.pendingCertPreview = null;
      this.pendingCertIsPdf = false;
      this.pendingCertName = '';
      this.certError = '';
    }
    this.editMode = true;
  }

  // ── Experience clamping ──────────────────────────────────────────────────
  clampExperience(): void {
    const ctrl = this.form.get('experienceYears')!;
    const val = Number(ctrl.value);
    if (isNaN(val) || val < 0) ctrl.setValue(0, { emitEvent: false });
  }

  // ── Travel radius clamping (0–10) ────────────────────────────────────────
  clampTravelRadius(): void {
    const ctrl = this.form.get('travelRadiusKm')!;
    let val = Number(ctrl.value);
    if (isNaN(val) || val < 0) val = 0;
    if (val > 10) val = 10;
    ctrl.setValue(val, { emitEvent: false });
  }

  // ── Service-area checkbox helpers ────────────────────────────────────────
  isAreaSelected(area: string): boolean {
    return this.selectedServiceAreas.has(area);
  }

  toggleArea(area: string, checked: boolean): void {
    if (checked) {
      this.selectedServiceAreas.add(area);
    } else {
      this.selectedServiceAreas.delete(area);
    }
    this.syncServiceAreasToForm();
  }

  private syncServiceAreasToForm(): void {
    this.form.patchValue({ serviceAreas: [...this.selectedServiceAreas].join(', ') });
  }

  // ── Certificate upload ───────────────────────────────────────────────────

  /** Step 1 — user picks a file; we validate and hold it as "pending" */
  onCertFileSelected(event: Event): void {
    this.certError = '';
    const file = (event.target as HTMLInputElement).files?.[0];
    // Reset the input so the same file can be re-selected after removal
    (event.target as HTMLInputElement).value = '';
    if (!file) return;

    const allowedTypes = ['image/jpeg', 'image/png', 'image/webp', 'application/pdf'];
    if (!allowedTypes.includes(file.type)) {
      this.certError = 'Only JPG, PNG, WEBP, or PDF files are allowed.';
      return;
    }
    if (file.size > MAX_CERT_SIZE_MB * 1024 * 1024) {
      this.certError = `File must be smaller than ${MAX_CERT_SIZE_MB} MB.`;
      return;
    }

    this.pendingCertFile = file;
    this.pendingCertIsPdf = file.type === 'application/pdf';
    // Pre-fill name from filename (without extension)
    this.pendingCertName = file.name.replace(/\.[^.]+$/, '').replace(/[_-]/g, ' ');

    if (!this.pendingCertIsPdf) {
      const reader = new FileReader();
      reader.onload = e => this.pendingCertPreview = e.target?.result as string;
      reader.readAsDataURL(file);
    } else {
      this.pendingCertPreview = null;
    }
  }

  /** Step 2 — user confirms the name and we add it to the list */
  addCertificate(): void {
    if (!this.pendingCertFile) return;
    const name = this.pendingCertName.trim();
    if (!name) {
      this.certError = 'Please enter a name for this certificate.';
      return;
    }

    const safeName = `${this.profId}_${Date.now()}_${this.pendingCertFile.name.replace(/\s+/g, '_')}`;
    const assetPath = `assets/certificates/${safeName}`;

    // Persist the base64 preview to localStorage so it survives save/reload
    if (this.pendingCertPreview) {
      this.savePreviewToStorage(assetPath, this.pendingCertPreview);
    }

    const entry: CertEntry = {
      name,
      assetPath,
      isPdf: this.pendingCertIsPdf,
      previewUrl: this.pendingCertPreview,
      file: this.pendingCertFile
    };

    this.certificates = [...this.certificates, entry];
    this.syncCertsToForm();

    // Reset pending state
    this.pendingCertFile = null;
    this.pendingCertPreview = null;
    this.pendingCertIsPdf = false;
    this.pendingCertName = '';
    this.certError = '';
  }

  cancelPendingCert(): void {
    this.pendingCertFile = null;
    this.pendingCertPreview = null;
    this.pendingCertIsPdf = false;
    this.pendingCertName = '';
    this.certError = '';
  }

  removeCertificate(index: number): void {
    const removed = this.certificates[index];
    if (removed) {
      this.removePreviewFromStorage(removed.assetPath);
      if (this.viewingCert === removed) this.viewingCert = null;
    }
    this.certificates = this.certificates.filter((_, i) => i !== index);
    this.syncCertsToForm();
  }

  private syncCertsToForm(): void {
    this.form.patchValue({ certifications: this.serialiseCertificates() });
  }

  // ── Certificate viewer ───────────────────────────────────────────────────
  viewCertificate(cert: CertEntry): void {
    this.viewingCert = cert;
  }

  closeViewer(): void {
    this.viewingCert = null;
  }

  // ── Save profile ─────────────────────────────────────────────────────────
  save(): void {
    if (this.form.invalid) return;
    this.saving = true;
    this.profileService.updateProfile(this.profId, this.form.value).subscribe({
      next: data => {
        this.profile = data;
        // Re-parse from DB response — previews are restored from localStorage
        // (which we populated in addCertificate), so the viewer keeps working.
        this.certificates = this.parseCertificates(data.certifications);
        this.editMode = false;
        this.success = 'Profile updated!';
        this.saving = false;
        setTimeout(() => this.success = '', 3000);
      },
      error: () => { this.error = 'Failed to save.'; this.saving = false; }
    });
  }

  // ── Photo upload ─────────────────────────────────────────────────────────
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
    if (checked && !this.form.value.travelRadiusKm) {
      this.form.patchValue({ travelRadiusKm: 5 });
    }
    if (!checked) {
      this.form.patchValue({ travelRadiusKm: 0 });
    }
  }
}
