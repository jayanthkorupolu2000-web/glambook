import { Component, OnInit, ViewEncapsulation } from '@angular/core';
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
  styleUrls: ['./prof-profile.component.css'],
  encapsulation: ViewEncapsulation.None,
  styles: [`
    .pp-page{min-height:100vh;background:#fdf2f4;padding:0 0 3rem;font-family:'DM Sans','Inter',system-ui,sans-serif;}
    .pp-hero{background:linear-gradient(135deg,#0d1b3e 0%,#1a3a6e 60%,#e8476a 100%);padding:2rem 2rem 2.5rem;position:relative;overflow:hidden;}
    .pp-hero::after{content:'';position:absolute;right:-60px;top:-60px;width:260px;height:260px;border-radius:50%;background:rgba(232,71,106,0.18);pointer-events:none;}
    .pp-hero__inner{max-width:1200px;margin:0 auto;display:flex;align-items:center;justify-content:space-between;gap:1.5rem;flex-wrap:wrap;}
    .pp-hero__eyebrow{font-size:0.72rem;font-weight:700;letter-spacing:0.10em;text-transform:uppercase;color:rgba(255,255,255,0.60);margin-bottom:0.3rem;}
    .pp-hero__title{font-size:1.85rem;font-weight:800;color:#ffffff;letter-spacing:-0.03em;margin-bottom:0.3rem;line-height:1.15;}
    .pp-hero__sub{font-size:0.88rem;color:rgba(255,255,255,0.68);margin:0;}
    .pp-hero__actions{display:flex;align-items:center;gap:0.65rem;flex-wrap:wrap;padding-top:0.25rem;}
    .pp-edit-btn{display:inline-flex;align-items:center;padding:0.6rem 1.25rem;border-radius:12px;border:1.5px solid rgba(255,255,255,0.35);background:rgba(255,255,255,0.12);color:#ffffff;font-size:0.85rem;font-weight:700;cursor:pointer;transition:background 0.18s ease;}
    .pp-edit-btn:hover{background:rgba(255,255,255,0.22);}
    .pp-cancel-btn{display:inline-flex;align-items:center;padding:0.55rem 1.1rem;border-radius:10px;border:1.5px solid rgba(255,255,255,0.30);background:rgba(255,255,255,0.10);color:#ffffff;font-size:0.85rem;font-weight:600;cursor:pointer;}
    .pp-save-btn{display:inline-flex;align-items:center;padding:0.55rem 1.1rem;border-radius:10px;border:none;background:#0d9488;color:#ffffff;font-size:0.85rem;font-weight:700;cursor:pointer;box-shadow:0 4px 12px rgba(13,148,136,0.35);}
    .pp-save-btn:hover:not(:disabled){background:#0f766e;}
    .pp-save-btn:disabled{opacity:0.45;cursor:not-allowed;}
    .pp-alerts{max-width:1200px;margin:1rem auto 0;padding:0 1.5rem;display:flex;flex-direction:column;gap:0.5rem;}
    .pp-alert{padding:0.65rem 1rem;border-radius:10px;font-size:0.85rem;font-weight:600;display:flex;align-items:center;}
    .pp-alert--success{background:rgba(22,163,74,0.10);border:1px solid rgba(22,163,74,0.28);color:#15803d;}
    .pp-alert--danger{background:rgba(220,38,38,0.10);border:1px solid rgba(220,38,38,0.28);color:#b91c1c;}
    .pp-loading{display:flex;align-items:center;justify-content:center;gap:0.75rem;padding:3rem;color:#64748b;font-weight:600;}
    .pp-spinner{width:26px;height:26px;border:3px solid #e2e8f0;border-top-color:#0d9488;border-radius:50%;animation:pp-spin 0.7s linear infinite;}
    @keyframes pp-spin{to{transform:rotate(360deg);}}
    .pp-card{max-width:1200px;margin:1.5rem auto 0;padding:0 1.5rem;}
    .pp-card__layout{background:#ffffff;border-radius:20px;box-shadow:0 4px 24px rgba(13,27,62,0.10),0 1px 0 rgba(255,255,255,0.9) inset;border:1.5px solid #cbd5e1;padding:0;display:flex;overflow:hidden;}
    .pp-sidebar{width:200px;flex-shrink:0;display:flex;flex-direction:column;align-items:center;text-align:center;padding:2rem 1.25rem;background:linear-gradient(180deg,#f1f5f9 0%,#e8edf5 100%);border-right:2px solid #cbd5e1;}
    .pp-avatar-wrap{margin-bottom:0.85rem;}
    .pp-avatar-img{width:96px;height:96px;border-radius:50%;object-fit:cover;border:3px solid #ffffff;box-shadow:0 4px 16px rgba(13,27,62,0.15);}
    .pp-avatar-initials{width:96px;height:96px;border-radius:50%;background:linear-gradient(135deg,#0d1b3e,#1a3a6e);color:#ffffff;font-size:2.2rem;font-weight:800;display:flex;align-items:center;justify-content:center;box-shadow:0 4px 16px rgba(13,27,62,0.25);}
    .pp-choose-photo-btn{display:inline-flex;align-items:center;padding:0.38rem 0.85rem;border-radius:8px;border:1.5px solid #e2e8f0;background:#ffffff;color:#334155;font-size:0.78rem;font-weight:600;cursor:pointer;margin-bottom:0.5rem;transition:border-color 0.15s ease;}
    .pp-choose-photo-btn:hover{border-color:#0d9488;background:#f0fdfa;}
    .pp-upload-btn{display:inline-flex;align-items:center;padding:0.38rem 0.85rem;border-radius:8px;border:none;background:#0d9488;color:#ffffff;font-size:0.78rem;font-weight:700;cursor:pointer;margin-bottom:0.5rem;box-shadow:0 3px 8px rgba(13,148,136,0.30);}
    .pp-stars-row{display:flex;gap:2px;margin-top:0.65rem;}
    .pp-star{font-size:1.15rem;color:#f5a623;}
    .pp-star--off{color:#e2e8f0;}
    .pp-rating-text{font-size:0.75rem;color:#94a3b8;margin-top:0.25rem;font-weight:500;}
    .pp-status-pill{display:inline-flex;align-items:center;font-size:0.70rem;font-weight:800;padding:0.25rem 0.75rem;border-radius:999px;margin-top:0.65rem;letter-spacing:0.04em;}
    .pp-status-pill--active{background:rgba(22,163,74,0.12);color:#15803d;border:1px solid rgba(22,163,74,0.30);}
    .pp-status-pill--pending{background:rgba(217,119,6,0.12);color:#b45309;border:1px solid rgba(217,119,6,0.30);}
    .pp-status-pill--other{background:rgba(220,38,38,0.10);color:#b91c1c;border:1px solid rgba(220,38,38,0.25);}
    .pp-main{flex:1;min-width:0;padding:0;}
    .pp-info-grid{display:grid;grid-template-columns:1fr 1fr;gap:0;}
    .pp-info-item{padding:1rem 1.5rem;border-bottom:1px solid #e2e8f0;border-right:1px solid #e2e8f0;}
    .pp-info-item:nth-child(even){border-right:none;}
    .pp-info-item--full{grid-column:1/-1;border-right:none;}
    .pp-info-label{font-size:0.68rem;font-weight:800;text-transform:uppercase;letter-spacing:0.08em;color:#94a3b8;margin-bottom:0.3rem;display:block;}
    .pp-info-value{font-size:0.95rem;font-weight:600;color:#0d1b3e;margin:0;line-height:1.4;}
    .pp-info-value--email{color:#1e6fd9;}
    .pp-info-value--muted{font-weight:400;color:#64748b;}
    .pp-divider{height:2px;background:#e2e8f0;margin:0;}
    .pp-section{padding:1.25rem 1.5rem;}
    .pp-section-title{font-size:0.78rem;font-weight:800;text-transform:uppercase;letter-spacing:0.07em;color:#475569;margin-bottom:0.85rem;display:flex;align-items:center;padding-bottom:0.6rem;border-bottom:2px solid #e2e8f0;}
    .pp-section-title .fa-solid{color:#0d9488;margin-right:0.5rem;}
    .pp-empty-text{font-size:0.85rem;color:#94a3b8;padding:0.25rem 0;}
    .pp-cert-list{display:flex;flex-direction:column;gap:0.5rem;}
    .pp-cert-row{display:flex;align-items:center;gap:0.65rem;padding:0.6rem 0.85rem;background:#f8fafc;border-radius:10px;border:1.5px solid #e2e8f0;transition:border-color 0.15s ease;}
    .pp-cert-row:hover{border-color:#0d9488;}
    .pp-cert-icon{width:32px;height:32px;border-radius:8px;display:flex;align-items:center;justify-content:center;flex-shrink:0;}
    .pp-cert-icon--img{background:rgba(46,125,50,0.10);}
    .pp-cert-icon--pdf{background:rgba(198,40,40,0.10);}
    .pp-cert-icon svg{width:16px;height:16px;}
    .pp-cert-name{flex:1;font-size:0.85rem;font-weight:600;color:#334155;}
    .pp-cert-view-btn{padding:0.25rem 0.65rem;border-radius:6px;border:1px solid rgba(13,148,136,0.30);background:rgba(13,148,136,0.08);color:#0d9488;font-size:0.75rem;font-weight:700;cursor:pointer;}
    .pp-cert-remove-btn{padding:0.25rem 0.5rem;border-radius:6px;border:1px solid rgba(220,38,38,0.25);background:rgba(220,38,38,0.08);color:#dc2626;font-size:0.75rem;cursor:pointer;}
    .pp-avail-pill{display:inline-flex;align-items:center;gap:0.35rem;font-size:0.82rem;font-weight:600;padding:0.25rem 0.65rem;border-radius:999px;}
    .pp-avail-pill--yes{background:rgba(22,163,74,0.10);color:#15803d;}
    .pp-avail-pill--no{background:#f1f5f9;color:#94a3b8;}
    .pp-avail-dot{width:7px;height:7px;border-radius:50%;background:currentColor;}
    .pp-avail-radius{font-weight:400;color:#64748b;font-size:0.75rem;}
    .pp-home-info-box{background:rgba(13,148,136,0.06);border:1px solid rgba(13,148,136,0.20);border-radius:10px;padding:0.75rem 1rem;font-size:0.83rem;color:#0d9488;}
    .pp-home-info-box .fa-solid{color:#0d9488;}
    .pp-service-area-list{display:flex;flex-direction:column;gap:0.3rem;max-height:180px;overflow-y:auto;padding:0.5rem;background:#f8fafc;border-radius:10px;border:1px solid #f1f5f9;}
    .pp-pending-cert{background:#f8fafc;border:1px solid #f1f5f9;border-radius:12px;padding:1rem;margin-bottom:0.75rem;}
    .pp-pending-cert__file{font-size:0.85rem;font-weight:600;color:#334155;margin-bottom:0.5rem;}
    .pp-pending-cert__size{color:#94a3b8;font-weight:400;}
    .pp-pending-cert__preview{max-width:100%;max-height:150px;border-radius:8px;margin-bottom:0.5rem;display:block;}
    .pp-pending-cert__pdf-note{font-size:0.80rem;color:#94a3b8;font-style:italic;margin-bottom:0.5rem;}
    .pp-add-cert-btn{display:inline-flex;align-items:center;padding:0.45rem 1rem;border-radius:8px;border:1.5px dashed #0d9488;background:rgba(13,148,136,0.05);color:#0d9488;font-size:0.82rem;font-weight:700;cursor:pointer;}
    .pp-viewer-overlay{position:fixed;inset:0;background:rgba(13,27,62,0.60);z-index:3000;display:flex;align-items:center;justify-content:center;padding:1rem;}
    .pp-viewer-card{background:#ffffff;border-radius:20px;box-shadow:0 20px 60px rgba(0,0,0,0.25);width:100%;max-width:600px;overflow:hidden;}
    .pp-viewer-header{display:flex;align-items:center;justify-content:space-between;padding:1rem 1.5rem;border-bottom:1px solid #f1f5f9;background:rgba(13,27,62,0.03);}
    .pp-viewer-title{display:flex;align-items:center;gap:0.65rem;font-size:0.92rem;font-weight:700;color:#0d1b3e;}
    .pp-viewer-close{background:#f1f5f9;border:none;width:28px;height:28px;border-radius:50%;cursor:pointer;font-size:0.85rem;color:#64748b;}
    .pp-viewer-img{width:100%;max-height:500px;object-fit:contain;display:block;}
    .pp-viewer-fallback{display:flex;flex-direction:column;align-items:center;padding:2.5rem;gap:0.75rem;text-align:center;color:#64748b;font-size:0.85rem;}
    .pp-viewer-cert-title{font-size:1rem;font-weight:700;color:#0d1b3e;margin:0;}
    .pp-fallback-icon{width:60px;height:60px;border-radius:16px;display:flex;align-items:center;justify-content:center;}
    .pp-fallback-icon--img{background:rgba(46,125,50,0.10);}
    .pp-fallback-icon--pdf{background:rgba(198,40,40,0.10);}
    .pp-fallback-icon svg{width:30px;height:30px;}
    @media(max-width:768px){.pp-card__layout{flex-direction:column;} .pp-sidebar{width:100%;} .pp-info-grid{grid-template-columns:1fr;}}
  `]
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
