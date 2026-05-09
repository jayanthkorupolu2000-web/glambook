import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../../services/auth.service';
import { PortfolioItem, PortfolioService } from '../../../services/portfolio.service';

@Component({
  selector: 'app-prof-portfolio',
  templateUrl: './prof-portfolio.component.html',
  styleUrls: ['./prof-portfolio.component.scss']
})
export class ProfPortfolioComponent implements OnInit {
  items: PortfolioItem[] = [];
  filtered: PortfolioItem[] = [];
  activeTab = 'ALL';
  loading = false;
  showModal = false;
  submitting = false;
  profId = 0;
  error = '';
  success = '';

  // ── Unified form ──────────────────────────────────────────────────────────
  form: FormGroup;

  // Mode: 'single' | 'pair'
  uploadMode: 'single' | 'pair' = 'single';

  // Preview state
  singlePreview: string | null = null;
  beforePreview: string | null = null;
  afterPreview:  string | null = null;
  isVideo = false;

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private portfolioService: PortfolioService
  ) {
    this.form = this.fb.group({
      uploadMode:   ['single'],
      filePath:     [''],
      beforePath:   [''],
      afterPath:    [''],
      serviceTag:   ['', Validators.required],
      tags:         [''],
      caption:      [''],
      testimonial:  [''],
      featured:     [false]
    });
  }

  ngOnInit(): void {
    this.profId = this.auth.getUserId() || 0;
    this.load();

    // Keep uploadMode in sync with the form control
    this.form.get('uploadMode')?.valueChanges.subscribe(mode => {
      this.uploadMode = mode;
      this.clearPreviews();
    });
  }

  // ── Load from backend ─────────────────────────────────────────────────────

  load(): void {
    this.loading = true;
    this.portfolioService.getPortfolioByProfessional(this.profId).subscribe({
      next: data => {
        this.items = Array.isArray(data) ? data : [];
        this.applyTab(this.activeTab);
        this.loading = false;
      },
      error: () => { this.items = []; this.loading = false; }
    });
  }

  applyTab(tab: string): void {
    this.activeTab = tab;
    if (tab === 'ALL')            this.filtered = [...this.items];
    else if (tab === 'BEFORE_AFTER') this.filtered = this.items.filter(i => i.mediaType === 'BEFORE_AFTER_PHOTO');
    else if (tab === 'VIDEO')     this.filtered = this.items.filter(i => i.mediaType === 'VIDEO_CLIP');
    else if (tab === 'FEATURED')  this.filtered = this.items.filter(i => i.isFeatured);
  }

  // ── File input handler ────────────────────────────────────────────────────

  onFileSelected(event: Event, slot: 'single' | 'before' | 'after'): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.error = '';

    // Validate size
    const maxMB = file.type.startsWith('video/') ? 30 : 5;
    if (file.size > maxMB * 1024 * 1024) {
      this.error = `File must be under ${maxMB} MB.`;
      return;
    }

    // Generate the assets path from the filename
    const safeName = file.name.replace(/[^a-zA-Z0-9._-]/g, '_');
    const assetPath = `/assets/portfolio/${safeName}`;

    // Read as data-URL for preview AND for persistent display
    const reader = new FileReader();
    reader.onload = e => {
      const dataUrl = e.target?.result as string;

      // Store data-URL in localStorage so it survives page refresh
      this.storeDataUrl(assetPath, dataUrl);

      if (slot === 'single') {
        this.singlePreview = dataUrl;
        this.isVideo = file.type.startsWith('video/');
      } else if (slot === 'before') {
        this.beforePreview = dataUrl;
      } else {
        this.afterPreview = dataUrl;
      }
    };
    reader.readAsDataURL(file);

    // Patch the form with the generated path
    if (slot === 'single')       this.form.patchValue({ filePath: assetPath });
    else if (slot === 'before')  this.form.patchValue({ beforePath: assetPath });
    else                         this.form.patchValue({ afterPath: assetPath });
  }

  /** Store a data-URL in localStorage keyed by asset path */
  private storeDataUrl(assetPath: string, dataUrl: string): void {
    try {
      const store = this.getDataUrlStore();
      store[assetPath] = dataUrl;
      localStorage.setItem('portfolio_assets', JSON.stringify(store));
    } catch { /* quota exceeded — skip */ }
  }

  /** Retrieve stored data-URLs */
  private getDataUrlStore(): Record<string, string> {
    try {
      const raw = localStorage.getItem('portfolio_assets');
      return raw ? JSON.parse(raw) : {};
    } catch { return {}; }
  }

  /** Resolve display URL: localStorage data-URL → server path → assets path */
  resolveUrl(path: string | null | undefined): string {
    if (!path) return '';
    // Check localStorage first (same browser session)
    try {
      const store = localStorage.getItem('portfolio_assets');
      if (store) {
        const parsed = JSON.parse(store) as Record<string, string>;
        if (parsed[path]) return parsed[path];
      }
    } catch { /* ignore */ }
    // Already a full URL or data-URL
    if (path.startsWith('http') || path.startsWith('data:')) return path;
    // Backend file path
    if (path.startsWith('/api/') || path.startsWith('/uploads/')) {
      return 'http://localhost:8080' + path;
    }
    // Angular assets path
    return path;
  }

  // ── Submit — saves metadata to backend in one step ────────────────────────

  submit(): void {
    this.error = '';
    if (this.form.get('serviceTag')?.invalid) {
      this.error = 'Service tag is required.';
      return;
    }

    const v = this.form.value;

    if (this.uploadMode === 'single' && !v.filePath) {
      this.error = 'Please select a file.';
      return;
    }
    if (this.uploadMode === 'pair' && (!v.beforePath || !v.afterPath)) {
      this.error = 'Both before and after photos are required.';
      return;
    }

    this.submitting = true;

    const dto = this.uploadMode === 'pair'
      ? { beforeDataUrl: this.beforePreview ?? undefined, afterDataUrl: this.afterPreview ?? undefined,
          serviceTag: v.serviceTag, tags: v.tags, caption: v.caption,
          testimonial: v.testimonial, featured: v.featured }
      : { dataUrl: this.singlePreview ?? undefined,
          serviceTag: v.serviceTag, tags: v.tags, caption: v.caption,
          testimonial: v.testimonial, featured: v.featured };

    this.portfolioService.addPortfolioItem(this.profId, dto).subscribe({
      next: () => {
        this.success = '✅ Portfolio item saved!';
        this.showModal = false;
        this.submitting = false;
        this.load();
      },
      error: (e) => {
        this.error = e?.error?.message || 'Failed to save. Please try again.';
        this.submitting = false;
      }
    });
  }

  // ── Delete ────────────────────────────────────────────────────────────────

  deleteItem(item: PortfolioItem): void {
    if (!confirm('Delete this portfolio item?')) return;
    this.portfolioService.deletePortfolioItem(this.profId, item.id).subscribe({
      next: () => this.load(),
      error: () => { this.error = 'Failed to delete.'; }
    });
  }

  // ── Modal helpers ─────────────────────────────────────────────────────────

  openModal(): void {
    this.form.reset({
      uploadMode: 'single', filePath: '', beforePath: '', afterPath: '',
      serviceTag: '', tags: '', caption: '', testimonial: '', featured: false
    });
    this.uploadMode = 'single';
    this.clearPreviews();
    this.error = '';
    this.success = '';
    this.showModal = true;
  }

  private clearPreviews(): void {
    this.singlePreview = this.beforePreview = this.afterPreview = null;
    this.isVideo = false;
  }

  // ── Display helpers ───────────────────────────────────────────────────────

  /** Resolve a portfolio item's primary display URL — checks localStorage first */
  displayUrl(item: PortfolioItem): string {
    const raw = item.filePath || item.photoUrl || item.videoUrl || item.beforePhotoUrl || '';
    return this.resolveUrl(raw);
  }

  /** Resolve before-photo URL for a pair item */
  beforeUrl(item: PortfolioItem): string {
    return this.resolveUrl(item.beforePhotoUrl || item.filePath);
  }

  /** Resolve after-photo URL for a pair item */
  afterUrl(item: PortfolioItem): string {
    return this.resolveUrl(item.afterPhotoUrl);
  }

  isVideoItem(item: PortfolioItem): boolean {
    return item.mediaType === 'VIDEO_CLIP';
  }

  isPairItem(item: PortfolioItem): boolean {
    return item.mediaType === 'BEFORE_AFTER_PHOTO';
  }

  onImgError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.style.display = 'none';
    const parent = img.parentElement;
    if (parent && !parent.querySelector('.pf-missing')) {
      const div = document.createElement('div');
      div.className = 'pf-missing';
      div.innerHTML = '<span>🖼️</span><small>Image not found</small>';
      parent.appendChild(div);
    }
  }
}
