import { Component, computed, input, output } from '@angular/core';
import { NgbPagination } from '@ng-bootstrap/ng-bootstrap';
import { Pagina } from '../core/modelos';

/**
 * Paginación de una lista de la API (`pagina` empieza en 0) con el texto «Mostrando del elemento X al Y de Z
 * totales» (6.5). NgbPagination numera desde 1; la conversión se hace aquí.
 */
@Component({
  selector: 'app-paginacion',
  imports: [NgbPagination],
  template: `
    @if (datos(); as p) {
      @if (p.totalElementos > 0) {
        <div
          class="d-flex flex-column flex-md-row align-items-center justify-content-between gap-2 mt-4"
        >
          <p class="text-body-secondary small m-0" aria-live="polite">{{ texto() }}</p>
          @if (p.totalPaginas > 1) {
            <ngb-pagination
              [collectionSize]="p.totalElementos"
              [pageSize]="p.tamano"
              [page]="p.pagina + 1"
              [maxSize]="5"
              [rotate]="true"
              [boundaryLinks]="true"
              [ellipses]="false"
              [attr.aria-label]="etiqueta()"
              (pageChange)="cambiar($event)"
            />
          }
        </div>
      }
    }
  `,
})
export class Paginacion {
  readonly datos = input.required<Pagina<unknown> | null>();
  readonly etiqueta = input('Paginación');
  /** Página elegida, empezando en 0. */
  readonly pagina = output<number>();

  readonly texto = computed(() => {
    const p = this.datos();
    return p ? textoPaginacion(p) : '';
  });

  cambiar(numero: number): void {
    const p = this.datos();
    if (p && numero - 1 !== p.pagina) {
      this.pagina.emit(numero - 1);
    }
  }
}

export function textoPaginacion(p: Pagina<unknown>): string {
  if (p.totalElementos === 0 || p.contenido.length === 0) {
    return `${p.totalElementos} totales`;
  }
  const desde = p.pagina * p.tamano + 1;
  const hasta = desde + p.contenido.length - 1;
  return `Mostrando del elemento ${desde} al ${hasta} de ${p.totalElementos} totales`;
}
