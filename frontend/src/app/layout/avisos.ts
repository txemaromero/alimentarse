import { Component, effect, inject } from '@angular/core';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import {
  faCircleCheck,
  faCircleExclamation,
  faCircleInfo,
  faTriangleExclamation,
} from '@fortawesome/free-solid-svg-icons';
import { NgbToast } from '@ng-bootstrap/ng-bootstrap';
import { Anunciador } from '../core/anunciador';
import { Notificaciones, TipoAviso } from '../core/notificaciones';

const ESTILOS: Record<TipoAviso, { clase: string; icono: typeof faCircleCheck }> = {
  exito: { clase: 'text-bg-success', icono: faCircleCheck },
  error: { clase: 'text-bg-danger', icono: faCircleExclamation },
  info: { clase: 'text-bg-dark', icono: faCircleInfo },
  aviso: { clase: 'text-bg-warning', icono: faTriangleExclamation },
};

/**
 * Muestra los avisos de `Notificaciones` como toasts en la esquina inferior derecha, y los anuncia a los lectores de
 * pantalla con `Anunciador`; los errores, como urgentes (D-075).
 *
 * Los toasts son solo visuales (sin `role` y con `aria-live="off"`): cada uno sería una región viva que aparece a la vez
 * que su texto, y un lector de pantalla no la anuncia (NVDA no decía nada al apuntar un consumo). Así, además, nada se
 * anuncia dos veces.
 */
@Component({
  selector: 'app-avisos',
  imports: [NgbToast, FaIconComponent],
  template: `
    <!-- Una región con nombre mientras hay avisos: fuera de los landmarks, axe marca su texto -->
    @if (notificaciones.avisos().length) {
      <section class="toast-container position-fixed bottom-0 end-0 p-3" aria-label="Mensajes">
        @for (aviso of notificaciones.avisos(); track aviso.id) {
          <ngb-toast
            aria-live="off"
            [attr.role]="null"
            [class]="estilos[aviso.tipo].clase + ' animate__animated animate__fadeInUp'"
            [delay]="aviso.tipo === 'error' ? 8000 : 5000"
            (hidden)="notificaciones.quitar(aviso.id)"
          >
            <div class="d-flex align-items-start gap-2">
              <fa-icon [icon]="estilos[aviso.tipo].icono" class="mt-1" />
              <span class="flex-grow-1">{{ aviso.texto }}</span>
              <button
                type="button"
                class="btn-close"
                [class.btn-close-white]="aviso.tipo !== 'aviso'"
                aria-label="Cerrar aviso"
                (click)="notificaciones.quitar(aviso.id)"
              ></button>
            </div>
          </ngb-toast>
        }
      </section>
    }
  `,
  styles: `
    .toast-container {
      z-index: 1090;
    }
  `,
})
export class Avisos {
  protected readonly notificaciones = inject(Notificaciones);
  protected readonly estilos = ESTILOS;
  private readonly anunciador = inject(Anunciador);
  private ultimoAnunciado = 0;

  constructor() {
    effect(() => {
      for (const aviso of this.notificaciones.avisos()) {
        if (aviso.id > this.ultimoAnunciado) {
          this.ultimoAnunciado = aviso.id;
          this.anunciador.anunciar(aviso.texto, aviso.tipo === 'error');
        }
      }
    });
  }
}
