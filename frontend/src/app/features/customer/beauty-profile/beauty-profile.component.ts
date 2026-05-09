import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { AuthService } from '../../../services/auth.service';

// Use /api/v1/ path — the backend now handles both /api/ and /api/v1/
const API = 'http://localhost:8080/api/v1';

@Component({
  selector: 'app-beauty-profile',
  templateUrl: './beauty-profile.component.html'
})
export class BeautyProfileComponent implements OnInit {
  form!: FormGroup;
  loading = false;
  saving = false;
  success = '';
  error = '';
  editMode = false;

  skinTypes = ['Normal', 'Oily', 'Dry', 'Combination', 'Sensitive'];
  hairTypes = ['Straight', 'Wavy', 'Curly', 'Coily'];
  hairTextures = ['Fine', 'Medium', 'Thick'];

  constructor(private fb: FormBuilder, private auth: AuthService, private http: HttpClient) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      skinType:          [''],
      hairType:          [''],
      hairTexture:       [''],
      allergies:         [''],
      preferredServices: [''],
      notes:             ['']
    });
    this.form.disable();
    this.load();
  }

  load(): void {
    const id = this.auth.getUserId();
    if (!id) return;
    this.loading = true;
    this.http.get<any>(`${API}/customers/${id}/beauty-profile`).subscribe({
      next: data => {
        this.form.enable();
        this.form.patchValue({
          skinType:          data.skinType          || '',
          hairType:          data.hairType          || '',
          hairTexture:       data.hairTexture       || '',
          allergies:         data.allergies         || '',
          preferredServices: data.preferredServices || '',
          notes:             data.notes             || ''
        });
        this.form.disable();
        this.loading = false;
      },
      error: () => {
        // No profile yet — that's fine, just show empty form
        this.loading = false;
      }
    });
  }

  enableEdit(): void {
    this.editMode = true;
    this.form.enable();
    this.success = '';
    this.error = '';
  }

  cancel(): void {
    this.editMode = false;
    this.form.disable();
    this.load();
  }

  save(): void {
    const id = this.auth.getUserId();
    if (!id) return;
    this.saving = true;
    this.error = '';
    this.success = '';

    const payload = {
      skinType:          this.form.value.skinType          || null,
      hairType:          this.form.value.hairType          || null,
      hairTexture:       this.form.value.hairTexture       || null,
      allergies:         this.form.value.allergies         || null,
      preferredServices: this.form.value.preferredServices || null,
      notes:             this.form.value.notes             || null
    };

    this.http.put<any>(`${API}/customers/${id}/beauty-profile`, payload).subscribe({
      next: () => {
        this.success = '✅ Beauty profile saved successfully!';
        this.editMode = false;
        this.form.disable();
        this.saving = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to save. Please try again.';
        this.saving = false;
      }
    });
  }
}
