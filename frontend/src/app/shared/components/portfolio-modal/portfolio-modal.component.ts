import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { PortfolioItem, PortfolioService } from '../../../services/portfolio.service';

@Component({
  selector: 'app-portfolio-modal',
  templateUrl: './portfolio-modal.component.html',
  styleUrls: ['./portfolio-modal.component.scss']
})
export class PortfolioModalComponent implements OnChanges {
  /** The professional whose portfolio to show. Set to null to hide the modal. */
  @Input() professionalId: number | null = null;
  @Input() professionalName = '';

  /** Emitted when the user closes the modal */
  @Output() closed = new EventEmitter<void>();

  items: PortfolioItem[] = [];
  loading = false;
  error = '';

  // Lightbox state
  lightboxItem: PortfolioItem | null = null;
  lightboxSide: 'before' | 'after' = 'before';

  constructor(private portfolioService: PortfolioService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['professionalId'] && this.professionalId != null) {
      this.load();
    }
  }

  load(): void {
    if (!this.professionalId) return;
    this.loading = true;
    this.error = '';
    this.items = [];
    this.portfolioService.getPortfolioByProfessional(this.professionalId).subscribe({
      next: data => { this.items = Array.isArray(data) ? data : []; this.loading = false; },
      error: () => { this.error = 'Failed to load portfolio.'; this.loading = false; }
    });
  }

  close(): void {
    this.lightboxItem = null;
    this.closed.emit();
  }

  openLightbox(item: PortfolioItem, side: 'before' | 'after' = 'before'): void {
    this.lightboxItem = item;
    this.lightboxSide = side;
  }

  closeLightbox(): void { this.lightboxItem = null; }

  /** Resolve a portfolio item's display URL.
   *  Priority: localStorage data-URL → /api/v1/files/ server path → /assets/ path */
  url(path: string | null | undefined): string {
    if (!path) return '';
    // Check localStorage for a stored data-URL (same browser as professional)
    try {
      const store = localStorage.getItem('portfolio_assets');
      if (store) {
        const parsed = JSON.parse(store) as Record<string, string>;
        if (parsed[path]) return parsed[path];
      }
    } catch { /* ignore */ }
    // Already a full URL or data-URL
    if (path.startsWith('http') || path.startsWith('data:')) return path;
    // Backend file path — prefix with server base
    if (path.startsWith('/api/') || path.startsWith('/uploads/')) {
      return 'http://localhost:8080' + path;
    }
    // Angular assets path
    return path;
  }

  /** Primary display URL for a single-photo or video item */
  primaryUrl(item: PortfolioItem): string {
    // Prefer assets filePath
    if (item.filePath) return this.url(item.filePath);
    // Fall back to legacy upload paths
    if (item.photoUrl) return this.url(item.photoUrl);
    if (item.videoUrl) return this.url(item.videoUrl);
    return '';
  }

  get lightboxUrl(): string {
    if (!this.lightboxItem) return '';
    if (this.lightboxItem.mediaType === 'BEFORE_AFTER_PHOTO') {
      return this.lightboxSide === 'before'
        ? this.url(this.lightboxItem.beforePhotoUrl)
        : this.url(this.lightboxItem.afterPhotoUrl);
    }
    return this.primaryUrl(this.lightboxItem);
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

  get photoItems(): PortfolioItem[] {
    return this.items.filter(i => i.mediaType !== 'VIDEO_CLIP');
  }

  get videoItems(): PortfolioItem[] {
    return this.items.filter(i => i.mediaType === 'VIDEO_CLIP');
  }

  /** Resolve before-photo URL for a pair item */
  beforeUrl(item: PortfolioItem): string {
    return this.url(item.beforePhotoUrl || item.filePath);
  }

  /** Resolve after-photo URL for a pair item */
  afterUrl(item: PortfolioItem): string {
    return this.url(item.afterPhotoUrl);
  }
}
