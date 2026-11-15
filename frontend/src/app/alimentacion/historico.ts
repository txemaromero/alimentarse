import { DatePipe, formatDate } from '@angular/common';
import { Component, LOCALE_ID, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faArrowLeft, faPenToSquare, faUtensils } from '@fortawesome/free-solid-svg-icons';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { crearListado } from '../compartido/listado';
import { Paginacion } from '../compartido/paginacion';
import { Consumo, RecetaResumen } from '../core/modelos';
import { Notificaciones } from '../core/notificaciones';
import { TarjetaReceta } from '../recetas/tarjeta-receta';
import { ConsumosApi } from './consumos-api';
import { FORMATO_CONSUMO } from './mi-alimentacion';
import { abrirModalConsumo } from './modal-consumo';

/** Histórico completo de consumos, paginado; cada uno se puede cambiar de fecha o eliminar (6.7). */
@Component({
  selector: 'app-historico',
  imports: [RouterLink, DatePipe, FaIconComponent, TarjetaReceta, Paginacion],
  template: `
    <section class="container pb-5">
      <a routerLink="/mi-alimentacion" class="btn btn-link px-0 mt-3">
        <fa-icon [icon]="iconos.volver" class="me-1" />Mi alimentación
      </a>
      <h1 class="titulo-seccion mt-2">Histórico de consumos</h1>

      <section [attr.aria-busy]="listado.cargando()" aria-labelledby="historico-resultados">
        <h2 id="historico-resultados" class="visually-hidden">Consumos</h2>
        @if (listado.fallo()) {
          <div class="alert alert-danger d-flex align-items-center gap-3" role="alert">
            <span class="flex-grow-1">No se ha podido cargar el histórico.</span>
            <button
              type="button"
              class="btn btn-sm btn-outline-danger"
              (click)="listado.recargar()"
            >
              Reintentar
            </button>
          </div>
        } @else if (listado.resultado(); as pagina) {
          @if (pagina.contenido.length === 0) {
            <div class="text-center py-5">
              <fa-icon [icon]="iconos.vacio" size="3x" class="text-secondary mb-3" />
              <p class="lead">Todavía no has registrado ninguna receta consumida.</p>
              <a routerLink="/recetas" class="btn btn-primary">Ver recetas</a>
            </div>
          } @else {
            <div
              class="row row-cols-1 row-cols-sm-2 row-cols-lg-3 g-4"
              [class.opacity-50]="listado.cargando()"
            >
              @for (consumo of pagina.contenido; track consumo.id) {
                <div class="col animate__animated animate__fadeIn">
                  <app-tarjeta-receta
                    [receta]="consumo.receta"
                    (cambio)="actualizar(consumo, $event)"
                  >
                    <div class="d-flex align-items-center gap-2 mb-3 consumida">
                      <span class="small flex-grow-1">
                        Consumida el {{ consumo.fecha | date: formato }}
                      </span>
                      <button
                        type="button"
                        class="btn btn-sm btn-outline-secondary"
                        title="Editar consumo"
                        [attr.aria-label]="'Editar el consumo de ' + consumo.receta.titulo"
                        (click)="editar(consumo)"
                      >
                        <fa-icon [icon]="iconos.editar" />
                      </button>
                    </div>
                  </app-tarjeta-receta>
                </div>
              }
            </div>
            <app-paginacion
              [datos]="pagina"
              etiqueta="Páginas del histórico"
              (pagina)="listado.cargar($event)"
            />
          }
        } @else if (listado.cargando()) {
          <div class="text-center py-5">
            <span class="spinner-border text-secondary" role="status">
              <span class="visually-hidden">Cargando el histórico…</span>
            </span>
          </div>
        }
      </section>
    </section>
  `,
})
export class Historico implements OnInit {
  private readonly api = inject(ConsumosApi);
  private readonly modales = inject(NgbModal);
  private readonly notificaciones = inject(Notificaciones);
  private readonly locale = inject(LOCALE_ID);

  readonly listado = crearListado<Consumo, object>('historico', (_filtro, pagina) =>
    this.api.historico(pagina),
  );

  protected readonly formato = FORMATO_CONSUMO;
  protected readonly iconos = { volver: faArrowLeft, editar: faPenToSquare, vacio: faUtensils };

  ngOnInit(): void {
    this.listado.iniciar();
  }

  editar(consumo: Consumo): void {
    abrirModalConsumo(this.modales, consumo).subscribe((resultado) => {
      if (resultado.accion === 'eliminado') {
        this.notificaciones.exito(`Consumo de «${consumo.receta.titulo}» eliminado.`);
      } else {
        const cuando = formatDate(resultado.consumo.fecha, FORMATO_CONSUMO, this.locale);
        this.notificaciones.exito(`Consumo cambiado al ${cuando}.`);
      }
      // El orden depende de la fecha y el total puede cambiar: se vuelve a pedir la página
      this.listado.recargar();
    });
  }

  actualizar(consumo: Consumo, receta: RecetaResumen): void {
    this.listado.reemplazar({ ...consumo, receta }, (a, b) => a.id === b.id);
  }
}
