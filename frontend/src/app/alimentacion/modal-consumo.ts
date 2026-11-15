import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable } from 'rxjs';
import { ErrorCampo } from '../compartido/error-campo';
import { aInstanteUtc, aValorLocal, ahoraLocal, noFutura } from '../compartido/fechas';
import { aplicarErroresServidor, errorApi, esInvalido, mensajeError } from '../core/errores';
import { Consumo } from '../core/modelos';
import { ConsumosApi } from './consumos-api';
import { RequiereConexion } from '../compartido/requiere-conexion';

/** Cómo se cerró el modal: con la fecha cambiada o con el consumo borrado. */
export type ResultadoConsumo =
  { accion: 'guardado'; consumo: Consumo } | { accion: 'eliminado'; consumo: Consumo };

/** Modal del histórico (6.7): cambiar la fecha de un consumo o eliminarlo. Botones Cancelar / Eliminar / Guardar. */
@Component({
  selector: 'app-modal-consumo',
  imports: [ReactiveFormsModule, DatePipe, ErrorCampo, RequiereConexion],
  template: `
    <form [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
      <div class="modal-header">
        <h2 class="modal-title h5" id="modal-consumo-titulo">Editar consumo</h2>
        <button
          type="button"
          class="btn-close"
          aria-label="Cerrar"
          (click)="modal.dismiss()"
        ></button>
      </div>
      <div class="modal-body">
        <p class="mb-3">
          <strong>{{ consumo.receta.titulo }}</strong>
          <span class="d-block small text-body-secondary">
            Consumida el {{ consumo.fecha | date: "dd/MM/yyyy 'a las' HH:mm" }}
          </span>
        </p>

        @if (error(); as mensaje) {
          <div class="alert alert-danger" role="alert">{{ mensaje }}</div>
        }

        @if (confirmandoBorrado()) {
          <div class="alert alert-warning mb-0" role="alert">
            <p class="mb-2">¿Seguro que quieres eliminar este consumo? No se puede deshacer.</p>
            <div class="d-flex gap-2 justify-content-end">
              <button
                type="button"
                class="btn btn-sm btn-outline-secondary"
                (click)="confirmandoBorrado.set(false)"
              >
                No, conservarlo
              </button>
              <button
                type="button"
                class="btn btn-sm btn-danger"
                [appRequiereConexion]="enviando()"
                (click)="eliminar()"
              >
                Sí, eliminar
              </button>
            </div>
          </div>
        } @else {
          <label for="consumo-nueva-fecha" class="form-label">Fecha y hora</label>
          <input
            id="consumo-nueva-fecha"
            type="datetime-local"
            class="form-control"
            formControlName="fecha"
            ngbAutofocus
            [max]="maximo()"
            (focus)="maximo.set(ahora())"
            [class.is-invalid]="esInvalido(formulario.controls.fecha)"
            [attr.aria-invalid]="esInvalido(formulario.controls.fecha)"
            aria-describedby="consumo-nueva-fecha-error"
          />
          <app-error-campo
            [control]="formulario.controls.fecha"
            idMensaje="consumo-nueva-fecha-error"
          />
        }
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-outline-secondary" (click)="modal.dismiss()">
          Cancelar
        </button>
        <button
          type="button"
          class="btn btn-danger me-auto order-first"
          [appRequiereConexion]="enviando() || confirmandoBorrado()"
          (click)="confirmandoBorrado.set(true)"
        >
          Eliminar
        </button>
        <button
          type="submit"
          class="btn btn-primary"
          [appRequiereConexion]="formulario.invalid || enviando() || confirmandoBorrado()"
        >
          @if (enviando()) {
            <span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>
          }
          Guardar
        </button>
      </div>
    </form>
  `,
})
export class ModalConsumo implements OnInit {
  protected readonly modal = inject(NgbActiveModal);
  private readonly api = inject(ConsumosApi);

  /** Lo asigna `abrirModalConsumo`. */
  consumo!: Consumo;

  readonly formulario = new FormGroup({
    fecha: new FormControl('', { nonNullable: true, validators: [Validators.required, noFutura] }),
  });
  readonly enviando = signal(false);
  readonly error = signal<string | null>(null);
  readonly confirmandoBorrado = signal(false);
  readonly maximo = signal(ahoraLocal());

  protected readonly esInvalido = esInvalido;
  protected readonly ahora = ahoraLocal;

  ngOnInit(): void {
    this.formulario.reset({ fecha: aValorLocal(new Date(this.consumo.fecha)) });
  }

  guardar(): void {
    this.formulario.controls.fecha.updateValueAndValidity();
    if (this.formulario.invalid || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    const fecha = aInstanteUtc(this.formulario.controls.fecha.value);
    this.api.cambiarFecha(this.consumo.id, fecha).subscribe({
      next: (consumo) => this.modal.close({ accion: 'guardado', consumo } as ResultadoConsumo),
      error: (error: HttpErrorResponse) => this.fallo(error),
    });
  }

  eliminar(): void {
    if (this.enviando()) {
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    this.api.borrar(this.consumo.id).subscribe({
      next: () =>
        this.modal.close({ accion: 'eliminado', consumo: this.consumo } as ResultadoConsumo),
      error: (error: HttpErrorResponse) => {
        this.confirmandoBorrado.set(false);
        this.fallo(error);
      },
    });
  }

  private fallo(error: HttpErrorResponse): void {
    this.enviando.set(false);
    if (error.status >= 400 && error.status < 500 && error.status !== 401) {
      if (!aplicarErroresServidor(this.formulario, errorApi(error))) {
        this.error.set(
          error.status === 404
            ? 'Este consumo ya no existe. Cierra y vuelve a cargar el histórico.'
            : mensajeError(error),
        );
      }
    }
  }
}

/** Abre el modal y emite cómo se cerró (nada si se cancela). */
export function abrirModalConsumo(
  modales: NgbModal,
  consumo: Consumo,
): Observable<ResultadoConsumo> {
  const ref = modales.open(ModalConsumo, {
    ariaLabelledBy: 'modal-consumo-titulo',
    modalDialogClass: 'animate__animated animate__fadeInDown',
  });
  (ref.componentInstance as ModalConsumo).consumo = consumo;
  return new Observable<ResultadoConsumo>((suscriptor) => {
    ref.result.then(
      (resultado: ResultadoConsumo) => {
        suscriptor.next(resultado);
        suscriptor.complete();
      },
      () => suscriptor.complete(),
    );
  });
}
