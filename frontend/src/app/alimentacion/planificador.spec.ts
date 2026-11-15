import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { LOCALE_ID } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeEs from '@angular/common/locales/es';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { GRUPO_CARNE, paginaDe, recetaDePrueba } from '../../testing/datos';
import { ComidaPlanificada, IndicadorGrupo, Plan } from '../core/modelos';
import { Notificaciones } from '../core/notificaciones';
import { ModalPlanificar, abrirModalPlanificar } from './modal-planificar';
import { Planificador } from './planificador';

registerLocaleData(localeEs);

const CARNE_4: IndicadorGrupo = {
  grupo: GRUPO_CARNE,
  consumidas: 4,
  limite: 3,
  porcentaje: 100,
  color: 'ROJO',
};

/** Mañana a las 14:00 locales (un hueco que nunca ha pasado). */
function manana(hora = 14): Date {
  const ahora = new Date();
  return new Date(ahora.getFullYear(), ahora.getMonth(), ahora.getDate() + 1, hora);
}

function comida(cambios: Partial<ComidaPlanificada> = {}): ComidaPlanificada {
  return {
    id: 7,
    fecha: manana().toISOString(),
    receta: recetaDePrueba({ titulo: 'Pollo asado' }),
    disponible: true,
    excesos: [],
    ...cambios,
  };
}

describe('Planificador', () => {
  let fixture: ComponentFixture<Planificador>;
  let servidor: HttpTestingController;

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

  const peticionPlan = () => servidor.expectOne((r) => r.url === '/api/plan');

  async function crear(plan: Plan = { comidas: [], indicadores: [] }) {
    fixture = TestBed.createComponent(Planificador);
    await fixture.whenStable();
    peticionPlan().flush(plan);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('pide los 7 días que empiezan hoy, de medianoche a medianoche', async () => {
    fixture = TestBed.createComponent(Planificador);
    await fixture.whenStable();
    const peticion = peticionPlan();
    const ahora = new Date();
    expect(peticion.request.params.get('desde')).toBe(
      new Date(ahora.getFullYear(), ahora.getMonth(), ahora.getDate()).toISOString(),
    );
    expect(peticion.request.params.get('hasta')).toBe(
      new Date(
        ahora.getFullYear(),
        ahora.getMonth(),
        ahora.getDate() + 6,
        23,
        59,
        59,
      ).toISOString(),
    );
    peticion.flush({ comidas: [], indicadores: [] });
  });

  it('muestra 7 días con su comida y su cena', async () => {
    const html = await crear();
    expect(html.querySelectorAll('article.card').length).toBe(7);
    expect(html.querySelectorAll('.momento').length).toBe(14);
    expect(html.textContent).toContain('Añade alguna comida');
  });

  it('coloca cada comida en su hueco', async () => {
    const html = await crear({ comidas: [comida()], indicadores: [] });
    const manianaCard = html.querySelectorAll('article.card')[1];
    const huecos = manianaCard.querySelectorAll('.list-group-item');
    expect(huecos[0].textContent).toContain('Pollo asado');
    expect(huecos[1].textContent).not.toContain('Pollo asado');
  });

  it('avisa de lo que superaría su límite', async () => {
    const html = await crear({ comidas: [comida({ excesos: [CARNE_4] })], indicadores: [CARNE_4] });
    expect(html.querySelector('.exceso')?.textContent?.replace(/\s+/g, ' ')).toContain(
      'Te pasarías de: Carne (4/3).',
    );
    expect(html.querySelectorAll('app-indicador-grupo').length).toBe(1);
  });

  it('una receta que ya no está disponible no se puede comer ni abrir', async () => {
    const html = await crear({ comidas: [comida({ disponible: false })], indicadores: [] });
    expect(html.textContent).toContain('Esta receta ya no está disponible.');
    expect(html.querySelector('a[href="/recetas/1"]')).toBeNull();
    expect(html.querySelector('[aria-label="Ya la he comido: Pollo asado"]')).toBeNull();
    expect(html.querySelector('[aria-label="Quitar Pollo asado del planificador"]')).toBeTruthy();
  });

  it('marcar como comida la registra y recarga el plan', async () => {
    const html = await crear({ comidas: [comida()], indicadores: [] });

    (
      html.querySelector('[aria-label="Ya la he comido: Pollo asado"]') as HTMLButtonElement
    ).click();
    servidor.expectOne('/api/plan/7/consumir').flush({ id: 3 });
    peticionPlan().flush({ comidas: [], indicadores: [] });
    await fixture.whenStable();

    expect(TestBed.inject(Notificaciones).avisos()[0].texto).toBe(
      '«Pollo asado» añadida a tus consumos.',
    );
    expect(html.textContent).not.toContain('Pollo asado');
    // El botón pulsado ya no existe: el foco pasa a su hueco, no al principio de la página (D-075)
    expect(document.activeElement?.id).toMatch(/^hueco-.*-comida$/);
  });

  it('quitar la borra y recarga el plan', async () => {
    const html = await crear({ comidas: [comida()], indicadores: [] });

    (
      html.querySelector('[aria-label="Quitar Pollo asado del planificador"]') as HTMLButtonElement
    ).click();
    servidor.expectOne('/api/plan/7').flush(null);
    peticionPlan().flush({ comidas: [], indicadores: [] });
    await fixture.whenStable();

    expect(html.textContent).not.toContain('Pollo asado');
    expect(TestBed.inject(Notificaciones).avisos()[0].texto).toBe(
      '«Pollo asado» quitada del planificador.',
    );
    const hueco = document.activeElement as HTMLElement;
    expect(hueco.tagName).toBe('LI');
    expect(hueco.textContent).toContain('Añadir');
  });

  it('un 404 al operar avisa y refresca', async () => {
    const html = await crear({ comidas: [comida()], indicadores: [] });

    (
      html.querySelector('[aria-label="Ya la he comido: Pollo asado"]') as HTMLButtonElement
    ).click();
    servidor
      .expectOne('/api/plan/7/consumir')
      .flush(
        { codigo: 'NO_ENCONTRADO', mensaje: 'Esa comida no está.', errores: {} },
        { status: 404, statusText: 'Not Found' },
      );
    peticionPlan().flush({ comidas: [], indicadores: [] });
    await fixture.whenStable();

    expect(TestBed.inject(Notificaciones).avisos()[0].texto).toBe('Esa comida no está.');
  });

  it('avanza de semana y no deja retroceder desde la actual', async () => {
    const html = await crear();
    const botones = html.querySelectorAll<HTMLButtonElement>('section > div > .btn');
    expect(botones[0].disabled).toBe(true);

    botones[1].click();
    const peticion = peticionPlan();
    const ahora = new Date();
    expect(peticion.request.params.get('desde')).toBe(
      new Date(ahora.getFullYear(), ahora.getMonth(), ahora.getDate() + 7).toISOString(),
    );
    peticion.flush({ comidas: [], indicadores: [] });
    await fixture.whenStable();

    expect(botones[0].disabled).toBe(false);
  });

  it('abre en la semana que dice la URL', async () => {
    fixture = TestBed.createComponent(Planificador);
    fixture.componentRef.setInput('semana', '2');
    await fixture.whenStable();
    const ahora = new Date();
    const peticion = peticionPlan();
    expect(peticion.request.params.get('desde')).toBe(
      new Date(ahora.getFullYear(), ahora.getMonth(), ahora.getDate() + 14).toISOString(),
    );
    peticion.flush({ comidas: [], indicadores: [] });
  });

  it('guarda la semana en la URL y enlaza con su lista de la compra (D-076)', async () => {
    const html = await crear();
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const compra = () =>
      [...html.querySelectorAll('a')].find((a) => a.textContent!.includes('Lista de la compra'))!;
    expect(compra().getAttribute('href')).toBe('/planificador/compra');

    html.querySelectorAll<HTMLButtonElement>('section > div > .btn')[1].click();
    peticionPlan().flush({ comidas: [], indicadores: [] });
    await fixture.whenStable();

    expect(navegar).toHaveBeenCalledWith(
      [],
      expect.objectContaining({ queryParams: { semana: 1 }, replaceUrl: true }),
    );
    expect(compra().getAttribute('href')).toBe('/planificador/compra?semana=1');

    html.querySelectorAll<HTMLButtonElement>('section > div > .btn')[0].click();
    peticionPlan().flush({ comidas: [], indicadores: [] });
    await fixture.whenStable();
    // La semana actual no se escribe en la URL
    expect(navegar).toHaveBeenLastCalledWith(
      [],
      expect.objectContaining({ queryParams: { semana: null } }),
    );
  });

  it('si falla la carga deja reintentar', async () => {
    fixture = TestBed.createComponent(Planificador);
    await fixture.whenStable();
    peticionPlan().flush(null, { status: 500, statusText: 'Error' });
    await fixture.whenStable();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('.alert-danger')?.textContent).toContain('No se ha podido cargar');

    (html.querySelector('.alert-danger button') as HTMLButtonElement).click();
    peticionPlan().flush({ comidas: [], indicadores: [] });
    await fixture.whenStable();
    expect(html.querySelector('.alert-danger')).toBeNull();
  });

  it('al añadir abre el modal con el hueco elegido y recarga', async () => {
    const html = await crear();
    const resultado = Promise.resolve(comida());
    const abrir = vi.spyOn(TestBed.inject(NgbModal), 'open').mockReturnValue({
      componentInstance: {},
      result: resultado,
    } as never);

    (html.querySelectorAll('article.card')[1].querySelector('button') as HTMLButtonElement).click();
    expect(abrir).toHaveBeenCalledOnce();
    await resultado;
    await fixture.whenStable();

    peticionPlan().flush({ comidas: [comida()], indicadores: [] });
    await fixture.whenStable();
    expect(html.textContent).toContain('Pollo asado');
    expect(TestBed.inject(Notificaciones).avisos()[0].texto).toMatch(
      /^«Pollo asado» planificada: comida del /,
    );
    expect(document.activeElement?.textContent).toContain('Pollo asado');
  });
});

describe('ModalPlanificar', () => {
  let fixture: ComponentFixture<ModalPlanificar>;
  let servidor: HttpTestingController;
  const modal = { close: vi.fn(), dismiss: vi.fn() };
  const fecha = new Date(2030, 0, 2, 14);

  beforeEach(async () => {
    modal.close.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NgbActiveModal, useValue: modal },
      ],
    });
    servidor = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ModalPlanificar);
    fixture.componentInstance.titulo = 'Comida del lunes';
    fixture.componentInstance.fecha = fecha;
    await fixture.whenStable();
  });

  afterEach(() => servidor.verify());

  const carne: IndicadorGrupo = { ...CARNE_4, consumidas: 1, porcentaje: 33, color: 'VERDE' };

  it('ofrece las recomendadas y añade la elegida', async () => {
    servidor
      .expectOne('/api/recetas/recomendadas?page=0&size=5')
      .flush(paginaDe([{ ...recetaDePrueba({ id: 4, titulo: 'Lentejas' }), grupos: [carne] }]));
    await fixture.whenStable();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('h2')?.textContent).toContain('Comida del lunes');
    expect(html.textContent).toContain('Te quedan raciones de Carne (1/3).');

    (html.querySelector('.list-group-item') as HTMLButtonElement).click();
    const peticion = servidor.expectOne('/api/plan');
    expect(peticion.request.body).toEqual({ receta: 4, fecha: fecha.toISOString() });
    peticion.flush(comida());

    expect(modal.close).toHaveBeenCalledOnce();
  });

  it('sin recomendaciones se puede buscar igual', async () => {
    servidor
      .expectOne('/api/recetas/recomendadas?page=0&size=5')
      .flush(null, { status: 500, statusText: 'Error' });
    await fixture.whenStable();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('.list-group')).toBeNull();
    expect(html.querySelector('#planificar-receta')).toBeTruthy();
  });

  it('al elegir en el buscador añade esa receta', () => {
    servidor.expectOne('/api/recetas/recomendadas?page=0&size=5').flush(paginaDe([]));

    fixture.componentInstance.elegirSugerencia({
      item: { id: 9, titulo: 'Crema' },
      preventDefault: () => undefined,
    });

    const peticion = servidor.expectOne('/api/plan');
    expect(peticion.request.body.receta).toBe(9);
    peticion.flush(comida());
  });

  it('muestra el error si el hueco se ha ocupado', async () => {
    servidor.expectOne('/api/recetas/recomendadas?page=0&size=5').flush(paginaDe([]));

    fixture.componentInstance.elegir(4);
    servidor
      .expectOne('/api/plan')
      .flush(
        { codigo: 'HUECO_OCUPADO', mensaje: 'Ya tienes una receta.', errores: {} },
        { status: 409, statusText: 'Conflict' },
      );
    await fixture.whenStable();

    expect(
      (fixture.nativeElement as HTMLElement).querySelector('.alert-danger')?.textContent,
    ).toContain('Ya tienes una receta.');
    expect(modal.close).not.toHaveBeenCalled();
  });

  it('abrirModalPlanificar pasa el hueco y emite la comida', async () => {
    servidor.expectOne('/api/recetas/recomendadas?page=0&size=5').flush(paginaDe([]));
    const instancia: Partial<ModalPlanificar> = {};
    const nueva = comida();
    vi.spyOn(TestBed.inject(NgbModal), 'open').mockReturnValue({
      componentInstance: instancia,
      result: Promise.resolve(nueva),
    } as never);

    const emitida = await new Promise((resolver) =>
      abrirModalPlanificar(TestBed.inject(NgbModal), { titulo: 'Cena', fecha }).subscribe(resolver),
    );

    expect(instancia.titulo).toBe('Cena');
    expect(instancia.fecha).toBe(fecha);
    expect(emitida).toBe(nueva);
  });
});
