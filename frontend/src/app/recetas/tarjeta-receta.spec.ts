import { LOCALE_ID } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeEs from '@angular/common/locales/es';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { detalleDePrueba, recetaDePrueba } from '../../testing/datos';
import { OpcionesConfirmacion } from '../compartido/modal-confirmacion';
import { RecetaResumen } from '../core/modelos';
import { Notificaciones } from '../core/notificaciones';
import { TarjetaReceta } from './tarjeta-receta';

/** `NgbModal.open` simulado: acepta la confirmación ejecutando su acción, como haría el modal. */
function aceptarConfirmacion() {
  const instancia: { opciones?: OpcionesConfirmacion<unknown> } = {};
  let resolver!: (valor: unknown) => void;
  const resultado = new Promise((r) => (resolver = r));
  vi.spyOn(TestBed.inject(NgbModal), 'open').mockReturnValue({
    componentInstance: instancia,
    result: resultado,
  } as never);
  return {
    instancia,
    /** Lanza la acción (la petición queda pendiente para el test) y cierra el modal cuando responde. */
    aceptar: () => instancia.opciones!.accion().subscribe((r) => resolver({ resultado: r })),
  };
}

registerLocaleData(localeEs);

describe('TarjetaReceta', () => {
  let fixture: ComponentFixture<TarjetaReceta>;
  let servidor: HttpTestingController;
  let emitidas: RecetaResumen[];

  async function crear(receta: RecetaResumen, modoAutor = false) {
    fixture = TestBed.createComponent(TarjetaReceta);
    fixture.componentRef.setInput('receta', receta);
    fixture.componentRef.setInput('modoAutor', modoAutor);
    emitidas = [];
    fixture.componentInstance.cambio.subscribe((r) => emitidas.push(r));
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: LOCALE_ID, useValue: 'es' },
      ],
    });
    servidor = TestBed.inject(HttpTestingController);
  });

  afterEach(() => servidor.verify());

  it('muestra los datos de la receta', async () => {
    const html = await crear(recetaDePrueba());
    expect(html.querySelector('h3 a')?.textContent).toContain('Tortilla de patata');
    expect(html.querySelector('h3 a')?.getAttribute('href')).toBe('/recetas/1');
    expect(html.textContent).toContain('por lucia');
    expect(html.textContent).toContain('Plato');
    expect(html.textContent).toContain('45 min');
    expect(html.textContent).toContain('Media');
    const imagen = html.querySelector('img');
    expect(imagen?.getAttribute('src')).toBe('/api/imagenes/tortilla.jpg?ancho=800');
    expect(imagen?.getAttribute('srcset')).toBe(
      '/api/imagenes/tortilla.jpg?ancho=400 400w, /api/imagenes/tortilla.jpg?ancho=800 800w, ' +
        '/api/imagenes/tortilla.jpg?ancho=1200 1200w',
    );
    expect(html.querySelector('.boton-me-gusta')?.getAttribute('aria-pressed')).toBe('false');
  });

  it('muestra la nota media si tiene valoraciones (D-077)', async () => {
    let html = await crear(recetaDePrueba({ nota: 4.5, valoraciones: 12 }));
    expect(html.querySelector('.valoracion .visually-hidden')!.textContent).toBe(
      'Nota media: 4,5 de 5, 12 valoraciones',
    );
    expect(html.querySelector('.valoracion')!.textContent).toContain('4,5 (12)');
    fixture.destroy();

    html = await crear(recetaDePrueba({ nota: 5, valoraciones: 1 }));
    expect(html.querySelector('.valoracion')!.textContent).toContain('1 valoración');
    fixture.destroy();

    html = await crear(recetaDePrueba());
    expect(html.querySelector('.valoracion')).toBeNull();
  });

  it('sin imagen muestra un marcador', async () => {
    const html = await crear(recetaDePrueba({ imagen: null }));
    expect(html.querySelector('img')).toBeNull();
    expect(html.querySelector('.sin-imagen')).toBeTruthy();
  });

  it('el corazón da «me gusta» y emite la receta actualizada', async () => {
    const html = await crear(recetaDePrueba());
    (html.querySelector('.boton-me-gusta') as HTMLButtonElement).click();

    const peticion = servidor.expectOne('/api/recetas/1/like');
    expect(peticion.request.method).toBe('POST');
    peticion.flush({ likes: 4, meGusta: true });

    expect(emitidas).toEqual([expect.objectContaining({ id: 1, likes: 4, meGusta: true })]);
  });

  it('con «me gusta» dado, el corazón lo quita', async () => {
    const html = await crear(recetaDePrueba({ meGusta: true, likes: 4 }));
    (html.querySelector('.boton-me-gusta') as HTMLButtonElement).click();
    servidor.expectOne((p) => p.method === 'DELETE').flush({ likes: 3, meGusta: false });
    expect(emitidas[0].meGusta).toBe(false);
  });

  it('una receta no publicada no admite «me gusta» y lo indica', async () => {
    const html = await crear(recetaDePrueba({ publicada: false }));
    expect((html.querySelector('.boton-me-gusta') as HTMLButtonElement).disabled).toBe(true);
    expect(html.textContent).toContain('No publicada');
  });

  it('sin modo autor no ofrece publicar ni editar', async () => {
    const html = await crear(recetaDePrueba());
    expect(html.querySelector('[aria-label^="Editar"]')).toBeNull();
  });

  it('en modo autor publica y emite el nuevo estado', async () => {
    const html = await crear(recetaDePrueba({ publicada: false }), true);
    expect(html.querySelector('[aria-label^="Editar"]')?.getAttribute('href')).toBe(
      '/recetas/1/editar',
    );
    (html.querySelector('[aria-label^="Publicar"]') as HTMLButtonElement).click();

    const peticion = servidor.expectOne('/api/recetas/1/publicada');
    expect(peticion.request.body).toEqual({ publicada: true });
    peticion.flush(detalleDePrueba({ publicada: true, esAutor: true }));

    expect(emitidas).toEqual([expect.objectContaining({ id: 1, publicada: true })]);
  });

  it('en modo autor elimina la receta tras confirmarlo y lo avisa', async () => {
    const html = await crear(recetaDePrueba(), true);
    const eliminadas: RecetaResumen[] = [];
    fixture.componentInstance.eliminada.subscribe((r) => eliminadas.push(r));
    const modal = aceptarConfirmacion();

    (html.querySelector('[aria-label="Eliminar Tortilla de patata"]') as HTMLButtonElement).click();
    expect(modal.instancia.opciones?.mensaje).toContain('«Tortilla de patata»');
    modal.aceptar();
    const peticion = servidor.expectOne('/api/recetas/1');
    expect(peticion.request.method).toBe('DELETE');
    peticion.flush(null, { status: 204, statusText: 'No Content' });
    await fixture.whenStable();

    expect(eliminadas).toEqual([expect.objectContaining({ id: 1 })]);
    expect(TestBed.inject(Notificaciones).avisos()[0].texto).toBe(
      '«Tortilla de patata» se ha eliminado.',
    );
  });

  it('una receta oculta por la administración lo indica', async () => {
    const html = await crear(recetaDePrueba({ oculta: true }), true);
    expect(html.querySelector('.badge.text-bg-danger')?.textContent).toContain('Oculta');
    expect((html.querySelector('.boton-me-gusta') as HTMLButtonElement).disabled).toBe(true);
  });

  it('una receta eliminada (histórico) no enlaza ni admite «me gusta»', async () => {
    const html = await crear(recetaDePrueba({ eliminada: true, publicada: false, imagen: null }));
    expect(html.querySelector('a')).toBeNull();
    expect(html.querySelector('.boton-me-gusta')).toBeNull();
    expect(html.textContent).toContain('Eliminada');
    expect(html.textContent).toContain('Tortilla de patata');
    expect(html.textContent).toContain('ya no se puede abrir');
  });

  it('si su autor se dio de baja, no muestra el autor', async () => {
    const html = await crear(recetaDePrueba({ eliminada: true, publicada: false, autor: null }));
    expect(html.textContent).not.toContain('por ');
  });
});
