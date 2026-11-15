import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  ElementRef,
  Injector,
  OnInit,
  afterNextRender,
  inject,
  signal,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormArray,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faPlus, faTrashCan, faXmark } from '@fortawesome/free-solid-svg-icons';
import { NgbActiveModal, NgbHighlight, NgbModal, NgbTypeahead } from '@ng-bootstrap/ng-bootstrap';
import {
  Observable,
  catchError,
  debounceTime,
  distinctUntilChanged,
  map,
  of,
  switchMap,
} from 'rxjs';
import { AdminApi } from '../admin/admin-api';
import { ErrorCampo } from '../compartido/error-campo';
import { RequiereConexion } from '../compartido/requiere-conexion';
import { aplicarErroresServidor, errorApi, esInvalido, mensajeError } from '../core/errores';
import { AlimentoBedca, Ingrediente, NutricionIngrediente } from '../core/modelos';
import { FUENTE_BEDCA } from '../recetas/informacion-nutricional';

/** Piezas que admite el servidor por ingrediente. */
export const MAXIMO_PIEZAS = 10;

/** Un alimento elegido en las sugerencias, o nada; un texto escrito sin elegir no vale. */
const elegidoOVacio = (control: AbstractControl) => {
  const valor: unknown = control.value;
  return typeof valor === 'string' && valor.trim() ? { noElegido: true } : null;
};

type FilaPieza = FormGroup<{
  pieza: FormControl<string>;
  gramos: FormControl<number | null>;
}>;

/** Para `[ngbTypeahead]`: alimentos de BEDCA a partir de 2 caracteres; un error es «sin sugerencias». */
export function busquedaAlimentos(
  admin: AdminApi,
): (texto$: Observable<string>) => Observable<AlimentoBedca[]> {
  return (texto$) =>
    texto$.pipe(
      debounceTime(250),
      map((texto) => (typeof texto === 'string' ? texto.trim() : '')),
      distinctUntilChanged(),
      switchMap((texto) =>
        texto.length < 2 ? of([]) : admin.alimentosBedca(texto).pipe(catchError(() => of([]))),
      ),
    );
}

export const nombreAlimento = (valor: AlimentoBedca | string | null): string =>
  typeof valor === 'string' ? valor : (valor?.nombre ?? '');

/**
 * Los datos nutricionales de un ingrediente (D-080, D-081): el alimento de BEDCA con el que se calcula su información
 * nutricional, buscado por palabras y con sus valores por 100 g a la vista, y el peso de sus piezas («unidad», «lata»),
 * para cantidades como «2 latas». Se guardan juntos.
 */
@Component({
  selector: 'app-modal-datos-nutricionales',
  imports: [
    ReactiveFormsModule,
    DecimalPipe,
    FaIconComponent,
    NgbTypeahead,
    NgbHighlight,
    ErrorCampo,
    RequiereConexion,
  ],
  templateUrl: './modal-datos-nutricionales.html',
})
export class ModalDatosNutricionales implements OnInit {
  protected readonly modal = inject(NgbActiveModal);
  private readonly admin = inject(AdminApi);
  private readonly elemento = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);

  /** Lo asigna `abrirModalDatosNutricionales`. */
  ingrediente!: Ingrediente;

  readonly formulario = new FormGroup({
    alimento: new FormControl<AlimentoBedca | string | null>(null, {
      validators: [elegidoOVacio],
    }),
    piezas: new FormArray<FilaPieza>([]),
  });
  readonly cargando = signal(true);
  readonly fallo = signal(false);
  readonly enviando = signal(false);
  readonly error = signal<string | null>(null);
  /** El alimento elegido, con sus valores. */
  readonly seleccionado = toSignal(
    this.formulario.controls.alimento.valueChanges.pipe(
      map((valor) => (valor && typeof valor === 'object' ? valor : null)),
    ),
    { initialValue: null },
  );

  protected readonly buscar = busquedaAlimentos(this.admin);
  protected readonly nombre = nombreAlimento;
  protected readonly esInvalido = esInvalido;
  protected readonly fuente = FUENTE_BEDCA;
  protected readonly maximoPiezas = MAXIMO_PIEZAS;
  protected readonly iconos = { quitar: faXmark, borrar: faTrashCan, anadir: faPlus };

  get piezas(): FormArray<FilaPieza> {
    return this.formulario.controls.piezas;
  }

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.fallo.set(false);
    this.admin.nutricionIngrediente(this.ingrediente.id).subscribe({
      next: (datos) => this.rellenar(datos),
      error: () => {
        this.cargando.set(false);
        this.fallo.set(true);
      },
    });
  }

  /** Los valores que se ven, además de la energía. */
  filas(a: AlimentoBedca): { nombre: string; valor: number | null; unidad: string }[] {
    return [
      { nombre: 'Grasas', valor: a.grasas, unidad: 'g' },
      { nombre: 'de las cuales saturadas', valor: a.grasasSaturadas, unidad: 'g' },
      { nombre: 'Hidratos de carbono', valor: a.hidratos, unidad: 'g' },
      { nombre: 'Fibra', valor: a.fibra, unidad: 'g' },
      { nombre: 'Proteínas', valor: a.proteinas, unidad: 'g' },
      { nombre: 'Sodio', valor: a.sodioMg, unidad: 'mg' },
    ];
  }

  quitarAlimento(): void {
    this.formulario.controls.alimento.setValue(null);
  }

  anadirPieza(): void {
    if (this.piezas.length < MAXIMO_PIEZAS) {
      this.piezas.push(this.fila('', null));
    }
  }

  quitarPieza(indice: number): void {
    this.piezas.removeAt(indice);
  }

  guardar(): void {
    // Sin los datos cargados, guardar borraría los que tiene
    if (this.cargando() || this.fallo()) {
      return;
    }
    if (this.formulario.invalid || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const alimento = this.formulario.controls.alimento.value;
    this.enviando.set(true);
    this.error.set(null);
    this.admin
      .guardarNutricion(this.ingrediente.id, {
        alimento: alimento && typeof alimento === 'object' ? alimento.id : null,
        piezas: this.piezas.getRawValue().map((fila) => ({
          pieza: fila.pieza.trim(),
          gramos: fila.gramos ?? 0,
        })),
      })
      .subscribe({
        next: (ingrediente) => this.modal.close(ingrediente),
        error: (error: HttpErrorResponse) => {
          this.enviando.set(false);
          if (error.status >= 400 && error.status < 500 && error.status !== 401) {
            if (!aplicarErroresServidor(this.formulario, errorApi(error))) {
              this.error.set(mensajeError(error));
            }
          }
        },
      });
  }

  private rellenar(datos: NutricionIngrediente): void {
    this.formulario.controls.alimento.setValue(datos.alimento);
    this.piezas.clear();
    for (const pieza of datos.piezas) {
      this.piezas.push(this.fila(pieza.pieza, pieza.gramos));
    }
    this.cargando.set(false);
    // El campo no existía al abrirse el modal (ngbAutofocus): el foco pasa a él al pintarlo
    afterNextRender(
      () => this.elemento.nativeElement.querySelector<HTMLInputElement>('#alimento-bedca')?.focus(),
      { injector: this.injector },
    );
  }

  private fila(pieza: string, gramos: number | null): FilaPieza {
    return new FormGroup({
      pieza: new FormControl(pieza, {
        nonNullable: true,
        validators: [
          Validators.required,
          Validators.maxLength(30),
          Validators.pattern(/^\s*\p{L}+\s*$/u),
        ],
      }),
      gramos: new FormControl(gramos, [
        Validators.required,
        Validators.min(0.1),
        Validators.max(10000),
      ]),
    });
  }
}

/** Abre el modal y emite el ingrediente con sus datos nuevos (nada si se cancela). */
export function abrirModalDatosNutricionales(
  modales: NgbModal,
  ingrediente: Ingrediente,
): Observable<Ingrediente> {
  const ref = modales.open(ModalDatosNutricionales, {
    ariaLabelledBy: 'modal-datos-nutricionales-titulo',
    modalDialogClass: 'animate__animated animate__fadeInDown',
  });
  (ref.componentInstance as ModalDatosNutricionales).ingrediente = ingrediente;
  return new Observable<Ingrediente>((suscriptor) => {
    ref.result.then(
      (resultado: Ingrediente) => {
        suscriptor.next(resultado);
        suscriptor.complete();
      },
      () => suscriptor.complete(),
    );
  });
}
