import { AbstractControl, FormGroup } from '@angular/forms';

export function markFormGroupTouched(formGroup: FormGroup): void {
  Object.values(formGroup.controls).forEach((control: AbstractControl) => {
    control.markAsTouched();
    control.updateValueAndValidity();

    if (control instanceof FormGroup) {
      markFormGroupTouched(control);
    }
  });
}

export function getValidationMessage(
  fieldLabel: string,
  control: AbstractControl | null
): string {
  if (!control || !control.errors || !control.touched) {
    return '';
  }

  if (control.errors['required']) {
    return `${fieldLabel} không được để trống.`;
  }

  if (control.errors['email']) {
    return `${fieldLabel} không đúng định dạng email.`;
  }

  if (control.errors['minlength']) {
    return `${fieldLabel} phải có ít nhất ${control.errors['minlength'].requiredLength} ký tự.`;
  }

  if (control.errors['maxlength']) {
    return `${fieldLabel} không được vượt quá ${control.errors['maxlength'].requiredLength} ký tự.`;
  }

  if (control.errors['pattern']) {
    return `${fieldLabel} không đúng định dạng.`;
  }

  return `${fieldLabel} không hợp lệ.`;
}