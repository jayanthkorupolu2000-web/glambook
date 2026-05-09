import { Component, OnInit } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { AuthService } from '../../../services/auth.service';

const BASE = 'http://localhost:8080/api/v1/professionals';

interface Slot {
  id: number;
  availDate: string;
  startTime: string;   // "HH:mm" 24-hr from backend
  endTime: string;
  isBooked: boolean;
  slotType: 'WORKING' | 'LUNCH_BREAK' | 'BREAK' | 'BLOCKED';
}

@Component({
  selector: 'app-prof-availability',
  templateUrl: './prof-availability.component.html',
  styleUrls: ['./prof-availability.component.scss']
})
export class ProfAvailabilityComponent implements OnInit {
  slots: Slot[] = [];
  loading = false;
  submitting = false;
  profId = 0;
  error = '';
  success = '';
  deleteMsg = '';
  overlapWarning = '';   // orange overlap message

  // ── Form fields (bound with ngModel, not reactive form) ──────────────────
  availDate = '';
  startHour = '';    // '1'–'12'
  startMin  = '00';
  startAmPm = 'AM';
  slotType  = 'WORKING';

  // Calculated end time (display only)
  calculatedEndTime = '';   // "HH:mm" 24-hr
  calculatedEndDisplay = ''; // "h:mm AM/PM"

  // View date filter
  viewDate = '';

  // Hours 1–12, minutes 00/15/30/45
  hours   = ['1','2','3','4','5','6','7','8','9','10','11','12'];
  minutes = ['00','15','30','45'];

  // Default slot duration in minutes (used to auto-calc end time)
  defaultDurationMins = 60;
  professionalServices: any[] = [];

  slotTypes = [
    { value: 'WORKING',      label: 'Working Hours',  color: '#1a9e5c' },
    { value: 'LUNCH_BREAK',  label: 'Lunch Break',    color: '#c77c00' },
    { value: 'BREAK',        label: 'Short Break',    color: '#1565c0' },
    { value: 'BLOCKED',      label: 'Blocked',        color: '#c0392b' }
  ];

  constructor(private fb: FormBuilder, private auth: AuthService) {}

  ngOnInit(): void {
    this.profId = this.auth.getUserId() || 0;
    this.viewDate = new Date().toISOString().split('T')[0];
    this.load();
    this.loadServices();
  }

  // ── Load slots ────────────────────────────────────────────────────────────
  load(): void {
    this.loading = true;
    fetch(`${BASE}/${this.profId}/availability`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('auth_token')}` }
    }).then(r => r.json()).then(data => {
      this.slots = Array.isArray(data) ? data : [];
      this.loading = false;
    }).catch(() => this.loading = false);
  }

  // ── Load professional's services to get durations ─────────────────────────
  loadServices(): void {
    fetch(`${BASE}/${this.profId}/services?activeOnly=true`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('auth_token')}` }
    }).then(r => r.json()).then(data => {
      this.professionalServices = Array.isArray(data) ? data : [];
      if (this.professionalServices.length > 0) {
        // Use the shortest active service duration as the default slot size
        const durations = this.professionalServices
          .map((s: any) => s.durationMins || 60)
          .filter((d: number) => d > 0);
        this.defaultDurationMins = durations.length > 0 ? Math.min(...durations) : 60;
      }
    }).catch(() => {});
  }

  // ── 12-hr → 24-hr conversion ──────────────────────────────────────────────
  to24hr(hour: string, min: string, ampm: string): string {
    let h = parseInt(hour, 10);
    if (ampm === 'AM' && h === 12) h = 0;
    if (ampm === 'PM' && h !== 12) h += 12;
    return `${String(h).padStart(2, '0')}:${min}`;
  }

  // ── Recalculate end time whenever start changes ───────────────────────────
  onStartChange(): void {
    this.overlapWarning = '';
    if (!this.startHour || !this.startMin || !this.startAmPm) {
      this.calculatedEndTime = '';
      this.calculatedEndDisplay = '';
      return;
    }
    const start24 = this.to24hr(this.startHour, this.startMin, this.startAmPm);
    const [sh, sm] = start24.split(':').map(Number);
    const totalMins = sh * 60 + sm + this.defaultDurationMins;
    const eh = Math.floor(totalMins / 60) % 24;
    const em = totalMins % 60;
    this.calculatedEndTime = `${String(eh).padStart(2,'0')}:${String(em).padStart(2,'0')}`;
    this.calculatedEndDisplay = this.formatTime(this.calculatedEndTime);
  }

  // ── Overlap detection ─────────────────────────────────────────────────────
  private timeToMins(t: string): number {
    const [h, m] = t.split(':').map(Number);
    return h * 60 + m;
  }

  private checkOverlap(date: string, start24: string, end24: string): Slot | null {
    const newStart = this.timeToMins(start24);
    const newEnd   = this.timeToMins(end24);
    return this.slots.find(s =>
      s.availDate === date &&
      this.timeToMins(s.startTime) < newEnd &&
      this.timeToMins(s.endTime)   > newStart
    ) ?? null;
  }

  // ── Add slot ──────────────────────────────────────────────────────────────
  addSlot(): void {
    if (!this.availDate || !this.startHour || !this.startMin || !this.startAmPm) {
      this.error = 'Please fill in all fields.';
      return;
    }
    if (!this.calculatedEndTime) {
      this.error = 'Start time is required to calculate end time.';
      return;
    }

    const start24 = this.to24hr(this.startHour, this.startMin, this.startAmPm);
    const end24   = this.calculatedEndTime;

    // Overlap check
    const conflict = this.checkOverlap(this.availDate, start24, end24);
    if (conflict) {
      this.overlapWarning =
        `Slot cannot be added due to overlapping with ` +
        `${this.formatTime(conflict.startTime)} – ${this.formatTime(conflict.endTime)} ` +
        `(${this.slotTypeLabel(conflict.slotType)}).`;
      return;
    }

    this.submitting = true;
    this.error = '';
    this.overlapWarning = '';

    const body = {
      availDate: this.availDate,
      startTime: start24,
      endTime:   end24,
      slotType:  this.slotType
    };

    fetch(`${BASE}/${this.profId}/availability`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${localStorage.getItem('auth_token')}`
      },
      body: JSON.stringify(body)
    }).then(r => {
      if (!r.ok) return r.json().then(e => { throw new Error(e.message || 'Failed'); });
      return r.json();
    }).then(() => {
      this.success = 'Slot added!';
      setTimeout(() => this.success = '', 2500);
      this.startHour = '';
      this.startMin  = '00';
      this.startAmPm = 'AM';
      this.calculatedEndTime = '';
      this.calculatedEndDisplay = '';
      this.load();
      this.submitting = false;
    }).catch(e => { this.error = e.message; this.submitting = false; });
  }

  // ── Delete slot ───────────────────────────────────────────────────────────
  deleteSlot(id: number): void {
    if (!confirm('Delete this slot?')) return;
    fetch(`${BASE}/${this.profId}/availability/${id}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${localStorage.getItem('auth_token')}` }
    }).then(r => {
      if (!r.ok) throw new Error();
      this.deleteMsg = 'Slot deleted successfully!';
      setTimeout(() => this.deleteMsg = '', 2500);
      this.load();
    }).catch(() => this.error = 'Cannot delete a booked slot.');
  }

  // ── Generate default schedule ─────────────────────────────────────────────
  generateDefaultSchedule(): void {
    if (!this.availDate) { this.error = 'Please select a date first.'; return; }

    const defaultSlots = [
      { startTime: '09:00', endTime: '10:30', slotType: 'WORKING' },
      { startTime: '10:30', endTime: '10:50', slotType: 'BREAK' },
      { startTime: '10:50', endTime: '12:00', slotType: 'WORKING' },
      { startTime: '12:00', endTime: '13:00', slotType: 'LUNCH_BREAK' },
      { startTime: '13:00', endTime: '15:30', slotType: 'WORKING' },
      { startTime: '15:30', endTime: '15:50', slotType: 'BREAK' },
      { startTime: '15:50', endTime: '20:00', slotType: 'WORKING' }
    ];

    this.submitting = true;
    this.error = '';
    const token = localStorage.getItem('auth_token');

    const addNext = (index: number) => {
      if (index >= defaultSlots.length) {
        this.submitting = false;
        this.success = 'Default schedule generated for ' + this.availDate;
        setTimeout(() => this.success = '', 3000);
        this.load();
        return;
      }
      const slot = { availDate: this.availDate, ...defaultSlots[index] };
      fetch(`${BASE}/${this.profId}/availability`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify(slot)
      }).then(() => addNext(index + 1))
        .catch(() => addNext(index + 1));
    };
    addNext(0);
  }

  // ── Inline slot-type editing ──────────────────────────────────────────────
  /** slotId → currently selected type in the inline dropdown */
  editingType: Record<number, string> = {};
  /** slotId → true while PATCH is in flight */
  updatingSlot: Record<number, boolean> = {};

  startEditType(slot: Slot): void {
    this.editingType[slot.id] = slot.slotType;
  }

  cancelEditType(slotId: number): void {
    delete this.editingType[slotId];
  }

  updateSlotType(slot: Slot): void {
    const newType = this.editingType[slot.id];
    if (!newType || newType === slot.slotType) {
      delete this.editingType[slot.id];
      return;
    }
    this.updatingSlot[slot.id] = true;
    fetch(`${BASE}/${this.profId}/availability/${slot.id}/type`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${localStorage.getItem('auth_token')}`
      },
      body: JSON.stringify({ slotType: newType })
    }).then(r => {
      if (!r.ok) return r.json().then(e => { throw new Error(e.message || 'Failed'); });
      return r.json();
    }).then(() => {
      delete this.editingType[slot.id];
      this.updatingSlot[slot.id] = false;
      this.success = 'Slot type updated!';
      setTimeout(() => this.success = '', 2500);
      this.load();
    }).catch(e => {
      this.error = e.message || 'Failed to update slot.';
      this.updatingSlot[slot.id] = false;
    });
  }

  isEditing(slotId: number): boolean {
    return slotId in this.editingType;
  }
  get slotsForViewDate(): Slot[] {
    return this.slots
      .filter(s => s.availDate === this.viewDate)
      .sort((a, b) => a.startTime.localeCompare(b.startTime));
  }

  get uniqueDates(): string[] {
    return [...new Set(this.slots.map(s => s.availDate))].sort();
  }

  slotTypeLabel(type: string): string {
    return this.slotTypes.find(t => t.value === type)?.label ?? type;
  }

  slotTypeColor(type: string): string {
    return this.slotTypes.find(t => t.value === type)?.color ?? '#888';
  }

  today(): string {
    return new Date().toISOString().split('T')[0];
  }

  formatTime(t: string): string {
    if (!t) return '';
    const [h, m] = t.split(':');
    const hour = parseInt(h, 10);
    return `${hour % 12 || 12}:${m} ${hour >= 12 ? 'PM' : 'AM'}`;
  }

  get formValid(): boolean {
    return !!(this.availDate && this.startHour && this.startMin && this.startAmPm && this.calculatedEndTime);
  }
}
