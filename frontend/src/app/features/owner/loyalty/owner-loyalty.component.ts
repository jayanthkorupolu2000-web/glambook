import { Component, OnInit } from '@angular/core';
import { LoyaltyResponse } from '../../../models/owner.model';
import { OwnerIdService } from '../../../services/owner-id.service';
import { OwnerLoyaltyService } from '../../../services/owner-loyalty.service';

@Component({
  selector: 'app-owner-loyalty',
  templateUrl: './owner-loyalty.component.html'
})
export class OwnerLoyaltyComponent implements OnInit {
  loyalty: (LoyaltyResponse & { editPoints: number })[] = [];
  loading = false;
  ownerId = 0;

  constructor(private loyaltyService: OwnerLoyaltyService, private ownerIdService: OwnerIdService) {}

  ngOnInit(): void {
    this.ownerIdService.getOwnerId().subscribe(id => {
      this.ownerId = id;
      this.load();
    });
  }

  load(): void {
    this.loading = true;
    this.loyaltyService.getLoyalty(this.ownerId).subscribe({
      next: data => { this.loyalty = data.map(l => ({ ...l, editPoints: 0 })); this.loading = false; },
      error: () => this.loading = false
    });
  }

  save(item: LoyaltyResponse & { editPoints: number }): void {
    this.loyaltyService.updatePoints(this.ownerId, item.customerId, item.editPoints).subscribe({
      next: updated => {
        const idx = this.loyalty.findIndex(l => l.customerId === item.customerId);
        if (idx !== -1) this.loyalty[idx] = { ...updated, editPoints: 0 };
      },
      error: () => alert('Failed to update points.')
    });
  }

  tierBadge(tier: string): string {
    const map: Record<string, string> = { BRONZE: 'secondary', SILVER: 'light text-dark', GOLD: 'warning' };
    return `badge bg-${map[tier] ?? 'secondary'}`;
  }
}
