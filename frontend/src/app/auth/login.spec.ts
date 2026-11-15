import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { loginDePrueba } from '../../testing/jwt';
import { AccesoGoogle } from '../core/acceso-google';
import { Sesion } from '../core/sesion';
import { Login } from './login';

describe('Login', () => {
  let fixture: ComponentFixture<Login>;
  let servidor: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    servidor = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  });

  afterEach(() => servidor.verify());

  /** Crea la pantalla, que pregunta si se puede entrar con Google. */
  async function crear(google = false, motivo?: string) {
    fixture = TestBed.createComponent(Login);
    if (motivo) {
      fixture.componentRef.setInput('google', motivo);
    }
    servidor.expectOne('/api/auth/proveedores').flush({ google });
    await fixture.whenStable();
  }

  function elemento<T extends HTMLElement>(selector: string): T {
    return fixture.nativeElement.querySelector(selector) as T;
  }

  async function escribir(selector: string, valor: string) {
    const input = elemento<HTMLInputElement>(selector);
    input.value = valor;
    input.dispatchEvent(new Event('input'));
    input.dispatchEvent(new Event('blur'));
    await fixture.whenStable();
  }

  it('el botón está deshabilitado hasta que el formulario es válido', async () => {
    await crear();
    const boton = elemento<HTMLButtonElement>('button[type=submit]');
    expect(boton.disabled).toBe(true);

    await escribir('#login-usuario', 'lucia');
    await escribir('#login-password', 'demo1234');

    expect(boton.disabled).toBe(false);
  });

  it('muestra el error del campo obligatorio al salir de él vacío', async () => {
    await crear();
    await escribir('#login-usuario', '');
    expect(elemento('#login-usuario-error').textContent).toContain('Este campo es obligatorio');
    expect(elemento('#login-usuario').classList).toContain('is-invalid');
  });

  it('inicia la sesión y vuelve a la página pedida', async () => {
    await crear();
    fixture.componentRef.setInput('returnUrl', '/recetas/3');
    await escribir('#login-usuario', ' lucia ');
    await escribir('#login-password', 'demo1234');

    elemento<HTMLButtonElement>('button[type=submit]').click();
    const peticion = servidor.expectOne('/api/auth/login');
    expect(peticion.request.body).toEqual({ usuario: 'lucia', password: 'demo1234' });
    peticion.flush(loginDePrueba());

    expect(TestBed.inject(Sesion).autenticado()).toBe(true);
    expect(router.navigateByUrl).toHaveBeenCalledWith('/recetas/3');
  });

  it('ignora un returnUrl externo', async () => {
    await crear();
    fixture.componentRef.setInput('returnUrl', '//malicioso.example');
    await escribir('#login-usuario', 'lucia');
    await escribir('#login-password', 'demo1234');

    elemento<HTMLButtonElement>('button[type=submit]').click();
    servidor.expectOne('/api/auth/login').flush(loginDePrueba());

    expect(router.navigateByUrl).toHaveBeenCalledWith('/');
  });

  it('muestra el mensaje de la API si las credenciales no son válidas', async () => {
    await crear();
    await escribir('#login-usuario', 'lucia');
    await escribir('#login-password', 'mala');

    elemento<HTMLButtonElement>('button[type=submit]').click();
    servidor.expectOne('/api/auth/login').flush(
      {
        codigo: 'CREDENCIALES_INVALIDAS',
        mensaje: 'Usuario o contraseña incorrectos',
        errores: {},
      },
      { status: 401, statusText: 'Unauthorized' },
    );
    await fixture.whenStable();

    expect(elemento('[role=alert]').textContent).toContain('Usuario o contraseña incorrectos');
    expect(TestBed.inject(Sesion).autenticado()).toBe(false);
    expect(elemento<HTMLButtonElement>('button[type=submit]').disabled).toBe(false);
  });

  it('muestra u oculta la contraseña', async () => {
    await crear();
    const boton = elemento<HTMLButtonElement>('button[aria-label="Mostrar la contraseña"]');
    expect(elemento<HTMLInputElement>('#login-password').type).toBe('password');

    boton.click();
    await fixture.whenStable();

    expect(elemento<HTMLInputElement>('#login-password').type).toBe('text');
    expect(boton.getAttribute('aria-pressed')).toBe('true');
  });

  describe('con Google', () => {
    afterEach(() => vi.restoreAllMocks());

    it('sin credenciales en el servidor no hay botón', async () => {
      await crear();
      expect(fixture.nativeElement.textContent).not.toContain('Entrar con Google');
    });

    it('si falla la consulta tampoco', async () => {
      fixture = TestBed.createComponent(Login);
      servidor
        .expectOne('/api/auth/proveedores')
        .flush(null, { status: 503, statusText: 'Service Unavailable' });
      await fixture.whenStable();
      expect(fixture.nativeElement.textContent).not.toContain('Entrar con Google');
    });

    it('sin conexión no lo pregunta', async () => {
      vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(false);
      fixture = TestBed.createComponent(Login);
      await fixture.whenStable();
      servidor.expectNone('/api/auth/proveedores');
      expect(fixture.nativeElement.textContent).not.toContain('Entrar con Google');
    });

    it('lleva a Google recordando la página a la que volver', async () => {
      const entrar = vi
        .spyOn(TestBed.inject(AccesoGoogle), 'entrar')
        .mockImplementation(() => undefined);
      await crear(true);
      fixture.componentRef.setInput('returnUrl', '/recetas/3');
      await fixture.whenStable();

      const boton = [...fixture.nativeElement.querySelectorAll('button')].find((b) =>
        b.textContent.includes('Entrar con Google'),
      ) as HTMLButtonElement;
      boton.click();

      expect(entrar).toHaveBeenCalledWith('/recetas/3');
    });

    it.each([
      ['bloqueada', 'Tu cuenta está bloqueada'],
      ['email-no-verificado', 'no tiene el email verificado'],
      ['otra-cuenta', 'vinculada con otra cuenta de Google'],
      ['cancelado', 'Has cancelado el acceso con Google'],
      ['error', 'No se ha podido entrar con Google'],
      ['desconocido', 'No se ha podido entrar con Google'],
    ])('al volver con «%s» lo explica', async (motivo, texto) => {
      await crear(true, motivo);
      expect(elemento('[role=alert]').textContent).toContain(texto);
    });
  });
});
