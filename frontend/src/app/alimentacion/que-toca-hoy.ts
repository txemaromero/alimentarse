import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faCircleCheck, faCircleInfo } from '@fortawesome/free-solid-svg-icons';
import { crearListado } from '../compartido/listado';
import { FiltroRecetas, Grupo, RecetaRecomendada, RecetaResumen } from '../core/modelos';
import { FiltrosRecetas, filtrosActivos } from '../recetas/filtros-recetas';
import { RecetasApi } from '../recetas/recetas-api';
import { RejillaRecetas } from '../recetas/rejilla-recetas';
import { ConsumosApi } from './consumos-api';
import { motivoRecomendacion } from './recomendacion';

/**
 * «¿Qué toca hoy?» (6.3): recetas publicadas sin ingredientes de los grupos que el usuario ya ha agotado en los
 * últimos 7 días, con los filtros del listado. Primero las de los grupos menos consumidos, y cada tarjeta explica por
 * qué se recomienda (D-067).
 */
@Component({
  selector: 'app-que-toca-hoy',
  imports: [FiltrosRecetas, RejillaRecetas, RouterLink, FaIconComponent],
  template: `
    <section class="container pb-5">
      <h1 class="titulo-seccion">¿Qué toca hoy?</h1>

      <div class="alert alert-info d-flex gap-3 align-items-start" role="note">
        <fa-icon [icon]="iconoInfo" class="mt-1" />
        <div>
          <p class="mb-1">
            Te recomendamos estas recetas teniendo en cuenta tu historial de alimentación en los
            últimos 7 días: no llevan ingredientes de los grupos cuyas raciones semanales ya has
            completado. Primero verás las de los grupos que menos has comido.
            <a routerLink="/planificador" class="alert-link">Planifica tu semana</a>.
          </p>
          @if (agotados().length > 0) {
            <p class="mb-0">
              <strong>Grupos completados esta semana:</strong>
              {{ nombres(agotados()) }}.
              <a routerLink="/mi-alimentacion" class="alert-link">Ver mi alimentación</a>
            </p>
          } @else if (resumenCargado()) {
            <p class="mb-0">Esta semana aún no has completado ningún grupo.</p>
          }
        </div>
      </div>

      <app-filtros-recetas
        [filtro]="listado.filtro()"
        prefijo="recomendadas"
        (filtrar)="listado.filtrar($event)"
      />
      <app-rejilla-recetas
        [resultado]="listado.resultado()"
        [cargando]="listado.cargando()"
        [fallo]="listado.fallo()"
        [extra]="motivo"
        [mensajeVacio]="
          hayFiltros()
            ? 'Ninguna receta recomendada coincide con los filtros.'
            : 'Ahora mismo no hay recetas que encajen con tu semana.'
        "
        (pagina)="listado.cargar($event)"
        (cambio)="actualizar($event)"
        (reintentar)="listado.recargar()"
      >
        <p vacio class="text-body-secondary">
          @if (hayFiltros()) {
            Prueba a quitar algún filtro.
          } @else {
            Cuando pasen unos días se liberarán raciones. Mientras tanto, puedes
            <a routerLink="/recetas">ver todas las recetas</a>.
          }
        </p>
      </app-rejilla-recetas>

      <ng-template #motivo let-receta>
        @if (motivoDe(receta); as texto) {
          <p class="small text-success-emphasis mb-3 motivo">
            <fa-icon [icon]="iconoMotivo" class="me-1" />{{ texto }}
          </p>
        }
      </ng-template>
    </section>
  `,
})
export class QueTocaHoy implements OnInit {
  private readonly api = inject(RecetasApi);
  private readonly consumos = inject(ConsumosApi);

  readonly listado = crearListado<RecetaRecomendada, FiltroRecetas>(
    'que-toca-hoy',
    (filtro, pagina) => this.api.recomendadas(filtro, pagina),
  );
  /** Grupos con el límite semanal alcanzado (indicador en rojo). */
  readonly agotados = signal<Grupo[]>([]);
  readonly resumenCargado = signal(false);
  readonly hayFiltros = computed(() => filtrosActivos(this.listado.filtro()) > 0);

  protected readonly iconoInfo = faCircleInfo;
  protected readonly iconoMotivo = faCircleCheck;

  ngOnInit(): void {
    this.listado.iniciar();
    // Solo es información adicional: si falla, la pantalla funciona igual
    this.consumos.resumen().subscribe({
      next: (resumen) => {
        this.agotados.set(
          resumen.grupos7Dias.filter((g) => g.color === 'ROJO').map((g) => g.grupo),
        );
        this.resumenCargado.set(true);
      },
      error: () => undefined,
    });
  }

  nombres(grupos: Grupo[]): string {
    return grupos.map((g) => g.nombre).join(', ');
  }

  motivoDe(receta: RecetaRecomendada): string | null {
    return motivoRecomendacion(receta.grupos);
  }

  /** La tarjeta devuelve la receta recibida con los cambios: conserva `grupos`. */
  actualizar(receta: RecetaResumen): void {
    this.listado.reemplazar(receta as RecetaRecomendada, (a, b) => a.id === b.id);
  }
}
