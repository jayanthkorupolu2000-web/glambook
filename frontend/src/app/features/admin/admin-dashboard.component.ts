import { Component } from '@angular/core';

export interface DashCard {
  icon: string;          // FA icon class e.g. 'fa-solid fa-users'
  title: string;
  desc: string;
  route: string;
  route2?: string;       // second route for Reports & Analytics split button
  btn2Label?: string;    // label for second button
  colorClass: string;    // ad-card--blue | --pink | --coral | --amber | --navy | --teal
  tags?: { label: string; type: 'men' | 'women' }[];
  hover: boolean;
}

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent {

  cards: DashCard[] = [
    {
      icon: 'fa-solid fa-users',
      title: 'User Management',
      desc: 'View all users',
      route: '/dashboard/admin/users',
      colorClass: 'blue',
      tags: [
        { label: 'Men',   type: 'men'   },
        { label: 'Women', type: 'women' }
      ],
      hover: false
    },
    {
      icon: 'fa-solid fa-store',
      title: 'Salon Owners',
      desc: 'Manage salon owners',
      route: '/dashboard/admin/owners',
      colorClass: 'pink',
      tags: [
        { label: 'Beauty',   type: 'women' },
        { label: 'Grooming', type: 'men'   }
      ],
      hover: false
    },
    {
      icon: 'fa-solid fa-chart-line',
      title: 'Reports & Analytics',
      desc: 'Appointments, payments & insights',
      route: '/dashboard/admin/reports',
      route2: '/dashboard/admin/analytics',
      btn2Label: 'Analytics',
      colorClass: 'coral',
      hover: false
    },
    {
      icon: 'fa-solid fa-comment-exclamation',
      title: 'Complaints',
      desc: 'Review & resolve complaints',
      route: '/dashboard/admin/complaints',
      colorClass: 'amber',
      hover: false
    },
    {
      icon: 'fa-solid fa-user-lock',
      title: 'Suspensions',
      desc: 'Manage user suspensions',
      route: '/dashboard/admin/user-management',
      colorClass: 'navy',
      hover: false
    },
    {
      icon: 'fa-solid fa-file-shield',
      title: 'Policies',
      desc: 'Publish & manage policies',
      route: '/dashboard/admin/policy',
      colorClass: 'teal',
      hover: false
    }
  ];
}
