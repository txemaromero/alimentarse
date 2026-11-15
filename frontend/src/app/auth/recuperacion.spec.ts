import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { OlvidoPassword } from './olvido-password';
import { RestablecerPassword } from './restablecer-password';

describe('recuperación de contraseña', () => {
  let servidor: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    servidor = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => servidor.verify());

  describe('OlvidoPassword', () => {
    it('envía el email y muestra el mensaje de la API', async () => {
      const fixture = TestBed.createComponent(OlvidoPassword);
      fixture.componentInstance.formulario.setValue({ email: 'lucia@toca.local' });

      fixture.componentInstance.enviar();
      const peticion = servidor.expectOne('/api/auth/password/olvido');
      expect(peticion.request.body).toEqual({ email: 'lucia@toca.local' });
      peticion.flush({ mensaje: 'Si el email existe, te hemos enviado un enlace.' });
      await fixture.whenStable();

      const html = fixture.nativeElement as HTMLElement;
      expect(html.querySelector('[role=status]')?.textContent).toContain(
        'Si el email existe, te hemos enviado un enlace.',
      );
      expect(html.querySelector('form')).toBeNull();
    });

    it('muestra el límite de intentos', async () => {
      const fixture = TestBed.createComponent(OlvidoPassword);
      fixture.componentInstance.formulario.setValue({ email: 'lucia@toca.local' });

      fixture.componentInstance.enviar();
      servidor
        .expectOne('/api/auth/password/olvido')
        .flush(
          { codigo: 'DEMASIADOS_INTENTOS', mensaje: 'Demasiados intentos', errores: {} },
          { status: 429, statusText: 'Too Many Requests' },
        );
      await fixture.whenStable();

      expect(fixture.nativeElement.querySelector('.alert-danger').textContent).toContain(
        'Demasiados intentos',
      );
    });
  });

  describe('RestablecerPassword', () => {
    it('sin token avisa de que el enlace no es válido', async () => {
      const fixture = TestBed.createComponent(RestablecerPassword);
      await fixture.whenStable();
      expect(fixture.nativeElement.textContent).toContain('El enlace no es válido');
      expect(fixture.nativeElement.querySelector('form')).toBeNull();
    });

    it('guarda la contraseña nueva con el token del enlace y lleva al login', async () => {
      const fixture = TestBed.createComponent(RestablecerPassword);
      fixture.componentRef.setInput('token', 'abc123');
      fixture.componentInstance.formulario.setValue({
        password: 'nueva-clave',
        confirmacion: 'nueva-clave',
      });

      fixture.componentInstance.guardar();
      const peticion = servidor.expectOne('/api/auth/password/restablecer');
      expect(peticion.request.body).toEqual({ token: 'abc123', password: 'nueva-clave' });
      peticion.flush({ mensaje: 'Contraseña cambiada. Ya puedes iniciar sesión.' });

      expect(router.navigate).toHaveBeenCalledWith(['/login']);
    });

    it('con un token caducado ofrece pedir otro enlace', async () => {
      const fixture = TestBed.createComponent(RestablecerPassword);
      fixture.componentRef.setInput('token', 'caducado');
      fixture.componentInstance.formulario.setValue({
        password: 'nueva-clave',
        confirmacion: 'nueva-clave',
      });

      fixture.componentInstance.guardar();
      servidor.expectOne('/api/auth/password/restablecer').flush(
        {
          codigo: 'TOKEN_INVALIDO',
          mensaje: 'El enlace ha caducado o ya se ha usado',
          errores: {},
        },
        { status: 400, statusText: 'Bad Request' },
      );
      await fixture.whenStable();

      const alerta = fixture.nativeElement.querySelector('.alert-danger') as HTMLElement;
      expect(alerta.textContent).toContain('El enlace ha caducado o ya se ha usado');
      expect(alerta.querySelector('a')?.getAttribute('href')).toBe('/olvido-password');
    });
  });
});
