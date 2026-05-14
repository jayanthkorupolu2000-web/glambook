import { Component } from '@angular/core';

@Component({
  selector: 'app-professional-dashboard',
  template: `
    <app-prof-sidebar (sidebarToggled)="onSidebarToggle($event)"></app-prof-sidebar>
    <div class="prof-content" [class.prof-content--shifted]="sidebarOpen">
      <router-outlet></router-outlet>
    </div>
  `,
  styles: [`
    .prof-content {
      min-height: 100vh;
      transition: margin-left .28s cubic-bezier(.4,0,.2,1);
      margin-left: 0;
    }
    @media (min-width: 992px) {
      .prof-content--shifted {
        margin-left: 260px;
      }
    }
  `]
})
export class ProfessionalDashboardComponent {
  sidebarOpen = true;

  onSidebarToggle(open: boolean): void {
    this.sidebarOpen = open;
  }
}
