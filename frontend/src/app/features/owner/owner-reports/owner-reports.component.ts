import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { OwnerReportResponse } from '../../../models/owner.model';
import { OwnerIdService } from '../../../services/owner-id.service';
import { OwnerReportService } from '../../../services/owner-report.service';

@Component({
  selector: 'app-owner-reports',
  templateUrl: './owner-reports.component.html',
  styleUrls: ['./owner-reports.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class OwnerReportsComponent implements OnInit {
  report: OwnerReportResponse | null = null;
  loading = false;

  readonly C = 2 * Math.PI * 48; // circumference for r=48 → 301.6

  constructor(private reportService: OwnerReportService, private ownerIdService: OwnerIdService) {}

  ngOnInit(): void {
    this.ownerIdService.getOwnerId().subscribe(id => {
      this.loading = true;
      this.reportService.getReport(id).subscribe({
        next: data => { this.report = data; this.loading = false; },
        error: () => this.loading = false
      });
    });
  }

  // ── Appointment donut ──────────────────────────────────────────
  get apptDonut(): { completed: number; cancelled: number; pending: number; total: number } {
    if (!this.report) return { completed: 0, cancelled: 0, pending: 0, total: 0 };
    const total = this.report.totalAppointments || 0;
    const completed = this.report.completedAppointments || 0;
    const cancelled = this.report.cancelledAppointments || 0;
    const pending = Math.max(0, total - completed - cancelled);
    return { completed, cancelled, pending, total };
  }

  donutDash(value: number, total: number): string {
    if (!total) return `0 ${this.C}`;
    return `${(value / total) * this.C} ${this.C}`;
  }

  donutOffset(prev: number, total: number): number {
    if (!total) return 0;
    return -(prev / total) * this.C;
  }

  // ── Completion rate ────────────────────────────────────────────
  get completionRate(): number {
    if (!this.report?.totalAppointments) return 0;
    return Math.round((this.report.completedAppointments / this.report.totalAppointments) * 100);
  }

  // ── Complaint donut ────────────────────────────────────────────
  get complaintTotal(): number {
    if (!this.report) return 0;
    return (this.report.openComplaints || 0)
         + (this.report.forwardedComplaints || 0)
         + (this.report.resolvedComplaints || 0);
  }

  // ── Bar chart: appointments vs revenue (normalised) ───────────
  get revenueBarPct(): number {
    if (!this.report?.totalRevenue) return 0;
    // Scale: cap at 100% for display — use 10000 as reference max
    return Math.min(100, Math.round((this.report.totalRevenue / 10000) * 100));
  }

  // ── Rating stars ───────────────────────────────────────────────
  stars(n: number): number[] { return [1, 2, 3, 4, 5]; }

  ratingPct(rating: number): number {
    return Math.round((rating / 5) * 100);
  }

  // ── Line chart (simulated trend from report data) ──────────────
  readonly lineLabels = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

  // Chart area: x from 40→290, y from 10→100 (baseline)
  private readonly chartLeft   = 40;
  private readonly chartRight  = 290;
  private readonly chartTop    = 10;
  private readonly chartBottom = 100;

  get lineRawValues(): number[] {
    if (!this.report) return [0, 0, 0, 0, 0, 0, 0];
    const total = this.report.totalAppointments || 0;
    const base = total / 7;
    return [
      Math.round(base * 0.6),
      Math.round(base * 0.8),
      Math.round(base * 1.2),
      Math.round(base * 1.0),
      Math.round(base * 1.4),
      Math.round(base * 1.1),
      Math.round(base * 0.9)
    ];
  }

  get linePoints(): { x: number; y: number; val: number }[] {
    const vals = this.lineRawValues;
    const max = Math.max(...vals, 1);
    const h = this.chartBottom - this.chartTop;
    const w = this.chartRight - this.chartLeft;
    return vals.map((v, i) => ({
      x: this.chartLeft + (i / (vals.length - 1)) * w,
      y: this.chartBottom - (v / max) * h,
      val: v
    }));
  }

  get yTicks(): { y: number; label: string }[] {
    const vals = this.lineRawValues;
    const max = Math.max(...vals, 1);
    const h = this.chartBottom - this.chartTop;
    return [0, 0.25, 0.5, 0.75, 1].map(pct => ({
      y: this.chartBottom - pct * h,
      label: Math.round(pct * max).toString()
    }));
  }

  linePath(area: boolean): string {
    const pts = this.linePoints;
    if (!pts.length) return '';
    const d = pts.map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ');
    if (!area) return d;
    const last = pts[pts.length - 1];
    const first = pts[0];
    return `${d} L${last.x.toFixed(1)},${this.chartBottom} L${first.x.toFixed(1)},${this.chartBottom} Z`;
  }

  get Math() { return Math; }
}
