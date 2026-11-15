import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Observable, Subject, of, throwError } from 'rxjs';
import type { Mock } from 'vitest';
import { paginaDe } from '../../testing/datos';
import { EstadoListados } from '../core/estado-listados';
import { Pagina } from '../core/modelos';
import { crearListado } from './listado';
import { Paginacion, textoPaginacion } from './paginacion';

interface Filtro {
  texto?: string;
}

describe('crearListado', () => {
  let fuente: Mock<(f: Filtro, p: number) => Observable<Pagina<string>>>;

  @Component({ template: '' })
  class Anfitrion {
    readonly listado = crearListado<string, Filtro>('prueba', (f, p) => fuente(f, p));
  }

  function crear() {
    return TestBed.createComponent(Anfitrion).componentInstance.listado;
  }

  beforeEach(() => {
    fuente = vi.fn((_f: Filtro, p: number) => of(paginaDe(['a', 'b'], { pagina: p })));
  });

  it('empieza en la primera página sin filtro y la recuerda', () => {
    const listado = crear();
    listado.iniciar();
    expect(fuente).toHaveBeenCalledWith({}, 0);
    expect(listado.resultado()?.contenido).toEqual(['a', 'b']);
    expect(TestBed.inject(EstadoListados).leer('prueba')).toEqual({ filtro: {}, pagina: 0 });
  });

  it('al volver retoma el filtro y la página guardados', () => {
    TestBed.inject(EstadoListados).guardar('prueba', { texto: 'x' }, 3);
    const listado = crear();
    listado.iniciar();
    expect(listado.filtro()).toEqual({ texto: 'x' });
    expect(fuente).toHaveBeenCalledWith({ texto: 'x' }, 3);
  });

  it('filtrar vuelve a la primera página', () => {
    const listado = crear();
    listado.cargar(2);
    listado.filtrar({ texto: 'y' });
    expect(fuente).toHaveBeenLastCalledWith({ texto: 'y' }, 0);
  });

  it('si la página ya no existe, carga la última', () => {
    fuente.mockImplementation((_f: Filtro, p: number) =>
      of(
        p === 5
          ? paginaDe<string>([], { pagina: 5, totalElementos: 20, totalPaginas: 3 })
          : paginaDe(['z'], { pagina: p, totalPaginas: 3 }),
      ),
    );
    const listado = crear();
    listado.cargar(5);
    expect(fuente).toHaveBeenLastCalledWith({}, 2);
    expect(listado.resultado()?.pagina).toBe(2);
  });

  it('marca el fallo y permite recargar', () => {
    fuente.mockReturnValueOnce(throwError(() => new Error('red')));
    const listado = crear();
    listado.iniciar();
    expect(listado.fallo()).toBe(true);
    expect(listado.cargando()).toBe(false);

    listado.recargar();
    expect(listado.fallo()).toBe(false);
    expect(listado.resultado()?.contenido).toEqual(['a', 'b']);
  });

  it('ignora la respuesta de una petición anterior', () => {
    const lenta = new Subject<Pagina<string>>();
    fuente.mockReturnValueOnce(lenta).mockReturnValueOnce(of(paginaDe(['nuevo'])));
    const listado = crear();
    listado.iniciar();
    listado.filtrar({ texto: 'n' });
    lenta.next(paginaDe(['viejo']));
    expect(listado.resultado()?.contenido).toEqual(['nuevo']);
  });

  it('reemplaza un elemento cargado', () => {
    const listado = crear();
    listado.iniciar();
    listado.reemplazar('B', (a, b) => a.toLowerCase() === b.toLowerCase());
    expect(listado.resultado()?.contenido).toEqual(['a', 'B']);
  });
});

describe('Paginacion', () => {
  it('describe el tramo mostrado', () => {
    expect(
      textoPaginacion(paginaDe(['a', 'b', 'c'], { pagina: 2, tamano: 9, totalElementos: 21 })),
    ).toBe('Mostrando del elemento 19 al 21 de 21 totales');
    expect(textoPaginacion(paginaDe([]))).toBe('0 totales');
  });

  it('emite la página elegida empezando en 0', async () => {
    const fixture = TestBed.createComponent(Paginacion);
    fixture.componentRef.setInput(
      'datos',
      paginaDe(new Array(9).fill('x'), { totalElementos: 30, totalPaginas: 4 }),
    );
    const emitidas: number[] = [];
    fixture.componentInstance.pagina.subscribe((p) => emitidas.push(p));
    await fixture.whenStable();

    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('Mostrando del elemento 1 al 9 de 30 totales');
    const enlaces = Array.from(html.querySelectorAll<HTMLAnchorElement>('a.page-link'));
    enlaces.find((a) => a.textContent?.trim() === '3')!.click();
    // NgbPagination emite pageChange de forma asíncrona (EventEmitter(true))
    await new Promise((resolver) => setTimeout(resolver));

    expect(emitidas).toEqual([2]);
  });
});
