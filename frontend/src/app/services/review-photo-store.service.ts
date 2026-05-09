import { Injectable } from '@angular/core';

const STORAGE_KEY = 'review_photos';

@Injectable({ providedIn: 'root' })
export class ReviewPhotoStoreService {

  /** Save an array of data-URL strings for a given reviewId */
  savePhotos(reviewId: number, dataUrls: string[]): void {
    const all = this.loadAll();
    all[reviewId] = dataUrls;
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(all));
    } catch {
      // quota exceeded — trim oldest entries (keep last 50 reviews)
      const keys = Object.keys(all).map(Number).sort((a, b) => a - b);
      while (keys.length > 50) {
        delete all[keys.shift()!];
      }
      all[reviewId] = dataUrls;
      localStorage.setItem(STORAGE_KEY, JSON.stringify(all));
    }
  }

  /** Append new data-URLs to an existing review's photos */
  appendPhotos(reviewId: number, dataUrls: string[]): void {
    const existing = this.getPhotos(reviewId);
    this.savePhotos(reviewId, [...existing, ...dataUrls]);
  }

  /** Get stored data-URLs for a reviewId (empty array if none) */
  getPhotos(reviewId: number): string[] {
    return this.loadAll()[reviewId] ?? [];
  }

  /** Merge stored photos into a list of review objects */
  mergeIntoReviews<T extends { id: number; photos?: string[] }>(reviews: T[]): T[] {
    const all = this.loadAll();
    return reviews.map(r => {
      const stored = all[r.id] ?? [];
      if (stored.length === 0) return r;
      return { ...r, photos: [...(r.photos ?? []), ...stored] };
    });
  }

  private loadAll(): Record<number, string[]> {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? JSON.parse(raw) : {};
    } catch {
      return {};
    }
  }
}
