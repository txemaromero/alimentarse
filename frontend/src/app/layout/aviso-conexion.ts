import { Component, effect, inject } from '@angular/core';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faWifi } from '@fortawesome/free-solid-svg-icons';
import { ID_AVISO_SIN_CONEXION } from '../compartido/requiere-conexion';
import { Conexion } from '../core/conexion';
import { Notificaciones } from '../core/notificaciones';

/**
 * Banner «Sin conexión: mostrando datos guardados» mientras `navigator.onLine` es falso (9). Los botones que guardan
 * algo se deshabilitan y apuntan a este aviso (`RequiereConexion`). Al volver la conexión, se avisa con un toast.
 */
@Component({
  selector: 'app-aviso-conexion',
  imports: [FaIconComponent],
  template: `
    <div role="status">
      @if (!conexion.enLinea()) {
        <div [id]="idAviso" class="aviso-conexion animate__animated animate__fadeInDown">
          <div class="container d-flex align-items-center gap-2 py-2">
            <span class="icono" aria-hidden="true"><fa-icon [icon]="icono" /></span>
            <span>
              <strong>Sin conexión: mostrando datos guardados.</strong>
              Los cambios no se pueden guardar hasta que vuelvas a tener conexión.
            </span>
          </div>
        </div>
      }
    </div>
  `,
  styles: `
    :host {
      display: block;
      position: sticky;
      top: 0;
      z-index: 1020;
    }
    .aviso-conexion {
      background-color: #fff3cd;
      color: #4d3800;
      border-bottom: 1px solid #e6c86e;
      font-size: 0.95rem;
    }
    .icono {
      position: relative;
      display: inline-block;
    }
    /* Wifi tachado */
    .icono::after {
      content: '';
      position: absolute;
      left: -2px;
      right: -2px;
      top: 50%;
      border-top: 2px solid currentColor;
      transform: rotate(-35deg);
    }
  `,
})
export class AvisoConexion {
  protected readonly conexion = inject(Conexion);
  protected readonly idAviso = ID_AVISO_SIN_CONEXION;
  protected readonly icono = faWifi;

  constructor() {
    const notificaciones = inject(Notificaciones);
    let antes = this.conexion.enLinea();
    effect(() => {
      const ahora = this.conexion.enLinea();
      if (ahora && !antes) {
        notificaciones.info('Vuelves a tener conexión.');
      }
      antes = ahora;
    });
  }
}
