import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class SidebarToggleService {
  private toggleSource = new Subject<void>();
  toggle$ = this.toggleSource.asObservable();

  toggle(): void {
    this.toggleSource.next();
  }
}
