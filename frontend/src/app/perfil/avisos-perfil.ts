import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RequiereConexion } from '../compartido/requiere-conexion';
import { AvisosPush } from '../core/avisos-push';
import { Conexion } from '../core/conexion';
import { mensajeError } from '../core/errores';
import { Notificaciones } from '../core/notificaciones';
import { Sesion } from '../core/sesion';

/**
 * «Avisos» en «Mi perfil» (D-078): activar en este navegador el recordatorio de la comida y la cena planificadas, y
 * enviar un aviso de prueba. Explica por qué no se puede cuando el navegador no los admite o los ha bloqueado.
 */
@Component({
  selector: 'app-avisos-perfil',
  imports: [RequiereConexion],
  template: `
    <section class="card" aria-labelledby="perfil-avisos">
      <div class="card-body">
        <h2 id="perfil-avisos" class="h5 mb-3">Avisos</h2>
        @switch (avisos.estado()) {
          @case ('comprobando') {
            <p class="small text-body-secondary mb-0">Comprobando si puedes recibir avisos…</p>
          }
          @case ('no-disponibles') {
            <p class="small text-body-secondary mb-0">
              Los avisos no están disponibles en este navegador. Prueba con Toca instalada como
              aplicación o con otro navegador.
            </p>
          }
          @case ('bloqueados') {
            <p class="small mb-0">
              Has bloqueado las notificaciones de Toca en este navegador. Para recibir los avisos,
              permítelas en la configuración del sitio y vuelve a esta pantalla.
            </p>
          }
          @default {
            <div class="form-check form-switch mb-1">
              <input
                class="form-check-input"
                type="checkbox"
                role="switch"
                id="perfil-avisos-comidas"
                [checked]="activados()"
                [disabled]="ocupado() || !conexion.enLinea()"
                aria-describedby="perfil-avisos-ayuda"
                (change)="alternar($event)"
              />
              <label class="form-check-label" for="perfil-avisos-comidas"
                >Recordarme la comida y la cena del planificador</label
              >
            </div>
            <p id="perfil-avisos-ayuda" class="small text-body-secondary">
              Una hora antes, en este navegador. Al pulsar el aviso se abre la receta.
            </p>
            @if (activados()) {
              <button
                type="button"
                class="btn btn-sm btn-outline-secondary"
                [appRequiereConexion]="probando()"
                (click)="probar()"
              >
                Enviar un aviso de prueba
              </button>
            }
          }
        }
      </div>
    </section>
  `,
})
export class AvisosPerfil implements OnInit {
  protected readonly avisos = inject(AvisosPush);
  protected readonly conexion = inject(Conexion);
  private readonly sesion = inject(Sesion);
  private readonly notificaciones = inject(Notificaciones);

  readonly ocupado = signal(false);
  readonly probando = signal(false);
  readonly activados = computed(() => this.avisos.estado() === 'activados');

  ngOnInit(): void {
    void this.avisos.comprobar();
  }

  async alternar(evento: Event): Promise<void> {
    const casilla = evento.target as HTMLInputElement;
    const activar = casilla.checked;
    this.ocupado.set(true);
    try {
      if (activar) {
        await this.avisos.activar(this.sesion.usuario()!.id);
        this.notificaciones.exito('Avisos activados en este navegador.');
      } else {
        await this.avisos.desactivar();
        this.notificaciones.info('Avisos desactivados en este navegador.');
      }
    } catch (e) {
      casilla.checked = !activar;
      if (e instanceof HttpErrorResponse && e.status >= 400 && e.status < 500 && e.status !== 401) {
        // El servidor dice por qué: por ejemplo, el tope de navegadores (D-089)
        this.notificaciones.error(mensajeError(e));
      } else if (this.avisos.estado() !== 'bloqueados') {
        // Si el usuario ha denegado el permiso, la pantalla ya lo explica
        this.notificaciones.error(
          activar
            ? 'No se han podido activar los avisos.'
            : 'No se han podido desactivar los avisos.',
        );
      }
    } finally {
      this.ocupado.set(false);
    }
  }

  probar(): void {
    this.probando.set(true);
    this.avisos.probar().subscribe({
      next: ({ enviados }) => {
        this.probando.set(false);
        if (enviados > 0) {
          this.notificaciones.exito('Aviso de prueba enviado: llegará en unos segundos.');
        } else {
          this.notificaciones.error('El servicio de avisos no ha aceptado el aviso de prueba.');
        }
      },
      error: (e: HttpErrorResponse) => {
        this.probando.set(false);
        if (e.status >= 400 && e.status < 500) {
          this.notificaciones.error(mensajeError(e));
        }
      },
    });
  }
}
