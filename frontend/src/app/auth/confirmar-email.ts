import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthApi } from '../core/auth-api';
import { mensajeError } from '../core/errores';
import { Sesion } from '../core/sesion';

/**
 * El enlace del correo para confirmar el email (D-088): `/confirmar-email?token=…`. Funciona sin sesión, porque puede
 * abrirse en otro dispositivo; si la sesión de este navegador es de esa cuenta, deja de mostrar el aviso.
 */
@Component({
  selector: 'app-confirmar-email',
  imports: [RouterLink],
  template: `
    <section class="panel-formulario animate__animated animate__fadeIn">
      <div class="card">
        <div class="card-body">
          <h1 class="titulo-seccion mt-0">Confirmar el email</h1>
          @if (confirmado(); as email) {
            <div class="alert alert-success" role="status">
              Listo: <strong class="text-break">{{ email }}</strong> es el email confirmado de tu
              cuenta.
            </div>
            <div class="d-grid">
              @if (sesion.autenticado()) {
                <a routerLink="/" class="btn btn-primary">Ir a la portada</a>
              } @else {
                <a routerLink="/login" class="btn btn-primary">Iniciar sesión</a>
              }
            </div>
          } @else if (error(); as mensaje) {
            <div class="alert alert-danger" role="alert">{{ mensaje }}</div>
            <div class="d-grid">
              @if (sesion.autenticado()) {
                <a routerLink="/mi-perfil" class="btn btn-primary">Ir a Mi perfil</a>
              } @else {
                <a routerLink="/login" class="btn btn-primary">Iniciar sesión</a>
              }
            </div>
          } @else {
            <div class="text-center py-3">
              <span class="spinner-border text-secondary" role="status">
                <span class="visually-hidden">Confirmando el email…</span>
              </span>
            </div>
          }
        </div>
      </div>
    </section>
  `,
})
export class ConfirmarEmail implements OnInit {
  private readonly authApi = inject(AuthApi);
  protected readonly sesion = inject(Sesion);

  /** El token del enlace (query param `token`). */
  readonly token = input<string>();

  readonly confirmado = signal<string | null>(null);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    const token = this.token()?.trim();
    if (!token) {
      this.error.set('Falta el token del enlace. Ábrelo tal como te llegó en el correo.');
      return;
    }
    this.authApi.confirmarEmail(token).subscribe({
      next: (respuesta) => {
        if (this.sesion.usuario()?.id === respuesta.usuario) {
          this.sesion.actualizarEmailVerificado(true);
        }
        this.confirmado.set(respuesta.email);
      },
      error: (error: HttpErrorResponse) =>
        this.error.set(
          error.status >= 400 && error.status < 500
            ? mensajeError(error)
            : 'No se ha podido confirmar el email. Inténtalo de nuevo más tarde.',
        ),
    });
  }
}
