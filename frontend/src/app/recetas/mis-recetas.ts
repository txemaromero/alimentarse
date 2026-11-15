import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faPlus } from '@fortawesome/free-solid-svg-icons';
import { crearListado } from '../compartido/listado';
import { FiltroRecetas, RecetaResumen } from '../core/modelos';
import { filtrosActivos, FiltrosRecetas } from './filtros-recetas';
import { RecetasApi } from './recetas-api';
import { RejillaRecetas } from './rejilla-recetas';

/** Recetas propias, publicadas o no, con publicar/despublicar, editar y eliminar en cada tarjeta. */
@Component({
  selector: 'app-mis-recetas',
  imports: [FiltrosRecetas, RejillaRecetas, RouterLink, FaIconComponent],
  template: `
    <section class="container pb-5">
      <h1 class="titulo-seccion">Mis recetas</h1>
      <div class="d-flex justify-content-end mb-3">
        <a routerLink="/recetas/nueva" class="btn btn-primary">
          <fa-icon [icon]="iconoNueva" class="me-1" />Nueva receta
        </a>
      </div>
      <app-filtros-recetas
        [filtro]="listado.filtro()"
        [conAutor]="false"
        (filtrar)="listado.filtrar($event)"
      />
      <app-rejilla-recetas
        [resultado]="listado.resultado()"
        [cargando]="listado.cargando()"
        [fallo]="listado.fallo()"
        [modoAutor]="true"
        [mensajeVacio]="
          hayFiltros()
            ? 'Ninguna de tus recetas coincide con los filtros.'
            : 'Todavía no has creado ninguna receta.'
        "
        (pagina)="listado.cargar($event)"
        (cambio)="actualizar($event)"
        (eliminada)="listado.recargar()"
        (reintentar)="listado.recargar()"
      >
        @if (!hayFiltros()) {
          <a vacio routerLink="/recetas/nueva" class="btn btn-outline-primary mt-2">
            Crear mi primera receta
          </a>
        }
      </app-rejilla-recetas>
    </section>
  `,
})
export class MisRecetas implements OnInit {
  private readonly api = inject(RecetasApi);

  readonly listado = crearListado<RecetaResumen, FiltroRecetas>('mis-recetas', (filtro, pagina) =>
    this.api.mias(filtro, pagina),
  );
  protected readonly iconoNueva = faPlus;

  ngOnInit(): void {
    this.listado.iniciar();
  }

  hayFiltros(): boolean {
    return filtrosActivos(this.listado.filtro()) > 0;
  }

  actualizar(receta: RecetaResumen): void {
    this.listado.reemplazar(receta, (a, b) => a.id === b.id);
  }
}
