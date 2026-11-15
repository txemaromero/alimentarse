import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable } from 'rxjs';
import { ErrorCampo } from '../compartido/error-campo';
import { RequiereConexion } from '../compartido/requiere-conexion';
import { noVacio } from '../compartido/validadores';
import { aplicarErroresServidor, errorApi, esInvalido, mensajeError } from '../core/errores';
import { RecetaDetalle } from '../core/modelos';
import { AdminApi } from './admin-api';

export const LONGITUD_MAXIMA_MOTIVO = 500;

/** Ocultar una receta (moderación, D-063): el motivo es obligatorio y lo verá su autor. */
@Component({
  selector: 'app-modal-ocultar-receta',
  imports: [ReactiveFormsModule, ErrorCampo, RequiereConexion],
  template: `
    <form [formGroup]="formulario" (ngSubmit)="ocultar()" novalidate>
      <div class="modal-header">
        <h2 class="modal-title h5" id="modal-ocultar-titulo">Ocultar la receta</h2>
        <button
          type="button"
          class="btn-close"
          aria-label="Cerrar"
          (click)="modal.dismiss()"
        ></button>
      </div>
      <div class="modal-body">
        <p>
          «<strong>{{ receta.titulo }}</strong
          >», de {{ receta.autor }}, dejará de verse en la aplicación. Su autor la seguirá viendo,
          con el motivo, y no podrá publicarla mientras siga oculta.
        </p>
        @if (error(); as mensaje) {
          <div class="alert alert-danger" role="alert">{{ mensaje }}</div>
        }
        <label for="ocultar-motivo" class="form-label">Motivo</label>
        <textarea
          id="ocultar-motivo"
          class="form-control"
          rows="3"
          formControlName="motivo"
          ngbAutofocus
          [maxlength]="maximo"
          [class.is-invalid]="esInvalido(formulario.controls.motivo)"
          [attr.aria-invalid]="esInvalido(formulario.controls.motivo)"
          aria-describedby="ocultar-motivo-ayuda ocultar-motivo-error"
        ></textarea>
        <div id="ocultar-motivo-ayuda" class="form-text">
          Lo verá el autor. Como mucho, {{ maximo }} caracteres.
        </div>
        <app-error-campo [control]="formulario.controls.motivo" idMensaje="ocultar-motivo-error" />
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-outline-secondary" (click)="modal.dismiss()">
          Cancelar
        </button>
        <button
          type="submit"
          class="btn btn-danger"
          [appRequiereConexion]="formulario.invalid || enviando()"
        >
          @if (enviando()) {
            <span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>
          }
          Ocultar
        </button>
      </div>
    </form>
  `,
})
export class ModalOcultarReceta {
  protected readonly modal = inject(NgbActiveModal);
  private readonly api = inject(AdminApi);

  /** Lo asigna `abrirModalOcultarReceta`. */
  receta!: RecetaDetalle;

  readonly formulario = new FormGroup({
    motivo: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, noVacio, Validators.maxLength(LONGITUD_MAXIMA_MOTIVO)],
    }),
  });
  readonly enviando = signal(false);
  readonly error = signal<string | null>(null);

  protected readonly maximo = LONGITUD_MAXIMA_MOTIVO;
  protected readonly esInvalido = esInvalido;

  ocultar(): void {
    if (this.formulario.invalid || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    const motivo = this.formulario.controls.motivo.value.trim();
    this.api.ocultarReceta(this.receta.id, true, motivo).subscribe({
      next: (receta) => this.modal.close(receta),
      error: (error: HttpErrorResponse) => {
        this.enviando.set(false);
        if (error.status >= 400 && error.status < 500 && error.status !== 401) {
          if (!aplicarErroresServidor(this.formulario, errorApi(error))) {
            this.error.set(mensajeError(error));
          }
        }
      },
    });
  }
}

/** Abre el modal y emite la receta ya oculta (nada si se cancela). */
export function abrirModalOcultarReceta(
  modales: NgbModal,
  receta: RecetaDetalle,
): Observable<RecetaDetalle> {
  const ref = modales.open(ModalOcultarReceta, {
    ariaLabelledBy: 'modal-ocultar-titulo',
    modalDialogClass: 'animate__animated animate__fadeInDown',
  });
  (ref.componentInstance as ModalOcultarReceta).receta = receta;
  return new Observable<RecetaDetalle>((suscriptor) => {
    ref.result.then(
      (oculta: RecetaDetalle) => {
        suscriptor.next(oculta);
        suscriptor.complete();
      },
      () => suscriptor.complete(),
    );
  });
}
