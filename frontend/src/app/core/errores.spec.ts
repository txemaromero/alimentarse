import { HttpErrorResponse } from '@angular/common/http';
import { FormArray, FormControl, FormGroup } from '@angular/forms';
import { mensajeValidacion } from '../compartido/error-campo';
import { cabeEnBcrypt, coincideCon, noVacio } from '../compartido/validadores';
import {
  MENSAJE_ERROR_GENERICO,
  aplicarErroresServidor,
  errorApi,
  esInvalido,
  mensajeError,
} from './errores';
import { Notificaciones } from './notificaciones';

function respuesta(status: number, error: unknown): HttpErrorResponse {
  return new HttpErrorResponse({ status, error });
}

describe('errores', () => {
  it('extrae el cuerpo de error de la API', () => {
    const error = respuesta(409, {
      codigo: 'CONFLICTO',
      mensaje: 'Ya existe',
      errores: { email: 'Ese email ya está registrado' },
    });
    expect(errorApi(error)).toEqual({
      codigo: 'CONFLICTO',
      mensaje: 'Ya existe',
      errores: { email: 'Ese email ya está registrado' },
    });
    expect(mensajeError(error)).toBe('Ya existe');
  });

  it('usa el mensaje genérico si la respuesta no es de la API', () => {
    expect(errorApi(respuesta(502, '<html>Bad gateway</html>'))).toBeNull();
    expect(mensajeError(respuesta(502, null))).toBe(MENSAJE_ERROR_GENERICO);
    expect(mensajeError(new Error('x'), 'Otro')).toBe('Otro');
  });

  it('coloca los errores por campo de la API en el formulario', () => {
    const formulario = new FormGroup({
      id: new FormControl('lucia'),
      email: new FormControl('lucia@toca.local'),
    });
    const aplicado = aplicarErroresServidor(formulario, {
      codigo: 'CONFLICTO',
      mensaje: 'Revisa los datos',
      errores: { email: 'Ese email ya está registrado', otro: 'Se ignora' },
    });

    expect(aplicado).toBe(true);
    expect(formulario.controls.email.errors).toEqual({ servidor: 'Ese email ya está registrado' });
    expect(formulario.controls.email.touched).toBe(true);
    expect(formulario.controls.id.valid).toBe(true);
  });

  it('los campos de las listas («piezas[0].gramos») van a su control', () => {
    const formulario = new FormGroup({
      piezas: new FormArray([new FormGroup({ gramos: new FormControl(0) })]),
    });
    expect(
      aplicarErroresServidor(formulario, {
        codigo: 'VALIDACION',
        mensaje: '',
        errores: { 'piezas[0].gramos': 'Debe pesar más de 0 g' },
      }),
    ).toBe(true);
    expect(formulario.get('piezas.0.gramos')!.errors).toEqual({
      servidor: 'Debe pesar más de 0 g',
    });
  });

  it('el error del servidor desaparece al cambiar el valor', () => {
    const formulario = new FormGroup({ email: new FormControl('a@b.es') });
    aplicarErroresServidor(formulario, {
      codigo: 'CONFLICTO',
      mensaje: '',
      errores: { email: 'Ya existe' },
    });
    formulario.controls.email.setValue('c@d.es');
    expect(formulario.controls.email.valid).toBe(true);
  });

  it('sin errores por campo no coloca nada', () => {
    const formulario = new FormGroup({ email: new FormControl('') });
    expect(aplicarErroresServidor(formulario, null)).toBe(false);
    expect(
      aplicarErroresServidor(formulario, { codigo: 'VALIDACION', mensaje: '', errores: {} }),
    ).toBe(false);
  });

  it('un control solo se muestra inválido después de tocarlo', () => {
    const control = new FormControl('', { validators: [noVacio] });
    control.setValue('   ');
    expect(control.invalid).toBe(true);
    expect(esInvalido(control)).toBe(false);
    control.markAsTouched();
    expect(esInvalido(control)).toBe(true);
  });
});

describe('mensajes de validación', () => {
  it.each([
    [{ servidor: 'Ya existe' }, 'Ya existe'],
    [{ required: true }, 'Este campo es obligatorio'],
    [{ email: true }, 'El email no es válido'],
    [{ minlength: { requiredLength: 8 } }, 'Debe tener al menos 8 caracteres'],
    [{ maxlength: { requiredLength: 30 } }, 'No puede tener más de 30 caracteres'],
    [
      { bcrypt: true },
      'La contraseña es demasiado larga: la ñ, las tildes y los emojis cuentan como más de un carácter',
    ],
    [{ min: { min: 1 } }, 'El valor mínimo es 1'],
    [{ max: { max: 99 } }, 'El valor máximo es 99'],
    [{ min: { min: 0.1 } }, 'El valor mínimo es 0,1'],
    [{ max: { max: 10000 } }, 'El valor máximo es 10.000'],
    [{ noElegido: true }, 'Elige una de las sugerencias, o deja el campo vacío'],
    [{ noCoinciden: true }, 'Las contraseñas no coinciden'],
    [{ espacios: true }, 'No puede estar vacío ni tener solo espacios'],
    [{ desconocido: true }, 'El valor no es válido'],
  ])('%o → %s', (errores, mensaje) => {
    expect(mensajeValidacion(errores)).toBe(mensaje);
  });

  it('usa el mensaje propio para el patrón', () => {
    expect(mensajeValidacion({ pattern: {} }, 'Solo letras')).toBe('Solo letras');
  });

  it('cabeEnBcrypt cuenta bytes, y deja los de más de 72 caracteres a maxLength (D-090)', () => {
    const password = new FormControl('', { validators: [cabeEnBcrypt] });
    for (const valida of ['a'.repeat(72), 'ñ'.repeat(36), '😀'.repeat(18), 'ñ'.repeat(73), '']) {
      password.setValue(valida);
      expect(password.valid, valida).toBe(true);
    }
    for (const invalida of ['ñ'.repeat(37), 'ñ'.repeat(40), '😀'.repeat(19)]) {
      password.setValue(invalida);
      expect(password.hasError('bcrypt'), invalida).toBe(true);
    }
  });

  it('coincideCon compara con el control hermano', () => {
    const formulario = new FormGroup({
      password: new FormControl('demo1234'),
      confirmacion: new FormControl('', { validators: [coincideCon('password')] }),
    });
    const confirmacion = formulario.controls.confirmacion;
    confirmacion.setValue('otra');
    expect(confirmacion.hasError('noCoinciden')).toBe(true);
    confirmacion.setValue('demo1234');
    expect(confirmacion.valid).toBe(true);
  });
});

describe('Notificaciones', () => {
  it('añade avisos, evita duplicados y los quita', () => {
    const notificaciones = new Notificaciones();
    notificaciones.exito('Guardado');
    notificaciones.exito('Guardado');
    notificaciones.error('Falló');
    notificaciones.info('Hola');
    notificaciones.aviso('Cuidado');

    expect(notificaciones.avisos().map((a) => a.tipo)).toEqual(['exito', 'error', 'info', 'aviso']);

    notificaciones.quitar(notificaciones.avisos()[0].id);
    expect(notificaciones.avisos().map((a) => a.texto)).toEqual(['Falló', 'Hola', 'Cuidado']);
  });
});
