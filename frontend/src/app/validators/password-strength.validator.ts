import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function passwordStrengthValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value: string = control.value || '';
    if (!value) return null;

    const errors: ValidationErrors = {};

    if (value.length < 8) {
      errors['minLength'] = true;
    }
    if (!/[A-Z]/.test(value)) {
      errors['noUppercase'] = true;
    }
    if (!/[0-9]/.test(value)) {
      errors['noNumber'] = true;
    }

    return Object.keys(errors).length ? { passwordStrength: errors } : null;
  };
}
