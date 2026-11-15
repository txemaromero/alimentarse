import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ingredienteDePrueba } from '../../testing/datos';
import { FiltroRecetas } from '../core/modelos';
import { FiltrosRecetas, filtrosActivos } from './filtros-recetas';

describe('FiltrosRecetas', () => {
  let fixture: ComponentFixture<FiltrosRecetas>;
  let componente: FiltrosRecetas;
  let servidor: HttpTestingController;
  let emitidos: FiltroRecetas[];

  async function crear(filtro: FiltroRecetas = {}, conAutor = true) {
    fixture = TestBed.createComponent(FiltrosRecetas);
    componente = fixture.componentInstance;
    fixture.componentRef.setInput('filtro', filtro);
    fixture.componentRef.setInput('conAutor', conAutor);
    emitidos = [];
    componente.filtrar.subscribe((f) => emitidos.push(f));
    await fixture.whenStable();
    servidor.expectOne('/api/tipos-receta').flush([
      { id: 1, nombre: 'Plato' },
      { id: 2, nombre: 'Postre' },
    ]);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servidor = TestBed.inject(HttpTestingController);
  });

  it('cuenta los filtros activos', () => {
    expect(filtrosActivos({})).toBe(0);
    expect(filtrosActivos({ titulo: '  ', tipo: 2, ingredientes: [] })).toBe(1);
    expect(
      filtrosActivos({
        titulo: 'a',
        usuario: 'b',
        comensales: 2,
        ingredientes: [ingredienteDePrueba()],
      }),
    ).toBe(4);
  });

  it('empieza plegado si no hay filtros', async () => {
    const html = await crear();
    expect(html.querySelector('form')).toBeNull();
    expect(html.querySelector('.badge')).toBeNull();
  });

  it('empieza desplegado con los filtros aplicados y su número', async () => {
    const html = await crear({ titulo: 'sopa', tipo: 2 });
    expect(html.querySelector('form')).toBeTruthy();
    expect(html.querySelector('.badge')?.textContent).toContain('2 activos');
    expect((html.querySelector('#filtro-titulo') as HTMLInputElement).value).toBe('sopa');
  });

  it('emite solo los filtros con valor, sin espacios sobrantes', async () => {
    await crear({ titulo: 'x' });
    componente.formulario.setValue({
      titulo: '  tortilla ',
      tipo: 1,
      usuario: ' ',
      dificultad: 'Fácil',
      comensales: 2,
      tiempoMax: null,
    });
    componente.aplicar();
    expect(emitidos).toEqual([{ titulo: 'tortilla', tipo: 1, dificultad: 'Fácil', comensales: 2 }]);
  });

  it('no emite si un número no es válido', async () => {
    await crear();
    componente.formulario.controls.comensales.setValue(0);
    componente.aplicar();
    expect(emitidos).toEqual([]);
  });

  it('añade y quita ingredientes como chips', async () => {
    const html = await crear({ titulo: 'x' });
    const patata = ingredienteDePrueba();
    const evento = { item: patata, preventDefault: vi.fn() };
    componente.elegirIngrediente(evento);
    await fixture.whenStable();

    expect(evento.preventDefault).toHaveBeenCalled();
    expect(html.querySelector('.chip')?.textContent).toContain('Patata');
    componente.aplicar();
    expect(emitidos[0].ingredientes).toEqual([patata]);

    (html.querySelector('.chip .btn-close') as HTMLButtonElement).click();
    await fixture.whenStable();
    expect(html.querySelector('.chip')).toBeNull();
  });

  it('Restablecer vacía el formulario y emite un filtro vacío', async () => {
    await crear({ titulo: 'sopa', ingredientes: [ingredienteDePrueba()] });
    componente.restablecer();
    expect(componente.formulario.controls.titulo.value).toBe('');
    expect(componente.ingredientes()).toEqual([]);
    expect(emitidos).toEqual([{}]);
  });

  it('en «Mis recetas» no hay filtro por autor', async () => {
    const html = await crear({ titulo: 'x' }, false);
    expect(html.querySelector('#filtro-usuario')).toBeNull();
    componente.formulario.controls.usuario.setValue('marcos');
    componente.aplicar();
    expect(emitidos[0].usuario).toBeUndefined();
  });
});
