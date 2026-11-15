import {
  Component,
  DestroyRef,
  ElementRef,
  Injector,
  afterNextRender,
  effect,
  inject,
  input,
  signal,
  untracked,
} from '@angular/core';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faArrowDown, faArrowUp, faTrashCan } from '@fortawesome/free-solid-svg-icons';
import { errorImagen } from '../compartido/imagenes';
import { Anunciador } from '../core/anunciador';
import { Foto, FotoRequest } from '../core/modelos';

/** Fotos que admite el servidor por receta (D-083). */
export const MAXIMO_FOTOS = 6;

/** Una foto de la lista: una que ya tenía la receta o un fichero elegido ahora. */
interface FotoEditable {
  /** Identifica la foto en la lista aunque cambie de sitio. */
  clave: number;
  /** La que ya tenía la receta, o null si es nueva. */
  existente: Foto | null;
  fichero: File | null;
  /** Para la miniatura: la de la imagen o una URL `blob:` del fichero. */
  url: string;
  descripcion: string;
  error: string | null;
}

/** Lo que hay que mandar: la galería en orden y los ficheros nuevos a los que apunta (`nueva`). */
export interface FotosPedidas {
  fotos: FotoRequest[];
  ficheros: File[];
}

/**
 * Las fotos de una receta en el formulario (D-083): hasta 6, la primera es la principal. Se añaden varias a la vez,
 * se reordenan con «Subir» y «Bajar», se quitan y cada una puede llevar una descripción (su texto alternativo).
 */
@Component({
  selector: 'app-editor-fotos',
  imports: [FaIconComponent],
  templateUrl: './editor-fotos.html',
  styleUrl: './editor-fotos.scss',
})
export class EditorFotos {
  private readonly anunciador = inject(Anunciador);
  private readonly elemento = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);

  /** Las fotos que ya tiene la receta (al editarla). */
  readonly iniciales = input<Foto[]>([]);

  readonly fotos = signal<FotoEditable[]>([]);
  /** Error de la galería en conjunto (al añadir ficheros o el que dé el servidor). */
  readonly error = signal<string | null>(null);
  /** Si ha cambiado algo desde que se cargó: para avisar antes de salir sin guardar. */
  readonly cambiado = signal(false);

  protected readonly maximo = MAXIMO_FOTOS;
  protected readonly iconos = { subir: faArrowUp, bajar: faArrowDown, quitar: faTrashCan };
  private siguiente = 0;

  constructor() {
    effect(() => {
      const iniciales = this.iniciales();
      untracked(() => {
        this.soltarUrls(this.fotos());
        this.fotos.set(
          iniciales.map((foto) => ({
            clave: this.siguiente++,
            existente: foto,
            fichero: null,
            url: foto.url,
            descripcion: foto.descripcion ?? '',
            error: null,
          })),
        );
        this.cambiado.set(false);
      });
    });
    inject(DestroyRef).onDestroy(() => this.soltarUrls(this.fotos()));
  }

  /** Añade los ficheros elegidos que sean imágenes válidas, hasta el máximo. */
  anadir(evento: Event): void {
    const campo = evento.target as HTMLInputElement;
    const elegidos = [...(campo.files ?? [])];
    campo.value = '';
    const errores: string[] = [];
    const nuevas: FotoEditable[] = [];
    for (const fichero of elegidos) {
      const error = errorImagen(fichero);
      if (error) {
        errores.push(`«${fichero.name}»: ${error}`);
      } else if (this.fotos().length + nuevas.length >= MAXIMO_FOTOS) {
        errores.push(`Como mucho ${MAXIMO_FOTOS} fotos: «${fichero.name}» no se ha añadido`);
      } else {
        nuevas.push({
          clave: this.siguiente++,
          existente: null,
          fichero,
          url: URL.createObjectURL(fichero),
          descripcion: '',
          error: null,
        });
      }
    }
    this.error.set(errores.length ? errores.join('. ') + '.' : null);
    if (nuevas.length) {
      this.fotos.update((fotos) => [...fotos, ...nuevas]);
      this.cambiado.set(true);
      this.anunciador.anunciar(
        nuevas.length === 1 ? 'Foto añadida.' : `${nuevas.length} fotos añadidas.`,
      );
    }
  }

  describir(clave: number, evento: Event): void {
    const descripcion = (evento.target as HTMLInputElement).value;
    this.fotos.update((fotos) =>
      fotos.map((foto) => (foto.clave === clave ? { ...foto, descripcion, error: null } : foto)),
    );
    this.cambiado.set(true);
  }

  /** Sube (-1) o baja (+1) la foto un puesto; el foco sigue en el botón pulsado o, si ya no se puede, en el otro. */
  mover(indice: number, paso: -1 | 1): void {
    const destino = indice + paso;
    const fotos = [...this.fotos()];
    if (destino < 0 || destino >= fotos.length) {
      return;
    }
    [fotos[indice], fotos[destino]] = [fotos[destino], fotos[indice]];
    this.fotos.set(fotos);
    this.cambiado.set(true);
    const clave = fotos[destino].clave;
    const accion = paso < 0 ? 'subir' : 'bajar';
    const otra = paso < 0 ? 'bajar' : 'subir';
    this.enfocar(
      `[data-clave="${clave}"] [data-accion="${accion}"]:not(:disabled)`,
      `[data-clave="${clave}"] [data-accion="${otra}"]`,
    );
    this.anunciador.anunciar(
      destino === 0
        ? 'Foto movida al primer puesto: es la principal.'
        : `Foto movida al puesto ${destino + 1}.`,
    );
  }

  /** Quita la foto; el foco pasa a la siguiente (o a la anterior, o al campo para añadir). */
  quitar(indice: number): void {
    const fotos = [...this.fotos()];
    const [quitada] = fotos.splice(indice, 1);
    this.soltarUrls([quitada]);
    this.fotos.set(fotos);
    this.cambiado.set(true);
    const vecina = fotos[indice] ?? fotos[indice - 1];
    this.enfocar(
      vecina ? `[data-clave="${vecina.clave}"] [data-accion="quitar"]` : '#receta-fotos',
      '#receta-fotos',
    );
    this.anunciador.anunciar('Foto quitada.');
  }

  /** La galería que se manda al guardar, con los ficheros nuevos en el orden en que aparecen. */
  peticion(): FotosPedidas {
    const ficheros: File[] = [];
    const fotos = this.fotos().map((foto): FotoRequest => {
      const descripcion = foto.descripcion.trim() || null;
      if (foto.existente) {
        return { existente: foto.existente.nombre, nueva: null, descripcion };
      }
      ficheros.push(foto.fichero!);
      return { existente: null, nueva: ficheros.length - 1, descripcion };
    });
    return { fotos, ficheros };
  }

  /**
   * Coloca un error del servidor: «fotos[2]…» en esa foto (en el orden en que se mandó, que es el de la lista) y
   * «fotos» en la galería. Devuelve si lo ha colocado.
   */
  errorServidor(campo: string, mensaje: string): boolean {
    if (campo === 'fotos') {
      this.error.set(mensaje);
      return true;
    }
    const posicion = /^fotos\[(\d+)\]/.exec(campo);
    if (!posicion || !this.fotos()[Number(posicion[1])]) {
      return false;
    }
    const indice = Number(posicion[1]);
    this.fotos.update((fotos) =>
      fotos.map((foto, i) => (i === indice ? { ...foto, error: mensaje } : foto)),
    );
    return true;
  }

  /** «Vista previa de la foto 2», el texto alternativo de la miniatura. */
  altMiniatura(indice: number): string {
    return `Vista previa de la foto ${indice + 1}`;
  }

  private enfocar(selector: string, alternativo: string): void {
    afterNextRender(
      () => {
        const raiz = this.elemento.nativeElement;
        const destino =
          raiz.querySelector<HTMLElement>(selector) ?? raiz.querySelector<HTMLElement>(alternativo);
        destino?.focus();
      },
      { injector: this.injector },
    );
  }

  private soltarUrls(fotos: FotoEditable[]): void {
    for (const foto of fotos) {
      if (foto.fichero) {
        URL.revokeObjectURL(foto.url);
      }
    }
  }
}
