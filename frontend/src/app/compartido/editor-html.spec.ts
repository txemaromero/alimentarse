import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { EditorHtml, htmlConTexto, textoDeHtml } from './editor-html';
import { aInstanteUtc, aValorLocal, noFutura } from './fechas';

@Component({
  imports: [ReactiveFormsModule, EditorHtml],
  template: `
    <span id="etiqueta">Instrucciones</span>
    <app-editor-html [formControl]="control" idCampo="campo" idEtiqueta="etiqueta" />
  `,
})
class Anfitrion {
  readonly control = new FormControl('<p>Hola <strong>mundo</strong></p>', {
    nonNullable: true,
    validators: htmlConTexto,
  });
}

describe('EditorHtml', () => {
  it('extrae el texto visible de un HTML', () => {
    expect(textoDeHtml('<p> Hola <b>mundo</b> </p><ul><li>x</li></ul>')).toBe('Hola mundo x');
    expect(textoDeHtml('<p><br></p>')).toBe('');
  });

  it('exige algún texto visible', () => {
    expect(htmlConTexto(new FormControl('<p>   </p>'))).toEqual({ required: true });
    expect(htmlConTexto(new FormControl(''))).toEqual({ required: true });
    expect(htmlConTexto(new FormControl('<p>Batir</p>'))).toBeNull();
  });

  it('muestra el valor del control en un área editable accesible', async () => {
    const fixture = TestBed.createComponent(Anfitrion);
    await fixture.whenStable();
    const html = fixture.nativeElement as HTMLElement;

    const area = html.querySelector('#campo') as HTMLElement;
    expect(area.getAttribute('contenteditable')).toBe('true');
    expect(area.getAttribute('aria-labelledby')).toBe('etiqueta');
    expect(area.querySelector('strong')?.textContent).toBe('mundo');
    expect(html.querySelectorAll('[role=toolbar] button').length).toBe(10);
  });

  it('refleja los cambios de valor y el estado deshabilitado', async () => {
    const fixture = TestBed.createComponent(Anfitrion);
    await fixture.whenStable();
    const html = fixture.nativeElement as HTMLElement;

    fixture.componentInstance.control.setValue('<ol><li>Paso uno</li></ol>');
    expect(html.querySelector('#campo li')?.textContent).toBe('Paso uno');

    fixture.componentInstance.control.disable();
    await fixture.whenStable();
    expect(html.querySelector('#campo')?.getAttribute('contenteditable')).toBe('false');
    expect((html.querySelector('[role=toolbar] button') as HTMLButtonElement).disabled).toBe(true);
  });
});

describe('fechas', () => {
  it('formatea para datetime-local y convierte a UTC', () => {
    const fecha = new Date(2026, 0, 5, 9, 7);
    expect(aValorLocal(fecha)).toBe('2026-01-05T09:07');
    expect(aInstanteUtc('2026-01-05T09:07')).toBe(fecha.toISOString());
  });

  it('rechaza fechas futuras, admite ahora y el pasado', () => {
    expect(noFutura(new FormControl(aValorLocal(new Date())))).toBeNull();
    expect(noFutura(new FormControl('2020-01-01T10:00'))).toBeNull();
    expect(noFutura(new FormControl(aValorLocal(new Date(Date.now() + 86_400_000))))).toEqual({
      futura: true,
    });
    expect(noFutura(new FormControl('no-es-fecha'))).toEqual({ fechaInvalida: true });
    expect(noFutura(new FormControl(''))).toBeNull();
  });
});
