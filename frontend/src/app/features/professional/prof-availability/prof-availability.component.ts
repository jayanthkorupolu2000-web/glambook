import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../../services/auth.service';

const BASE = 'http://localhost:8080/api/v1/professionals';

interface Slot {
  id: number;
  availDate: string;
  startTime: string;
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
  form: FormGroup;
  loading = false;
  submitting = false;
  profId = 0;
  error = '';
  success = '';

  // View date filter
  viewDate = '';

  slotTypes = [
    { value: 'WORKING',      label: '🕐 Working Hours',  desc: 'Customers can book this time' },
    { value: 'LUNCH_BREAK',  label: '🍽️ Lunch Break',    desc: 'Lunch — not bookable' },
    { value: 'BREAK',        label: '☕ Short Break',     desc: 'Short break — not bookable' },
    { value: 'BLOCKED',      label: '🚫 Blocked',         desc: 'Blocked off — not bookable' }
  ];

  constructor(private fb: FormBuilder, private auth: AuthService) {
    this.form = this.fb.group({
      availDate:  ['', Validators.required],
      startTime:  ['', Validators.required],
      endTime:    ['', Validators.required],
      slotType:   ['WORKING', Validators.required]
    });
  }

  ngOnInit(): void {
    this.profId = this.auth.getUserId() || 0;
    this.viewDate = new Date().toISOString().split('T')[0];
    this.load();
  }

  load(): void {
    this.loading = true;
    fetch(`${BASE}/${this.profId}/availability`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('auth_token')}` }
    }).then(r => r.json()).then(data => {
      this.slots = Array.isArray(data) ? data : [];
      this.loading = false;
    }).catch(() => this.loading = false);
  }

  addSlot(): void {
    if (this.form.invalid) return;
    this.submitting = true;
    this.error = '';
    fetch(`${BASE}/${this.profId}/availability`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${localStorage.getItem('auth_token')}`
      },
      body: JSON.stringify(this.form.value)
    }).then(r => {
      if (!r.ok) return r.json().then(e => { throw new Error(e.message || 'Failed'); });
      return r.json();
    }).then(() => {
      this.success = 'Slot added!';
      setTimeout(() => this.success = '', 2000);
      this.form.patchValue({ startTime: '', endTime: '' });
      this.load();
      this.submitting = false;
    }).catch(e => { this.error = e.message; this.submitting = false; });
  }

  deleteSlot(id: number): void {
    if (!confirm('Delete this slot?')) return;
    fetch(`${BASE}/${this.profId}/availability/${id}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${localStorage.getItem('auth_token')}` }
    }).then(r => {
      if (!r.ok) throw new Error();
      this.load();
    }).catch(() => this.error = 'Cannot delete a booked slot.');
  }

  get slotsForViewDate(): Slot[] {
    return this.slots
      .filter(s => s.availDate === this.viewDate)
      .sort((a, b) => a.startTime.localeCompare(b.startTime));
  }

  get uniqueDates(): string[] {
    return [...new Set(this.slots.map(s => s.availDate))].sort();
  }

  slotBadgeClass(type: string): string {
    const map: Record<string, string> = {
      WORKING:     'badge bg-success',
      LUNCH_BREAK: 'badge bg-warning text-dark',
      BREAK:       'badge bg-info text-dark',
      BLOCKED:     'badge bg-danger'
    };
    return map[type] ?? 'badge bg-secondary';
  }

  slotTypeLabel(type: string): string {
    const found = this.slotTypes.find(t => t.value === type);
    return found ? found.label : type;
  }

  getSlotTypeDesc(type: string): string {
    const found = this.slotTypes.find(t => t.value === type);
    return found ? found.desc : '';
  }

  today(): string {
    return new Date().toISOString().split('T')[0];
  }

  formatTime(t: string): string {
    if (!t) return '';
    const [h, m] = t.split(':');
    const hour = parseInt(h);
    return `${hour % 12 || 12}:${m} ${hour >= 12 ? 'PM' : 'AM'}`;
  }

  /** Generate default schedule for a date: 9 AM–8 PM with standard breaks */
  generateDefaultSchedule(): void {
    const date = this.form.value.availDate;
    if (!date) { this.error = 'Please select a date first.'; return; }

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
        this.success = 'Default schedule generated for ' + date;
        setTimeout(() => this.success = '', 3000);
        this.load();
        return;
      }
      const slot = { availDate: date, ...defaultSlots[index] };
      fetch(`${BASE}/${this.profId}/availability`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify(slot)
      }).then(() => addNext(index + 1))
        .catch(() => addNext(index + 1)); // skip duplicates silently
    };

    addNext(0);
  }
}
