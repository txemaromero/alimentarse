import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Anunciador } from '../core/anunciador';
import { Foto } from '../core/modelos';
import { EditorFotos, MAXIMO_FOTOS } from './editor-fotos';

const PRINCIPAL: Foto = { nombre: 'a.jpg', url: '/api/imagenes/a.jpg', descripcion: 'La tortilla' };
const SEGUNDA: Foto = { nombre: 'b.jpg', url: '/api/imagenes/b.jpg', descripcion: null };

describe('EditorFotos', () => {
  let fixture: ComponentFixture<EditorFotos>;
  let editor: EditorFotos;
  let html: HTMLElement;
  let anunciar: ReturnType<typeof vi.spyOn>;
  const creadas: string[] = [];
  const soltadas: string[] = [];

  beforeAll(() => {
    // jsdom no tiene URL.createObjectURL
    Object.defineProperty(URL, 'createObjectURL', {
      configurable: true,
      value: (fichero: File) => {
        const url = `blob:${fichero.name}`;
        creadas.push(url);
        return url;
      },
    });
    Object.defineProperty(URL, 'revokeObjectURL', {
      configurable: true,
      value: (url: string) => soltadas.push(url),
    });
  });

  beforeEach(async () => {
    creadas.length = 0;
    soltadas.length = 0;
    TestBed.configureTestingModule({});
    anunciar = vi.spyOn(TestBed.inject(Anunciador), 'anunciar');
    fixture = TestBed.createComponent(EditorFotos);
    editor = fixture.componentInstance;
    html = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  });

  function fichero(nombre: string, tipo = 'image/png'): File {
    return new File(['x'], nombre, { type: tipo });
  }

  function elegir(...ficheros: File[]): void {
    editor.anadir({ target: { files: ficheros, value: 'C:\\fakepath\\x' } } as unknown as Event);
  }

  function textos(selector: string): string[] {
    return [...html.querySelectorAll(selector)].map((e) =>
      e.textContent!.replace(/\s+/g, ' ').trim(),
    );
  }

  it('al editar muestra las fotos que tiene, la primera como principal', async () => {
    fixture.componentRef.setInput('iniciales', [PRINCIPAL, SEGUNDA]);
    await fixture.whenStable();

    expect(textos('.foto .fw-semibold')).toEqual(['Foto 1', 'Foto 2']);
    expect(html.querySelectorAll('.foto .badge')[0].textContent).toContain('Principal');
    expect(html.querySelector<HTMLInputElement>('.foto input')!.value).toBe('La tortilla');
    expect(html.querySelector<HTMLImageElement>('.miniatura')!.alt).toBe(
      'Vista previa de la foto 1',
    );
    expect(editor.cambiado()).toBe(false);
    expect(editor.peticion()).toEqual({
      fotos: [
        { existente: 'a.jpg', nueva: null, descripcion: 'La tortilla' },
        { existente: 'b.jpg', nueva: null, descripcion: null },
      ],
      ficheros: [],
    });
  });

  it('añade las imágenes válidas, avisa de las demás y no pasa del máximo', async () => {
    fixture.componentRef.setInput('iniciales', [PRINCIPAL]);
    await fixture.whenStable();
    const validas = Array.from({ length: MAXIMO_FOTOS }, (_, i) => fichero(`f${i}.png`));

    elegir(fichero('animada.gif', 'image/gif'), ...validas);
    await fixture.whenStable();

    expect(editor.fotos().length).toBe(MAXIMO_FOTOS);
    expect(editor.cambiado()).toBe(true);
    expect(anunciar).toHaveBeenCalledWith('5 fotos añadidas.');
    const error = html.querySelector('#receta-fotos-error')!.textContent!;
    expect(error).toContain('«animada.gif»: La imagen debe ser JPEG, PNG o WebP');
    expect(error).toContain('Como mucho 6 fotos: «f5.png» no se ha añadido');
    expect(html.querySelector<HTMLInputElement>('#receta-fotos')!.disabled).toBe(true);
    expect(textos('.foto .badge').filter((t) => t === 'Nueva').length).toBe(5);

    const { fotos, ficheros } = editor.peticion();
    expect(ficheros.map((f) => f.name)).toEqual(['f0.png', 'f1.png', 'f2.png', 'f3.png', 'f4.png']);
    expect(fotos[1]).toEqual({ existente: null, nueva: 0, descripcion: null });
    expect(fotos[5]).toEqual({ existente: null, nueva: 4, descripcion: null });
  });

  it('una sola foto añadida se anuncia en singular y sin errores no hay mensaje', async () => {
    elegir(fichero('una.jpg', 'image/jpeg'));
    await fixture.whenStable();
    expect(anunciar).toHaveBeenCalledWith('Foto añadida.');
    expect(html.querySelector('#receta-fotos-error')).toBeNull();
  });

  it('subir y bajar cambian el orden, mantienen el foco y lo anuncian', async () => {
    fixture.componentRef.setInput('iniciales', [PRINCIPAL, SEGUNDA]);
    await fixture.whenStable();

    html.querySelector<HTMLButtonElement>('[aria-label="Subir la foto 2"]')!.click();
    await fixture.whenStable();

    expect(editor.fotos().map((f) => f.existente!.nombre)).toEqual(['b.jpg', 'a.jpg']);
    expect(anunciar).toHaveBeenCalledWith('Foto movida al primer puesto: es la principal.');
    // Ya no se puede subir más: el foco pasa a «Bajar» de la misma foto
    expect(document.activeElement?.getAttribute('aria-label')).toBe('Bajar la foto 1');

    html.querySelector<HTMLButtonElement>('[aria-label="Bajar la foto 1"]')!.click();
    await fixture.whenStable();
    expect(editor.fotos().map((f) => f.existente!.nombre)).toEqual(['a.jpg', 'b.jpg']);
    expect(anunciar).toHaveBeenCalledWith('Foto movida al puesto 2.');

    editor.mover(0, -1);
    editor.mover(1, 1);
    expect(editor.fotos().map((f) => f.existente!.nombre)).toEqual(['a.jpg', 'b.jpg']);
  });

  it('quitar una foto suelta su vista previa y pasa el foco a la vecina o al campo', async () => {
    elegir(fichero('uno.png'), fichero('dos.png'));
    await fixture.whenStable();

    html.querySelector<HTMLButtonElement>('[aria-label="Quitar la foto 1"]')!.click();
    await fixture.whenStable();
    expect(soltadas).toEqual(['blob:uno.png']);
    expect(anunciar).toHaveBeenCalledWith('Foto quitada.');
    expect(document.activeElement?.getAttribute('aria-label')).toBe('Quitar la foto 1');

    html.querySelector<HTMLButtonElement>('[aria-label="Quitar la foto 1"]')!.click();
    await fixture.whenStable();
    expect(document.activeElement?.id).toBe('receta-fotos');
    expect(editor.peticion()).toEqual({ fotos: [], ficheros: [] });
  });

  it('la descripción se guarda sin espacios, y vacía es null', async () => {
    fixture.componentRef.setInput('iniciales', [PRINCIPAL]);
    await fixture.whenStable();
    const campo = html.querySelector<HTMLInputElement>('#' + 'foto-descripcion-0')!;

    campo.value = '  Recién hecha ';
    campo.dispatchEvent(new Event('input'));
    expect(editor.peticion().fotos[0].descripcion).toBe('Recién hecha');
    expect(editor.cambiado()).toBe(true);

    campo.value = '   ';
    campo.dispatchEvent(new Event('input'));
    expect(editor.peticion().fotos[0].descripcion).toBeNull();
  });

  it('coloca los errores del servidor en su foto o en la galería', async () => {
    fixture.componentRef.setInput('iniciales', [PRINCIPAL, SEGUNDA]);
    await fixture.whenStable();

    expect(editor.errorServidor('fotos[1].existente', 'Esa foto no es de esta receta')).toBe(true);
    expect(editor.errorServidor('fotos', 'Hay ficheros que no corresponden a ninguna foto')).toBe(
      true,
    );
    expect(editor.errorServidor('fotos[7]', 'No existe')).toBe(false);
    expect(editor.errorServidor('otra', 'No')).toBe(false);
    await fixture.whenStable();

    const segunda = html.querySelectorAll('.foto')[1];
    expect(segunda.querySelector('.invalid-feedback')!.textContent).toContain(
      'no es de esta receta',
    );
    expect(segunda.querySelector('input')!.getAttribute('aria-describedby')).toMatch(
      /^foto-error-/,
    );
    expect(html.querySelector('#receta-fotos-error')!.textContent).toContain('no corresponden');

    // Al cambiar la descripción, el error de esa foto desaparece
    const campo = segunda.querySelector('input')!;
    campo.value = 'Otra';
    campo.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    expect(html.querySelectorAll('.foto')[1].querySelector('.invalid-feedback')).toBeNull();
  });

  it('al destruirse suelta las vistas previas de los ficheros nuevos', async () => {
    elegir(fichero('nueva.png'));
    fixture.componentRef.setInput('iniciales', [PRINCIPAL]);
    await fixture.whenStable();
    // Cargar otra galería suelta las vistas previas de la anterior
    expect(soltadas).toEqual(['blob:nueva.png']);

    elegir(fichero('otra.png'));
    fixture.destroy();
    expect(soltadas).toEqual(['blob:nueva.png', 'blob:otra.png']);
  });
});
