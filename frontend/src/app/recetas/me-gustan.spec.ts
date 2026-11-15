import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { paginaDe, recetaDePrueba } from '../../testing/datos';
import { MeGustan } from './me-gustan';

describe('MeGustan', () => {
  let fixture: ComponentFixture<MeGustan>;
  let servidor: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    servidor = TestBed.inject(HttpTestingController);
  });

  afterEach(() => servidor.verify());

  async function crear(recetas = [recetaDePrueba({ meGusta: true })]) {
    fixture = TestBed.createComponent(MeGustan);
    await fixture.whenStable();
    servidor.expectOne('/api/tipos-receta').flush([{ id: 1, nombre: 'Plato' }]);
    servidor.expectOne('/api/recetas/me-gustan?page=0&size=9').flush(paginaDe(recetas));
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('lista las recetas que me gustan', async () => {
    const html = await crear();
    expect(html.querySelector('h1')?.textContent).toContain('Recetas que me gustan');
    expect(html.querySelectorAll('app-tarjeta-receta').length).toBe(1);
  });

  it('al quitar el «me gusta» la receta sale de la lista', async () => {
    const html = await crear();
    (html.querySelector('.boton-me-gusta') as HTMLButtonElement).click();
    servidor.expectOne('/api/recetas/1/like').flush({ likes: 2, meGusta: false });
    await fixture.whenStable();

    servidor.expectOne('/api/recetas/me-gustan?page=0&size=9').flush(paginaDe([]));
    await fixture.whenStable();
    expect(html.querySelectorAll('app-tarjeta-receta').length).toBe(0);
  });

  it('sin «me gusta» explica cómo añadirlas', async () => {
    const html = await crear([]);
    expect(html.textContent).toContain('Todavía no le has dado «me gusta» a ninguna receta.');
    expect(html.querySelector('a[href="/recetas"]')).toBeTruthy();
  });
});
