import {
  Component,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
  untracked,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  FormControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faFilter, faRotateLeft } from '@fortawesome/free-solid-svg-icons';
import {
  NgbAccordionModule,
  NgbHighlight,
  NgbTypeahead,
  NgbTypeaheadSelectItemEvent,
} from '@ng-bootstrap/ng-bootstrap';
import { ErrorCampo } from '../compartido/error-campo';
import { CatalogoApi } from '../core/catalogo-api';
import { esInvalido } from '../core/errores';
import { DIFICULTADES, Dificultad, FiltroRecetas, Ingrediente } from '../core/modelos';
import { busquedaIngredientes, nombreIngrediente } from '../ingredientes/busqueda-ingredientes';
import { IngredientesApi } from '../ingredientes/ingredientes-api';

/** Número de filtros con valor (para el encabezado del acordeón). */
export function filtrosActivos(filtro: FiltroRecetas): number {
  return [
    filtro.titulo?.trim(),
    filtro.tipo,
    filtro.usuario?.trim(),
    filtro.dificultad,
    filtro.comensales,
    filtro.tiempoMax,
    filtro.ingredientes?.length,
  ].filter(Boolean).length;
}

/**
 * Filtros del listado de recetas (6.5) en un acordeón plegable: título, tipo, autor, dificultad, comensales mínimos,
 * tiempo máximo e ingredientes (chips; la receta debe tenerlos todos). Emite el filtro al pulsar Filtrar o
 * Restablecer.
 */
@Component({
  selector: 'app-filtros-recetas',
  imports: [
    ReactiveFormsModule,
    NgbAccordionModule,
    NgbTypeahead,
    NgbHighlight,
    FaIconComponent,
    ErrorCampo,
  ],
  templateUrl: './filtros-recetas.html',
})
export class FiltrosRecetas {
  private readonly ingredientesApi = inject(IngredientesApi);

  /** Filtro aplicado ahora mismo en el listado. */
  readonly filtro = input<FiltroRecetas>({});
  /** Mostrar el filtro por autor (no tiene sentido en «Mis recetas»). */
  readonly conAutor = input(true);
  /** Prefijo de los id de los campos, por si hay dos formularios en la página. */
  readonly prefijo = input('filtro');
  readonly filtrar = output<FiltroRecetas>();

  readonly tipos = toSignal(inject(CatalogoApi).tipos$, { initialValue: [] });
  readonly dificultades = DIFICULTADES;

  readonly formulario = inject(NonNullableFormBuilder).group({
    titulo: [''],
    tipo: new FormControl<number | null>(null),
    usuario: [''],
    dificultad: new FormControl<Dificultad | null>(null),
    comensales: new FormControl<number | null>(null, [Validators.min(1), Validators.max(100)]),
    tiempoMax: new FormControl<number | null>(null, [Validators.min(1), Validators.max(1440)]),
  });
  readonly ingredientes = signal<Ingrediente[]>([]);
  readonly busqueda = new FormControl<Ingrediente | string | null>(null);

  readonly activos = computed(() => filtrosActivos(this.filtro()));
  /** El acordeón empieza abierto si ya hay filtros aplicados. */
  readonly empiezaPlegado = computed(() => untracked(() => this.activos() === 0));

  protected readonly esInvalido = esInvalido;
  protected readonly nombreIngrediente = nombreIngrediente;
  protected readonly iconos = { filtrar: faFilter, restablecer: faRotateLeft };
  protected readonly buscarIngredientes = busquedaIngredientes(this.ingredientesApi, () =>
    this.ingredientes().map((i) => i.id),
  );

  constructor() {
    // Cuando el listado aplica otro filtro, el formulario lo refleja
    effect(() => {
      const filtro = this.filtro();
      untracked(() => this.mostrar(filtro));
    });
  }

  elegirIngrediente(evento: NgbTypeaheadSelectItemEvent<Ingrediente>): void {
    evento.preventDefault();
    this.ingredientes.update((lista) => [...lista, evento.item]);
    this.busqueda.setValue(null);
  }

  quitarIngrediente(ingrediente: Ingrediente): void {
    this.ingredientes.update((lista) => lista.filter((i) => i.id !== ingrediente.id));
  }

  aplicar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    const v = this.formulario.getRawValue();
    const filtro: FiltroRecetas = {};
    if (v.titulo.trim()) filtro.titulo = v.titulo.trim();
    if (v.tipo) filtro.tipo = Number(v.tipo);
    if (this.conAutor() && v.usuario.trim()) filtro.usuario = v.usuario.trim();
    if (v.dificultad) filtro.dificultad = v.dificultad;
    if (v.comensales) filtro.comensales = v.comensales;
    if (v.tiempoMax) filtro.tiempoMax = v.tiempoMax;
    if (this.ingredientes().length) filtro.ingredientes = this.ingredientes();
    this.filtrar.emit(filtro);
  }

  restablecer(): void {
    this.mostrar({});
    this.filtrar.emit({});
  }

  id(campo: string): string {
    return `${this.prefijo()}-${campo}`;
  }

  private mostrar(filtro: FiltroRecetas): void {
    this.formulario.reset({
      titulo: filtro.titulo ?? '',
      tipo: filtro.tipo ?? null,
      usuario: filtro.usuario ?? '',
      dificultad: filtro.dificultad ?? null,
      comensales: filtro.comensales ?? null,
      tiempoMax: filtro.tiempoMax ?? null,
    });
    this.ingredientes.set(filtro.ingredientes ?? []);
    this.busqueda.setValue(null);
  }
}
