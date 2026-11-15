import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable } from 'rxjs';
import { ErrorCampo } from '../compartido/error-campo';
import { RequiereConexion } from '../compartido/requiere-conexion';
import { noVacio } from '../compartido/validadores';
import { aplicarErroresServidor, errorApi, esInvalido, mensajeError } from '../core/errores';
import { GrupoAdmin } from '../core/modelos';
import { AdminApi } from './admin-api';

/** Alta o edición de un grupo de alimentos (D-065): nombre, raciones por semana (o sin límite) y si es básico. */
@Component({
  selector: 'app-modal-grupo',
  imports: [ReactiveFormsModule, ErrorCampo, RequiereConexion],
  template: `
    <form [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
      <div class="modal-header">
        <h2 class="modal-title h5" id="modal-grupo-titulo">
          {{ grupo ? 'Editar grupo' : 'Nuevo grupo' }}
        </h2>
        <button
          type="button"
          class="btn-close"
          aria-label="Cerrar"
          (click)="modal.dismiss()"
        ></button>
      </div>
      <div class="modal-body">
        @if (error(); as mensaje) {
          <div class="alert alert-danger" role="alert">{{ mensaje }}</div>
        }
        <div class="mb-3">
          <label for="grupo-nombre" class="form-label">Nombre</label>
          <input
            id="grupo-nombre"
            type="text"
            class="form-control"
            formControlName="nombre"
            maxlength="100"
            autocomplete="off"
            ngbAutofocus
            [class.is-invalid]="esInvalido(formulario.controls.nombre)"
            [attr.aria-invalid]="esInvalido(formulario.controls.nombre)"
            aria-describedby="grupo-nombre-error"
          />
          <app-error-campo [control]="formulario.controls.nombre" idMensaje="grupo-nombre-error" />
        </div>

        <div class="form-check mb-2">
          <input
            id="grupo-sin-limite"
            type="checkbox"
            class="form-check-input"
            formControlName="sinLimite"
          />
          <label for="grupo-sin-limite" class="form-check-label">Sin límite semanal</label>
        </div>
        @if (!formulario.controls.sinLimite.value) {
          <div class="mb-3">
            <label for="grupo-raciones" class="form-label">Raciones por semana, como máximo</label>
            <input
              id="grupo-raciones"
              type="number"
              class="form-control"
              formControlName="racionesSemana"
              min="1"
              max="99"
              [class.is-invalid]="esInvalido(formulario.controls.racionesSemana)"
              [attr.aria-invalid]="esInvalido(formulario.controls.racionesSemana)"
              aria-describedby="grupo-raciones-error"
            />
            <app-error-campo
              [control]="formulario.controls.racionesSemana"
              idMensaje="grupo-raciones-error"
            />
          </div>
        }

        <div class="form-check">
          <input
            id="grupo-basico"
            type="checkbox"
            class="form-check-input"
            formControlName="basico"
            aria-describedby="grupo-basico-ayuda"
          />
          <label for="grupo-basico" class="form-check-label">Grupo básico</label>
          <div id="grupo-basico-ayuda" class="form-text">
            Como la sal o el aceite: está en casi todas las recetas, así que no sale en
            «Ingredientes más usados».
          </div>
        </div>

        <p class="small text-body-secondary mt-3 mb-0">
          Los cambios en las raciones se aplican al momento a los indicadores y a «¿Qué toca hoy?»
          de todos los usuarios, también sobre lo que ya han comido esta semana.
        </p>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-outline-secondary" (click)="modal.dismiss()">
          Cancelar
        </button>
        <button
          type="submit"
          class="btn btn-primary"
          [appRequiereConexion]="formulario.invalid || enviando()"
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
export class ModalGrupo implements OnInit {
  protected readonly modal = inject(NgbActiveModal);
  private readonly api = inject(AdminApi);
  private readonly destruir = inject(DestroyRef);

  /** El grupo que se edita, o undefined para crear uno. Lo asigna `abrirModalGrupo`. */
  grupo?: GrupoAdmin;

  readonly formulario = new FormGroup({
    nombre: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, noVacio, Validators.maxLength(100)],
    }),
    sinLimite: new FormControl(false, { nonNullable: true }),
    racionesSemana: new FormControl<number | null>(null, {
      validators: [Validators.required, Validators.min(1), Validators.max(99)],
    }),
    basico: new FormControl(false, { nonNullable: true }),
  });
  readonly enviando = signal(false);
  readonly error = signal<string | null>(null);

  protected readonly esInvalido = esInvalido;

  ngOnInit(): void {
    const raciones = this.formulario.controls.racionesSemana;
    // Sin límite, el campo de raciones no cuenta (deshabilitado no se valida)
    this.formulario.controls.sinLimite.valueChanges
      .pipe(takeUntilDestroyed(this.destruir))
      .subscribe((sinLimite) => (sinLimite ? raciones.disable() : raciones.enable()));
    if (this.grupo) {
      this.formulario.setValue({
        nombre: this.grupo.nombre,
        sinLimite: this.grupo.racionesSemana === null,
        racionesSemana: this.grupo.racionesSemana,
        basico: this.grupo.basico,
      });
    }
  }

  guardar(): void {
    if (this.formulario.invalid || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    const v = this.formulario.getRawValue();
    const datos = {
      nombre: v.nombre.trim(),
      racionesSemana: v.sinLimite ? null : Number(v.racionesSemana),
      basico: v.basico,
    };
    const peticion = this.grupo
      ? this.api.editarGrupo(this.grupo.id, datos)
      : this.api.crearGrupo(datos);
    peticion.subscribe({
      next: (grupo) => this.modal.close(grupo),
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

/** Abre el modal y emite el grupo guardado (nada si se cancela). */
export function abrirModalGrupo(modales: NgbModal, grupo?: GrupoAdmin): Observable<GrupoAdmin> {
  const ref = modales.open(ModalGrupo, {
    ariaLabelledBy: 'modal-grupo-titulo',
    modalDialogClass: 'animate__animated animate__fadeInDown',
  });
  (ref.componentInstance as ModalGrupo).grupo = grupo;
  return new Observable<GrupoAdmin>((suscriptor) => {
    ref.result.then(
      (guardado: GrupoAdmin) => {
        suscriptor.next(guardado);
        suscriptor.complete();
      },
      () => suscriptor.complete(),
    );
  });
}
