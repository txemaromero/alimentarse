import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormGroup } from '@angular/forms';
import { ErrorApi } from './modelos';

export const MENSAJE_ERROR_GENERICO =
  'Ha ocurrido un error inesperado. Inténtalo de nuevo más tarde.';

/** El cuerpo de error de la API ({codigo, mensaje, errores}), si lo hay. */
export function errorApi(error: unknown): ErrorApi | null {
  if (error instanceof HttpErrorResponse && error.error && typeof error.error.codigo === 'string') {
    return {
      codigo: error.error.codigo,
      mensaje: error.error.mensaje ?? MENSAJE_ERROR_GENERICO,
      errores: error.error.errores ?? {},
    };
  }
  return null;
}

/** Mensaje para el usuario: el de la API o uno genérico. */
export function mensajeError(error: unknown, porDefecto = MENSAJE_ERROR_GENERICO): string {
  return errorApi(error)?.mensaje ?? porDefecto;
}

/**
 * Coloca los errores por campo de la API (patrón «Input Feedback») junto a cada control del formulario, con la clave
 * `servidor`. Devuelve si se ha colocado alguno.
 */
export function aplicarErroresServidor(formulario: FormGroup, error: ErrorApi | null): boolean {
  if (!error) {
    return false;
  }
  let aplicado = false;
  for (const [campo, mensaje] of Object.entries(error.errores)) {
    // La API nombra los campos de las listas como «piezas[0].gramos»; el formulario, «piezas.0.gramos»
    const control = formulario.get(campo.replace(/\[(\d+)\]/g, '.$1'));
    if (control) {
      control.setErrors({ ...(control.errors ?? {}), servidor: mensaje });
      control.markAsTouched();
      aplicado = true;
    }
  }
  return aplicado;
}

/** ¿Hay que mostrar el control como no válido? */
export function esInvalido(control: AbstractControl): boolean {
  return control.invalid && (control.touched || control.dirty);
}
