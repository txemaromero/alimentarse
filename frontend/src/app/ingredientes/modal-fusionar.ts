import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { NgbActiveModal, NgbModal, NgbTypeahead } from '@ng-bootstrap/ng-bootstrap';
import { Observable } from 'rxjs';
import { AdminApi } from '../admin/admin-api';
import { ErrorCampo } from '../compartido/error-campo';
import { RequiereConexion } from '../compartido/requiere-conexion';
import { aplicarErroresServidor, errorApi, esInvalido, mensajeError } from '../core/errores';
import { FusionIngredientes, Ingrediente } from '../core/modelos';
import { busquedaIngredientes, nombreIngrediente } from './busqueda-ingredientes';
import { IngredientesApi } from './ingredientes-api';

/** Exige que se haya elegido una sugerencia, no solo escrito un texto. */
const elegido = (control: { value: unknown }) =>
  control.value && typeof control.value === 'object' ? null : { required: true };

/**
 * Fusionar un ingrediente duplicado con otro (D-065): sus recetas pasan a llevar el elegido y el duplicado se borra.
 */
@Component({
  selector: 'app-modal-fusionar',
  imports: [ReactiveFormsModule, NgbTypeahead, ErrorCampo, RequiereConexion],
  template: `
    <form [formGroup]="formulario" (ngSubmit)="fusionar()" novalidate>
      <div class="modal-header">
        <h2 class="modal-title h5" id="modal-fusionar-titulo">Fusionar «{{ origen.nombre }}»</h2>
        <button
          type="button"
          class="btn-close"
          aria-label="Cerrar"
          (click)="modal.dismiss()"
        ></button>
      </div>
      <div class="modal-body">
        <p>
          Las recetas que llevan <strong>{{ origen.nombre }}</strong> pasarán a llevar el
          ingrediente que elijas, y «{{ origen.nombre }}» se borrará. Si una receta ya llevaba los
          dos, se suman sus cantidades.
        </p>
        @if (error(); as mensaje) {
          <div class="alert alert-danger" role="alert">{{ mensaje }}</div>
        }
        <label for="fusionar-destino" class="form-label">Fusionar con</label>
        <input
          id="fusionar-destino"
          type="text"
          class="form-control"
          formControlName="destino"
          autocomplete="off"
          placeholder="Escribe para buscar…"
          ngbAutofocus
          [ngbTypeahead]="buscar"
          [inputFormatter]="nombre"
          [resultFormatter]="nombre"
          [class.is-invalid]="esInvalido(formulario.controls.destino)"
          [attr.aria-invalid]="esInvalido(formulario.controls.destino)"
          aria-describedby="fusionar-destino-error"
        />
        <app-error-campo
          [control]="formulario.controls.destino"
          idMensaje="fusionar-destino-error"
        />
        @if (cambiaDeGrupo(); as grupo) {
          <div class="alert alert-warning mt-3 mb-0" role="status">
            Es de otro grupo: esas recetas dejarán de contar como
            <strong>{{ origen.grupo.nombre }}</strong> y pasarán a contar como
            <strong>{{ grupo }}</strong
            >, también en lo que ya se ha comido.
          </div>
        }
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
          Fusionar
        </button>
      </div>
    </form>
  `,
})
export class ModalFusionar {
  protected readonly modal = inject(NgbActiveModal);
  private readonly admin = inject(AdminApi);

  /** El duplicado. Lo asigna `abrirModalFusionar`. */
  origen!: Ingrediente;

  readonly formulario = new FormGroup({
    destino: new FormControl<Ingrediente | string | null>(null, { validators: [elegido] }),
  });
  readonly enviando = signal(false);
  readonly error = signal<string | null>(null);

  protected readonly buscar = busquedaIngredientes(inject(IngredientesApi), () => [this.origen.id]);
  protected readonly nombre = nombreIngrediente;
  protected readonly esInvalido = esInvalido;

  /** El nombre del grupo del elegido si es distinto del del duplicado. */
  cambiaDeGrupo(): string | null {
    const destino = this.formulario.controls.destino.value;
    return destino && typeof destino === 'object' && destino.grupo.id !== this.origen.grupo.id
      ? destino.grupo.nombre
      : null;
  }

  fusionar(): void {
    const destino = this.formulario.controls.destino.value;
    if (this.formulario.invalid || this.enviando() || !destino || typeof destino !== 'object') {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    this.admin.fusionarIngrediente(this.origen.id, destino.id).subscribe({
      next: (resultado) => this.modal.close(resultado),
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

/** Abre el modal y emite el resultado de la fusión (nada si se cancela). */
export function abrirModalFusionar(
  modales: NgbModal,
  origen: Ingrediente,
): Observable<FusionIngredientes> {
  const ref = modales.open(ModalFusionar, {
    ariaLabelledBy: 'modal-fusionar-titulo',
    modalDialogClass: 'animate__animated animate__fadeInDown',
  });
  (ref.componentInstance as ModalFusionar).origen = origen;
  return new Observable<FusionIngredientes>((suscriptor) => {
    ref.result.then(
      (resultado: FusionIngredientes) => {
        suscriptor.next(resultado);
        suscriptor.complete();
      },
      () => suscriptor.complete(),
    );
  });
}
