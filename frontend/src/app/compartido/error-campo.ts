import { Component, computed, effect, input, signal } from '@angular/core';
import { AbstractControl, ValidationErrors } from '@angular/forms';
import { esInvalido } from '../core/errores';

/**
 * Mensaje de error de un campo de formulario («Input Feedback»), debajo del control y enlazado con
 * `aria-describedby` mediante `idMensaje`. Se muestra cuando el control no es válido y el usuario ya lo ha tocado.
 */
@Component({
  selector: 'app-error-campo',
  template: `
    @if (mensaje(); as texto) {
      <div class="invalid-feedback d-block" [id]="idMensaje()">{{ texto }}</div>
    }
  `,
})
export class ErrorCampo {
  readonly control = input.required<AbstractControl>();
  /** Id del mensaje, para el `aria-describedby` del control. */
  readonly idMensaje = input.required<string>();
  /** Mensaje para el validador `pattern`, que no tiene un texto genérico útil. */
  readonly mensajePatron = input('El formato no es válido');

  /**
   * El estado de un control de formulario reactivo no es una signal: cada evento del control (valor, estado,
   * tocado…) incrementa este contador para que `mensaje` se recalcule con OnPush.
   */
  private readonly cambios = signal(0);

  protected readonly mensaje = computed(() => {
    this.cambios();
    const control = this.control();
    return esInvalido(control) && control.errors
      ? mensajeValidacion(control.errors, this.mensajePatron())
      : null;
  });

  constructor() {
    effect((alLimpiar) => {
      const suscripcion = this.control().events.subscribe(() => this.cambios.update((n) => n + 1));
      alLimpiar(() => suscripcion.unsubscribe());
    });
  }
}

/** «0,1», «10.000». */
function numeroEnEspanol(valor: unknown): string {
  return Number(valor).toLocaleString('es');
}

/** Texto en español del primer error del control. */
export function mensajeValidacion(
  errores: ValidationErrors,
  mensajePatron = 'El formato no es válido',
): string {
  if (errores['servidor']) {
    return errores['servidor'];
  }
  if (errores['required']) {
    return 'Este campo es obligatorio';
  }
  if (errores['email']) {
    return 'El email no es válido';
  }
  if (errores['minlength']) {
    return `Debe tener al menos ${errores['minlength'].requiredLength} caracteres`;
  }
  if (errores['maxlength']) {
    return `No puede tener más de ${errores['maxlength'].requiredLength} caracteres`;
  }
  if (errores['bcrypt']) {
    return 'La contraseña es demasiado larga: la ñ, las tildes y los emojis cuentan como más de un carácter';
  }
  if (errores['pattern']) {
    return mensajePatron;
  }
  if (errores['min']) {
    return `El valor mínimo es ${numeroEnEspanol(errores['min'].min)}`;
  }
  if (errores['max']) {
    return `El valor máximo es ${numeroEnEspanol(errores['max'].max)}`;
  }
  if (errores['noCoinciden']) {
    return 'Las contraseñas no coinciden';
  }
  if (errores['futura']) {
    return 'La fecha no puede ser futura';
  }
  if (errores['noElegido']) {
    return 'Elige una de las sugerencias, o deja el campo vacío';
  }
  if (errores['fechaInvalida']) {
    return 'La fecha no es válida';
  }
  if (errores['espacios']) {
    return 'No puede estar vacío ni tener solo espacios';
  }
  return 'El valor no es válido';
}
