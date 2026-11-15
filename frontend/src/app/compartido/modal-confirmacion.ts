import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable } from 'rxjs';
import { mensajeError } from '../core/errores';
import { RequiereConexion } from './requiere-conexion';

/** Qué pregunta el modal y qué hace al aceptar. */
export interface OpcionesConfirmacion<T> {
  titulo: string;
  mensaje: string;
  /** Texto del botón que confirma, p. ej. «Sí, eliminar». */
  aceptar: string;
  /** La acción que se lanza al aceptar; el modal espera su respuesta y muestra los errores 4xx. */
  accion: () => Observable<T>;
}

/**
 * Confirmación de una acción que no se puede deshacer (eliminar una receta, un grupo…). La acción se ejecuta dentro del
 * modal: así el usuario ve que está en marcha y, si falla, el motivo sin perder el contexto.
 */
@Component({
  selector: 'app-modal-confirmacion',
  imports: [RequiereConexion],
  template: `
    <div class="modal-header">
      <h2 class="modal-title h5" id="modal-confirmacion-titulo">{{ opciones.titulo }}</h2>
      <button
        type="button"
        class="btn-close"
        aria-label="Cerrar"
        [disabled]="enviando()"
        (click)="modal.dismiss()"
      ></button>
    </div>
    <div class="modal-body">
      <p class="mb-0">{{ opciones.mensaje }}</p>
      @if (error(); as mensaje) {
        <div class="alert alert-danger mt-3 mb-0" role="alert">{{ mensaje }}</div>
      }
    </div>
    <div class="modal-footer">
      <button
        type="button"
        class="btn btn-outline-secondary"
        ngbAutofocus
        [disabled]="enviando()"
        (click)="modal.dismiss()"
      >
        Cancelar
      </button>
      <button
        type="button"
        class="btn btn-danger"
        [appRequiereConexion]="enviando()"
        (click)="aceptar()"
      >
        @if (enviando()) {
          <span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>
        }
        {{ opciones.aceptar }}
      </button>
    </div>
  `,
})
export class ModalConfirmacion {
  protected readonly modal = inject(NgbActiveModal);

  /** Lo asigna `confirmar`. */
  opciones!: OpcionesConfirmacion<unknown>;

  readonly enviando = signal(false);
  readonly error = signal<string | null>(null);

  aceptar(): void {
    if (this.enviando()) {
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    this.opciones.accion().subscribe({
      next: (resultado) => this.modal.close({ resultado }),
      error: (error: HttpErrorResponse) => {
        this.enviando.set(false);
        // El 401 y los 5xx ya los avisa el interceptor
        if (error.status >= 400 && error.status < 500 && error.status !== 401) {
          this.error.set(mensajeError(error));
        } else {
          this.modal.dismiss();
        }
      },
    });
  }
}

/** Abre la confirmación y emite el resultado de la acción si se acepta y sale bien (nada si se cancela). */
export function confirmar<T>(modales: NgbModal, opciones: OpcionesConfirmacion<T>): Observable<T> {
  const ref = modales.open(ModalConfirmacion, {
    ariaLabelledBy: 'modal-confirmacion-titulo',
    modalDialogClass: 'animate__animated animate__fadeInDown',
    backdrop: 'static',
  });
  (ref.componentInstance as ModalConfirmacion).opciones = opciones as OpcionesConfirmacion<unknown>;
  return new Observable<T>((suscriptor) => {
    ref.result.then(
      ({ resultado }: { resultado: T }) => {
        suscriptor.next(resultado);
        suscriptor.complete();
      },
      () => suscriptor.complete(),
    );
  });
}
