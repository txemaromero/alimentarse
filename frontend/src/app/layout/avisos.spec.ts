import { TestBed } from '@angular/core/testing';
import { Anunciador, REGION_CORTES, RETRASO_ANUNCIO } from '../core/anunciador';
import { Notificaciones } from '../core/notificaciones';
import { Avisos } from './avisos';

describe('Avisos', () => {
  let notificaciones: Notificaciones;

  async function crear() {
    const fixture = TestBed.createComponent(Avisos);
    await fixture.whenStable();
    return fixture;
  }

  beforeEach(() => {
    notificaciones = TestBed.inject(Notificaciones);
  });

  it('muestra cada aviso con su estilo; los toasts no son regiones vivas', async () => {
    const fixture = await crear();
    notificaciones.exito('Receta guardada.');
    notificaciones.error('No se ha podido guardar.');
    await fixture.whenStable();

    const toasts = (fixture.nativeElement as HTMLElement).querySelectorAll('ngb-toast');
    expect(toasts).toHaveLength(2);
    expect(toasts[0].textContent).toContain('Receta guardada.');
    expect(toasts[0].className).toContain('text-bg-success');
    expect(toasts[1].className).toContain('text-bg-danger');
    for (const toast of Array.from(toasts)) {
      expect(toast.getAttribute('role')).toBeNull();
      expect(toast.getAttribute('aria-live')).toBe('off');
    }
  });

  it('anuncia cada aviso una sola vez; los errores, como urgentes', async () => {
    const anunciar = vi.spyOn(TestBed.inject(Anunciador), 'anunciar');
    const fixture = await crear();
    notificaciones.exito('Receta guardada.');
    await fixture.whenStable();
    notificaciones.error('No se ha podido guardar.');
    await fixture.whenStable();
    notificaciones.quitar(notificaciones.avisos()[0].id);
    await fixture.whenStable();

    expect(anunciar.mock.calls).toEqual([
      ['Receta guardada.', false],
      ['No se ha podido guardar.', true],
    ]);
  });

  it('el texto llega a la región viva de <body>, fuera del componente', async () => {
    const fixture = await crear();
    notificaciones.exito('Añadida a tus consumos.');
    await fixture.whenStable();
    await new Promise((r) => setTimeout(r, RETRASO_ANUNCIO + 20));

    const region = document.getElementById(REGION_CORTES)!;
    expect(region.textContent).toBe('Añadida a tus consumos.');
    expect((fixture.nativeElement as HTMLElement).contains(region)).toBe(false);
  });

  it('vuelve a anunciar un aviso igual al anterior, una vez cerrado', async () => {
    const anunciar = vi.spyOn(TestBed.inject(Anunciador), 'anunciar');
    const fixture = await crear();
    notificaciones.exito('Añadida a tus consumos.');
    await fixture.whenStable();
    notificaciones.quitar(notificaciones.avisos()[0].id);
    notificaciones.exito('Añadida a tus consumos.');
    await fixture.whenStable();

    expect(anunciar).toHaveBeenCalledTimes(2);
  });

  it('el aviso amarillo usa el botón de cerrar oscuro, que contrasta con su fondo', async () => {
    const fixture = await crear();
    notificaciones.aviso('Sin conexión.');
    notificaciones.info('Hay una versión nueva.');
    await fixture.whenStable();

    const cerrar = (fixture.nativeElement as HTMLElement).querySelectorAll('.btn-close');
    expect(cerrar[0].classList).not.toContain('btn-close-white');
    expect(cerrar[1].classList).toContain('btn-close-white');
  });

  it('el botón de cerrar quita el aviso', async () => {
    const fixture = await crear();
    notificaciones.info('Hay una versión nueva.');
    await fixture.whenStable();
    const html = fixture.nativeElement as HTMLElement;

    html.querySelector<HTMLButtonElement>('button[aria-label="Cerrar aviso"]')!.click();
    await fixture.whenStable();

    expect(notificaciones.avisos()).toEqual([]);
    expect(html.querySelectorAll('ngb-toast')).toHaveLength(0);
  });
});
