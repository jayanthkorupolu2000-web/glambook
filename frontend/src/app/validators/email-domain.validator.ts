import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Accepts only commonly-used personal email domains.
 */
const ALLOWED_DOMAINS = [
  'gmail.com',
  'yahoo.com',
  'yahoo.in',
  'outlook.com',
  'hotmail.com',
  'live.com',
  'icloud.com',
  'me.com',
  'protonmail.com',
  'rediffmail.com'
];

export function emailDomainValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value: string = (control.value || '').trim().toLowerCase();
    if (!value) return null; // let `required` handle empty

    const atIndex = value.lastIndexOf('@');
    if (atIndex === -1) return { emailDomain: true };

    const domain = value.slice(atIndex + 1);
    return ALLOWED_DOMAINS.includes(domain) ? null : { emailDomain: true };
  };
}
