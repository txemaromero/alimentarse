import {
  Observable,
  catchError,
  debounceTime,
  distinctUntilChanged,
  map,
  of,
  switchMap,
} from 'rxjs';
import { Ingrediente } from '../core/modelos';
import { IngredientesApi } from './ingredientes-api';

/**
 * Función para `[ngbTypeahead]`: sugerencias de ingredientes del servidor a partir de 1 carácter, sin los ya
 * elegidos. Los errores se tratan como «sin sugerencias» para no romper la escritura.
 */
export function busquedaIngredientes(
  api: IngredientesApi,
  excluidos: () => number[] = () => [],
): (texto$: Observable<string>) => Observable<Ingrediente[]> {
  return (texto$) =>
    texto$.pipe(
      debounceTime(250),
      map((texto) => (typeof texto === 'string' ? texto.trim() : '')),
      distinctUntilChanged(),
      switchMap((texto) =>
        texto.length < 1
          ? of([])
          : api.sugerencias(texto).pipe(
              map((lista) => lista.filter((i) => !excluidos().includes(i.id))),
              catchError(() => of([])),
            ),
      ),
    );
}

export const nombreIngrediente = (valor: Ingrediente | string | null): string =>
  typeof valor === 'string' ? valor : (valor?.nombre ?? '');
