import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { detalleDePrueba, ingredienteDePrueba } from '../../testing/datos';
import { Notificaciones } from '../core/notificaciones';
import { errorImagen } from '../compartido/imagenes';
import { FormularioReceta, MAXIMO_INGREDIENTES, PATRON_VIDEO } from './formulario-receta';

describe('FormularioReceta', () => {
  let fixture: ComponentFixture<FormularioReceta>;
  let componente: FormularioReceta;
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

  async function crear(id?: string) {
    fixture = TestBed.createComponent(FormularioReceta);
    componente = fixture.componentInstance;
    if (id) {
      fixture.componentRef.setInput('id', id);
    }
    await fixture.whenStable();
    servidor.expectOne('/api/tipos-receta').flush([{ id: 1, nombre: 'Plato' }]);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  function rellenar() {
    componente.formulario.patchValue({
      titulo: ' Revuelto de setas ',
      instrucciones: '<p>Saltear las setas.</p>',
      tipo: 1,
      tiempo: 15,
      comensales: 2,
      dificultad: 'Fácil',
    });
    componente.busqueda.setValue(ingredienteDePrueba({ id: 56, nombre: 'Huevo' }));
    componente.cantidad.setValue(' 3 unidades ');
    componente.anadirIngrediente();
  }

  it('el vídeo es un enlace https a YouTube o Vimeo, o nada (D-083)', () => {
    for (const valido of [
      '',
      '  ',
      'https://www.youtube.com/watch?v=abc',
      'https://youtu.be/abc',
      ' https://vimeo.com/123 ',
      'https://player.vimeo.com/video/1',
    ]) {
      expect(PATRON_VIDEO.test(valido), valido).toBe(true);
    }
    for (const invalido of [
      'http://youtube.com/x',
      'https://youtube.com.ejemplo.com/x',
      'https://ejemplo.com',
      'youtube.com/x',
    ]) {
      expect(PATRON_VIDEO.test(invalido), invalido).toBe(false);
    }
  });

  it('valida los ficheros de imagen', () => {
    expect(errorImagen(new File(['x'], 'a.gif', { type: 'image/gif' }))).toContain('JPEG, PNG');
    const grande = new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'a.png', { type: 'image/png' });
    expect(errorImagen(grande)).toContain('5 MB');
    expect(errorImagen(new File(['x'], 'a.webp', { type: 'image/webp' }))).toBeNull();
  });

  it('el botón Guardar está deshabilitado hasta completar la receta', async () => {
    const html = await crear();
    const guardar = html.querySelector('button[type=submit]') as HTMLButtonElement;
    expect(guardar.disabled).toBe(true);

    rellenar();
    await fixture.whenStable();

    expect(componente.formulario.valid).toBe(true);
    expect(guardar.disabled).toBe(false);
  });

  it('añade ingredientes con su cantidad y no repite ninguno', async () => {
    const html = await crear();
    expect(componente.puedeAnadir()).toBe(false);
    rellenar();
    await fixture.whenStable();

    expect(html.querySelector('.chip')?.textContent).toContain('Huevo');
    expect(html.querySelector('.chip')?.textContent).toContain('3 unidades');
    expect(componente.busqueda.value).toBeNull();

    componente.busqueda.setValue(ingredienteDePrueba({ id: 56, nombre: 'Huevo' }));
    componente.cantidad.setValue('1');
    componente.anadirIngrediente();
    expect(componente.filas.length).toBe(1);
    expect(componente.errorIngredientes()).toContain('ya está en la lista');

    componente.quitarIngrediente(0);
    expect(componente.filas.hasError('sinIngredientes')).toBe(true);
  });

  it('no deja añadir más ingredientes de los que admite el servidor (D-089)', async () => {
    await crear();
    for (let id = 1; id <= MAXIMO_INGREDIENTES; id++) {
      componente.busqueda.setValue(ingredienteDePrueba({ id, nombre: `Ingrediente ${id}` }));
      componente.cantidad.setValue('1');
      componente.anadirIngrediente();
    }
    expect(componente.filas.length).toBe(MAXIMO_INGREDIENTES);
    expect(componente.errorIngredientes()).toBeNull();

    componente.busqueda.setValue(ingredienteDePrueba({ id: 999, nombre: 'Uno más' }));
    componente.cantidad.setValue('1');
    componente.anadirIngrediente();

    expect(componente.filas.length).toBe(MAXIMO_INGREDIENTES);
    expect(componente.errorIngredientes()).toBe('Como mucho 50 ingredientes.');
  });

  it('crea la receta y lleva a su detalle', async () => {
    await crear();
    rellenar();

    componente.guardar();

    expect(componente.datos()).toEqual({
      titulo: 'Revuelto de setas',
      instrucciones: '<p>Saltear las setas.</p>',
      tipo: 1,
      tiempo: 15,
      comensales: 2,
      dificultad: 'Fácil',
      ingredientes: [{ ingrediente: 56, cantidad: '3 unidades' }],
      fotos: null,
      video: null,
    });
    const peticion = servidor.expectOne('/api/recetas');
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toBeInstanceOf(FormData);
    // Sin fotos, la galería va vacía (D-083)
    const receta = (peticion.request.body as FormData).get('receta') as Blob;
    expect(JSON.parse(await receta.text()).fotos).toEqual([]);
    peticion.flush(detalleDePrueba({ id: 12, esAutor: true, publicada: false }));

    expect(router.navigate).toHaveBeenCalledWith(['/recetas', 12]);
    expect(TestBed.inject(Notificaciones).avisos()[0].texto).toContain('no está publicada');
    expect(componente.hayCambiosSinGuardar()).toBe(false);
  });

  it('coloca los errores del servidor junto a cada campo', async () => {
    const html = await crear();
    rellenar();
    componente.guardar();
    servidor.expectOne('/api/recetas').flush(
      {
        codigo: 'VALIDACION',
        mensaje: 'Revisa los datos',
        errores: {
          titulo: 'Máximo 100 caracteres',
          'ingredientes[0].cantidad': 'Indica la cantidad',
          fotos: 'Hay ficheros que no corresponden a ninguna foto',
        },
      },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();

    expect(html.querySelector('#receta-titulo-error')?.textContent).toContain('Máximo 100');
    expect(html.querySelector('#receta-ingredientes-error')?.textContent).toContain(
      'Indica la cantidad',
    );
    expect(html.querySelector('#receta-fotos-error')?.textContent).toContain('no corresponden');
    expect(router.navigate).not.toHaveBeenCalled();
    expect(componente.hayCambiosSinGuardar()).toBe(true);
  });

  it('en edición carga la receta y guarda con PUT', async () => {
    await crear('1');
    servidor.expectOne('/api/recetas/1').flush(detalleDePrueba({ esAutor: true }));
    await fixture.whenStable();

    expect(componente.formulario.controls.titulo.value).toBe('Tortilla de patata');
    expect(componente.filas.length).toBe(2);
    expect(componente.fotosIniciales().map((f) => f.nombre)).toEqual(['tortilla.jpg']);
    expect(componente.hayCambiosSinGuardar()).toBe(false);

    componente.guardar();
    const peticion = servidor.expectOne('/api/recetas/1');
    expect(peticion.request.method).toBe('PUT');
    // Se conserva la foto que tenía
    const receta = (peticion.request.body as FormData).get('receta') as Blob;
    expect(JSON.parse(await receta.text()).fotos).toEqual([
      { existente: 'tortilla.jpg', nueva: null, descripcion: null },
    ]);
    peticion.flush(detalleDePrueba({ esAutor: true }));
    expect(router.navigate).toHaveBeenCalledWith(['/recetas', 1]);
  });

  it('quien no es el autor vuelve al detalle', async () => {
    await crear('1');
    servidor.expectOne('/api/recetas/1').flush(detalleDePrueba({ esAutor: false }));
    expect(router.navigate).toHaveBeenCalledWith(['/recetas', 1]);
    expect(TestBed.inject(Notificaciones).avisos()[0].tipo).toBe('error');
  });

  it('Cancelar vuelve a Mis recetas en un alta', async () => {
    await crear();
    componente.cancelar();
    expect(router.navigate).toHaveBeenCalledWith(['/mis-recetas']);
  });
});
