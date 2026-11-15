import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { crearListado } from '../compartido/listado';
import { FiltroRecetas, RecetaResumen } from '../core/modelos';
import { filtrosActivos, FiltrosRecetas } from './filtros-recetas';
import { RecetasApi } from './recetas-api';
import { RejillaRecetas } from './rejilla-recetas';

/**
 * «Recetas que me gustan» (D-062): las publicadas a las que el usuario ha dado «me gusta», de la más reciente a la más
 * antigua, con los filtros del listado. Al quitar el «me gusta» desde una tarjeta, la receta sale de la lista.
 */
@Component({
  selector: 'app-me-gustan',
  imports: [FiltrosRecetas, RejillaRecetas, RouterLink],
  template: `
    <section class="container pb-5">
      <h1 class="titulo-seccion">Recetas que me gustan</h1>
      <app-filtros-recetas [filtro]="listado.filtro()" (filtrar)="listado.filtrar($event)" />
      <app-rejilla-recetas
        [resultado]="listado.resultado()"
        [cargando]="listado.cargando()"
        [fallo]="listado.fallo()"
        [mensajeVacio]="
          hayFiltros()
            ? 'Ninguna de las recetas que te gustan coincide con los filtros.'
            : 'Todavía no le has dado «me gusta» a ninguna receta.'
        "
        (pagina)="listado.cargar($event)"
        (cambio)="actualizar($event)"
        (reintentar)="listado.recargar()"
      >
        <!-- Un único elemento dentro del @if: si hubiera varios, no se proyectarían en [vacio] -->
        @if (!hayFiltros()) {
          <div vacio>
            <p class="text-body-secondary">
              Pulsa el corazón de las recetas que te gusten para tenerlas siempre a mano.
            </p>
            <a routerLink="/recetas" class="btn btn-outline-primary">Ver recetas</a>
          </div>
        }
      </app-rejilla-recetas>
    </section>
  `,
})
export class MeGustan implements OnInit {
  private readonly api = inject(RecetasApi);

  readonly listado = crearListado<RecetaResumen, FiltroRecetas>('me-gustan', (filtro, pagina) =>
    this.api.meGustan(filtro, pagina),
  );

  ngOnInit(): void {
    this.listado.iniciar();
  }

  hayFiltros(): boolean {
    return filtrosActivos(this.listado.filtro()) > 0;
  }

  /** Sin el «me gusta» ya no pertenece a la lista: se vuelve a pedir la página (el total cambia). */
  actualizar(receta: RecetaResumen): void {
    if (receta.meGusta) {
      this.listado.reemplazar(receta, (a, b) => a.id === b.id);
    } else {
      this.listado.recargar();
    }
  }
}
