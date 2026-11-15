import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import {
  NgbActiveModal,
  NgbModal,
  NgbTypeahead,
  NgbTypeaheadSelectItemEvent,
} from '@ng-bootstrap/ng-bootstrap';
import {
  Observable,
  catchError,
  debounceTime,
  distinctUntilChanged,
  map,
  of,
  switchMap,
} from 'rxjs';
import { RequiereConexion } from '../compartido/requiere-conexion';
import { mensajeError } from '../core/errores';
import { ComidaPlanificada, RecetaRecomendada, SugerenciaReceta } from '../core/modelos';
import { RecetasApi } from '../recetas/recetas-api';
import { PlanApi } from './plan-api';
import { motivoRecomendacion } from './recomendacion';

const SUGERENCIAS = 5;

/** Modal del planificador (D-068): elegir receta para un hueco, entre las recomendadas o buscándola por título. */
@Component({
  selector: 'app-modal-planificar',
  imports: [ReactiveFormsModule, NgbTypeahead, RequiereConexion],
  template: `
    <div class="modal-header">
      <h2 class="modal-title h5" id="modal-planificar-titulo">{{ titulo }}</h2>
      <button
        type="button"
        class="btn-close"
        aria-label="Cerrar"
        (click)="modal.dismiss()"
      ></button>
    </div>
    <div class="modal-body">
      @if (error(); as mensaje) {
        <div class="alert alert-danger" role="alert">{{ mensaje }}</div>
      }

      <label for="planificar-receta" class="form-label">Busca una receta</label>
      <input
        id="planificar-receta"
        type="text"
        class="form-control"
        autocomplete="off"
        placeholder="Escribe al menos dos letras…"
        ngbAutofocus
        [formControl]="busqueda"
        [ngbTypeahead]="sugerir"
        [inputFormatter]="tituloDe"
        [resultFormatter]="tituloDe"
        (selectItem)="elegirSugerencia($event)"
      />

      @if (recomendadas().length > 0) {
        <h3 class="h6 mt-4">Te recomendamos</h3>
        <div class="list-group">
          @for (receta of recomendadas(); track receta.id) {
            <button
              type="button"
              class="list-group-item list-group-item-action"
              [appRequiereConexion]="enviando()"
              (click)="elegir(receta.id)"
            >
              <span class="d-block text-body fw-semibold">{{ receta.titulo }}</span>
              @if (motivo(receta); as texto) {
                <span class="d-block small text-success-emphasis">{{ texto }}</span>
              }
            </button>
          }
        </div>
      }
    </div>
    <div class="modal-footer">
      <button type="button" class="btn btn-outline-secondary" (click)="modal.dismiss()">
        Cancelar
      </button>
    </div>
  `,
})
export class ModalPlanificar implements OnInit {
  protected readonly modal = inject(NgbActiveModal);
  private readonly plan = inject(PlanApi);
  private readonly recetas = inject(RecetasApi);

  /** Los asigna `abrirModalPlanificar`. */
  titulo = '';
  fecha!: Date;

  readonly busqueda = new FormControl<string | SugerenciaReceta>('', { nonNullable: true });
  readonly recomendadas = signal<RecetaRecomendada[]>([]);
  readonly enviando = signal(false);
  readonly error = signal<string | null>(null);

  protected readonly motivo = (receta: RecetaRecomendada) => motivoRecomendacion(receta.grupos);
  protected readonly tituloDe = (receta: SugerenciaReceta | string) =>
    typeof receta === 'string' ? receta : receta.titulo;

  readonly sugerir = (texto$: Observable<string>): Observable<SugerenciaReceta[]> =>
    texto$.pipe(
      debounceTime(250),
      map((texto) => (typeof texto === 'string' ? texto.trim() : '')),
      distinctUntilChanged(),
      switchMap((texto) =>
        texto.length < 2 ? of([]) : this.recetas.sugerencias(texto).pipe(catchError(() => of([]))),
      ),
    );

  ngOnInit(): void {
    // Solo son una ayuda: si fallan, se puede buscar igual
    this.recetas.recomendadas({}, 0, SUGERENCIAS).subscribe({
      next: (pagina) => this.recomendadas.set(pagina.contenido),
      error: () => this.recomendadas.set([]),
    });
  }

  elegirSugerencia(evento: NgbTypeaheadSelectItemEvent<SugerenciaReceta>): void {
    this.elegir(evento.item.id);
  }

  elegir(receta: number): void {
    if (this.enviando()) {
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    this.plan.anadir(receta, this.fecha).subscribe({
      next: (comida) => this.modal.close(comida),
      error: (error: HttpErrorResponse) => {
        this.enviando.set(false);
        if (error.status >= 400 && error.status < 500 && error.status !== 401) {
          this.error.set(mensajeError(error));
        }
      },
    });
  }
}

/** Abre el modal y emite la comida añadida (nada si se cancela). */
export function abrirModalPlanificar(
  modales: NgbModal,
  opciones: { titulo: string; fecha: Date },
): Observable<ComidaPlanificada> {
  const ref = modales.open(ModalPlanificar, {
    ariaLabelledBy: 'modal-planificar-titulo',
    modalDialogClass: 'animate__animated animate__fadeInDown',
  });
  const componente = ref.componentInstance as ModalPlanificar;
  componente.titulo = opciones.titulo;
  componente.fecha = opciones.fecha;
  return new Observable<ComidaPlanificada>((suscriptor) => {
    ref.result.then(
      (comida: ComidaPlanificada) => {
        suscriptor.next(comida);
        suscriptor.complete();
      },
      () => suscriptor.complete(),
    );
  });
}
