import { DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormGroup, ValidationErrors, ValidatorFn } from '@angular/forms';

/** El valor debe ser igual al del control hermano `otro` (confirmación de contraseña). */
export function coincideCon(otro: string): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const valorOtro = control.parent?.get(otro)?.value;
    return control.value && valorOtro !== undefined && control.value !== valorOtro
      ? { noCoinciden: true }
      : null;
  };
}

/** Rechaza los textos con solo espacios (equivale a `@NotBlank` del backend). */
export const noVacio: ValidatorFn = (control) =>
  typeof control.value === 'string' && control.value.length > 0 && control.value.trim() === ''
    ? { espacios: true }
    : null;

const MAXIMO_BYTES_BCRYPT = 72;

/**
 * La contraseña cabe en los 72 bytes de bcrypt, y no solo en 72 caracteres: la ñ y las tildes ocupan 2 y un emoji, 4
 * (`@CabeEnBcrypt` del backend, D-090). Con más de 72 caracteres no falla: ya lo dice `maxLength`, y así no salen dos
 * mensajes.
 */
export const cabeEnBcrypt: ValidatorFn = (control) =>
  typeof control.value === 'string' &&
  control.value.length <= MAXIMO_BYTES_BCRYPT &&
  new TextEncoder().encode(control.value).length > MAXIMO_BYTES_BCRYPT
    ? { bcrypt: true }
    : null;

/**
 * Vuelve a validar la confirmación cuando cambia el campo original, para que el error «no coinciden» aparezca o
 * desaparezca sin tener que tocar de nuevo la confirmación.
 */
export function revalidarConfirmacion(
  formulario: FormGroup,
  original: string,
  confirmacion: string,
  destroyRef: DestroyRef,
): void {
  formulario
    .get(original)
    ?.valueChanges.pipe(takeUntilDestroyed(destroyRef))
    .subscribe(() => formulario.get(confirmacion)?.updateValueAndValidity({ emitEvent: false }));
}
