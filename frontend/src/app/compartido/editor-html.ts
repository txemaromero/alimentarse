import {
  Component,
  ElementRef,
  afterNextRender,
  forwardRef,
  inject,
  input,
  signal,
  viewChild,
  DestroyRef,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, ValidatorFn } from '@angular/forms';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import {
  IconDefinition,
  faBold,
  faHeading,
  faItalic,
  faLink,
  faListOl,
  faListUl,
  faQuoteLeft,
  faRotateLeft,
  faRotateRight,
  faUnderline,
} from '@fortawesome/free-solid-svg-icons';
import { Editor } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';

interface BotonFormato {
  id: string;
  titulo: string;
  icono: IconDefinition;
  /** Marca o nodo cuyo estado activo refleja el botón (aria-pressed). */
  activo?: [string, Record<string, unknown>?];
  accion: (editor: Editor) => void;
}

/**
 * Editor de texto enriquecido (TipTap) para las instrucciones de una receta, como control de formulario. El valor es
 * HTML (cadena vacía si no hay contenido). Solo ofrece el formato que admite el saneado del backend (D-035):
 * negrita, cursiva, subrayado, subtítulos, listas, citas y enlaces.
 */
@Component({
  selector: 'app-editor-html',
  imports: [FaIconComponent],
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => EditorHtml), multi: true },
  ],
  templateUrl: './editor-html.html',
  styleUrl: './editor-html.scss',
})
export class EditorHtml implements ControlValueAccessor {
  /** Id del área editable. */
  readonly idCampo = input.required<string>();
  /** Id de la etiqueta visible (un `<label for>` no sirve con un área editable). */
  readonly idEtiqueta = input.required<string>();
  readonly descripcion = input<string | null>(null);
  readonly invalido = input(false);

  private readonly area = viewChild.required<ElementRef<HTMLElement>>('area');
  private editor: Editor | null = null;
  private valorPendiente = '';
  private alCambiar: (valor: string) => void = () => undefined;
  private alTocar: () => void = () => undefined;

  /** Cambia con cada transacción del editor para refrescar la barra (OnPush). */
  protected readonly version = signal(0);
  protected readonly deshabilitado = signal(false);

  protected readonly botones: BotonFormato[] = [
    {
      id: 'negrita',
      titulo: 'Negrita',
      icono: faBold,
      activo: ['bold'],
      accion: (e) => e.chain().focus().toggleBold().run(),
    },
    {
      id: 'cursiva',
      titulo: 'Cursiva',
      icono: faItalic,
      activo: ['italic'],
      accion: (e) => e.chain().focus().toggleItalic().run(),
    },
    {
      id: 'subrayado',
      titulo: 'Subrayado',
      icono: faUnderline,
      activo: ['underline'],
      accion: (e) => e.chain().focus().toggleUnderline().run(),
    },
    {
      id: 'subtitulo',
      titulo: 'Subtítulo',
      icono: faHeading,
      activo: ['heading', { level: 3 }],
      accion: (e) => e.chain().focus().toggleHeading({ level: 3 }).run(),
    },
    {
      id: 'lista',
      titulo: 'Lista',
      icono: faListUl,
      activo: ['bulletList'],
      accion: (e) => e.chain().focus().toggleBulletList().run(),
    },
    {
      id: 'pasos',
      titulo: 'Lista numerada (pasos)',
      icono: faListOl,
      activo: ['orderedList'],
      accion: (e) => e.chain().focus().toggleOrderedList().run(),
    },
    {
      id: 'cita',
      titulo: 'Cita o consejo',
      icono: faQuoteLeft,
      activo: ['blockquote'],
      accion: (e) => e.chain().focus().toggleBlockquote().run(),
    },
    {
      id: 'enlace',
      titulo: 'Enlace',
      icono: faLink,
      activo: ['link'],
      accion: (e) => this.enlace(e),
    },
  ];
  protected readonly historial: BotonFormato[] = [
    {
      id: 'deshacer',
      titulo: 'Deshacer',
      icono: faRotateLeft,
      accion: (e) => e.chain().focus().undo().run(),
    },
    {
      id: 'rehacer',
      titulo: 'Rehacer',
      icono: faRotateRight,
      accion: (e) => e.chain().focus().redo().run(),
    },
  ];

  constructor() {
    afterNextRender(() => this.crear());
    inject(DestroyRef).onDestroy(() => this.editor?.destroy());
  }

  writeValue(valor: string | null): void {
    this.valorPendiente = valor ?? '';
    if (this.editor && this.valorActual() !== this.valorPendiente) {
      this.editor.commands.setContent(this.valorPendiente, { emitUpdate: false });
    }
  }

  registerOnChange(fn: (valor: string) => void): void {
    this.alCambiar = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.alTocar = fn;
  }

  setDisabledState(deshabilitado: boolean): void {
    this.deshabilitado.set(deshabilitado);
    this.editor?.setEditable(!deshabilitado);
  }

  activo(boton: BotonFormato): boolean {
    this.version();
    return !!(this.editor && boton.activo && this.editor.isActive(...boton.activo));
  }

  ejecutar(boton: BotonFormato): void {
    if (this.editor && !this.deshabilitado()) {
      boton.accion(this.editor);
    }
  }

  private crear(): void {
    const atributos: Record<string, string> = {
      id: this.idCampo(),
      // Clases globales (styles.scss): TipTap crea el área fuera del encapsulado de estilos
      class: 'editor-html-contenido instrucciones',
      role: 'textbox',
      'aria-multiline': 'true',
      'aria-labelledby': this.idEtiqueta(),
    };
    const descripcion = this.descripcion();
    if (descripcion) {
      atributos['aria-describedby'] = descripcion;
    }
    this.editor = new Editor({
      element: this.area().nativeElement,
      extensions: [
        StarterKit.configure({
          heading: { levels: [3] },
          code: false,
          codeBlock: false,
          horizontalRule: false,
          strike: false,
          link: {
            openOnClick: false,
            autolink: true,
            defaultProtocol: 'https',
            protocols: ['http', 'https', 'mailto'],
          },
        }),
      ],
      content: this.valorPendiente,
      editable: !this.deshabilitado(),
      editorProps: { attributes: atributos },
      onUpdate: () => this.alCambiar(this.valorActual()),
      onBlur: () => this.alTocar(),
      onTransaction: () => this.version.update((n) => n + 1),
    });
  }

  private valorActual(): string {
    if (!this.editor || this.editor.isEmpty) {
      return '';
    }
    // Sin los párrafos vacíos del final, que TipTap deja al salir de una lista
    return this.editor.getHTML().replace(/(<p><\/p>)+$/, '');
  }

  private enlace(editor: Editor): void {
    const anterior = (editor.getAttributes('link')['href'] as string | undefined) ?? '';
    const url = window.prompt('Dirección del enlace (déjala vacía para quitarlo):', anterior);
    if (url === null) {
      return;
    }
    if (url.trim() === '') {
      editor.chain().focus().extendMarkRange('link').unsetLink().run();
    } else {
      editor.chain().focus().extendMarkRange('link').setLink({ href: url.trim() }).run();
    }
  }
}

/** Texto visible de un HTML (sin etiquetas ni espacios sobrantes). */
export function textoDeHtml(html: string): string {
  const documento = new DOMParser().parseFromString(html, 'text/html');
  return (documento.body.textContent ?? '').replace(/\s+/g, ' ').trim();
}

/** Obligatorio para HTML: debe tener algún texto visible (como comprueba el backend tras sanear). */
export const htmlConTexto: ValidatorFn = (control) =>
  textoDeHtml((control.value as string) ?? '') ? null : { required: true };
