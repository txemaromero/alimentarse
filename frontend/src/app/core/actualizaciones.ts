import { DestroyRef, Injectable, InjectionToken, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { SwUpdate } from '@angular/service-worker';
import { filter, interval } from 'rxjs';

/** Cómo se recarga la página (se sustituye en los tests). */
export const RECARGAR_PAGINA = new InjectionToken<() => void>('RECARGAR_PAGINA', {
  providedIn: 'root',
  factory: () => () => location.reload(),
});

/** Cada cuánto se busca una versión nueva, además de al arrancar. */
const INTERVALO_COMPROBACION_MS = 6 * 60 * 60 * 1000;

/**
 * Versiones nuevas de la aplicación (service worker). Cuando hay una lista para usar, `nuevaVersion` pasa a true y la
 * interfaz ofrece recargar. Sin service worker (desarrollo y tests) no hace nada.
 */
@Injectable({ providedIn: 'root' })
export class Actualizaciones {
  private readonly sw = inject(SwUpdate, { optional: true });
  private readonly recargarPagina = inject(RECARGAR_PAGINA);
  private readonly destroyRef = inject(DestroyRef);
  private iniciado = false;

  readonly nuevaVersion = signal(false);
  /** El service worker no puede seguir sirviendo la versión actual: hay que recargar. */
  readonly irrecuperable = signal(false);

  iniciar(): void {
    const sw = this.sw;
    if (this.iniciado || !sw?.isEnabled) {
      return;
    }
    this.iniciado = true;
    sw.versionUpdates
      .pipe(
        filter((evento) => evento.type === 'VERSION_READY'),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.nuevaVersion.set(true));
    sw.unrecoverable
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.irrecuperable.set(true));
    interval(INTERVALO_COMPROBACION_MS)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => sw.checkForUpdate().catch(() => false));
  }

  recargar(): void {
    this.recargarPagina();
  }

  posponer(): void {
    this.nuevaVersion.set(false);
  }
}
