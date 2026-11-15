import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { aceptarConfirmacion } from '../../testing/confirmacion';
import { detalleDePrueba, paginaDe, recetaDePrueba } from '../../testing/datos';
import { RecetaResumen } from '../core/modelos';
import { MisRecetas } from './mis-recetas';

describe('MisRecetas', () => {
  let fixture: ComponentFixture<MisRecetas>;
  let servidor: HttpTestingController;
  const borrador = recetaDePrueba({ id: 2, titulo: 'Bizcocho', publicada: false, likes: 0 });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    servidor = TestBed.inject(HttpTestingController);
  });

  afterEach(() => servidor.verify());

  async function crear(recetas: RecetaResumen[] = [recetaDePrueba(), borrador]) {
    fixture = TestBed.createComponent(MisRecetas);
    await fixture.whenStable();
    servidor.expectOne('/api/tipos-receta').flush([{ id: 1, nombre: 'Plato' }]);
    servidor.expectOne('/api/recetas/mias?page=0&size=9').flush(paginaDe(recetas));
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  function boton(html: HTMLElement, nombre: string): HTMLButtonElement {
    return html.querySelector<HTMLButtonElement>(`button[aria-label="${nombre}"]`)!;
  }

  it('lista mis recetas, publicadas o no, con sus acciones de autor', async () => {
    const html = await crear();

    expect(html.querySelector('h1')?.textContent).toContain('Mis recetas');
    const tarjetas = html.querySelectorAll('app-tarjeta-receta');
    expect(tarjetas).toHaveLength(2);
    expect(tarjetas[0].textContent).toContain('Publicada');
    expect(tarjetas[1].textContent).toContain('No publicada');
    expect(boton(html, 'Publicar Bizcocho')).toBeTruthy();
    expect(html.querySelector('a[aria-label="Editar Bizcocho"]')?.getAttribute('href')).toBe(
      '/recetas/2/editar',
    );
    expect(html.querySelector('a[href="/recetas/nueva"]')?.textContent).toContain('Nueva receta');
  });

  it('al publicar una receta, su tarjeta cambia sin recargar la lista', async () => {
    const html = await crear();

    boton(html, 'Publicar Bizcocho').click();
    const peticion = servidor.expectOne('/api/recetas/2/publicada');
    expect(peticion.request.body).toEqual({ publicada: true });
    peticion.flush(detalleDePrueba({ id: 2, titulo: 'Bizcocho', publicada: true, esAutor: true }));
    await fixture.whenStable();

    expect(html.querySelectorAll('app-tarjeta-receta')[1].textContent).not.toContain(
      'No publicada',
    );
    expect(boton(html, 'Dejar de publicar Bizcocho')).toBeTruthy();
  });

  it('al eliminar una receta, la lista se vuelve a pedir', async () => {
    const html = await crear();
    const modal = aceptarConfirmacion();

    boton(html, 'Eliminar Bizcocho').click();
    expect(modal.instancia.opciones?.mensaje).toContain('«Bizcocho»');
    modal.aceptar();
    servidor
      .expectOne((p) => p.method === 'DELETE' && p.url === '/api/recetas/2')
      .flush(null, { status: 204, statusText: 'No Content' });
    await fixture.whenStable();

    servidor.expectOne('/api/recetas/mias?page=0&size=9').flush(paginaDe([recetaDePrueba()]));
    await fixture.whenStable();
    expect(html.querySelectorAll('app-tarjeta-receta')).toHaveLength(1);
  });

  it('sin recetas invita a crear la primera', async () => {
    const html = await crear([]);

    expect(html.textContent).toContain('Todavía no has creado ninguna receta.');
    expect(html.querySelector('a.btn-outline-primary')?.textContent).toContain(
      'Crear mi primera receta',
    );
  });
});
