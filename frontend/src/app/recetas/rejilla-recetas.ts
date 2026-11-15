import { NgTemplateOutlet } from '@angular/common';
import { Component, TemplateRef, input, output } from '@angular/core';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faBowlFood } from '@fortawesome/free-solid-svg-icons';
import { Paginacion } from '../compartido/paginacion';
import { Pagina, RecetaResumen } from '../core/modelos';
import { TarjetaReceta } from './tarjeta-receta';

/** Rejilla paginada de tarjetas de receta con los estados de carga, error y lista vacía. */
@Component({
  selector: 'app-rejilla-recetas',
  imports: [TarjetaReceta, Paginacion, FaIconComponent, NgTemplateOutlet],
  template: `
    <section [attr.aria-busy]="cargando()" aria-labelledby="titulo-resultados">
      <h2 id="titulo-resultados" class="visually-hidden">Resultados</h2>

      @if (fallo()) {
        <div class="alert alert-danger d-flex align-items-center gap-3" role="alert">
          <span class="flex-grow-1">No se han podido cargar las recetas.</span>
          <button type="button" class="btn btn-sm btn-outline-danger" (click)="reintentar.emit()">
            Reintentar
          </button>
        </div>
      } @else if (resultado(); as p) {
        @if (p.contenido.length === 0) {
          <div class="text-center py-5 vacio animate__animated animate__fadeIn">
            <fa-icon [icon]="iconoVacio" size="3x" class="text-secondary mb-3" />
            <p class="lead mb-1">{{ mensajeVacio() }}</p>
            <ng-content select="[vacio]" />
          </div>
        } @else {
          <div
            class="row row-cols-1 row-cols-sm-2 row-cols-lg-3 g-4"
            [class.opacity-50]="cargando()"
          >
            @for (receta of p.contenido; track receta.id) {
              <div class="col animate__animated animate__fadeIn">
                <app-tarjeta-receta
                  [receta]="receta"
                  [modoAutor]="modoAutor()"
                  (cambio)="cambio.emit($event)"
                  (eliminada)="eliminada.emit($event)"
                >
                  @if (extra(); as plantilla) {
                    <ng-container
                      [ngTemplateOutlet]="plantilla"
                      [ngTemplateOutletContext]="{ $implicit: receta }"
                    />
                  }
                </app-tarjeta-receta>
              </div>
            }
          </div>
          <app-paginacion
            [datos]="p"
            etiqueta="Páginas de recetas"
            (pagina)="pagina.emit($event)"
          />
        }
      } @else if (cargando()) {
        <div class="text-center py-5">
          <span class="spinner-border text-secondary" role="status">
            <span class="visually-hidden">Cargando recetas…</span>
          </span>
        </div>
      }
    </section>
  `,
})
export class RejillaRecetas {
  readonly resultado = input.required<Pagina<RecetaResumen> | null>();
  readonly cargando = input(false);
  readonly fallo = input(false);
  readonly modoAutor = input(false);
  readonly mensajeVacio = input('No hay recetas que coincidan con la búsqueda.');
  /** Contenido extra de cada tarjeta; recibe la receta como variable implícita (`let-receta`). */
  readonly extra = input<TemplateRef<unknown> | null>(null);

  readonly pagina = output<number>();
  readonly cambio = output<RecetaResumen>();
  readonly eliminada = output<RecetaResumen>();
  readonly reintentar = output<void>();

  protected readonly iconoVacio = faBowlFood;
}
