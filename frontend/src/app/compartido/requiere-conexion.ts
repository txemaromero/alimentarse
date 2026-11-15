import { Directive, computed, inject, input } from '@angular/core';
import { Conexion } from '../core/conexion';

/** Id del aviso «Sin conexión…», que explica por qué están deshabilitados los botones. */
export const ID_AVISO_SIN_CONEXION = 'aviso-sin-conexion';

/**
 * Para los botones que guardan algo en el servidor: los deshabilita sin conexión y los enlaza con el aviso que lo
 * explica. Sustituye a `[disabled]`: `[appRequiereConexion]="formulario.invalid || enviando()"`, o solo
 * `appRequiereConexion` si no hay otra condición.
 */
@Directive({
  selector: 'button[appRequiereConexion]',
  host: {
    '[disabled]': 'deshabilitado()',
    '[attr.aria-describedby]': 'sinConexion() ? idAviso : null',
  },
})
export class RequiereConexion {
  private readonly conexion = inject(Conexion);

  /** Otra condición para deshabilitar el botón. */
  readonly appRequiereConexion = input(false, {
    transform: (valor: boolean | '' | null | undefined) => valor === true,
  });

  protected readonly idAviso = ID_AVISO_SIN_CONEXION;
  readonly sinConexion = computed(() => !this.conexion.enLinea());
  readonly deshabilitado = computed(() => this.sinConexion() || this.appRequiereConexion());
}
