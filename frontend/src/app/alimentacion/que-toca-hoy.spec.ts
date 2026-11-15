import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { GRUPO_CARNE, paginaDe, recetaDePrueba } from '../../testing/datos';
import { ResumenAlimentacion } from '../core/modelos';
import { QueTocaHoy } from './que-toca-hoy';

const DULCES = { id: 15, nombre: 'Dulces', racionesSemana: 1 };

function resumen(grupos7Dias: ResumenAlimentacion['grupos7Dias']): ResumenAlimentacion {
  return { ultimas: [], masConsumidas: [], ingredientesMasUsados: [], grupos7Dias };
}

describe('QueTocaHoy', () => {
  let fixture: ComponentFixture<QueTocaHoy>;
  let servidor: HttpTestingController;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    servidor = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(QueTocaHoy);
    await fixture.whenStable();
    servidor.expectOne('/api/tipos-receta').flush([]);
  });

  afterEach(() => servidor.verify());

  it('pide las recomendadas y explica los grupos ya completados', async () => {
    servidor
      .expectOne('/api/recetas/recomendadas?page=0&size=9')
      .flush(paginaDe([{ ...recetaDePrueba(), grupos: [] }]));
    servidor.expectOne('/api/alimentacion/resumen').flush(
      resumen([
        { grupo: DULCES, consumidas: 1, limite: 1, porcentaje: 100, color: 'ROJO' },
        { grupo: GRUPO_CARNE, consumidas: 1, limite: 3, porcentaje: 33, color: 'VERDE' },
      ]),
    );
    await fixture.whenStable();

    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('.alert-info')?.textContent).toContain('últimos 7 días');
    expect(html.querySelector('.alert-info')?.textContent).toContain(
      'Grupos completados esta semana: Dulces.',
    );
    expect(html.querySelectorAll('app-tarjeta-receta').length).toBe(1);
  });

  it('explica por qué se recomienda cada receta', async () => {
    const carne = { grupo: GRUPO_CARNE, consumidas: 2, limite: 3, porcentaje: 67, color: 'VERDE' };
    servidor
      .expectOne('/api/recetas/recomendadas?page=0&size=9')
      .flush(paginaDe([{ ...recetaDePrueba(), grupos: [carne] }]));
    servidor.expectOne('/api/alimentacion/resumen').flush(resumen([]));
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).querySelector('.motivo')?.textContent).toContain(
      'Te quedan raciones de Carne (2/3).',
    );
  });

  it('al dar «me gusta» la tarjeta conserva su motivo', async () => {
    const carne = { grupo: GRUPO_CARNE, consumidas: 1, limite: 3, porcentaje: 33, color: 'VERDE' };
    servidor
      .expectOne('/api/recetas/recomendadas?page=0&size=9')
      .flush(paginaDe([{ ...recetaDePrueba(), grupos: [carne] }]));
    servidor.expectOne('/api/alimentacion/resumen').flush(resumen([]));
    await fixture.whenStable();

    const html = fixture.nativeElement as HTMLElement;
    (html.querySelector('.boton-me-gusta') as HTMLButtonElement).click();
    servidor.expectOne('/api/recetas/1/like').flush({ likes: 4, meGusta: true });
    await fixture.whenStable();

    expect(html.querySelector('.boton-me-gusta')?.textContent).toContain('4');
    expect(html.querySelector('.motivo')?.textContent).toContain('Carne (1/3)');
  });

  it('sin grupos completados lo dice', async () => {
    servidor.expectOne('/api/recetas/recomendadas?page=0&size=9').flush(paginaDe([]));
    servidor.expectOne('/api/alimentacion/resumen').flush(resumen([]));
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'Esta semana aún no has completado ningún grupo.',
    );
  });

  it('si no queda ninguna receta muestra un mensaje amable', async () => {
    servidor.expectOne('/api/recetas/recomendadas?page=0&size=9').flush(paginaDe([]));
    servidor.expectOne('/api/alimentacion/resumen').flush(resumen([]));
    await fixture.whenStable();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('Ahora mismo no hay recetas que encajen con tu semana.');
    expect(html.querySelector('a[href="/recetas"]')).toBeTruthy();
  });

  it('aplica los filtros a las recomendadas', async () => {
    servidor.expectOne('/api/recetas/recomendadas?page=0&size=9').flush(paginaDe([]));
    servidor.expectOne('/api/alimentacion/resumen').flush(resumen([]));

    fixture.componentInstance.listado.filtrar({ dificultad: 'Fácil' });
    servidor
      .expectOne('/api/recetas/recomendadas?page=0&size=9&dificultad=F%C3%A1cil')
      .flush(paginaDe([]));
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'Ninguna receta recomendada coincide con los filtros.',
    );
  });
});
