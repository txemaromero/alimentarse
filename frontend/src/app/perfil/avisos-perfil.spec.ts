import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { loginDePrueba } from '../../testing/jwt';
import { AvisosPush, EstadoAvisos } from '../core/avisos-push';
import { Notificaciones } from '../core/notificaciones';
import { Sesion } from '../core/sesion';
import { AvisosPerfil } from './avisos-perfil';

describe('AvisosPerfil', () => {
  let fixture: ComponentFixture<AvisosPerfil>;
  let avisos: {
    estado: ReturnType<typeof signal<EstadoAvisos>>;
    comprobar: ReturnType<typeof vi.fn>;
    activar: ReturnType<typeof vi.fn>;
    desactivar: ReturnType<typeof vi.fn>;
    probar: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    localStorage.clear();
    avisos = {
      estado: signal<EstadoAvisos>('desactivados'),
      comprobar: vi.fn(async () => undefined),
      activar: vi.fn(async () => avisos.estado.set('activados')),
      desactivar: vi.fn(async () => avisos.estado.set('desactivados')),
      probar: vi.fn(() => of({ enviados: 1 })),
    };
    TestBed.configureTestingModule({ providers: [{ provide: AvisosPush, useValue: avisos }] });
    TestBed.inject(Sesion).iniciar(loginDePrueba());
  });

  async function crear(estado: EstadoAvisos = 'desactivados') {
    avisos.estado.set(estado);
    fixture = TestBed.createComponent(AvisosPerfil);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  const interruptor = (html: HTMLElement) =>
    html.querySelector<HTMLInputElement>('#perfil-avisos-comidas')!;
  const ultimoAviso = () => TestBed.inject(Notificaciones).avisos().at(-1);

  it('pregunta al servidor si hay avisos al abrirse', async () => {
    await crear();
    expect(avisos.comprobar).toHaveBeenCalled();
  });

  it('explica por qué no se pueden activar', async () => {
    let html = await crear('no-disponibles');
    expect(html.textContent).toContain('no están disponibles en este navegador');
    expect(interruptor(html)).toBeNull();
    fixture.destroy();

    html = await crear('bloqueados');
    expect(html.textContent).toContain('Has bloqueado las notificaciones');
    fixture.destroy();

    html = await crear('comprobando');
    expect(html.textContent).toContain('Comprobando');
  });

  it('activa los avisos de este usuario y ofrece una prueba', async () => {
    const html = await crear();
    expect(html.textContent).not.toContain('Enviar un aviso de prueba');

    interruptor(html).click();
    await fixture.whenStable();

    expect(avisos.activar).toHaveBeenCalledWith('lucia');
    expect(ultimoAviso()?.texto).toBe('Avisos activados en este navegador.');
    expect(interruptor(html).checked).toBe(true);

    [...html.querySelectorAll('button')].find((b) => b.textContent!.includes('prueba'))!.click();
    await fixture.whenStable();
    expect(avisos.probar).toHaveBeenCalled();
    expect(ultimoAviso()?.texto).toContain('Aviso de prueba enviado');
  });

  it('los desactiva', async () => {
    const html = await crear('activados');
    interruptor(html).click();
    await fixture.whenStable();
    expect(avisos.desactivar).toHaveBeenCalled();
    expect(ultimoAviso()?.texto).toBe('Avisos desactivados en este navegador.');
  });

  it('si no se pueden activar, el interruptor vuelve atrás y lo avisa', async () => {
    avisos.activar.mockRejectedValueOnce(new Error('Sin servicio'));
    const html = await crear();

    interruptor(html).click();
    await fixture.whenStable();

    expect(interruptor(html).checked).toBe(false);
    expect(ultimoAviso()?.texto).toBe('No se han podido activar los avisos.');
  });

  it('si el servidor no los admite, dice por qué (D-089)', async () => {
    avisos.activar.mockRejectedValueOnce(
      new HttpErrorResponse({
        status: 409,
        error: {
          codigo: 'LIMITE_NAVEGADORES',
          mensaje: 'Ya tienes los avisos activados en 10 navegadores.',
        },
      }),
    );
    const html = await crear();

    interruptor(html).click();
    await fixture.whenStable();

    expect(interruptor(html).checked).toBe(false);
    expect(ultimoAviso()?.texto).toBe('Ya tienes los avisos activados en 10 navegadores.');
  });

  it('si el usuario los bloquea al pedir permiso, no hace falta otro aviso', async () => {
    avisos.activar.mockImplementationOnce(async () => {
      avisos.estado.set('bloqueados');
      throw new Error('Denegado');
    });
    const html = await crear();

    interruptor(html).click();
    await fixture.whenStable();

    expect(TestBed.inject(Notificaciones).avisos()).toEqual([]);
    expect(html.textContent).toContain('Has bloqueado las notificaciones');
  });

  it('si la prueba no llega o el servidor la rechaza, lo dice', async () => {
    avisos.probar.mockReturnValueOnce(of({ enviados: 0 }));
    const html = await crear('activados');
    const prueba = () =>
      [...html.querySelectorAll('button')].find((b) => b.textContent!.includes('prueba'))!;

    prueba().click();
    await fixture.whenStable();
    expect(ultimoAviso()?.texto).toContain('no ha aceptado');

    avisos.probar.mockReturnValueOnce(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 409,
            error: {
              codigo: 'SIN_SUSCRIPCIONES',
              mensaje: 'Activa antes los avisos.',
              errores: {},
            },
          }),
      ),
    );
    prueba().click();
    await fixture.whenStable();
    expect(ultimoAviso()?.texto).toBe('Activa antes los avisos.');
  });
});
