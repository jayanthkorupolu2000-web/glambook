import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CommunicationResponse } from '../../../models/professional.model';
import { AuthService } from '../../../services/auth.service';

const BASE = 'http://localhost:8080/api/v1/professionals';

interface CustomerThread {
  customerId: number;
  customerName: string;
  lastMessage: string;
  unread: number;
}

@Component({
  selector: 'app-prof-messages',
  templateUrl: './prof-messages.component.html'
})
export class ProfMessagesComponent implements OnInit {
  threads: CustomerThread[] = [];
  messages: CommunicationResponse[] = [];
  selectedCustomerId: number | null = null;
  selectedCustomerName = '';
  form: FormGroup;
  loading = false;
  sending = false;
  profId = 0;
  error = '';

  constructor(private fb: FormBuilder, private auth: AuthService) {
    this.form = this.fb.group({
      message: ['', [Validators.required, Validators.maxLength(2000)]],
      type: ['GENERAL', Validators.required],
      appointmentId: [null]
    });
  }

  ngOnInit(): void {
    this.profId = this.auth.getUserId() || 0;
    this.loadThreads();
  }

  loadThreads(): void {
    this.loading = true;
    fetch(`${BASE}/${this.profId}/communications/threads`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('auth_token')}` }
    }).then(r => r.json()).then(data => {
      this.threads = Array.isArray(data) ? data : [];
      this.loading = false;
    }).catch(() => this.loading = false);
  }

  selectCustomer(customerId: number, name: string): void {
    this.selectedCustomerId = customerId;
    this.selectedCustomerName = name;
    this.loadThread(customerId);
  }

  loadThread(customerId: number): void {
    fetch(`${BASE}/${this.profId}/communications?customerId=${customerId}`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('auth_token')}` }
    }).then(r => r.json()).then(data => {
      this.messages = Array.isArray(data) ? data : [];
    }).catch(() => {});
  }

  send(): void {
    if (this.form.invalid || !this.selectedCustomerId) return;
    this.sending = true;
    const body = { ...this.form.value, customerId: this.selectedCustomerId };
    fetch(`${BASE}/${this.profId}/communications`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${localStorage.getItem('auth_token')}`
      },
      body: JSON.stringify(body)
    }).then(r => r.json()).then(msg => {
      this.messages = [msg, ...this.messages];
      this.form.patchValue({ message: '' });
      this.sending = false;
    }).catch(() => { this.error = 'Failed to send.'; this.sending = false; });
  }

  typeColor(type: string): string {
    const m: Record<string, string> = { REMINDER: 'info', AFTERCARE: 'success', FOLLOWUP: 'warning', GENERAL: 'secondary' };
    return `badge bg-${m[type] ?? 'secondary'}`;
  }
}
