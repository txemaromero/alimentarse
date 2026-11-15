import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { loginDePrueba } from '../../testing/jwt';
import { AccesoGoogle } from '../core/acceso-google';
import { Notificaciones } from '../core/notificaciones';
import { Sesion } from '../core/sesion';
import { LoginGoogle } from './login-google';

describe('LoginGoogle', () => {
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

  it('inicia la sesión de la cookie y vuelve a la página de antes de ir a Google', async () => {
    vi.spyOn(TestBed.inject(AccesoGoogle), 'destino').mockReturnValue('/planificador');
    const exito = vi.spyOn(TestBed.inject(Notificaciones), 'exito');
    const fixture = TestBed.createComponent(LoginGoogle);
    await fixture.whenStable();

    expect(fixture.nativeElement.textContent).toContain('Iniciando la sesión');
    servidor.expectOne('/api/auth/sesion').flush(loginDePrueba());
    await fixture.whenStable();

    expect(TestBed.inject(Sesion).usuario()?.id).toBe('lucia');
    expect(exito).toHaveBeenCalledWith('¡Hola, Lucía!');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/planificador', { replaceUrl: true });
  });

  it('sin página guardada va a la portada', async () => {
    const fixture = TestBed.createComponent(LoginGoogle);
    servidor.expectOne('/api/auth/sesion').flush(loginDePrueba());
    await fixture.whenStable();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/', { replaceUrl: true });
  });

  it('sin sesión lo dice y enlaza con el login', async () => {
    const fixture = TestBed.createComponent(LoginGoogle);
    servidor
      .expectOne('/api/auth/sesion')
      .flush(
        { codigo: 'SIN_SESION', mensaje: 'No hay ninguna sesión iniciada.', errores: {} },
        { status: 401, statusText: 'Unauthorized' },
      );
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('[role=alert]').textContent).toContain(
      'No se ha podido iniciar la sesión con Google',
    );
    expect(fixture.nativeElement.querySelector('a[href="/login"]')).not.toBeNull();
    expect(TestBed.inject(Sesion).autenticado()).toBe(false);
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });
});
