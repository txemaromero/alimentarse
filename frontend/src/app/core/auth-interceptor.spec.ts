import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { loginDePrueba } from '../../testing/jwt';
import { authInterceptor } from './auth-interceptor';
import { Notificaciones } from './notificaciones';
import { Sesion } from './sesion';

describe('authInterceptor', () => {
  let http: HttpClient;
  let servidor: HttpTestingController;
  let sesion: Sesion;
  let router: Router;
  let notificaciones: Notificaciones;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    servidor = TestBed.inject(HttpTestingController);
    sesion = TestBed.inject(Sesion);
    router = TestBed.inject(Router);
    notificaciones = TestBed.inject(Notificaciones);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => servidor.verify());

  it('no envía el token: la sesión va en una cookie (D-070)', () => {
    sesion.iniciar(loginDePrueba());
    http.get('/api/recetas').subscribe();
    expect(servidor.expectOne('/api/recetas').request.headers.has('Authorization')).toBe(false);
  });

  it('ante un 401 cierra la sesión y lleva al login con returnUrl', () => {
    sesion.iniciar(loginDePrueba());
    vi.spyOn(router, 'url', 'get').mockReturnValue('/recetas/5');
    let recibido: unknown;
    http.get('/api/recetas/5').subscribe({ error: (e) => (recibido = e) });

    servidor
      .expectOne('/api/recetas/5')
      .flush({ codigo: 'NO_AUTENTICADO' }, { status: 401, statusText: 'Unauthorized' });

    expect(recibido).toBeTruthy();
    expect(sesion.autenticado()).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { returnUrl: '/recetas/5' },
    });
    expect(notificaciones.avisos()[0].tipo).toBe('aviso');
  });

  it('si la cuenta se ha bloqueado, cierra la sesión y lo dice', () => {
    sesion.iniciar(loginDePrueba());
    vi.spyOn(router, 'url', 'get').mockReturnValue('/recetas');
    http.get('/api/recetas').subscribe({ error: () => undefined });

    servidor.expectOne('/api/recetas').flush(
      {
        codigo: 'CUENTA_BLOQUEADA',
        mensaje: 'Tu cuenta está bloqueada por la administración.',
        errores: {},
      },
      { status: 401, statusText: 'Unauthorized' },
    );

    expect(sesion.autenticado()).toBe(false);
    expect(notificaciones.avisos()[0].texto).toBe(
      'Tu cuenta está bloqueada por la administración.',
    );
  });

  it('un 401 del login no cierra la sesión ni redirige (lo trata la pantalla)', () => {
    http.post('/api/auth/login', {}).subscribe({ error: () => undefined });
    servidor
      .expectOne('/api/auth/login')
      .flush({ codigo: 'CREDENCIALES_INVALIDAS' }, { status: 401, statusText: 'Unauthorized' });
    expect(router.navigate).not.toHaveBeenCalled();
    expect(notificaciones.avisos()).toEqual([]);
  });

  it('avisa de los errores 5xx con el mensaje de la API', () => {
    http.get('/api/recetas').subscribe({ error: () => undefined });
    servidor
      .expectOne('/api/recetas')
      .flush(
        { codigo: 'ERROR_INTERNO', mensaje: 'Error del servidor', errores: {} },
        { status: 500, statusText: 'Server Error' },
      );
    expect(notificaciones.avisos()).toEqual([
      expect.objectContaining({ tipo: 'error', texto: 'Error del servidor' }),
    ]);
  });

  it('avisa cuando no hay conexión con el servidor', () => {
    http.get('/api/recetas').subscribe({ error: () => undefined });
    servidor.expectOne('/api/recetas').error(new ProgressEvent('error'), { status: 0 });
    expect(notificaciones.avisos()[0].texto).toContain('No se puede conectar');
  });

  it('deja los errores 4xx a la pantalla, sin aviso', () => {
    http.get('/api/recetas/9').subscribe({ error: () => undefined });
    servidor
      .expectOne('/api/recetas/9')
      .flush({ codigo: 'NO_ENCONTRADO' }, { status: 404, statusText: 'Not Found' });
    expect(notificaciones.avisos()).toEqual([]);
  });
});
