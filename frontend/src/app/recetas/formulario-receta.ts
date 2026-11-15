import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  ElementRef,
  OnInit,
  computed,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormArray,
  FormControl,
  FormGroup,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { Router, RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faPlus, faSeedling } from '@fortawesome/free-solid-svg-icons';
import { NgbHighlight, NgbModal, NgbTypeahead } from '@ng-bootstrap/ng-bootstrap';
import { EditorHtml, htmlConTexto } from '../compartido/editor-html';
import { ErrorCampo } from '../compartido/error-campo';
import { noVacio } from '../compartido/validadores';
import { CatalogoApi } from '../core/catalogo-api';
import { aplicarErroresServidor, errorApi, esInvalido, mensajeError } from '../core/errores';
import {
  DIFICULTADES,
  Dificultad,
  Foto,
  Ingrediente,
  RecetaDetalle,
  RecetaRequest,
} from '../core/modelos';
import { Notificaciones } from '../core/notificaciones';
import { NOMBRE_APLICACION } from '../core/titulo-pagina';
import { busquedaIngredientes, nombreIngrediente } from '../ingredientes/busqueda-ingredientes';
import { IngredientesApi } from '../ingredientes/ingredientes-api';
import { abrirModalIngrediente } from '../ingredientes/modal-ingrediente';
import { EditorFotos } from './editor-fotos';
import { RecetasApi } from './recetas-api';
import { RequiereConexion } from '../compartido/requiere-conexion';

/** Lo que una fila necesita del ingrediente: el resto (como su alimento de BEDCA) no se usa aquí. */
type IngredienteDeFila = Pick<Ingrediente, 'id' | 'nombre' | 'grupo'>;

type FilaIngrediente = FormGroup<{
  ingrediente: FormControl<IngredienteDeFila>;
  cantidad: FormControl<string>;
}>;

const alMenosUno = (control: AbstractControl): ValidationErrors | null =>
  (control as FormArray).length > 0 ? null : { sinIngredientes: true };

/** Los mismos que admite el servidor (D-089). */
export const MAXIMO_INGREDIENTES = 50;

/** Un enlace https a YouTube o Vimeo, con espacios alrededor o sin nada (D-083). */
export const PATRON_VIDEO =
  /^\s*(https:\/\/((www\.|m\.)?youtube\.com|youtu\.be|((www|player)\.)?vimeo\.com)(\/\S*)?)?\s*$/i;

/** Alta (`/recetas/nueva`) y edición (`/recetas/:id/editar`) de una receta. */
@Component({
  selector: 'app-formulario-receta',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    FaIconComponent,
    NgbTypeahead,
    NgbHighlight,
    EditorHtml,
    ErrorCampo,
    RequiereConexion,
    EditorFotos,
  ],
  templateUrl: './formulario-receta.html',
  styleUrl: './formulario-receta.scss',
})
export class FormularioReceta implements OnInit {
  private readonly recetasApi = inject(RecetasApi);
  private readonly ingredientesApi = inject(IngredientesApi);
  private readonly modales = inject(NgbModal);
  private readonly notificaciones = inject(Notificaciones);
  private readonly router = inject(Router);
  private readonly titulo = inject(Title);
  private readonly fb = inject(NonNullableFormBuilder);

  /** `:id` de la ruta; sin él, es un alta. */
  readonly id = input<string>();
  readonly esEdicion = computed(() => !!this.id());

  readonly tipos = toSignal(inject(CatalogoApi).tipos$, { initialValue: [] });
  readonly dificultades = DIFICULTADES;

  readonly formulario = this.fb.group({
    titulo: ['', [Validators.required, noVacio, Validators.maxLength(100)]],
    instrucciones: ['', [htmlConTexto, Validators.maxLength(20_000)]],
    tipo: new FormControl<number | null>(null, Validators.required),
    tiempo: new FormControl<number | null>(null, [
      Validators.required,
      Validators.min(1),
      Validators.max(1440),
    ]),
    comensales: new FormControl<number | null>(null, [
      Validators.required,
      Validators.min(1),
      Validators.max(100),
    ]),
    dificultad: new FormControl<Dificultad | null>(null, Validators.required),
    ingredientes: this.fb.array<FilaIngrediente>([], alMenosUno),
    // Un enlace https a YouTube o Vimeo (D-083); el servidor lo vuelve a comprobar
    video: ['', [Validators.maxLength(500), Validators.pattern(PATRON_VIDEO)]],
  });

  /** Ingrediente que se está añadiendo (no forma parte del envío). */
  readonly busqueda = new FormControl<Ingrediente | string | null>(null);
  readonly cantidad = new FormControl('', {
    nonNullable: true,
    validators: Validators.maxLength(100),
  });
  readonly errorIngredientes = signal<string | null>(null);

  /** Las fotos que ya tiene la receta, al editarla (D-083). */
  readonly fotosIniciales = signal<Foto[]>([]);

  readonly cargando = signal(false);
  readonly noEncontrada = signal(false);
  readonly enviando = signal(false);
  readonly error = signal<string | null>(null);
  private guardada = false;

  private readonly campoCantidad = viewChild<ElementRef<HTMLInputElement>>('campoCantidad');
  private readonly editorFotos = viewChild.required(EditorFotos);

  protected readonly esInvalido = esInvalido;
  protected readonly nombreIngrediente = nombreIngrediente;
  protected readonly iconos = { anadir: faPlus, crear: faSeedling };
  protected readonly buscarIngredientes = busquedaIngredientes(this.ingredientesApi, () =>
    this.filas.controls.map((fila) => fila.controls.ingrediente.value.id),
  );

  get filas(): FormArray<FilaIngrediente> {
    return this.formulario.controls.ingredientes;
  }

  ngOnInit(): void {
    const id = this.id();
    if (id) {
      this.cargar(Number(id));
    }
  }

  /** Para el guard de salida: hay cambios que se perderían. */
  hayCambiosSinGuardar(): boolean {
    return !this.guardada && (this.formulario.dirty || this.editorFotos().cambiado());
  }

  // ---- Ingredientes ----

  puedeAnadir(): boolean {
    return (
      typeof this.busqueda.value === 'object' &&
      this.busqueda.value !== null &&
      this.cantidad.value.trim() !== '' &&
      this.cantidad.valid
    );
  }

  /** Al elegir una sugerencia, el valor del buscador pasa a ser el ingrediente: se pide la cantidad. */
  alElegirIngrediente(): void {
    this.errorIngredientes.set(null);
    setTimeout(() => this.campoCantidad()?.nativeElement.focus());
  }

  anadirIngrediente(): void {
    const ingrediente = this.busqueda.value;
    if (!this.puedeAnadir() || typeof ingrediente !== 'object' || ingrediente === null) {
      return;
    }
    if (this.filas.controls.some((f) => f.controls.ingrediente.value.id === ingrediente.id)) {
      this.errorIngredientes.set(`«${ingrediente.nombre}» ya está en la lista.`);
      return;
    }
    if (this.filas.length >= MAXIMO_INGREDIENTES) {
      this.errorIngredientes.set(`Como mucho ${MAXIMO_INGREDIENTES} ingredientes.`);
      return;
    }
    this.filas.push(this.fila(ingrediente, this.cantidad.value.trim()));
    this.filas.markAsDirty();
    this.busqueda.setValue(null);
    this.cantidad.setValue('');
    this.errorIngredientes.set(null);
  }

  quitarIngrediente(indice: number): void {
    this.filas.removeAt(indice);
    this.filas.markAsDirty();
    this.filas.markAsTouched();
  }

  crearIngrediente(): void {
    const escrito = typeof this.busqueda.value === 'string' ? this.busqueda.value.trim() : '';
    abrirModalIngrediente(this.modales, { nombreInicial: escrito }).subscribe((creado) => {
      this.notificaciones.exito(`Ingrediente «${creado.nombre}» creado.`);
      this.busqueda.setValue(creado);
      setTimeout(() => this.campoCantidad()?.nativeElement.focus());
    });
  }

  // ---- Envío ----

  guardar(): void {
    if (this.formulario.invalid || this.enviando()) {
      this.formulario.markAllAsTouched();
      if (this.filas.length === 0) {
        this.errorIngredientes.set('Añade al menos un ingrediente');
      }
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    const id = this.id();
    const { fotos, ficheros } = this.editorFotos().peticion();
    const receta = { ...this.datos(), fotos };
    const peticion = id
      ? this.recetasApi.editar(Number(id), receta, ficheros)
      : this.recetasApi.crear(receta, ficheros);
    peticion.subscribe({
      next: (receta) => {
        this.guardada = true;
        this.notificaciones.exito(
          id
            ? 'Receta guardada.'
            : 'Receta creada. Todavía no está publicada: publícala cuando quieras que la vean los demás.',
        );
        this.router.navigate(['/recetas', receta.id]);
      },
      error: (error: HttpErrorResponse) => this.falloAlGuardar(error),
    });
  }

  cancelar(): void {
    const id = this.id();
    this.router.navigate(id ? ['/recetas', id] : ['/mis-recetas']);
  }

  /** Cuerpo de la parte `receta` a partir del formulario, sin las fotos (las pone el editor de fotos). */
  datos(): RecetaRequest {
    const v = this.formulario.getRawValue();
    return {
      titulo: v.titulo.trim(),
      instrucciones: v.instrucciones,
      tipo: Number(v.tipo),
      tiempo: Number(v.tiempo),
      comensales: Number(v.comensales),
      dificultad: v.dificultad!,
      ingredientes: v.ingredientes.map((fila) => ({
        ingrediente: fila.ingrediente.id,
        cantidad: fila.cantidad,
      })),
      fotos: null,
      video: v.video.trim() || null,
    };
  }

  private cargar(id: number): void {
    this.cargando.set(true);
    this.recetasApi.detalle(id).subscribe({
      next: (receta) => {
        this.cargando.set(false);
        if (!receta.esAutor) {
          this.notificaciones.error('Solo el autor puede editar esta receta.');
          this.router.navigate(['/recetas', receta.id]);
          return;
        }
        this.mostrar(receta);
      },
      error: (error: HttpErrorResponse) => {
        this.cargando.set(false);
        if (error.status === 404 || error.status === 400) {
          this.noEncontrada.set(true);
        } else {
          this.error.set(mensajeError(error, 'No se ha podido cargar la receta.'));
        }
      },
    });
  }

  private mostrar(receta: RecetaDetalle): void {
    this.titulo.setTitle(`Editar «${receta.titulo}» · ${NOMBRE_APLICACION}`);
    this.filas.clear();
    for (const i of receta.ingredientes) {
      this.filas.push(this.fila({ id: i.id, nombre: i.nombre, grupo: i.grupo }, i.cantidad));
    }
    this.formulario.reset({
      titulo: receta.titulo,
      instrucciones: receta.instrucciones,
      tipo: receta.tipo.id,
      tiempo: receta.tiempo,
      comensales: receta.comensales,
      dificultad: receta.dificultad,
      video: receta.video ?? '',
    });
    this.fotosIniciales.set(receta.fotos);
  }

  private fila(ingrediente: IngredienteDeFila, cantidad: string): FilaIngrediente {
    return this.fb.group({
      ingrediente: this.fb.control(ingrediente),
      cantidad: this.fb.control(cantidad, [Validators.required, Validators.maxLength(100)]),
    });
  }

  private falloAlGuardar(error: HttpErrorResponse): void {
    this.enviando.set(false);
    if (error.status < 400 || error.status >= 500 || error.status === 401) {
      return;
    }
    const detalle = errorApi(error);
    let colocado = aplicarErroresServidor(this.formulario, detalle);
    for (const [campo, mensaje] of Object.entries(detalle?.errores ?? {})) {
      if (campo.startsWith('ingredientes')) {
        this.errorIngredientes.set(mensaje);
        colocado = true;
      } else if (campo.startsWith('fotos') && this.editorFotos().errorServidor(campo, mensaje)) {
        colocado = true;
      }
    }
    if (!colocado) {
      this.error.set(mensajeError(error));
    } else {
      this.error.set('Revisa los campos marcados.');
    }
  }
}
