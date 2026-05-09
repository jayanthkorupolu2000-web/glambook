import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export const VALID_CITIES = [
  'Visakhapatnam',
  'Vijayawada',
  'Hyderabad',
  'Ananthapur',
  'Khammam'
] as const;

export function cityValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) return null;
    return VALID_CITIES.includes(control.value)
      ? null
      : { invalidCity: { value: control.value } };
  };
}
