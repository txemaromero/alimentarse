import { DOCUMENT, DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faArrowLeft, faPrint } from '@fortawesome/free-solid-svg-icons';
import { ArticuloCompra, ComidaCompra, ListaCompra } from '../core/modelos';
import { Notificaciones } from '../core/notificaciones';
import { MarcasCompra } from './marcas-compra';
import { PlanApi } from './plan-api';
import { diasDeLaSemana, finDelDia, momentoDe, semanaDeLaUrl } from './semana-plan';

/**
 * Lista de la compra (D-076): los ingredientes de las comidas planificadas de una semana que aún no han pasado, por
 * grupo de alimentos, con casillas para marcar lo que ya se tiene. Se imprime o se guarda en PDF con el navegador.
 */
@Component({
  selector: 'app-lista-compra',
  imports: [DatePipe, RouterLink, FaIconComponent],
  templateUrl: './lista-compra.html',
  styleUrl: './lista-compra.scss',
})
export class ListaDeLaCompra implements OnInit {
  private readonly api = inject(PlanApi);
  private readonly marcas = inject(MarcasCompra);
  private readonly notificaciones = inject(Notificaciones);
  private readonly documento = inject(DOCUMENT);
  private readonly destruir = inject(DestroyRef);
  private readonly hoy = new Date();

  /** `?semana=`: la misma semana que se veía en el planificador (0 = la que empieza hoy). */
  readonly semana = input(0, { transform: semanaDeLaUrl });
  readonly dias = computed(() => diasDeLaSemana(this.hoy, this.semana()));
  readonly lista = signal<ListaCompra | null>(null);
  readonly fallo = signal(false);
  readonly marcados = signal<ReadonlySet<number>>(this.marcas.leer());
  readonly articulos = computed(() =>
    (this.lista()?.secciones ?? []).flatMap((seccion) => seccion.articulos),
  );
  readonly marcadosEnLista = computed(
    () => this.articulos().filter((articulo) => this.marcado(articulo)).length,
  );
  /** Para volver a la misma semana del planificador. */
  readonly parametrosSemana = computed(() => (this.semana() ? { semana: this.semana() } : {}));

  protected readonly iconos = { volver: faArrowLeft, imprimir: faPrint };

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.fallo.set(false);
    this.lista.set(null);
    const dias = this.dias();
    this.api
      .compra(dias[0], finDelDia(dias[dias.length - 1]))
      .pipe(takeUntilDestroyed(this.destruir))
      .subscribe({
        next: (lista) => this.lista.set(lista),
        error: () => this.fallo.set(true),
      });
  }

  marcado(articulo: ArticuloCompra): boolean {
    return this.marcados().has(articulo.ingrediente);
  }

  cambiar(articulo: ArticuloCompra, evento: Event): void {
    const marcado = (evento.target as HTMLInputElement).checked;
    this.marcas.marcar(articulo.ingrediente, marcado);
    this.marcados.update((marcados) => {
      const nuevos = new Set(marcados);
      if (marcado) {
        nuevos.add(articulo.ingrediente);
      } else {
        nuevos.delete(articulo.ingrediente);
      }
      return nuevos;
    });
  }

  desmarcarTodo(): void {
    const ids = this.articulos().map((articulo) => articulo.ingrediente);
    this.marcas.desmarcar(ids);
    this.marcados.update((marcados) => new Set([...marcados].filter((id) => !ids.includes(id))));
    this.notificaciones.info('Lista desmarcada: no queda nada marcado.');
  }

  /** ¿Hace falta el detalle de lo que pide cada receta? Si no hay total, o si sale de más de una comida. */
  desglose(articulo: ArticuloCompra): boolean {
    return (
      articulo.total === null ||
      articulo.cantidades.length > 1 ||
      articulo.cantidades.some((cantidad) => cantidad.veces > 1)
    );
  }

  momento(comida: ComidaCompra): string {
    return momentoDe(new Date(comida.fecha));
  }

  /** El diálogo de impresión del navegador, que también permite guardar en PDF. */
  imprimir(): void {
    this.documento.defaultView?.print();
  }
}
