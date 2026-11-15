import { DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, Subject, catchError, map, of, switchMap, tap } from 'rxjs';
import { EstadoListados } from '../core/estado-listados';
import { Pagina } from '../core/modelos';

type Respuesta<T> = { ok: true; pagina: Pagina<T> } | { ok: false };

/**
 * Estado de un listado paginado con filtros: filtro aplicado, página cargada, carga y fallo. Cancela la petición
 * anterior si llega otra (switchMap) y recuerda filtro y página en `EstadoListados` con la `clave` dada. Debe crearse
 * en un contexto de inyección (inicializador de un campo del componente).
 */
export function crearListado<T, F extends object>(
  clave: string,
  fuente: (filtro: F, pagina: number) => Observable<Pagina<T>>,
) {
  const estado = inject(EstadoListados);
  const guardado = estado.leer<F>(clave);

  const filtro = signal<F>(guardado?.filtro ?? ({} as F));
  const resultado = signal<Pagina<T> | null>(null);
  const cargando = signal(false);
  const fallo = signal(false);
  const peticiones = new Subject<{ filtro: F; pagina: number }>();

  peticiones
    .pipe(
      tap(() => {
        cargando.set(true);
        fallo.set(false);
      }),
      switchMap(({ filtro: f, pagina }) =>
        fuente(f, pagina).pipe(
          map((p): Respuesta<T> => ({ ok: true, pagina: p })),
          catchError(() => of<Respuesta<T>>({ ok: false })),
        ),
      ),
      takeUntilDestroyed(inject(DestroyRef)),
    )
    .subscribe((respuesta) => {
      if (!respuesta.ok) {
        cargando.set(false);
        fallo.set(true);
        return;
      }
      const p = respuesta.pagina;
      // Página fuera de rango (por ejemplo, tras borrar elementos): se pide la última
      if (p.contenido.length === 0 && p.pagina > 0 && p.totalPaginas > 0) {
        peticiones.next({ filtro: filtro(), pagina: p.totalPaginas - 1 });
        return;
      }
      cargando.set(false);
      resultado.set(p);
      estado.guardar(clave, filtro(), p.pagina);
    });

  return {
    filtro: filtro.asReadonly(),
    resultado: resultado.asReadonly(),
    cargando: cargando.asReadonly(),
    fallo: fallo.asReadonly(),
    /** Carga la página guardada (o la primera). */
    iniciar(): void {
      peticiones.next({ filtro: filtro(), pagina: guardado?.pagina ?? 0 });
    },
    cargar(pagina: number): void {
      peticiones.next({ filtro: filtro(), pagina });
    },
    /** Vuelve a pedir la página actual. */
    recargar(): void {
      peticiones.next({ filtro: filtro(), pagina: resultado()?.pagina ?? 0 });
    },
    filtrar(nuevo: F): void {
      filtro.set(nuevo);
      peticiones.next({ filtro: nuevo, pagina: 0 });
    },
    /** Sustituye un elemento ya cargado (por ejemplo, tras un «me gusta»). */
    reemplazar(cambiado: T, igual: (a: T, b: T) => boolean): void {
      resultado.update(
        (p) =>
          p && { ...p, contenido: p.contenido.map((e) => (igual(e, cambiado) ? cambiado : e)) },
      );
    },
  };
}

export type Listado<T, F extends object> = ReturnType<typeof crearListado<T, F>>;
