import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Anunciador } from '../core/anunciador';
import { Foto } from '../core/modelos';
import { GaleriaFotos } from './galeria-fotos';

const FOTOS: Foto[] = [
  { nombre: 'a.jpg', url: '/api/imagenes/a.jpg', descripcion: null },
  { nombre: 'b.jpg', url: '/api/imagenes/b.jpg', descripcion: 'La tortilla cortada' },
  { nombre: 'c.jpg', url: '/api/imagenes/c.jpg', descripcion: null },
];

describe('GaleriaFotos', () => {
  let fixture: ComponentFixture<GaleriaFotos>;
  let html: HTMLElement;
  let anunciar: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    anunciar = vi.spyOn(TestBed.inject(Anunciador), 'anunciar');
  });

  async function pintar(fotos: Foto[]): Promise<void> {
    fixture = TestBed.createComponent(GaleriaFotos);
    fixture.componentRef.setInput('fotos', fotos);
    fixture.componentRef.setInput('titulo', 'Tortilla');
    await fixture.whenStable();
    html = fixture.nativeElement as HTMLElement;
  }

  function principal(): HTMLImageElement {
    return html.querySelector<HTMLImageElement>('.marco-imagen img')!;
  }

  it('con una sola foto la muestra sin flechas ni miniaturas', async () => {
    await pintar([FOTOS[0]]);

    expect(principal().alt).toBe('Foto de Tortilla');
    expect(principal().getAttribute('src')).toBe('/api/imagenes/a.jpg?ancho=1200');
    expect(principal().getAttribute('srcset')).toContain('/api/imagenes/a.jpg?ancho=400 400w');
    expect(html.querySelector('.flecha')).toBeNull();
    expect(html.querySelector('.miniaturas')).toBeNull();
    expect(html.querySelector('figcaption')).toBeNull();
  });

  it('sin fotos muestra el marco vacío', async () => {
    await pintar([]);
    expect(html.querySelector('[role="img"]')!.getAttribute('aria-label')).toBe('Receta sin foto');
  });

  it('las flechas pasan de una foto a otra, en círculo, y lo anuncian', async () => {
    await pintar(FOTOS);
    expect(principal().alt).toBe('Tortilla: foto 1 de 3');
    expect(html.querySelector('.contador')!.textContent!.trim()).toBe('1 / 3');

    html.querySelector<HTMLButtonElement>('[aria-label="Foto siguiente"]')!.click();
    await fixture.whenStable();
    expect(principal().alt).toBe('La tortilla cortada');
    expect(html.querySelector('figcaption')!.textContent).toContain('La tortilla cortada');
    expect(anunciar).toHaveBeenCalledWith('Foto 2 de 3: La tortilla cortada');

    html.querySelector<HTMLButtonElement>('[aria-label="Foto anterior"]')!.click();
    html.querySelector<HTMLButtonElement>('[aria-label="Foto anterior"]')!.click();
    await fixture.whenStable();
    expect(principal().alt).toBe('Tortilla: foto 3 de 3');
    expect(anunciar).toHaveBeenLastCalledWith('Foto 3 de 3.');
  });

  it('las miniaturas muestran la foto elegida y marcan la actual', async () => {
    await pintar(FOTOS);
    const miniaturas = () => [...html.querySelectorAll<HTMLButtonElement>('.miniatura')];
    expect(miniaturas().map((b) => b.getAttribute('aria-label'))).toEqual([
      'Ver la foto 1 de 3',
      'Ver la foto 2 de 3: La tortilla cortada',
      'Ver la foto 3 de 3',
    ]);
    expect(miniaturas()[0].getAttribute('aria-current')).toBe('true');

    miniaturas()[2].click();
    await fixture.whenStable();
    expect(miniaturas()[2].getAttribute('aria-current')).toBe('true');
    expect(miniaturas()[0].hasAttribute('aria-current')).toBe(false);
    expect(principal().getAttribute('src')).toBe('/api/imagenes/c.jpg?ancho=1200');
  });

  it('las teclas de flecha también cambian de foto', async () => {
    await pintar(FOTOS);
    const figura = html.querySelector('figure')!;

    figura.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true }));
    await fixture.whenStable();
    expect(fixture.componentInstance.indice()).toBe(1);
    figura.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowLeft', bubbles: true }));
    figura.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));
    await fixture.whenStable();
    expect(fixture.componentInstance.indice()).toBe(0);
  });

  it('con otras fotos vuelve a la principal', async () => {
    await pintar(FOTOS);
    fixture.componentInstance.ver(2);
    fixture.componentRef.setInput('fotos', [FOTOS[1], FOTOS[2]]);
    await fixture.whenStable();
    expect(fixture.componentInstance.indice()).toBe(0);

    // Con una sola foto, las flechas del teclado no hacen nada
    fixture.componentRef.setInput('fotos', [FOTOS[0]]);
    await fixture.whenStable();
    fixture.componentInstance.tecla(new KeyboardEvent('keydown', { key: 'ArrowRight' }));
    expect(fixture.componentInstance.indice()).toBe(0);
  });
});
