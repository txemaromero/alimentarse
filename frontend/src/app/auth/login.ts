import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, input, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faEye, faEyeSlash, faRightToBracket } from '@fortawesome/free-solid-svg-icons';
import { ErrorCampo } from '../compartido/error-campo';
import { noVacio } from '../compartido/validadores';
import { AccesoGoogle } from '../core/acceso-google';
import { AuthApi } from '../core/auth-api';
import { AvisosPush } from '../core/avisos-push';
import { Conexion } from '../core/conexion';
import { esInvalido, mensajeError } from '../core/errores';
import { destinoSeguro } from '../core/guards';
import { Notificaciones } from '../core/notificaciones';
import { Sesion } from '../core/sesion';
import { RequiereConexion } from '../compartido/requiere-conexion';

/** Por qué no se ha podido entrar con Google (`/login?google=…`, D-085). */
export const MENSAJES_GOOGLE: Readonly<Record<string, string>> = {
  bloqueada: 'Tu cuenta está bloqueada por la administración.',
  'email-no-verificado':
    'Tu cuenta de Google no tiene el email verificado. Verifícalo en Google o regístrate con un usuario y una contraseña.',
  'otra-cuenta':
    'Tu cuenta de Toca con ese email ya está vinculada con otra cuenta de Google. Entra con tu usuario y tu contraseña.',
  cancelado: 'Has cancelado el acceso con Google.',
  error: 'No se ha podido entrar con Google. Inténtalo de nuevo.',
};

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, FaIconComponent, ErrorCampo, RequiereConexion],
  templateUrl: './login.html',
})
export class Login {
  private readonly authApi = inject(AuthApi);
  private readonly sesion = inject(Sesion);
  private readonly router = inject(Router);
  private readonly notificaciones = inject(Notificaciones);
  private readonly avisos = inject(AvisosPush);
  private readonly accesoGoogle = inject(AccesoGoogle);

  /** Página a la que volver tras iniciar sesión (query param `returnUrl`). */
  readonly returnUrl = input<string>();
  /** Por qué ha vuelto de Google sin entrar (query param `google`). */
  readonly google = input<string>();

  /** Si se puede entrar con Google: solo con las credenciales configuradas en el servidor (D-085). */
  readonly conGoogle = toSignal(
    inject(Conexion).enLinea()
      ? this.authApi.proveedores().pipe(
          map((proveedores) => proveedores.google),
          catchError(() => of(false)),
        )
      : of(false),
    { initialValue: false },
  );
  readonly mensajeGoogle = computed(() => {
    const motivo = this.google();
    return motivo ? (MENSAJES_GOOGLE[motivo] ?? MENSAJES_GOOGLE['error']) : null;
  });

  readonly formulario = inject(NonNullableFormBuilder).group({
    usuario: ['', [Validators.required, noVacio]],
    password: ['', Validators.required],
  });
  readonly enviando = signal(false);
  readonly error = signal<string | null>(null);
  readonly verPassword = signal(false);

  protected readonly esInvalido = esInvalido;
  protected readonly iconos = { ver: faEye, ocultar: faEyeSlash, google: faRightToBracket };

  entrarConGoogle(): void {
    this.accesoGoogle.entrar(this.returnUrl());
  }

  entrar(): void {
    if (this.formulario.invalid || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    const { usuario, password } = this.formulario.getRawValue();
    this.authApi.login(usuario.trim(), password).subscribe({
      next: (respuesta) => {
        this.sesion.iniciar(respuesta);
        // Si este navegador recibía los avisos de otra persona, dejan de llegar (D-078)
        void this.avisos.alIniciarSesion(respuesta.usuario.id);
        this.notificaciones.exito(`¡Hola, ${respuesta.usuario.nombre}!`);
        this.router.navigateByUrl(destinoSeguro(this.returnUrl()));
      },
      error: (error: HttpErrorResponse) => {
        this.enviando.set(false);
        // Los fallos de red y los 5xx ya los avisa el interceptor
        if (error.status >= 400 && error.status < 500) {
          this.error.set(mensajeError(error));
        }
      },
    });
  }
}
