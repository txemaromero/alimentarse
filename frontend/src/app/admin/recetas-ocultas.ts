import { Component, OnInit, inject } from '@angular/core';
import { crearListado } from '../compartido/listado';
import { RecetaResumen } from '../core/modelos';
import { RejillaRecetas } from '../recetas/rejilla-recetas';
import { AdminApi } from './admin-api';

/**
 * Moderación (D-063): las recetas ocultas por la administración. Desde el detalle de cada una se puede leer el motivo y
 * volver a mostrarla.
 */
@Component({
  selector: 'app-recetas-ocultas',
  imports: [RejillaRecetas],
  template: `
    <section class="container pb-5">
      <h1 class="titulo-seccion">Recetas ocultas</h1>
      <p class="text-body-secondary">
        Solo las ven sus autores y los administradores. Abre una para leer el motivo o volver a
        mostrarla.
      </p>
      <app-rejilla-recetas
        [resultado]="listado.resultado()"
        [cargando]="listado.cargando()"
        [fallo]="listado.fallo()"
        mensajeVacio="No hay ninguna receta oculta."
        (pagina)="listado.cargar($event)"
        (reintentar)="listado.recargar()"
      />
    </section>
  `,
})
export class RecetasOcultas implements OnInit {
  private readonly api = inject(AdminApi);

  readonly listado = crearListado<RecetaResumen, object>('admin-recetas-ocultas', (_f, pagina) =>
    this.api.recetasOcultas(pagina),
  );

  ngOnInit(): void {
    this.listado.iniciar();
  }
}
