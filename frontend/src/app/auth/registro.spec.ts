import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Notificaciones } from '../core/notificaciones';
import { Registro } from './registro';

describe('Registro', () => {
  let fixture: ComponentFixture<Registro>;
  let componente: Registro;
  let servidor: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [Registro],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    servidor = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(Registro);
    componente = fixture.componentInstance;
    await fixture.whenStable();
  });

  afterEach(() => servidor.verify());

  function rellenar(cambios: Record<string, string> = {}) {
    componente.formulario.setValue({
      id: 'nuevo.usuario',
      nombre: 'Ana',
      apellido1: 'Pérez',
      apellido2: '',
      email: 'ana@ejemplo.es',
      password: 'demo1234',
      confirmacion: 'demo1234',
      ...cambios,
    });
  }

  it('valida el nombre de usuario como el backend', () => {
    const id = componente.formulario.controls.id;
    for (const valido of ['ana', 'ana.perez_2', 'a-b']) {
      id.setValue(valido);
      expect(id.valid, valido).toBe(true);
    }
    for (const invalido of ['ab', 'josé', 'con espacio', 'x'.repeat(31)]) {
      id.setValue(invalido);
      expect(id.invalid, invalido).toBe(true);
    }
  });

  it('la contraseña debe caber en los 72 bytes de bcrypt, como en el backend (D-090)', () => {
    const password = componente.formulario.controls.password;
    password.setValue('ñ'.repeat(36));
    expect(password.valid).toBe(true);
    password.setValue('ñ'.repeat(40));
    expect(password.hasError('bcrypt')).toBe(true);
  });

  it('exige que las contraseñas coincidan, también al cambiar la primera', () => {
    rellenar({ confirmacion: 'otra-cosa' });
    expect(componente.formulario.controls.confirmacion.hasError('noCoinciden')).toBe(true);

    componente.formulario.controls.password.setValue('otra-cosa');
    expect(componente.formulario.controls.confirmacion.valid).toBe(true);
  });

  it('envía el registro sin la confirmación y lleva al login', () => {
    rellenar({ nombre: '  Ana ' });
    componente.registrar();

    const peticion = servidor.expectOne('/api/auth/registro');
    expect(peticion.request.body).toEqual({
      id: 'nuevo.usuario',
      nombre: 'Ana',
      apellido1: 'Pérez',
      apellido2: null,
      email: 'ana@ejemplo.es',
      password: 'demo1234',
    });
    peticion.flush({ id: 'nuevo.usuario', nombre: 'Ana' }, { status: 201, statusText: 'Created' });

    expect(router.navigate).toHaveBeenCalledWith(['/login']);
    expect(TestBed.inject(Notificaciones).avisos()[0].tipo).toBe('exito');
  });

  it('coloca junto a cada campo los conflictos que devuelve la API', async () => {
    rellenar();
    componente.registrar();
    servidor.expectOne('/api/auth/registro').flush(
      {
        codigo: 'CONFLICTO',
        mensaje: 'Revisa los datos',
        errores: {
          id: 'Ese nombre de usuario ya existe',
          email: 'Ese email ya está registrado',
        },
      },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();

    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('#registro-id-error')?.textContent).toContain(
      'Ese nombre de usuario ya existe',
    );
    expect(html.querySelector('#registro-email-error')?.textContent).toContain(
      'Ese email ya está registrado',
    );
    expect(html.querySelector('.alert-danger')).toBeNull();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('no envía nada si el formulario no es válido', () => {
    rellenar({ email: 'no-es-un-email' });
    componente.registrar();
    servidor.expectNone('/api/auth/registro');
    expect(componente.formulario.controls.email.touched).toBe(true);
  });
});
