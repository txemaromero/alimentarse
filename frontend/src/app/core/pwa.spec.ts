import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { SwUpdate, UnrecoverableStateEvent, VersionEvent } from '@angular/service-worker';
import { Subject } from 'rxjs';
import { ID_AVISO_SIN_CONEXION, RequiereConexion } from '../compartido/requiere-conexion';
import { AvisoConexion } from '../layout/aviso-conexion';
import { AvisoVersion } from '../layout/aviso-version';
import { Actualizaciones, RECARGAR_PAGINA } from './actualizaciones';
import { Conexion } from './conexion';
import { CLAVE_MARCAS_COMPRA, borrarDatosGuardados } from './datos-sin-conexion';
import { Notificaciones } from './notificaciones';

/** Simula que el navegador pierde o recupera la conexión. */
function cambiarConexion(enLinea: boolean): void {
  vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(enLinea);
  window.dispatchEvent(new Event(enLinea ? 'online' : 'offline'));
}

describe('PWA', () => {
  afterEach(() => vi.restoreAllMocks());

  describe('Conexion', () => {
    it('sigue los eventos online y offline del navegador', () => {
      const conexion = TestBed.inject(Conexion);
      expect(conexion.enLinea()).toBe(true);

      cambiarConexion(false);
      expect(conexion.enLinea()).toBe(false);

      cambiarConexion(true);
      expect(conexion.enLinea()).toBe(true);
    });
  });

  describe('RequiereConexion', () => {
    @Component({
      imports: [RequiereConexion],
      template: `
        <button id="guardar" [appRequiereConexion]="invalido()">Guardar</button>
        <button id="like" appRequiereConexion>Me gusta</button>
      `,
    })
    class Anfitrion {
      readonly invalido = signal(false);
    }

    it('deshabilita los botones sin conexión y los enlaza con el aviso', async () => {
      const fixture = TestBed.createComponent(Anfitrion);
      await fixture.whenStable();
      const html = fixture.nativeElement as HTMLElement;
      const guardar = html.querySelector('#guardar') as HTMLButtonElement;
      const like = html.querySelector('#like') as HTMLButtonElement;
      expect(guardar.disabled).toBe(false);
      expect(like.disabled).toBe(false);
      expect(like.hasAttribute('aria-describedby')).toBe(false);

      cambiarConexion(false);
      await fixture.whenStable();

      expect(guardar.disabled).toBe(true);
      expect(like.disabled).toBe(true);
      expect(like.getAttribute('aria-describedby')).toBe(ID_AVISO_SIN_CONEXION);
    });

    it('con conexión, respeta la condición propia del botón', async () => {
      const fixture = TestBed.createComponent(Anfitrion);
      await fixture.whenStable();
      const guardar = (fixture.nativeElement as HTMLElement).querySelector(
        '#guardar',
      ) as HTMLButtonElement;

      fixture.componentInstance.invalido.set(true);
      await fixture.whenStable();
      expect(guardar.disabled).toBe(true);
      expect(guardar.hasAttribute('aria-describedby')).toBe(false);
    });
  });

  describe('AvisoConexion', () => {
    it('muestra el banner sin conexión y avisa al recuperarla', async () => {
      const fixture = TestBed.createComponent(AvisoConexion);
      await fixture.whenStable();
      const html = fixture.nativeElement as HTMLElement;
      expect(html.querySelector(`#${ID_AVISO_SIN_CONEXION}`)).toBeNull();

      cambiarConexion(false);
      await fixture.whenStable();
      expect(html.querySelector(`#${ID_AVISO_SIN_CONEXION}`)?.textContent).toContain(
        'Sin conexión: mostrando datos guardados.',
      );
      expect(html.querySelector('[role=status]')).toBeTruthy();

      cambiarConexion(true);
      await fixture.whenStable();
      expect(html.querySelector(`#${ID_AVISO_SIN_CONEXION}`)).toBeNull();
      expect(TestBed.inject(Notificaciones).avisos()[0].texto).toBe('Vuelves a tener conexión.');
    });
  });

  describe('Actualizaciones', () => {
    let versiones: Subject<VersionEvent>;
    let irrecuperable: Subject<UnrecoverableStateEvent>;
    let recargar: ReturnType<typeof vi.fn>;

    beforeEach(() => {
      versiones = new Subject();
      irrecuperable = new Subject();
      recargar = vi.fn();
      TestBed.configureTestingModule({
        providers: [
          {
            provide: SwUpdate,
            useValue: {
              isEnabled: true,
              versionUpdates: versiones,
              unrecoverable: irrecuperable,
              checkForUpdate: vi.fn().mockResolvedValue(false),
            },
          },
          { provide: RECARGAR_PAGINA, useValue: recargar },
        ],
      });
    });

    function versionLista(): VersionEvent {
      return {
        type: 'VERSION_READY',
        currentVersion: { hash: 'a' },
        latestVersion: { hash: 'b' },
      };
    }

    it('ofrece recargar cuando hay una versión nueva lista', async () => {
      const fixture = TestBed.createComponent(AvisoVersion);
      await fixture.whenStable();
      const html = fixture.nativeElement as HTMLElement;
      expect(html.querySelector('.aviso')).toBeNull();

      versiones.next({ type: 'VERSION_DETECTED', version: { hash: 'b' } });
      await fixture.whenStable();
      expect(html.querySelector('.aviso')).toBeNull();

      versiones.next(versionLista());
      await fixture.whenStable();
      expect(html.querySelector('.aviso')?.textContent).toContain('Hay una versión nueva de Toca.');

      const botones = Array.from(html.querySelectorAll('button'));
      botones.find((b) => b.textContent?.includes('Recargar'))!.click();
      expect(recargar).toHaveBeenCalled();
    });

    it('«Más tarde» oculta el aviso', async () => {
      const fixture = TestBed.createComponent(AvisoVersion);
      await fixture.whenStable();
      versiones.next(versionLista());
      await fixture.whenStable();
      const html = fixture.nativeElement as HTMLElement;

      Array.from(html.querySelectorAll('button'))
        .find((b) => b.textContent?.includes('Más tarde'))!
        .click();
      await fixture.whenStable();

      expect(html.querySelector('.aviso')).toBeNull();
      expect(recargar).not.toHaveBeenCalled();
    });

    it('si el service worker no puede seguir, pide recargar', async () => {
      const fixture = TestBed.createComponent(AvisoVersion);
      await fixture.whenStable();
      irrecuperable.next({ type: 'UNRECOVERABLE_STATE', reason: 'hash' });
      await fixture.whenStable();
      expect((fixture.nativeElement as HTMLElement).textContent).toContain('necesita recargarse');
    });

    it('sin service worker no hace nada', () => {
      TestBed.resetTestingModule();
      const actualizaciones = TestBed.inject(Actualizaciones);
      actualizaciones.iniciar();
      expect(actualizaciones.nuevaVersion()).toBe(false);
    });
  });

  describe('borrarDatosGuardados', () => {
    function cacheFalsa(entradas: string[]) {
      const claves = [...entradas];
      return {
        claves,
        keys: vi.fn(async () => claves.map((url) => new Request(`http://localhost${url}`))),
        delete: vi.fn(async (peticion: Request) => {
          claves.splice(
            claves.indexOf(new URL(peticion.url).pathname + new URL(peticion.url).search),
            1,
          );
          return true;
        }),
      };
    }

    it('vacía solo las cachés de datos de la API del service worker', async () => {
      const datos = cacheFalsa(['/api/recetas?page=0', '/api/alimentacion/resumen']);
      const imagenes = cacheFalsa(['/api/imagenes/a.jpg']);
      const app = cacheFalsa(['/main.js']);
      const cachesPorNombre: Record<string, ReturnType<typeof cacheFalsa>> = {
        'ngsw:/:1:data:api-lecturas:cache': datos,
        'ngsw:/:1:data:imagenes:cache': imagenes,
        'ngsw:/:db:1:data:api-lecturas:meta': cacheFalsa(['/meta']),
        'ngsw:/:aaa:assets:app:cache': app,
        'otra-cache': cacheFalsa(['/x']),
      };
      const almacen = {
        keys: vi.fn(async () => Object.keys(cachesPorNombre)),
        open: vi.fn(async (nombre: string) => cachesPorNombre[nombre]),
      } as unknown as CacheStorage;

      await borrarDatosGuardados(almacen);

      expect(datos.claves).toEqual([]);
      expect(imagenes.claves).toEqual([]);
      expect(app.claves).toEqual(['/main.js']);
      expect(cachesPorNombre['ngsw:/:db:1:data:api-lecturas:meta'].claves).toEqual(['/meta']);
      expect(cachesPorNombre['otra-cache'].claves).toEqual(['/x']);
    });

    it('sin Cache Storage no falla', async () => {
      await expect(borrarDatosGuardados(undefined)).resolves.toBeUndefined();
    });

    it('borra también lo marcado en la lista de la compra (D-076)', async () => {
      localStorage.setItem(CLAVE_MARCAS_COMPRA, JSON.stringify({ 5: Date.now() }));
      await borrarDatosGuardados(undefined);
      expect(localStorage.getItem(CLAVE_MARCAS_COMPRA)).toBeNull();
    });

    it('sin almacenamiento local tampoco falla', async () => {
      vi.spyOn(Storage.prototype, 'removeItem').mockImplementation(() => {
        throw new Error('Sin acceso');
      });
      await expect(borrarDatosGuardados(undefined)).resolves.toBeUndefined();
    });
  });
});
