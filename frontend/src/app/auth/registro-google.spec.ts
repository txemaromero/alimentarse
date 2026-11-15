import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { loginDePrueba } from '../../testing/jwt';
import { AccesoGoogle } from '../core/acceso-google';
import { DatosRegistroGoogle } from '../core/modelos';
import { Notificaciones } from '../core/notificaciones';
import { Sesion } from '../core/sesion';
import { RegistroGoogle } from './registro-google';

const DATOS: DatosRegistroGoogle = {
  email: 'nuria@gmail.com',
  nombre: 'Nuria',
  apellido1: 'Vidal',
  apellido2: 'Soto',
};

const CADUCADO = { codigo: 'REGISTRO_GOOGLE_CADUCADO', mensaje: 'Ha caducado.', errores: {} };

describe('RegistroGoogle', () => {
  let fixture: ComponentFixture<RegistroGoogle>;
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

  async function crear(datos: DatosRegistroGoogle = DATOS) {
    fixture = TestBed.createComponent(RegistroGoogle);
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Cargando los datos de Google');
    servidor.expectOne('/api/auth/google/registro').flush(datos);
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

  async function escribir(selector: string, valor: string) {
    const input = elemento<HTMLInputElement>(selector);
    input.value = valor;
    input.dispatchEvent(new Event('input'));
    input.dispatchEvent(new Event('blur'));
    await fixture.whenStable();
  }

  function enviar() {
    elemento<HTMLButtonElement>('button[type=submit]').click();
  }

  it('rellena el nombre y los apellidos de Google y enseña el email, que no se cambia', async () => {
    await crear();

    expect(fixture.nativeElement.textContent).toContain('nuria@gmail.com');
    expect(elemento<HTMLInputElement>('#registro-google-nombre').value).toBe('Nuria');
    expect(elemento<HTMLInputElement>('#registro-google-apellido1').value).toBe('Vidal');
    expect(elemento<HTMLInputElement>('#registro-google-apellido2').value).toBe('Soto');
    expect(elemento('input[type=email]')).toBeNull();
    // Falta el nombre de usuario
    expect(elemento<HTMLButtonElement>('button[type=submit]').disabled).toBe(true);
  });

  it('crea la cuenta, inicia la sesión y vuelve a la página de antes de ir a Google', async () => {
    vi.spyOn(TestBed.inject(AccesoGoogle), 'destino').mockReturnValue('/recetas/3');
    const exito = vi.spyOn(TestBed.inject(Notificaciones), 'exito');
    await crear({ ...DATOS, apellido2: null });
    expect(elemento<HTMLInputElement>('#registro-google-apellido2').value).toBe('');
    await escribir('#registro-google-id', 'nuria');
    await escribir('#registro-google-nombre', ' Nuria ');

    enviar();
    const peticion = servidor.expectOne('/api/auth/google/registro');
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({
      id: 'nuria',
      nombre: 'Nuria',
      apellido1: 'Vidal',
      apellido2: null,
    });
    peticion.flush(loginDePrueba(['USER'], 3600, 'Nuria'));

    expect(TestBed.inject(Sesion).autenticado()).toBe(true);
    expect(exito).toHaveBeenCalledWith('Cuenta creada. ¡Hola, Nuria!');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/recetas/3', { replaceUrl: true });
  });

  it('un nombre de usuario ya usado se marca en su campo', async () => {
    await crear();
    await escribir('#registro-google-id', 'ocupado');

    enviar();
    servidor.expectOne('/api/auth/google/registro').flush(
      {
        codigo: 'CONFLICTO',
        mensaje: 'Hay datos que ya están en uso.',
        errores: { id: 'Ese nombre de usuario ya está en uso.' },
      },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();

    expect(elemento('#registro-google-id').classList).toContain('is-invalid');
    expect(elemento('#registro-google-id-error').textContent).toContain('ya está en uso');
    expect(TestBed.inject(Sesion).autenticado()).toBe(false);
  });

  it('otro error sin campos se muestra arriba', async () => {
    await crear();
    await escribir('#registro-google-id', 'nuria');

    enviar();
    servidor
      .expectOne('/api/auth/google/registro')
      .flush(
        { codigo: 'VALIDACION', mensaje: 'Los datos no son válidos.', errores: {} },
        { status: 400, statusText: 'Bad Request' },
      );
    await fixture.whenStable();

    expect(elemento('[role=alert]').textContent).toContain('Los datos no son válidos.');
  });

  it('si caduca mientras lo rellena, lo dice y permite volver a Google', async () => {
    const acceso = TestBed.inject(AccesoGoogle);
    const entrar = vi.spyOn(acceso, 'entrar').mockImplementation(() => undefined);
    vi.spyOn(acceso, 'destino').mockReturnValue('/recetas/3');
    await crear();
    await escribir('#registro-google-id', 'nuria');

    enviar();
    servidor
      .expectOne('/api/auth/google/registro')
      .flush(CADUCADO, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();

    expect(elemento('[role=alert]').textContent).toContain('ha caducado');
    boton('Volver a entrar con Google').click();
    // Conserva la página a la que volver
    expect(entrar).toHaveBeenCalledWith('/recetas/3');
  });

  it('sin registro pendiente lo dice desde el principio', async () => {
    fixture = TestBed.createComponent(RegistroGoogle);
    servidor
      .expectOne('/api/auth/google/registro')
      .flush(CADUCADO, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();

    expect(elemento('[role=alert]').textContent).toContain('ha caducado o no se ha empezado');
    expect(elemento('form')).toBeNull();
  });

  it('si no se pueden cargar los datos permite reintentarlo', async () => {
    fixture = TestBed.createComponent(RegistroGoogle);
    servidor
      .expectOne('/api/auth/google/registro')
      .flush(null, { status: 503, statusText: 'Service Unavailable' });
    await fixture.whenStable();

    expect(elemento('[role=alert]').textContent).toContain('No se han podido cargar');
    boton('Reintentar').click();
    servidor.expectOne('/api/auth/google/registro').flush(DATOS);
    await fixture.whenStable();

    expect(elemento('form')).not.toBeNull();
  });

  it('con el formulario inválido no envía nada', async () => {
    await crear();
    fixture.componentInstance.registrar();
    servidor.expectNone('/api/auth/google/registro');
    expect(fixture.componentInstance.formulario.controls.id.touched).toBe(true);
  });
});
