import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable } from 'rxjs';
import { ErrorCampo } from '../compartido/error-campo';
import { RequiereConexion } from '../compartido/requiere-conexion';
import { aplicarErroresServidor, errorApi, esInvalido, mensajeError } from '../core/errores';
import { PerfilApi } from './perfil-api';

/** Darse de baja (D-064): explica qué se borra y pide la contraseña para confirmarlo. */
@Component({
  selector: 'app-modal-baja',
  imports: [ReactiveFormsModule, ErrorCampo, RequiereConexion],
  template: `
    <form [formGroup]="formulario" (ngSubmit)="confirmar()" novalidate>
      <div class="modal-header">
        <h2 class="modal-title h5" id="modal-baja-titulo">Eliminar mi cuenta</h2>
        <button
          type="button"
          class="btn-close"
          aria-label="Cerrar"
          [disabled]="enviando()"
          (click)="modal.dismiss()"
        ></button>
      </div>
      <div class="modal-body">
        <p>Se borrarán <strong>para siempre</strong>:</p>
        <ul>
          <li>tus datos personales y tu acceso;</li>
          <li>tus recetas, publicadas o no, con sus fotos;</li>
          <li>todo lo que has apuntado que comes y tus «me gusta».</li>
        </ul>
        <p class="small text-body-secondary">
          Si alguien ya había comido alguna de tus recetas, en su histórico se conservarán solo su
          título y sus ingredientes, sin tu nombre. Si quieres guardar una copia, descarga antes tus
          datos.
        </p>
        @if (error(); as mensaje) {
          <div class="alert alert-danger" role="alert">{{ mensaje }}</div>
        }
        <label for="baja-password" class="form-label">Escribe tu contraseña para confirmarlo</label>
        <input
          id="baja-password"
          type="password"
          class="form-control"
          formControlName="password"
          autocomplete="current-password"
          ngbAutofocus
          [class.is-invalid]="esInvalido(formulario.controls.password)"
          [attr.aria-invalid]="esInvalido(formulario.controls.password)"
          aria-describedby="baja-password-error"
        />
        <app-error-campo [control]="formulario.controls.password" idMensaje="baja-password-error" />
      </div>
      <div class="modal-footer">
        <button
          type="button"
          class="btn btn-outline-secondary"
          [disabled]="enviando()"
          (click)="modal.dismiss()"
        >
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
          Eliminar mi cuenta
        </button>
      </div>
    </form>
  `,
})
export class ModalBaja {
  protected readonly modal = inject(NgbActiveModal);
  private readonly api = inject(PerfilApi);

  readonly formulario = new FormGroup({
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });
  readonly enviando = signal(false);
  readonly error = signal<string | null>(null);

  protected readonly esInvalido = esInvalido;

  confirmar(): void {
    if (this.formulario.invalid || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    this.api.darseDeBaja(this.formulario.controls.password.value).subscribe({
      next: () => this.modal.close(true),
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

/** Abre el modal; emite cuando la cuenta ya se ha eliminado (nada si se cancela). */
export function abrirModalBaja(modales: NgbModal): Observable<void> {
  const ref = modales.open(ModalBaja, {
    ariaLabelledBy: 'modal-baja-titulo',
    modalDialogClass: 'animate__animated animate__fadeInDown',
    backdrop: 'static',
  });
  return new Observable<void>((suscriptor) => {
    ref.result.then(
      () => {
        suscriptor.next();
        suscriptor.complete();
      },
      () => suscriptor.complete(),
    );
  });
}
