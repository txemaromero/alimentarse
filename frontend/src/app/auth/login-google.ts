import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AccesoGoogle } from '../core/acceso-google';
import { AuthApi } from '../core/auth-api';
import { AvisosPush } from '../core/avisos-push';
import { destinoSeguro } from '../core/guards';
import { Notificaciones } from '../core/notificaciones';
import { Sesion } from '../core/sesion';

/**
 * Vuelta de Google con la sesión ya iniciada (D-085): el backend ha puesto la cookie y aquí se piden los datos de la
 * sesión, que el login normal da en su respuesta.
 */
@Component({
  selector: 'app-login-google',
  imports: [RouterLink],
  template: `
    <section class="panel-formulario animate__animated animate__fadeIn">
      <div class="card">
        <div class="card-body">
          <h1 class="titulo-seccion mt-0">Entrar con Google</h1>
          @if (fallo()) {
            <div class="alert alert-danger" role="alert">
              No se ha podido iniciar la sesión con Google.
            </div>
            <div class="d-grid">
              <a routerLink="/login" class="btn btn-primary">Volver a iniciar sesión</a>
            </div>
          } @else {
            <div class="text-center py-3">
              <span class="spinner-border text-secondary" role="status">
                <span class="visually-hidden">Iniciando la sesión…</span>
              </span>
            </div>
          }
        </div>
      </div>
    </section>
  `,
})
export class LoginGoogle {
  private readonly sesion = inject(Sesion);
  private readonly router = inject(Router);
  private readonly notificaciones = inject(Notificaciones);
  private readonly avisos = inject(AvisosPush);
  private readonly accesoGoogle = inject(AccesoGoogle);

  readonly fallo = signal(false);

  constructor() {
    inject(AuthApi)
      .sesion()
      .subscribe({
        next: (respuesta) => {
          this.sesion.iniciar(respuesta);
          void this.avisos.alIniciarSesion(respuesta.usuario.id);
          this.notificaciones.exito(`¡Hola, ${respuesta.usuario.nombre}!`);
          this.router.navigateByUrl(destinoSeguro(this.accesoGoogle.destino()), {
            replaceUrl: true,
          });
        },
        error: () => this.fallo.set(true),
      });
  }
}
