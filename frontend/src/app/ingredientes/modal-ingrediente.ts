import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  FormControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable } from 'rxjs';
import { ErrorCampo } from '../compartido/error-campo';
import { noVacio } from '../compartido/validadores';
import { CatalogoApi, textoRaciones } from '../core/catalogo-api';
import { aplicarErroresServidor, errorApi, esInvalido, mensajeError } from '../core/errores';
import { Ingrediente } from '../core/modelos';
import { Sesion } from '../core/sesion';
import { IngredientesApi } from './ingredientes-api';
import { RequiereConexion } from '../compartido/requiere-conexion';

/**
 * Modal de alta o edición de un ingrediente (nombre + grupo). Se cierra con el ingrediente guardado. Lo usan la
 * pantalla de ingredientes y el formulario de receta («Crear ingrediente»).
 */
@Component({
  selector: 'app-modal-ingrediente',
  imports: [ReactiveFormsModule, ErrorCampo, RequiereConexion],
  template: `
    <form [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
      <div class="modal-header">
        <h2 class="modal-title h5" id="modal-ingrediente-titulo">
          {{ ingrediente ? 'Editar ingrediente' : 'Nuevo ingrediente' }}
        </h2>
        <button
          type="button"
          class="btn-close"
          aria-label="Cerrar"
          (click)="modal.dismiss()"
        ></button>
      </div>
      <div class="modal-body">
        @if (sinConfirmar()) {
          <div class="alert alert-warning">
            Para crear ingredientes, confirma antes tu email con el enlace que te enviamos. Si no lo
            encuentras, pide otro desde «Mi perfil».
          </div>
        }
        @if (error(); as mensaje) {
          <div class="alert alert-danger" role="alert">{{ mensaje }}</div>
        }
        <div class="mb-3">
          <label for="ingrediente-nombre" class="form-label">Nombre</label>
          <input
            id="ingrediente-nombre"
            type="text"
            class="form-control"
            formControlName="nombre"
            maxlength="100"
            autocomplete="off"
            ngbAutofocus
            [class.is-invalid]="esInvalido(formulario.controls.nombre)"
            [attr.aria-invalid]="esInvalido(formulario.controls.nombre)"
            aria-describedby="ingrediente-nombre-error"
          />
          <app-error-campo
            [control]="formulario.controls.nombre"
            idMensaje="ingrediente-nombre-error"
          />
        </div>
        <div>
          <label for="ingrediente-grupo" class="form-label">Grupo de alimentos</label>
          <select
            id="ingrediente-grupo"
            class="form-select"
            formControlName="grupo"
            [class.is-invalid]="esInvalido(formulario.controls.grupo)"
            [attr.aria-invalid]="esInvalido(formulario.controls.grupo)"
            aria-describedby="ingrediente-grupo-error"
          >
            <option [ngValue]="null" disabled>Elige un grupo…</option>
            @for (grupo of grupos(); track grupo.id) {
              <option [ngValue]="grupo.id">{{ grupo.nombre }} ({{ textoRaciones(grupo) }})</option>
            }
          </select>
          <app-error-campo
            [control]="formulario.controls.grupo"
            idMensaje="ingrediente-grupo-error"
          />
        </div>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-outline-secondary" (click)="modal.dismiss()">
          Cancelar
        </button>
        <button
          type="submit"
          class="btn btn-primary"
          [appRequiereConexion]="formulario.invalid || enviando() || sinConfirmar()"
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
export class ModalIngrediente implements OnInit {
  protected readonly modal = inject(NgbActiveModal);
  private readonly api = inject(IngredientesApi);
  private readonly sesion = inject(Sesion);

  /** Ingrediente a editar; sin él, es un alta. Lo asigna `abrirModalIngrediente`. */
  ingrediente: Ingrediente | null = null;
  /** Texto inicial del nombre en un alta (lo que se estaba buscando). */
  nombreInicial = '';

  readonly grupos = toSignal(inject(CatalogoApi).grupos$, { initialValue: [] });
  readonly formulario = inject(NonNullableFormBuilder).group({
    nombre: ['', [Validators.required, noVacio, Validators.maxLength(100)]],
    grupo: new FormControl<number | null>(null, Validators.required),
  });
  readonly enviando = signal(false);
  readonly error = signal<string | null>(null);
  private readonly alta = signal(true);

  /**
   * Crear ingredientes exige el email confirmado, salvo a los administradores (D-089): el catálogo es de todos. Editar
   * solo lo hace un administrador.
   */
  readonly sinConfirmar = computed(
    () => this.alta() && !this.sesion.esAdmin() && this.sesion.usuario()?.emailVerificado === false,
  );

  protected readonly esInvalido = esInvalido;
  protected readonly textoRaciones = textoRaciones;

  ngOnInit(): void {
    this.alta.set(this.ingrediente === null);
    this.formulario.reset({
      nombre: this.ingrediente?.nombre ?? this.nombreInicial,
      grupo: this.ingrediente?.grupo.id ?? null,
    });
  }

  guardar(): void {
    if (this.formulario.invalid || this.enviando() || this.sinConfirmar()) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    const datos = {
      nombre: this.formulario.controls.nombre.value.trim(),
      grupo: this.formulario.controls.grupo.value!,
    };
    const peticion = this.ingrediente
      ? this.api.editar(this.ingrediente.id, datos)
      : this.api.crear(datos);
    peticion.subscribe({
      next: (guardado) => this.modal.close(guardado),
      error: (error: HttpErrorResponse) => {
        this.enviando.set(false);
        if (error.status >= 400 && error.status < 500) {
          if (!aplicarErroresServidor(this.formulario, errorApi(error))) {
            this.error.set(mensajeError(error));
          }
        }
      },
    });
  }
}

/**
 * Abre el modal y devuelve un observable que emite el ingrediente guardado (y nada si se cancela).
 */
export function abrirModalIngrediente(
  modales: NgbModal,
  opciones: { ingrediente?: Ingrediente; nombreInicial?: string } = {},
): Observable<Ingrediente> {
  const ref = modales.open(ModalIngrediente, {
    ariaLabelledBy: 'modal-ingrediente-titulo',
    modalDialogClass: 'animate__animated animate__fadeInDown',
  });
  const componente = ref.componentInstance as ModalIngrediente;
  componente.ingrediente = opciones.ingrediente ?? null;
  componente.nombreInicial = opciones.nombreInicial ?? '';
  return new Observable<Ingrediente>((suscriptor) => {
    ref.result.then(
      (guardado: Ingrediente) => {
        suscriptor.next(guardado);
        suscriptor.complete();
      },
      () => suscriptor.complete(),
    );
  });
}
