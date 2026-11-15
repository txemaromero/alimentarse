import {
  Grupo,
  Ingrediente,
  NutricionReceta,
  NutricionSemana,
  Pagina,
  RecetaDetalle,
  RecetaResumen,
  Valoracion,
} from '../app/core/modelos';

export const GRUPO_CARNE: Grupo = { id: 1, nombre: 'Carne', racionesSemana: 3 };
export const GRUPO_HORTALIZAS: Grupo = { id: 4, nombre: 'Hortalizas', racionesSemana: null };

export function ingredienteDePrueba(cambios: Partial<Ingrediente> = {}): Ingrediente {
  return {
    id: 26,
    nombre: 'Patata',
    grupo: GRUPO_HORTALIZAS,
    alimento: 'Patata, cruda',
    ...cambios,
  };
}

export function recetaDePrueba(cambios: Partial<RecetaResumen> = {}): RecetaResumen {
  return {
    id: 1,
    titulo: 'Tortilla de patata',
    autor: 'lucia',
    tipo: { id: 1, nombre: 'Plato' },
    imagen: '/api/imagenes/tortilla.jpg',
    tiempo: 45,
    comensales: 4,
    dificultad: 'Media',
    likes: 3,
    meGusta: false,
    nota: null,
    valoraciones: 0,
    publicada: true,
    fechaCreacion: '2026-10-06T21:00:00Z',
    eliminada: false,
    oculta: false,
    ...cambios,
  };
}

export function detalleDePrueba(cambios: Partial<RecetaDetalle> = {}): RecetaDetalle {
  return {
    ...recetaDePrueba(),
    instrucciones: '<p>Pelar las patatas.</p><ol><li>Freír</li><li>Cuajar</li></ol>',
    ingredientes: [
      { id: 26, nombre: 'Patata', cantidad: '600 gramos', grupo: GRUPO_HORTALIZAS },
      { id: 30, nombre: 'Ternera', cantidad: '200 gramos', grupo: GRUPO_CARNE },
    ],
    grupos: [GRUPO_CARNE, GRUPO_HORTALIZAS],
    maxSemanal: 3,
    esAutor: false,
    motivoOculta: null,
    miValoracion: null,
    fotos: [{ nombre: 'tortilla.jpg', url: '/api/imagenes/tortilla.jpg', descripcion: null }],
    video: null,
    ...cambios,
  };
}

export function valoracionDePrueba(cambios: Partial<Valoracion> = {}): Valoracion {
  return {
    id: 7,
    usuario: 'marcos',
    nota: 4,
    comentario: 'Muy jugosa.',
    fecha: '2026-11-10T19:00:00Z',
    oculta: false,
    ...cambios,
  };
}

/** Estimación de un bizcocho para dos: harina y huevos cuentan; la levadura no tiene datos y la sal no tiene peso. */
export function nutricionDePrueba(cambios: Partial<NutricionReceta> = {}): NutricionReceta {
  return {
    comensales: 2,
    total: {
      energiaKcal: 811,
      energiaKj: 3394,
      grasas: 11.1,
      grasasSaturadas: 3.1,
      hidratos: 143,
      fibra: 7,
      proteinas: 32.5,
      sal: 0.37,
    },
    porRacion: {
      energiaKcal: 406,
      energiaKj: 1697,
      grasas: 5.6,
      grasasSaturadas: 1.6,
      hidratos: 71.5,
      fibra: 3.5,
      proteinas: 16.3,
      sal: 0.18,
    },
    ingredientes: [
      {
        id: 1,
        nombre: 'Harina',
        cantidad: '200 gramos',
        gramos: 200,
        alimento: 'Harina de trigo',
        estado: 'CALCULADO',
      },
      {
        id: 2,
        nombre: 'Huevos',
        cantidad: '2 unidades',
        gramos: 100,
        alimento: 'Huevo de gallina fresco',
        estado: 'CALCULADO',
      },
      {
        id: 3,
        nombre: 'Levadura',
        cantidad: '1 sobre',
        gramos: null,
        alimento: null,
        estado: 'SIN_DATOS',
      },
      {
        id: 4,
        nombre: 'Sal',
        cantidad: '1 pizca',
        gramos: null,
        alimento: 'Sal de mar',
        estado: 'SIN_PESO',
      },
    ],
    ...cambios,
  };
}

/** Nutrientes de una semana con dos raciones del bizcocho de `nutricionDePrueba` y un consumo sin datos (D-082). */
export function nutricionSemanaDePrueba(cambios: Partial<NutricionSemana> = {}): NutricionSemana {
  return {
    consumos: 3,
    estimados: 2,
    total: nutricionDePrueba().total,
    mediaDiaria: {
      energiaKcal: 116,
      energiaKj: 485,
      grasas: 1.6,
      grasasSaturadas: 0.4,
      hidratos: 20.4,
      fibra: 1,
      proteinas: 4.6,
      sal: 0.05,
    },
    referencia: { energia: 6, grasas: 2, grasasSaturadas: 2, hidratos: 8, proteinas: 9, sal: 1 },
    ...cambios,
  };
}

export function paginaDe<T>(contenido: T[], cambios: Partial<Pagina<T>> = {}): Pagina<T> {
  return {
    contenido,
    pagina: 0,
    tamano: 9,
    totalElementos: contenido.length,
    totalPaginas: contenido.length ? 1 : 0,
    ...cambios,
  };
}
