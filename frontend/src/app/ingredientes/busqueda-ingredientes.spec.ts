import { Observable, Subject, of, throwError } from 'rxjs';
import { GRUPO_CARNE, ingredienteDePrueba } from '../../testing/datos';
import { Ingrediente } from '../core/modelos';
import { busquedaIngredientes, nombreIngrediente } from './busqueda-ingredientes';
import { IngredientesApi } from './ingredientes-api';

describe('busquedaIngredientes', () => {
  const patata = ingredienteDePrueba();
  const ternera = ingredienteDePrueba({ id: 30, nombre: 'Ternera', grupo: GRUPO_CARNE });
  let sugerencias: ReturnType<typeof vi.fn<(q: string) => Observable<Ingrediente[]>>>;
  let texto$: Subject<string>;
  let recibidas: Ingrediente[][];

  function buscar(excluidos?: () => number[]) {
    const api = { sugerencias } as unknown as IngredientesApi;
    busquedaIngredientes(api, excluidos)(texto$).subscribe((lista) => recibidas.push(lista));
  }

  beforeEach(() => {
    vi.useFakeTimers();
    sugerencias = vi.fn(() => of([patata, ternera]));
    texto$ = new Subject<string>();
    recibidas = [];
  });

  afterEach(() => vi.useRealTimers());

  it('pide sugerencias cuando se deja de escribir', () => {
    buscar();
    texto$.next('p');
    texto$.next('pa');
    vi.advanceTimersByTime(249);
    expect(sugerencias).not.toHaveBeenCalled();

    vi.advanceTimersByTime(1);
    expect(sugerencias).toHaveBeenCalledExactlyOnceWith('pa');
    expect(recibidas).toEqual([[patata, ternera]]);
  });

  it('quita los espacios y no repite la petición si el texto no cambia', () => {
    buscar();
    texto$.next(' patata ');
    vi.advanceTimersByTime(250);
    texto$.next('patata');
    vi.advanceTimersByTime(250);

    expect(sugerencias).toHaveBeenCalledExactlyOnceWith('patata');
  });

  it('sin texto no llama al servidor y deja la lista vacía', () => {
    buscar();
    texto$.next('   ');
    vi.advanceTimersByTime(250);

    expect(sugerencias).not.toHaveBeenCalled();
    expect(recibidas).toEqual([[]]);
  });

  it('no sugiere los ingredientes ya elegidos', () => {
    buscar(() => [patata.id]);
    texto$.next('a');
    vi.advanceTimersByTime(250);

    expect(recibidas).toEqual([[ternera]]);
  });

  it('un error del servidor es una lista vacía y se puede seguir buscando', () => {
    sugerencias.mockReturnValueOnce(throwError(() => new Error('sin red')));
    buscar();
    texto$.next('pa');
    vi.advanceTimersByTime(250);
    texto$.next('pat');
    vi.advanceTimersByTime(250);

    expect(recibidas).toEqual([[], [patata, ternera]]);
  });

  it('el campo muestra el nombre del ingrediente elegido o el texto escrito', () => {
    expect(nombreIngrediente(patata)).toBe(patata.nombre);
    expect(nombreIngrediente('tom')).toBe('tom');
    expect(nombreIngrediente(null)).toBe('');
  });
});
