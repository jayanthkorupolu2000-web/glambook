import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Full-name rules:
 *  - 1 to 3 words (2nd and 3rd are optional)
 *  - Each word: starts with an uppercase letter, min 3 letters, letters only (no digits/special chars)
 *  - Words separated by a single space; no leading/trailing spaces
 *  - No digits or special characters anywhere
 */
export function fullNameValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value: string = control.value || '';
    if (!value) return null; // let `required` handle empty

    // Must not start or end with a space
    if (value !== value.trim()) {
      return { fullName: { trailingSpace: true } };
    }

    // No digits or special characters allowed
    if (/[^A-Za-z ]/.test(value)) {
      return { fullName: { invalidChars: true } };
    }

    // Split into words (collapse multiple spaces to catch them)
    const words = value.split(' ').filter(w => w.length > 0);

    // Must have 1–3 words
    if (words.length < 1 || words.length > 3) {
      return { fullName: { wordCount: true } };
    }

    for (const word of words) {
      // Each word must be at least 3 characters
      if (word.length < 3) {
        return { fullName: { wordTooShort: true } };
      }
      // First letter must be uppercase
      if (!/^[A-Z]/.test(word)) {
        return { fullName: { notCapitalized: true } };
      }
      // Rest of the word must be lowercase letters only
      if (!/^[A-Z][a-z]+$/.test(word)) {
        return { fullName: { invalidChars: true } };
      }
    }

    return null;
  };
}
