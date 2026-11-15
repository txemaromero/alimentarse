import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faEnvelope } from '@fortawesome/free-solid-svg-icons';
import { filter, map } from 'rxjs';
import { RequiereConexion } from '../compartido/requiere-conexion';
import { errorApi, mensajeError } from '../core/errores';
import { Notificaciones } from '../core/notificaciones';
import { Sesion } from '../core/sesion';
import { PerfilApi } from '../perfil/perfil-api';

/**
 * Aviso bajo la cabecera mientras el email de la cuenta está sin confirmar (D-088): se puede usar todo, pero se
 * recuerda confirmarlo y se puede pedir el enlace otra vez. No aparece en la propia pantalla de confirmación.
 */
@Component({
  selector: 'app-aviso-email',
  imports: [RouterLink, FaIconComponent, RequiereConexion],
  template: `
    @if (visible()) {
      <!-- Una región con nombre: fuera de los landmarks, axe lo marca (todo el contenido debe estar en alguno) -->
      <section class="aviso-email" aria-label="Email sin confirmar">
        <div class="container d-flex flex-wrap align-items-center gap-2 py-2">
          <span class="me-auto">
            <fa-icon [icon]="icono" class="me-1" aria-hidden="true" />
            <strong>Tu email está sin confirmar.</strong> Abre el enlace que te enviamos o pide
            otro.
          </span>
          <button
            type="button"
            class="btn btn-sm btn-outline-dark"
            [appRequiereConexion]="enviando()"
            (click)="enviarEnlace()"
          >
            Enviar el enlace
          </button>
          <a routerLink="/mi-perfil" class="btn btn-sm btn-link text-reset">Cambiar el email</a>
        </div>
      </section>
    }
  `,
  styles: `
    .aviso-email {
      background-color: #e7f1f7;
      color: #133b52;
      border-bottom: 1px solid #a9cbe0;
      font-size: 0.95rem;
    }
  `,
})
export class AvisoEmail {
  private readonly sesion = inject(Sesion);
  private readonly api = inject(PerfilApi);
  private readonly notificaciones = inject(Notificaciones);
  private readonly router = inject(Router);

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((evento) => evento instanceof NavigationEnd),
      map((evento) => evento.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );

  readonly enviando = signal(false);
  /** Las sesiones guardadas antes de D-088 no lo traen: entonces no se avisa. */
  readonly visible = computed(
    () =>
      this.sesion.autenticado() &&
      this.sesion.usuario()?.emailVerificado === false &&
      !this.url().startsWith('/confirmar-email'),
  );

  protected readonly icono = faEnvelope;

  enviarEnlace(): void {
    this.enviando.set(true);
    this.api.reenviarConfirmacion().subscribe({
      next: ({ email }) => {
        this.enviando.set(false);
        this.notificaciones.exito(`Te hemos enviado el enlace a ${email}.`);
      },
      error: (error: HttpErrorResponse) => {
        this.enviando.set(false);
        if (errorApi(error)?.codigo === 'EMAIL_YA_CONFIRMADO') {
          // Lo confirmó en otro dispositivo
          this.sesion.actualizarEmailVerificado(true);
          this.notificaciones.info('Tu email ya está confirmado.');
        } else if (error.status >= 400 && error.status < 500) {
          this.notificaciones.error(mensajeError(error));
        }
      },
    });
  }
}
