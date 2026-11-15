import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ErrorCampo } from '../compartido/error-campo';
import { RequiereConexion } from '../compartido/requiere-conexion';
import { AccesoGoogle } from '../core/acceso-google';
import { AuthApi } from '../core/auth-api';
import { AvisosPush } from '../core/avisos-push';
import { aplicarErroresServidor, errorApi, esInvalido, mensajeError } from '../core/errores';
import { destinoSeguro } from '../core/guards';
import { Notificaciones } from '../core/notificaciones';
import { Sesion } from '../core/sesion';

/**
 * Vuelta de Google con el email de una cuenta de Toca sin vincular (D-086): para vincularlas hay que escribir, una sola
 * vez, la contraseña de esa cuenta. Así nadie que haya creado una cuenta con el email de otra persona se queda con su
 * acceso con Google.
 */
@Component({
  selector: 'app-vincular-google',
  imports: [ReactiveFormsModule, RouterLink, ErrorCampo, RequiereConexion],
  template: `
    <section class="panel-formulario animate__animated animate__fadeIn">
      <div class="card">
        <div class="card-body">
          <h1 class="titulo-seccion mt-0">Vincular tu cuenta con Google</h1>

          @if (cargando()) {
            <div class="text-center py-3">
              <span class="spinner-border text-secondary" role="status">
                <span class="visually-hidden">Cargando…</span>
              </span>
            </div>
          } @else if (caducado()) {
            <div class="alert alert-warning" role="alert">
              La vinculación con Google ha caducado o no se ha empezado. Tienes 15 minutos para
              terminarla desde que vuelves de Google.
            </div>
            <div class="d-flex flex-column flex-sm-row gap-2 justify-content-end">
              <a routerLink="/login" class="btn btn-outline-secondary">Iniciar sesión</a>
              <button
                type="button"
                class="btn btn-primary"
                appRequiereConexion
                (click)="volverAGoogle()"
              >
                Volver a entrar con Google
              </button>
            </div>
          } @else if (fallo()) {
            <div class="alert alert-danger d-flex align-items-center gap-3" role="alert">
              <span class="flex-grow-1">No se ha podido cargar la vinculación.</span>
              <button type="button" class="btn btn-sm btn-outline-danger" (click)="cargar()">
                Reintentar
              </button>
            </div>
          } @else if (email(); as correo) {
            <p>
              Ya tienes una cuenta en Toca con el email
              <strong class="text-break">{{ correo }}</strong
              >. Para entrar también con Google, escribe su contraseña. Solo te la pediremos esta
              vez.
            </p>

            @if (error(); as mensaje) {
              <div class="alert alert-danger" role="alert">{{ mensaje }}</div>
            }

            <form [formGroup]="formulario" (ngSubmit)="vincular()" novalidate>
              <!-- Para que el gestor de contraseñas sepa de qué cuenta es -->
              <input
                type="text"
                class="d-none"
                autocomplete="username"
                [value]="correo"
                aria-hidden="true"
                tabindex="-1"
                readonly
              />
              <label for="vincular-password" class="form-label">Contraseña de Toca</label>
              <input
                id="vincular-password"
                type="password"
                class="form-control"
                formControlName="password"
                autocomplete="current-password"
                [class.is-invalid]="esInvalido(formulario.controls.password)"
                [attr.aria-invalid]="esInvalido(formulario.controls.password)"
                aria-describedby="vincular-password-ayuda vincular-password-error"
              />
              <div id="vincular-password-ayuda" class="form-text">
                ¿No la recuerdas? Pide un enlace en
                <a routerLink="/olvido-password">«He olvidado la contraseña»</a>, elige una nueva y
                vuelve a entrar con Google.
              </div>
              <app-error-campo
                [control]="formulario.controls.password"
                idMensaje="vincular-password-error"
              />

              <div class="d-flex flex-column flex-sm-row gap-2 justify-content-end mt-4">
                <a routerLink="/login" class="btn btn-outline-secondary">Cancelar</a>
                <button
                  type="submit"
                  class="btn btn-primary"
                  [appRequiereConexion]="formulario.invalid || enviando()"
                >
                  @if (enviando()) {
                    <span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>
                  }
                  Vincular y entrar
                </button>
              </div>
            </form>
          }
        </div>
      </div>
    </section>
  `,
})
export class VincularGoogle {
  private readonly authApi = inject(AuthApi);
  private readonly sesion = inject(Sesion);
  private readonly router = inject(Router);
  private readonly notificaciones = inject(Notificaciones);
  private readonly avisos = inject(AvisosPush);
  private readonly accesoGoogle = inject(AccesoGoogle);

  readonly formulario = inject(NonNullableFormBuilder).group({
    password: ['', Validators.required],
  });
  readonly cargando = signal(true);
  /** No hay vinculación pendiente: nunca la hubo o han pasado más de 15 minutos. */
  readonly caducado = signal(false);
  readonly fallo = signal(false);
  readonly email = signal<string | null>(null);
  readonly enviando = signal(false);
  readonly error = signal<string | null>(null);

  protected readonly esInvalido = esInvalido;

  constructor() {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.fallo.set(false);
    this.authApi.vinculacionGoogle().subscribe({
      next: (datos) => {
        this.email.set(datos.email);
        this.cargando.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.cargando.set(false);
        if (error.status === 404) {
          this.caducado.set(true);
        } else {
          this.fallo.set(true);
        }
      },
    });
  }

  volverAGoogle(): void {
    this.accesoGoogle.entrar(this.accesoGoogle.destino());
  }

  vincular(): void {
    if (this.formulario.invalid || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    this.authApi.vincularGoogle(this.formulario.controls.password.value).subscribe({
      next: (respuesta) => {
        this.sesion.iniciar(respuesta);
        void this.avisos.alIniciarSesion(respuesta.usuario.id);
        this.notificaciones.exito(
          `Cuenta vinculada con Google. ¡Hola, ${respuesta.usuario.nombre}!`,
        );
        this.router.navigateByUrl(destinoSeguro(this.accesoGoogle.destino()), {
          replaceUrl: true,
        });
      },
      error: (error: HttpErrorResponse) => {
        this.enviando.set(false);
        if (error.status === 404) {
          this.caducado.set(true);
        } else if (error.status >= 400 && error.status < 500) {
          // La contraseña incorrecta va junto a su campo; el resto (bloqueada, ya vinculada…), arriba
          if (!aplicarErroresServidor(this.formulario, errorApi(error))) {
            this.error.set(mensajeError(error));
          }
        }
      },
    });
  }
}
