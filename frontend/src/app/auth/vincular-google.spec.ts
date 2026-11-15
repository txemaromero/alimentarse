import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { loginDePrueba } from '../../testing/jwt';
import { AccesoGoogle } from '../core/acceso-google';
import { Notificaciones } from '../core/notificaciones';
import { Sesion } from '../core/sesion';
import { VincularGoogle } from './vincular-google';

const CADUCADA = { codigo: 'VINCULACION_GOOGLE_CADUCADA', mensaje: 'Ha caducado.', errores: {} };

describe('VincularGoogle', () => {
  let fixture: ComponentFixture<VincularGoogle>;
  let servidor: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    servidor = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  });

  afterEach(() => servidor.verify());

  async function crear() {
    fixture = TestBed.createComponent(VincularGoogle);
    await fixture.whenStable();
    servidor.expectOne('/api/auth/google/vincular').flush({ email: 'ana@gmail.com' });
    await fixture.whenStable();
  }

  function elemento<T extends HTMLElement>(selector: string): T {
    return fixture.nativeElement.querySelector(selector) as T;
  }

  function boton(texto: string): HTMLButtonElement {
    return [...fixture.nativeElement.querySelectorAll('button')].find((b: HTMLButtonElement) =>
      b.textContent?.includes(texto),
    ) as HTMLButtonElement;
  }

  async function enviar(password: string) {
    const campo = elemento<HTMLInputElement>('#vincular-password');
    campo.value = password;
    campo.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    elemento<HTMLButtonElement>('button[type=submit]').click();
  }

  it('enseña el email de la cuenta y pide su contraseña', async () => {
    await crear();

    expect(fixture.nativeElement.textContent).toContain('ana@gmail.com');
    expect(elemento('a[href="/olvido-password"]')).not.toBeNull();
    expect(elemento<HTMLButtonElement>('button[type=submit]').disabled).toBe(true);
  });

  it('con la contraseña vincula, inicia la sesión y vuelve a la página de antes', async () => {
    vi.spyOn(TestBed.inject(AccesoGoogle), 'destino').mockReturnValue('/planificador');
    const exito = vi.spyOn(TestBed.inject(Notificaciones), 'exito');
    await crear();

    await enviar('clave-de-ana');
    const peticion = servidor.expectOne('/api/auth/google/vincular');
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({ password: 'clave-de-ana' });
    peticion.flush(loginDePrueba(['USER'], 3600, 'Ana'));

    expect(TestBed.inject(Sesion).autenticado()).toBe(true);
    expect(exito).toHaveBeenCalledWith('Cuenta vinculada con Google. ¡Hola, Ana!');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/planificador', { replaceUrl: true });
  });

  it('una contraseña incorrecta se marca en su campo', async () => {
    await crear();
    await enviar('otra');
    servidor.expectOne('/api/auth/google/vincular').flush(
      {
        codigo: 'PASSWORD_INCORRECTA',
        mensaje: 'La contraseña no es correcta.',
        errores: { password: 'La contraseña no es correcta' },
      },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();

    expect(elemento('#vincular-password').classList).toContain('is-invalid');
    expect(elemento('#vincular-password-error').textContent).toContain('no es correcta');
    expect(TestBed.inject(Sesion).autenticado()).toBe(false);
  });

  it('si la cuenta está bloqueada lo dice arriba', async () => {
    await crear();
    await enviar('clave-de-ana');
    servidor
      .expectOne('/api/auth/google/vincular')
      .flush(
        { codigo: 'CUENTA_BLOQUEADA', mensaje: 'Tu cuenta está bloqueada.', errores: {} },
        { status: 403, statusText: 'Forbidden' },
      );
    await fixture.whenStable();

    expect(elemento('[role=alert]').textContent).toContain('Tu cuenta está bloqueada.');
  });

  it('si caduca mientras la escribe, permite volver a Google', async () => {
    const acceso = TestBed.inject(AccesoGoogle);
    const entrar = vi.spyOn(acceso, 'entrar').mockImplementation(() => undefined);
    vi.spyOn(acceso, 'destino').mockReturnValue('/recetas/3');
    await crear();
    await enviar('clave-de-ana');
    servidor
      .expectOne('/api/auth/google/vincular')
      .flush(CADUCADA, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();

    expect(elemento('[role=alert]').textContent).toContain('ha caducado');
    boton('Volver a entrar con Google').click();
    expect(entrar).toHaveBeenCalledWith('/recetas/3');
  });

  it('sin vinculación pendiente lo dice desde el principio', async () => {
    fixture = TestBed.createComponent(VincularGoogle);
    servidor
      .expectOne('/api/auth/google/vincular')
      .flush(CADUCADA, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();

    expect(elemento('[role=alert]').textContent).toContain('ha caducado o no se ha empezado');
    expect(elemento('form')).toBeNull();
  });

  it('si no se puede cargar permite reintentarlo', async () => {
    fixture = TestBed.createComponent(VincularGoogle);
    servidor
      .expectOne('/api/auth/google/vincular')
      .flush(null, { status: 503, statusText: 'Service Unavailable' });
    await fixture.whenStable();

    boton('Reintentar').click();
    servidor.expectOne('/api/auth/google/vincular').flush({ email: 'ana@gmail.com' });
    await fixture.whenStable();

    expect(elemento('form')).not.toBeNull();
  });

  it('sin contraseña no envía nada', async () => {
    await crear();
    fixture.componentInstance.vincular();
    servidor.expectNone('/api/auth/google/vincular');
    expect(fixture.componentInstance.formulario.controls.password.touched).toBe(true);
  });
});
