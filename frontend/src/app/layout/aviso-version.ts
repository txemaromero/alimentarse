import { Component, inject } from '@angular/core';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faArrowsRotate } from '@fortawesome/free-solid-svg-icons';
import { Actualizaciones } from '../core/actualizaciones';

/** Aviso de versión nueva disponible (SwUpdate) con el botón para recargar (9). */
@Component({
  selector: 'app-aviso-version',
  imports: [FaIconComponent],
  template: `
    <div role="status" aria-live="polite">
      @if (actualizaciones.irrecuperable()) {
        <div class="aviso card shadow animate__animated animate__fadeInUp">
          <div class="card-body d-flex flex-column gap-2">
            <p class="m-0">La aplicación necesita recargarse para seguir funcionando.</p>
            <button
              type="button"
              class="btn btn-primary btn-sm"
              (click)="actualizaciones.recargar()"
            >
              <fa-icon [icon]="icono" class="me-1" />Recargar
            </button>
          </div>
        </div>
      } @else if (actualizaciones.nuevaVersion()) {
        <div class="aviso card shadow animate__animated animate__fadeInUp">
          <div class="card-body d-flex flex-column gap-2">
            <p class="m-0"><strong>Hay una versión nueva de Toca.</strong> Recarga para usarla.</p>
            <div class="d-flex gap-2 justify-content-end">
              <button
                type="button"
                class="btn btn-outline-secondary btn-sm"
                (click)="actualizaciones.posponer()"
              >
                Más tarde
              </button>
              <button
                type="button"
                class="btn btn-primary btn-sm"
                (click)="actualizaciones.recargar()"
              >
                <fa-icon [icon]="icono" class="me-1" />Recargar
              </button>
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: `
    .aviso {
      position: fixed;
      left: 1rem;
      bottom: 1rem;
      z-index: 1095;
      max-width: min(22rem, calc(100vw - 2rem));
    }
  `,
})
export class AvisoVersion {
  protected readonly actualizaciones = inject(Actualizaciones);
  protected readonly icono = faArrowsRotate;

  constructor() {
    this.actualizaciones.iniciar();
  }
}
