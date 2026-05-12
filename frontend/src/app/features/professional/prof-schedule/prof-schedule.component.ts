import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../../services/auth.service';
import { ScheduleService, ScheduleSlot } from '../../../services/schedule.service';

@Component({
  selector: 'app-prof-schedule',
  templateUrl: './prof-schedule.component.html',
  styleUrls: ['./prof-schedule.component.scss']
})
export class ProfScheduleComponent implements OnInit {
  todaySlots: ScheduleSlot[] = [];
  selectedDateSlots: ScheduleSlot[] = [];
  loading = false;
  dateLoading = false;
  error = '';
  profId = 0;

  today = new Date().toISOString().split('T')[0];
  selectedDate = '';

  constructor(private scheduleService: ScheduleService, private auth: AuthService) {}

  ngOnInit(): void {
    this.profId = this.auth.getUserId() || 0;
    this.loadToday();
  }

  loadToday(): void {
    this.loading = true;
    this.scheduleService.getTodaySchedule(this.profId).subscribe({
      next: slots => { this.todaySlots = slots; this.loading = false; },
      error: () => { this.error = 'Failed to load today\'s schedule.'; this.loading = false; }
    });
  }

  onDateChange(): void {
    if (!this.selectedDate) return;
    this.dateLoading = true;
    this.scheduleService.getSlotsByDate(this.profId, this.selectedDate).subscribe({
      next: slots => { this.selectedDateSlots = slots; this.dateLoading = false; },
      error: () => { this.dateLoading = false; }
    });
  }

  statusClass(slot: ScheduleSlot): string {
    switch (slot.slotStatus) {
      case 'AVAILABLE':   return 'badge-available';
      case 'UNAVAILABLE': return slot.past ? 'badge-past' : 'badge-booked';
      case 'COMPLETED':   return 'badge-completed';
      default:            return 'badge-booked';
    }
  }

  statusLabel(slot: ScheduleSlot): string {
    if (slot.slotStatus === 'COMPLETED')   return '✓ Completed';
    if (slot.slotStatus === 'UNAVAILABLE') return slot.past ? 'Past' : 'Booked';
    return 'Available';
  }

  slotTypeIcon(type: string): string {
    const map: Record<string, string> = {
      WORKING:     '🕐',
      LUNCH_BREAK: '🍽️',
      BREAK:       '☕',
      BLOCKED:     '🚫'
    };
    return map[type] ?? '📌';
  }

  slotTypeLabel(type: string): string {
    const map: Record<string, string> = {
      WORKING:     'Working Hours',
      LUNCH_BREAK: 'Lunch Break',
      BREAK:       'Short Break',
      BLOCKED:     'Blocked'
    };
    return map[type] ?? type;
  }

  formatTime(t: string): string {
    if (!t) return '';
    const [h, m] = t.split(':');
    const hour = parseInt(h);
    return `${hour % 12 || 12}:${m} ${hour >= 12 ? 'PM' : 'AM'}`;
  }

  get availableCount(): number {
    return this.todaySlots.filter(s => s.slotStatus === 'AVAILABLE').length;
  }

  get bookedCount(): number {
    return this.todaySlots.filter(s => s.booked).length;
  }

  get totalRemainingSlots(): number {
    // Only count slots that are not completed and not past
    return this.todaySlots.filter(s => 
      s.slotStatus !== 'COMPLETED' && !s.past
    ).length;
  }
}
