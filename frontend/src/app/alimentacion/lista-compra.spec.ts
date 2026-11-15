import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { registerLocaleData } from '@angular/common';
import localeEs from '@angular/common/locales/es';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { GRUPO_HORTALIZAS } from '../../testing/datos';
import { CLAVE_MARCAS_COMPRA } from '../core/datos-sin-conexion';
import { ArticuloCompra, ListaCompra } from '../core/modelos';
import { Notificaciones } from '../core/notificaciones';
import { ListaDeLaCompra } from './lista-compra';

registerLocaleData(localeEs);

/** Mañana a la hora indicada, en hora local. */
function manana(hora = 14): Date {
  const ahora = new Date();
  return new Date(ahora.getFullYear(), ahora.getMonth(), ahora.getDate() + 1, hora);
}

const HUEVOS: ArticuloCompra = {
  ingrediente: 5,
  nombre: 'Huevos',
  total: '7 unidades',
  cantidades: [
    { receta: 2, titulo: 'Bizcocho', cantidad: '3 unidades', veces: 1 },
    { receta: 1, titulo: 'Tortilla de patata', cantidad: '4 unidades', veces: 1 },
  ],
};
const PATATA: ArticuloCompra = {
  ingrediente: 6,
  nombre: 'Patata',
  total: '500 gramos',
  cantidades: [{ receta: 1, titulo: 'Tortilla de patata', cantidad: '500 gramos', veces: 1 }],
};
const SAL: ArticuloCompra = {
  ingrediente: 9,
  nombre: 'Sal',
  total: null,
  cantidades: [
    { receta: 2, titulo: 'Bizcocho', cantidad: '1 pizca', veces: 1 },
    { receta: 1, titulo: 'Tortilla de patata', cantidad: 'al gusto', veces: 1 },
  ],
};

function lista(cambios: Partial<ListaCompra> = {}): ListaCompra {
  return {
    comidas: [
      { id: 7, fecha: manana().toISOString(), receta: 1, titulo: 'Tortilla de patata' },
      { id: 8, fecha: manana(21).toISOString(), receta: 2, titulo: 'Bizcocho' },
    ],
    secciones: [
      {
        grupo: { id: 10, nombre: 'Huevos', racionesSemana: 4 },
        basico: false,
        articulos: [HUEVOS],
      },
      { grupo: GRUPO_HORTALIZAS, basico: false, articulos: [PATATA] },
      {
        grupo: { id: 1, nombre: 'Esenciales', racionesSemana: null },
        basico: true,
        articulos: [SAL],
      },
    ],
    noDisponibles: 0,
    ...cambios,
  };
}

describe('ListaDeLaCompra', () => {
  let fixture: ComponentFixture<ListaDeLaCompra>;
  let servidor: HttpTestingController;

  beforeEach(() => {
    localStorage.removeItem(CLAVE_MARCAS_COMPRA);
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

  afterEach(() => {
    servidor.verify();
    vi.restoreAllMocks();
    localStorage.removeItem(CLAVE_MARCAS_COMPRA);
  });

  const peticion = () => servidor.expectOne((r) => r.url === '/api/plan/compra');

  async function crear(respuesta: ListaCompra = lista(), semana?: string) {
    fixture = TestBed.createComponent(ListaDeLaCompra);
    if (semana !== undefined) {
      fixture.componentRef.setInput('semana', semana);
    }
    await fixture.whenStable();
    peticion().flush(respuesta);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  const casilla = (html: HTMLElement, id: number) =>
    html.querySelector<HTMLInputElement>(`#articulo-${id}`)!;

  it('pide la semana de la URL, de medianoche a medianoche', async () => {
    fixture = TestBed.createComponent(ListaDeLaCompra);
    fixture.componentRef.setInput('semana', '2');
    await fixture.whenStable();
    const ahora = new Date();
    const pedida = peticion();
    expect(pedida.request.params.get('desde')).toBe(
      new Date(ahora.getFullYear(), ahora.getMonth(), ahora.getDate() + 14).toISOString(),
    );
    expect(pedida.request.params.get('hasta')).toBe(
      new Date(
        ahora.getFullYear(),
        ahora.getMonth(),
        ahora.getDate() + 20,
        23,
        59,
        59,
      ).toISOString(),
    );
    pedida.flush(lista());
  });

  it('muestra los ingredientes por grupo, con los básicos señalados', async () => {
    const html = await crear();
    const titulos = [...html.querySelectorAll('h2[id^="seccion-"]')].map((h) =>
      h.textContent!.replace(/\s+/g, ' ').trim(),
    );
    expect(titulos).toEqual(['Huevos', 'Hortalizas', 'Esenciales · suele haber en casa']);
    expect(html.querySelector('label[for="articulo-5"]')!.textContent).toContain(
      'Huevos – 7 unidades',
    );
  });

  it('el desglose por receta solo aparece si aporta algo, y describe la casilla', async () => {
    const html = await crear();
    // Dos recetas: total y desglose
    expect(casilla(html, 5).getAttribute('aria-describedby')).toBe('desglose-5');
    expect(html.querySelector('#desglose-5')!.textContent).toContain('Bizcocho: 3 unidades');
    // Una sola receta: el total basta
    expect(casilla(html, 6).getAttribute('aria-describedby')).toBeNull();
    expect(html.querySelector('#desglose-6')).toBeNull();
    // Sin total: solo el desglose
    expect(html.querySelector('label[for="articulo-9"]')!.textContent!.trim()).toBe('Sal');
    expect(html.querySelector('#desglose-9')!.textContent).toContain(
      'Tortilla de patata: al gusto',
    );
  });

  it('dice cuántas veces se come una receta', async () => {
    const html = await crear(
      lista({
        secciones: [
          {
            grupo: GRUPO_HORTALIZAS,
            basico: false,
            articulos: [
              {
                ...PATATA,
                total: '1.000 gramos',
                cantidades: [{ ...PATATA.cantidades[0], veces: 2 }],
              },
            ],
          },
        ],
      }),
    );
    expect(html.querySelector('#desglose-6')!.textContent!.replace(/\s+/g, ' ')).toContain(
      '500 gramos (2 veces)',
    );
  });

  it('lista las comidas de las que salen los ingredientes', async () => {
    const html = await crear();
    const comidas = [...html.querySelectorAll('section[aria-labelledby="titulo-comidas"] li')];
    expect(comidas.length).toBe(2);
    expect(comidas[0].textContent).toContain('comida:');
    expect(comidas[1].textContent).toContain('cena:');
    expect(comidas[0].querySelector('a')!.getAttribute('href')).toBe('/recetas/1');
  });

  it('recuerda lo marcado al volver a la pantalla', async () => {
    let html = await crear();
    expect(html.textContent).toContain('0 de 3 ingredientes marcados');

    casilla(html, 5).click();
    await fixture.whenStable();
    expect(html.querySelector('li.marcado')).not.toBeNull();
    expect(html.textContent).toContain('1 de 3 ingredientes marcados');

    fixture.destroy();
    html = await crear();
    expect(casilla(html, 5).checked).toBe(true);
    expect(casilla(html, 6).checked).toBe(false);

    casilla(html, 5).click();
    await fixture.whenStable();
    expect(html.querySelector('li.marcado')).toBeNull();
  });

  it('desmarca todo y lo avisa', async () => {
    const html = await crear();
    casilla(html, 5).click();
    casilla(html, 9).click();
    await fixture.whenStable();

    [...html.querySelectorAll<HTMLButtonElement>('button')]
      .find((b) => b.textContent!.includes('Desmarcar todo'))!
      .click();
    await fixture.whenStable();

    expect(casilla(html, 5).checked).toBe(false);
    expect(casilla(html, 9).checked).toBe(false);
    expect(TestBed.inject(Notificaciones).avisos().at(-1)?.texto).toContain('Lista desmarcada');
  });

  it('se imprime o se guarda en PDF con el diálogo del navegador', async () => {
    const imprimir = vi.spyOn(window, 'print').mockImplementation(() => undefined);
    const html = await crear();
    [...html.querySelectorAll<HTMLButtonElement>('button')]
      .find((b) => b.textContent!.includes('Imprimir o guardar en PDF'))!
      .click();
    expect(imprimir).toHaveBeenCalled();
  });

  it('sin comidas invita a planificar y no ofrece imprimir', async () => {
    const html = await crear({ comidas: [], secciones: [], noDisponibles: 0 });
    expect(html.textContent).toContain('No hay comidas planificadas para estos días.');
    expect(html.textContent).not.toContain('Imprimir');
    const planificar = [...html.querySelectorAll('a')].find((a) =>
      a.textContent!.includes('Planificar comidas'),
    );
    expect(planificar?.getAttribute('href')).toBe('/planificador');
  });

  it('avisa de las comidas cuya receta ya no está disponible', async () => {
    let html = await crear(lista({ noDisponibles: 1 }));
    expect(html.querySelector('.alert-warning')!.textContent).toContain(
      'Una comida planificada no entra',
    );
    fixture.destroy();

    html = await crear(lista({ noDisponibles: 2 }));
    expect(html.querySelector('.alert-warning')!.textContent).toContain(
      '2 comidas planificadas no entran',
    );
  });

  it('vuelve a la misma semana del planificador', async () => {
    let html = await crear(lista(), '3');
    const volver = () =>
      [...html.querySelectorAll('a')].find((a) => a.textContent!.includes('Volver'))!;
    expect(volver().getAttribute('href')).toBe('/planificador?semana=3');
    fixture.destroy();

    html = await crear(lista());
    expect(volver().getAttribute('href')).toBe('/planificador');
  });

  it('si falla la carga deja reintentar', async () => {
    fixture = TestBed.createComponent(ListaDeLaCompra);
    await fixture.whenStable();
    peticion().flush(null, { status: 500, statusText: 'Error' });
    await fixture.whenStable();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('.alert-danger')?.textContent).toContain('No se ha podido cargar');

    (html.querySelector('.alert-danger button') as HTMLButtonElement).click();
    peticion().flush(lista());
    await fixture.whenStable();
    expect(html.querySelector('.alert-danger')).toBeNull();
    expect(html.querySelectorAll('input[type="checkbox"]').length).toBe(3);
  });
});
