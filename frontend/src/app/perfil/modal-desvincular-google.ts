import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable } from 'rxjs';
import { ErrorCampo } from '../compartido/error-campo';
import { RequiereConexion } from '../compartido/requiere-conexion';
import { aplicarErroresServidor, errorApi, esInvalido, mensajeError } from '../core/errores';
import { Perfil } from '../core/modelos';
import { PerfilApi } from './perfil-api';

/** Dejar de entrar con Google (D-086): pide la contraseña, que será la única forma de entrar. */
@Component({
  selector: 'app-modal-desvincular-google',
  imports: [ReactiveFormsModule, ErrorCampo, RequiereConexion],
  template: `
    <form [formGroup]="formulario" (ngSubmit)="confirmar()" novalidate>
      <div class="modal-header">
        <h2 class="modal-title h5" id="modal-desvincular-titulo">Desvincular Google</h2>
        <button
          type="button"
          class="btn-close"
          aria-label="Cerrar"
          [disabled]="enviando()"
          (click)="modal.dismiss()"
        ></button>
      </div>
      <div class="modal-body">
        <p>
          Ya no podrás entrar con tu cuenta de Google: entrarás con tu nombre de usuario o tu email
          y tu contraseña. Si quieres, podrás volver a vincularla entrando con Google.
        </p>
        @if (error(); as mensaje) {
          <div class="alert alert-danger" role="alert">{{ mensaje }}</div>
        }
        <label for="desvincular-password" class="form-label">
          Escribe tu contraseña para confirmarlo
        </label>
        <input
          id="desvincular-password"
          type="password"
          class="form-control"
          formControlName="password"
          autocomplete="current-password"
          ngbAutofocus
          [class.is-invalid]="esInvalido(formulario.controls.password)"
          [attr.aria-invalid]="esInvalido(formulario.controls.password)"
          aria-describedby="desvincular-password-error"
        />
        <app-error-campo
          [control]="formulario.controls.password"
          idMensaje="desvincular-password-error"
        />
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
          class="btn btn-primary"
          [appRequiereConexion]="formulario.invalid || enviando()"
        >
          @if (enviando()) {
            <span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>
          }
          Desvincular
        </button>
      </div>
    </form>
  `,
})
export class ModalDesvincularGoogle {
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
    this.api.desvincularGoogle(this.formulario.controls.password.value).subscribe({
      next: (perfil) => this.modal.close(perfil),
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

/** Abre el modal; emite el perfil actualizado cuando ya se ha desvinculado (nada si se cancela). */
export function abrirModalDesvincularGoogle(modales: NgbModal): Observable<Perfil> {
  const ref = modales.open(ModalDesvincularGoogle, {
    ariaLabelledBy: 'modal-desvincular-titulo',
    modalDialogClass: 'animate__animated animate__fadeInDown',
  });
  return new Observable<Perfil>((suscriptor) => {
    ref.result.then(
      (perfil: Perfil) => {
        suscriptor.next(perfil);
        suscriptor.complete();
      },
      () => suscriptor.complete(),
    );
  });
}
