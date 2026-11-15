import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ingredienteDePrueba } from '../../testing/datos';
import { RecetaRequest } from '../core/modelos';
import { RecetasApi, cuerpoMultipart, parametrosFiltro } from './recetas-api';

const RECETA: RecetaRequest = {
  titulo: 'Revuelto',
  instrucciones: '<p>Batir</p>',
  tipo: 1,
  tiempo: 15,
  comensales: 2,
  dificultad: 'Fácil',
  ingredientes: [{ ingrediente: 56, cantidad: '3 unidades' }],
  fotos: [{ existente: null, nueva: 0, descripcion: 'En el plato' }],
  video: null,
};

describe('RecetasApi', () => {
  let api: RecetasApi;
  let servidor: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(RecetasApi);
    servidor = TestBed.inject(HttpTestingController);
  });

  afterEach(() => servidor.verify());

  it('solo envía los filtros con valor, y los ingredientes separados por comas', () => {
    const params = parametrosFiltro(
      {
        titulo: '  tortilla ',
        usuario: '   ',
        tipo: 2,
        dificultad: 'Fácil',
        comensales: 4,
        tiempoMax: 30,
        ingredientes: [ingredienteDePrueba({ id: 3 }), ingredienteDePrueba({ id: 7 })],
      },
      1,
      9,
    );
    expect(params.toString()).toBe(
      'page=1&size=9&titulo=tortilla&tipo=2&dificultad=F%C3%A1cil&comensales=4&tiempoMax=30&ingredientes=3,7',
    );
    expect(parametrosFiltro({}, 0, 9).toString()).toBe('page=0&size=9');
  });

  it('busca las publicadas y las propias en sus rutas', () => {
    api.buscar({ titulo: 'sopa' }, 2).subscribe();
    api.mias({}, 0).subscribe();
    expect(servidor.expectOne('/api/recetas?page=2&size=9&titulo=sopa').request.method).toBe('GET');
    servidor.expectOne('/api/recetas/mias?page=0&size=9');
  });

  it('da y quita «me gusta» con POST y DELETE', () => {
    api.meGusta(5, true).subscribe();
    api.meGusta(5, false).subscribe();
    const peticiones = servidor.match('/api/recetas/5/like');
    expect(peticiones.map((p) => p.request.method)).toEqual(['POST', 'DELETE']);
  });

  it('publica con PATCH', () => {
    api.publicar(4, true).subscribe();
    const peticion = servidor.expectOne('/api/recetas/4/publicada');
    expect(peticion.request.method).toBe('PATCH');
    expect(peticion.request.body).toEqual({ publicada: true });
  });

  it('el alta envía la receta como JSON y cada foto nueva como una parte «fotos» (D-083)', async () => {
    const a = new File(['x'], 'a.png', { type: 'image/png' });
    const b = new File(['y'], 'b.jpg', { type: 'image/jpeg' });
    api.crear(RECETA, [a, b]).subscribe();
    const cuerpo = servidor.expectOne('/api/recetas').request.body as FormData;

    const parte = cuerpo.get('receta') as Blob;
    expect(parte.type).toBe('application/json');
    expect(JSON.parse(await parte.text())).toEqual(RECETA);
    expect((cuerpo.getAll('fotos') as File[]).map((f) => f.name)).toEqual(['a.png', 'b.jpg']);
  });

  it('sin fotos nuevas no envía partes «fotos»', () => {
    expect(cuerpoMultipart(RECETA, []).has('fotos')).toBe(false);
  });

  it('la edición usa PUT', () => {
    api.editar(9, RECETA).subscribe();
    expect(servidor.expectOne('/api/recetas/9').request.method).toBe('PUT');
  });
});
