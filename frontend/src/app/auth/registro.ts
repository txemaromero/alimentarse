import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ErrorCampo } from '../compartido/error-campo';
import {
  cabeEnBcrypt,
  coincideCon,
  noVacio,
  revalidarConfirmacion,
} from '../compartido/validadores';
import { AuthApi } from '../core/auth-api';
import { aplicarErroresServidor, errorApi, esInvalido, mensajeError } from '../core/errores';
import { Notificaciones } from '../core/notificaciones';
import { RequiereConexion } from '../compartido/requiere-conexion';

/** Mismas reglas que `RegistroRequest` en el backend. */
export const PATRON_ID_USUARIO = /^[a-zA-Z0-9._-]*$/;

@Component({
  selector: 'app-registro',
  imports: [ReactiveFormsModule, RouterLink, ErrorCampo, RequiereConexion],
  templateUrl: './registro.html',
})
export class Registro {
  private readonly authApi = inject(AuthApi);
  private readonly router = inject(Router);
  private readonly notificaciones = inject(Notificaciones);

  readonly formulario = inject(NonNullableFormBuilder).group({
    id: [
      '',
      [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(30),
        Validators.pattern(PATRON_ID_USUARIO),
      ],
    ],
    nombre: ['', [Validators.required, noVacio, Validators.maxLength(100)]],
    apellido1: ['', [Validators.required, noVacio, Validators.maxLength(100)]],
    apellido2: ['', Validators.maxLength(100)],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
    password: [
      '',
      [Validators.required, Validators.minLength(8), Validators.maxLength(72), cabeEnBcrypt],
    ],
    confirmacion: ['', [Validators.required, coincideCon('password')]],
  });
  readonly enviando = signal(false);
  readonly error = signal<string | null>(null);

  protected readonly esInvalido = esInvalido;

  constructor() {
    revalidarConfirmacion(this.formulario, 'password', 'confirmacion', inject(DestroyRef));
  }

  registrar(): void {
    if (this.formulario.invalid || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    const valores = this.formulario.getRawValue();
    this.authApi
      .registro({
        id: valores.id.trim(),
        nombre: valores.nombre.trim(),
        apellido1: valores.apellido1.trim(),
        apellido2: valores.apellido2.trim() || null,
        email: valores.email.trim(),
        password: valores.password,
      })
      .subscribe({
        next: (perfil) => {
          this.notificaciones.exito(
            `Cuenta creada, ${perfil.nombre}. Ya puedes iniciar sesión con tu usuario o tu email.`,
          );
          this.router.navigate(['/login']);
        },
        error: (error: HttpErrorResponse) => {
          this.enviando.set(false);
          if (error.status >= 400 && error.status < 500) {
            const detalle = errorApi(error);
            // Los errores por campo (id o email ya usados…) van junto a cada campo
            if (!aplicarErroresServidor(this.formulario, detalle)) {
              this.error.set(mensajeError(error));
            }
          }
        },
      });
  }
}
