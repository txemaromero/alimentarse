import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ErrorCampo } from '../compartido/error-campo';
import { AuthApi } from '../core/auth-api';
import { esInvalido, mensajeError } from '../core/errores';
import { RequiereConexion } from '../compartido/requiere-conexion';

@Component({
  selector: 'app-olvido-password',
  imports: [ReactiveFormsModule, RouterLink, ErrorCampo, RequiereConexion],
  template: `
    <section class="panel-formulario animate__animated animate__fadeIn">
      <div class="card">
        <div class="card-body">
          <h1 class="titulo-seccion mt-0">Recuperar la contraseña</h1>

          @if (enviado(); as mensaje) {
            <div class="alert alert-success" role="status">{{ mensaje }}</div>
            <p>Si no lo ves en unos minutos, revisa la carpeta de correo no deseado.</p>
            <div class="d-grid">
              <a routerLink="/login" class="btn btn-primary">Volver a iniciar sesión</a>
            </div>
          } @else {
            <p>
              Escribe el email de tu cuenta y te enviaremos un enlace para elegir una contraseña
              nueva. El enlace sirve una sola vez durante 1 hora.
            </p>

            @if (error(); as mensaje) {
              <div class="alert alert-danger" role="alert">{{ mensaje }}</div>
            }

            <form [formGroup]="formulario" (ngSubmit)="enviar()" novalidate>
              <div class="mb-4">
                <label for="olvido-email" class="form-label">Email</label>
                <input
                  id="olvido-email"
                  type="email"
                  class="form-control"
                  formControlName="email"
                  autocomplete="email"
                  [class.is-invalid]="esInvalido(formulario.controls.email)"
                  [attr.aria-invalid]="esInvalido(formulario.controls.email)"
                  aria-describedby="olvido-email-error"
                />
                <app-error-campo
                  [control]="formulario.controls.email"
                  idMensaje="olvido-email-error"
                />
              </div>
              <div class="d-grid gap-2">
                <button
                  type="submit"
                  class="btn btn-primary"
                  [appRequiereConexion]="formulario.invalid || enviando()"
                >
                  @if (enviando()) {
                    <span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>
                  }
                  Enviar el enlace
                </button>
                <a routerLink="/login" class="btn btn-link">Volver a iniciar sesión</a>
              </div>
            </form>
          }
        </div>
      </div>
    </section>
  `,
})
export class OlvidoPassword {
  private readonly authApi = inject(AuthApi);

  readonly formulario = inject(NonNullableFormBuilder).group({
    email: ['', [Validators.required, Validators.email]],
  });
  readonly enviando = signal(false);
  readonly error = signal<string | null>(null);
  /** Mensaje de la API tras enviar (es el mismo exista o no el email). */
  readonly enviado = signal<string | null>(null);

  protected readonly esInvalido = esInvalido;

  enviar(): void {
    if (this.formulario.invalid || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    this.authApi.olvidoPassword(this.formulario.getRawValue().email.trim()).subscribe({
      next: (respuesta) => this.enviado.set(respuesta.mensaje),
      error: (error: HttpErrorResponse) => {
        this.enviando.set(false);
        if (error.status >= 400 && error.status < 500) {
          this.error.set(mensajeError(error));
        }
      },
    });
  }
}
