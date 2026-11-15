import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { recetaDePrueba } from '../../testing/datos';
import { loginDePrueba } from '../../testing/jwt';
import { Sesion } from '../core/sesion';
import { Inicio } from './inicio';

describe('Inicio', () => {
  let fixture: ComponentFixture<Inicio>;
  let servidor: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    servidor = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    TestBed.inject(Sesion).iniciar(loginDePrueba());
    fixture = TestBed.createComponent(Inicio);
    await fixture.whenStable();
  });

  afterEach(() => servidor.verify());

  it('muestra las mejor valoradas y las últimas', async () => {
    servidor
      .expectOne('/api/recetas/mejor-valoradas?limit=3')
      .flush([
        recetaDePrueba({ id: 1, titulo: 'Tortilla' }),
        recetaDePrueba({ id: 2, titulo: 'Lentejas' }),
      ]);
    servidor
      .expectOne('/api/recetas/ultimas?limit=3')
      .flush([recetaDePrueba({ id: 2, titulo: 'Lentejas' })]);
    await fixture.whenStable();

    const html = fixture.nativeElement as HTMLElement;
    const secciones = html.querySelectorAll('section[aria-labelledby^="titulo-"]');
    expect(secciones[0].querySelector('h2')?.textContent).toContain('Recetas mejor valoradas');
    expect(secciones[0].querySelectorAll('app-tarjeta-receta').length).toBe(2);
    expect(secciones[1].querySelector('h2')?.textContent).toContain('Últimas recetas añadidas');
    expect(secciones[1].querySelectorAll('app-tarjeta-receta').length).toBe(1);
    expect(html.textContent).toContain('Hola, Lucía');
  });

  it('un «me gusta» actualiza la receta en las dos secciones', async () => {
    servidor.expectOne('/api/recetas/mejor-valoradas?limit=3').flush([recetaDePrueba({ id: 2 })]);
    servidor.expectOne('/api/recetas/ultimas?limit=3').flush([recetaDePrueba({ id: 2 })]);

    fixture.componentInstance.actualizar(recetaDePrueba({ id: 2, likes: 9, meGusta: true }));

    expect(fixture.componentInstance.mejorValoradas().recetas![0].likes).toBe(9);
    expect(fixture.componentInstance.ultimas().recetas![0].likes).toBe(9);
  });

  it('elegir una sugerencia abre la receta; buscar texto lleva al listado filtrado', () => {
    servidor.expectOne('/api/recetas/mejor-valoradas?limit=3').flush([]);
    servidor.expectOne('/api/recetas/ultimas?limit=3').flush([]);
    const componente = fixture.componentInstance;

    const evento = { item: { id: 7, titulo: 'Sopa' }, preventDefault: vi.fn() };
    componente.abrir(evento);
    expect(router.navigate).toHaveBeenCalledWith(['/recetas', 7]);

    componente.busqueda.setValue('  sopa ');
    componente.buscar();
    expect(router.navigate).toHaveBeenCalledWith(['/recetas'], { queryParams: { titulo: 'sopa' } });
  });

  it('el buscador se envía con Angular, sin recargar la página', async () => {
    servidor.expectOne('/api/recetas/mejor-valoradas?limit=3').flush([]);
    servidor.expectOne('/api/recetas/ultimas?limit=3').flush([]);
    const formulario = (fixture.nativeElement as HTMLElement).querySelector(
      'form[role=search]',
    ) as HTMLFormElement;
    fixture.componentInstance.busqueda.setValue('lentejas');
    const envio = new Event('submit', { cancelable: true });

    formulario.dispatchEvent(envio);

    expect(envio.defaultPrevented).toBe(true);
    expect(router.navigate).toHaveBeenCalledWith(['/recetas'], {
      queryParams: { titulo: 'lentejas' },
    });
  });

  it('si falla una sección lo indica sin romper la otra', async () => {
    servidor
      .expectOne('/api/recetas/mejor-valoradas?limit=3')
      .flush(null, { status: 500, statusText: 'Error' });
    servidor.expectOne('/api/recetas/ultimas?limit=3').flush([]);
    await fixture.whenStable();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('No se han podido cargar estas recetas.');
    expect(html.textContent).toContain('Todavía no hay recetas publicadas.');
  });
});
