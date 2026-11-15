import { Component, OnInit, inject, input } from '@angular/core';
import { crearListado } from '../compartido/listado';
import { FiltroRecetas, RecetaResumen } from '../core/modelos';
import { FiltrosRecetas } from './filtros-recetas';
import { RecetasApi } from './recetas-api';
import { RejillaRecetas } from './rejilla-recetas';

/** Recetas publicadas con filtros (6.5) y 9 por página. */
@Component({
  selector: 'app-lista-recetas',
  imports: [FiltrosRecetas, RejillaRecetas],
  template: `
    <section class="container pb-5">
      <h1 class="titulo-seccion">Recetas</h1>
      <app-filtros-recetas [filtro]="listado.filtro()" (filtrar)="listado.filtrar($event)" />
      <app-rejilla-recetas
        [resultado]="listado.resultado()"
        [cargando]="listado.cargando()"
        [fallo]="listado.fallo()"
        (pagina)="listado.cargar($event)"
        (cambio)="actualizar($event)"
        (reintentar)="listado.recargar()"
      />
    </section>
  `,
})
export class ListaRecetas implements OnInit {
  private readonly api = inject(RecetasApi);

  /** Búsqueda que llega del buscador de la portada (`?titulo=`). */
  readonly titulo = input<string>();

  readonly listado = crearListado<RecetaResumen, FiltroRecetas>('recetas', (filtro, pagina) =>
    this.api.buscar(filtro, pagina),
  );

  ngOnInit(): void {
    const titulo = this.titulo()?.trim();
    if (titulo) {
      this.listado.filtrar({ titulo });
    } else {
      this.listado.iniciar();
    }
  }

  actualizar(receta: RecetaResumen): void {
    this.listado.reemplazar(receta, (a, b) => a.id === b.id);
  }
}
