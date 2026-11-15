import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ErrorCampo } from '../compartido/error-campo';
import { cabeEnBcrypt, coincideCon, revalidarConfirmacion } from '../compartido/validadores';
import { AuthApi } from '../core/auth-api';
import { aplicarErroresServidor, errorApi, esInvalido, mensajeError } from '../core/errores';
import { Notificaciones } from '../core/notificaciones';
import { RequiereConexion } from '../compartido/requiere-conexion';

@Component({
  selector: 'app-restablecer-password',
  imports: [ReactiveFormsModule, RouterLink, ErrorCampo, RequiereConexion],
  template: `
    <section class="panel-formulario animate__animated animate__fadeIn">
      <div class="card">
        <div class="card-body">
          <h1 class="titulo-seccion mt-0">Nueva contraseña</h1>

          @if (!token()) {
            <div class="alert alert-warning" role="alert">
              El enlace no es válido: le falta el código de recuperación. Ábrelo de nuevo desde el
              correo o pide otro.
            </div>
            <div class="d-grid">
              <a routerLink="/olvido-password" class="btn btn-primary">Pedir otro enlace</a>
            </div>
          } @else {
            @if (error(); as mensaje) {
              <div class="alert alert-danger" role="alert">
                {{ mensaje }}
                @if (tokenInvalido()) {
                  <a routerLink="/olvido-password" class="alert-link d-block mt-1">
                    Pedir otro enlace
                  </a>
                }
              </div>
            }

            <form [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
              <div class="mb-3">
                <label for="restablecer-password" class="form-label">Contraseña nueva</label>
                <input
                  id="restablecer-password"
                  type="password"
                  class="form-control"
                  formControlName="password"
                  autocomplete="new-password"
                  maxlength="72"
                  [class.is-invalid]="esInvalido(formulario.controls.password)"
                  [attr.aria-invalid]="esInvalido(formulario.controls.password)"
                  aria-describedby="restablecer-password-ayuda restablecer-password-error"
                />
                <div id="restablecer-password-ayuda" class="form-text">De 8 a 72 caracteres.</div>
                <app-error-campo
                  [control]="formulario.controls.password"
                  idMensaje="restablecer-password-error"
                />
              </div>
              <div class="mb-4">
                <label for="restablecer-confirmacion" class="form-label"
                  >Repite la contraseña</label
                >
                <input
                  id="restablecer-confirmacion"
                  type="password"
                  class="form-control"
                  formControlName="confirmacion"
                  autocomplete="new-password"
                  maxlength="72"
                  [class.is-invalid]="esInvalido(formulario.controls.confirmacion)"
                  [attr.aria-invalid]="esInvalido(formulario.controls.confirmacion)"
                  aria-describedby="restablecer-confirmacion-error"
                />
                <app-error-campo
                  [control]="formulario.controls.confirmacion"
                  idMensaje="restablecer-confirmacion-error"
                />
              </div>
              <div class="d-grid">
                <button
                  type="submit"
                  class="btn btn-primary"
                  [appRequiereConexion]="formulario.invalid || enviando()"
                >
                  @if (enviando()) {
                    <span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>
                  }
                  Guardar la contraseña
                </button>
              </div>
            </form>
          }
        </div>
      </div>
    </section>
  `,
})
export class RestablecerPassword {
  private readonly authApi = inject(AuthApi);
  private readonly router = inject(Router);
  private readonly notificaciones = inject(Notificaciones);

  /** Código del enlace del correo (query param `token`). */
  readonly token = input<string>();

  readonly formulario = inject(NonNullableFormBuilder).group({
    password: [
      '',
      [Validators.required, Validators.minLength(8), Validators.maxLength(72), cabeEnBcrypt],
    ],
    confirmacion: ['', [Validators.required, coincideCon('password')]],
  });
  readonly enviando = signal(false);
  readonly error = signal<string | null>(null);
  readonly tokenInvalido = signal(false);

  protected readonly esInvalido = esInvalido;

  constructor() {
    revalidarConfirmacion(this.formulario, 'password', 'confirmacion', inject(DestroyRef));
  }

  guardar(): void {
    const token = this.token();
    if (!token || this.formulario.invalid || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    this.tokenInvalido.set(false);
    this.authApi.restablecerPassword(token, this.formulario.getRawValue().password).subscribe({
      next: (respuesta) => {
        this.notificaciones.exito(respuesta.mensaje);
        this.router.navigate(['/login']);
      },
      error: (error: HttpErrorResponse) => {
        this.enviando.set(false);
        if (error.status >= 400 && error.status < 500) {
          const detalle = errorApi(error);
          this.tokenInvalido.set(detalle?.codigo === 'TOKEN_INVALIDO');
          if (!aplicarErroresServidor(this.formulario, detalle)) {
            this.error.set(mensajeError(error));
          }
        }
      },
    });
  }
}
