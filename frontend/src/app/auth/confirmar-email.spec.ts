import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { loginDePrueba } from '../../testing/jwt';
import { Sesion } from '../core/sesion';
import { ConfirmarEmail } from './confirmar-email';

describe('ConfirmarEmail', () => {
  let fixture: ComponentFixture<ConfirmarEmail>;
  let servidor: HttpTestingController;
  let sesion: Sesion;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    servidor = TestBed.inject(HttpTestingController);
    sesion = TestBed.inject(Sesion);
  });

  afterEach(() => servidor.verify());

  async function abrir(token?: string) {
    fixture = TestBed.createComponent(ConfirmarEmail);
    if (token !== undefined) {
      fixture.componentRef.setInput('token', token);
    }
    await fixture.whenStable();
  }

  function html(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  it('confirma con el token del enlace, también sin sesión', async () => {
    await abrir(' tok-1 ');
    expect(html().textContent).toContain('Confirmando el email');
    const peticion = servidor.expectOne('/api/auth/email/confirmar');
    expect(peticion.request.body).toEqual({ token: 'tok-1' });
    peticion.flush({ usuario: 'lucia', email: 'lucia@nuevo.es' });
    await fixture.whenStable();

    expect(html().querySelector('[role=status]')?.textContent).toContain('lucia@nuevo.es');
    expect(html().querySelector('a[href="/login"]')).not.toBeNull();
  });

  it('con la sesión de esa cuenta deja de mostrar el aviso', async () => {
    const respuesta = loginDePrueba();
    sesion.iniciar({ ...respuesta, usuario: { ...respuesta.usuario, emailVerificado: false } });

    await abrir('tok-1');
    servidor
      .expectOne('/api/auth/email/confirmar')
      .flush({ usuario: 'lucia', email: 'lucia@toca.local' });
    await fixture.whenStable();

    expect(sesion.usuario()?.emailVerificado).toBe(true);
    expect(html().querySelector('a[href="/"]')).not.toBeNull();
  });

  it('con la sesión de otra cuenta no la toca', async () => {
    const respuesta = loginDePrueba();
    sesion.iniciar({ ...respuesta, usuario: { ...respuesta.usuario, emailVerificado: false } });

    await abrir('tok-1');
    servidor
      .expectOne('/api/auth/email/confirmar')
      .flush({ usuario: 'marcos', email: 'marcos@toca.local' });
    await fixture.whenStable();

    expect(sesion.usuario()?.emailVerificado).toBe(false);
  });

  it('un enlace que no vale lo explica y lleva al perfil para pedir otro', async () => {
    sesion.iniciar(loginDePrueba());
    await abrir('viejo');
    servidor.expectOne('/api/auth/email/confirmar').flush(
      {
        codigo: 'TOKEN_INVALIDO',
        mensaje: 'El enlace no es válido o ha caducado. Pide otro desde «Mi perfil».',
        errores: {},
      },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();

    expect(html().querySelector('[role=alert]')?.textContent).toContain('ha caducado');
    expect(html().querySelector('a[href="/mi-perfil"]')).not.toBeNull();
  });

  it('sin token no pregunta al servidor', async () => {
    await abrir();
    servidor.expectNone('/api/auth/email/confirmar');
    expect(html().querySelector('[role=alert]')?.textContent).toContain('Falta el token');
  });

  it('si falla el servidor lo dice', async () => {
    await abrir('tok-1');
    servidor
      .expectOne('/api/auth/email/confirmar')
      .flush(null, { status: 503, statusText: 'Service Unavailable' });
    await fixture.whenStable();

    expect(html().querySelector('[role=alert]')?.textContent).toContain(
      'No se ha podido confirmar el email',
    );
  });
});
