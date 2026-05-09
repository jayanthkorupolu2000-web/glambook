import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { SalonOwner } from '../../../models';
import { OwnerIdService } from '../../../services/owner-id.service';

const API_BASE = 'http://localhost:8080';

@Component({
  selector: 'app-owner-profile',
  templateUrl: './owner-profile.component.html'
})
export class OwnerProfileComponent implements OnInit {
  profile: SalonOwner | null = null;
  loading = false;
  error: string | null = null;

  constructor(private http: HttpClient, private ownerIdService: OwnerIdService) {}

  ngOnInit(): void {
    this.ownerIdService.getOwnerId().subscribe(id => {
      if (!id) { this.error = 'Could not determine owner ID.'; return; }
      this.loading = true;
      this.http.get<SalonOwner>(`${API_BASE}/api/owners/${id}/profile`).subscribe({
        next: data => { this.profile = data; this.loading = false; },
        error: () => { this.error = 'Failed to load profile.'; this.loading = false; }
      });
    });
  }
}
