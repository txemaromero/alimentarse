/**
 * Tipos de la API. Salen de `api.generado.ts`, que se genera desde `docs/openapi.json` con `npm run api` (D-073):
 * aquí solo se les da el nombre que usa el frontend. No se escriben a mano, para que no se desajusten de los DTO.
 */
import type { components } from './api.generado';

/** Todos los esquemas de la API, por el nombre del DTO del backend. Útil para tipar los cuerpos de las peticiones. */
export type Esquemas = components['schemas'];

// ---- Sesión y usuarios ----

/** Usuario de la sesión, tal como lo devuelve el login. */
export type UsuarioSesion = Esquemas['UsuarioSesion'];
export type Rol = UsuarioSesion['roles'][number];

/** El JWT va en una cookie `HttpOnly` que el JavaScript no puede leer (D-070). `caduca`: fin de la sesión. */
export type LoginResponse = Esquemas['LoginResponse'];
export type Perfil = Esquemas['PerfilDto'];
export type Registro = Esquemas['RegistroRequest'];
export type Proveedores = Esquemas['ProveedoresDto'];
export type DatosRegistroGoogle = Esquemas['DatosRegistroGoogleDto'];
export type DatosVinculacionGoogle = Esquemas['DatosVinculacionGoogleDto'];
export type EmailConfirmado = Esquemas['EmailConfirmadoDto'];
export type EnlaceEnviado = Esquemas['EnlaceEnviadoDto'];
export type RegistroGoogle = Esquemas['RegistroGoogleRequest'];
export type ActualizarPerfil = Esquemas['ActualizarPerfilRequest'];
export type MensajeRespuesta = Esquemas['MensajeRespuesta'];

/** Cuerpo de todas las respuestas de error de la API. `errores`: mensaje de cada campo, o vacío. */
export type ErrorApi = Esquemas['ErrorRespuesta'];

/** Lista paginada; `pagina` empieza en 0. En el OpenAPI hay un esquema por cada tipo de contenido. */
export type Pagina<T> = Omit<Esquemas['PaginaRecetaResumenDto'], 'contenido'> & { contenido: T[] };

// ---- Catálogos ----

/** Grupo de alimentos. `racionesSemana: null` = sin límite. */
export type Grupo = Esquemas['GrupoDto'];
export type TipoReceta = Esquemas['TipoRecetaDto'];

// ---- Ingredientes ----

export type Ingrediente = Esquemas['IngredienteDto'];
export type IngredienteRequest = Esquemas['IngredienteRequest'];

/** Resultado de fusionar un ingrediente duplicado con otro (D-065): el que queda y cuántas recetas lo llevaban. */
export type FusionIngredientes = Esquemas['FusionDto'];

// ---- Recetas ----

export type Dificultad = Esquemas['RecetaRequest']['dificultad'];
export const DIFICULTADES = ['Fácil', 'Media', 'Difícil'] as const satisfies readonly Dificultad[];

/**
 * Tarjeta de receta (listados). `imagen`: URL relativa o null. `autor`: null solo en una receta eliminada cuyo autor
 * se dio de baja (D-064). `eliminada`: solo aparece en el histórico, sin imagen ni enlace (D-061). `oculta`: la ha
 * ocultado un administrador y solo la ven su autor y los administradores (D-063).
 */
export type RecetaResumen = Esquemas['RecetaResumenDto'];
export type IngredienteDeReceta = Esquemas['IngredienteRecetaDto'];

/**
 * Detalle de una receta. `instrucciones`: HTML ya saneado por el backend. `maxSemanal`: veces a la semana como máximo
 * (6.1), null = sin límite. `motivoOculta`: por qué la ocultó la administración, o null.
 */
export type RecetaDetalle = Esquemas['RecetaDetalleDto'];

/** Parte `receta` (JSON) del alta y la edición. */
export type RecetaRequest = Esquemas['RecetaRequest'];
export type SugerenciaReceta = Esquemas['SugerenciaRecetaDto'];
export type Likes = Esquemas['LikesDto'];

/**
 * Valoración de una receta (D-077): nota de 1 a 5 y comentario opcional, en texto plano. `oculta`: la ocultó la
 * administración; solo la ven su autor y los administradores, y no cuenta en la media.
 */
export type Valoracion = Esquemas['ValoracionDto'];

/** Una foto de la galería de una receta (D-083); `nombre` sirve para conservarla al editarla. */
export type Foto = Esquemas['FotoDto'];

/** Una foto de la galería pedida al guardar la receta: una que ya tenía (`existente`) o un fichero nuevo (`nueva`). */
export type FotoRequest = Esquemas['FotoRequest'];
/** Cómo queda la receta tras cambiar la valoración propia: `nota` media (null si no hay), `valoraciones` y `mia`. */
export type ResumenValoraciones = Esquemas['ResumenValoracionesDto'];

/** Aviso de prueba (D-078): `enviados`, a cuántos navegadores del usuario lo ha aceptado el servicio push. */
export type PruebaPush = Esquemas['PruebaPushDto'];

/** Filtros del listado de recetas (6.5). Los ingredientes se guardan completos para pintar los chips. */
export interface FiltroRecetas {
  titulo?: string;
  tipo?: number;
  usuario?: string;
  dificultad?: Dificultad;
  comensales?: number;
  tiempoMax?: number;
  ingredientes?: Ingrediente[];
}

// ---- Consumos ----

export type Consumo = Esquemas['ConsumoDto'];

// ---- Mi alimentación ----

export type ColorIndicador = IndicadorGrupo['color'];

/**
 * Indicador circular de un grupo en los últimos 7 días (6.4). `limite: null` = sin límite. `porcentaje`: progreso del
 * círculo, de 0 a 100 (100 si no hay límite).
 */
export type IndicadorGrupo = Esquemas['IndicadorGrupoDto'];

/** Receta de «¿Qué toca hoy?» con cómo van esta semana sus grupos de alimentos (D-067). */
export type RecetaRecomendada = Esquemas['RecetaRecomendadaDto'];

/** Cómo cambiaría un grupo con límite si se comiera una receta ahora (D-067). */
export type ImpactoGrupo = Esquemas['ImpactoGrupoDto'];

/**
 * Estimación nutricional de una receta con los datos de BEDCA (D-079). `total` y `porRacion` son `null` si ningún
 * ingrediente cuenta.
 */
export type NutricionReceta = Esquemas['NutricionRecetaDto'];

/** Energía (kcal y kJ), grasas, saturadas, hidratos, fibra y proteínas en gramos, y sal (sodio × 2,5) en gramos. */
export type ValoresNutricionales = Esquemas['ValoresNutricionalesDto'];

/** Cómo cuenta un ingrediente en la estimación: CALCULADO, SIN_DATOS (sin composición) o SIN_PESO. */
export type IngredienteNutricion = Esquemas['IngredienteNutricionDto'];

/**
 * Un alimento de BEDCA con su composición por 100 g, para asignarlo a un ingrediente (D-080). `null` = BEDCA no da
 * ese valor.
 */
export type AlimentoBedca = Esquemas['AlimentoBedcaDto'];

/** Los datos nutricionales de un ingrediente para editarlos: su alimento de BEDCA y sus piezas (D-081). */
export type NutricionIngrediente = Esquemas['NutricionIngredienteDto'];

/** El peso aproximado de una pieza de un ingrediente («unidad», «lata»), en gramos (D-079, D-081). */
export type Pieza = Esquemas['PiezaDto'];

/**
 * Una comida del planificador (D-068). `disponible: false` si la receta ya no se puede ver: ni cuenta ni se puede
 * consumir. `excesos`: grupos que superarían su límite ese día, contando esta comida.
 */
export type ComidaPlanificada = Esquemas['ComidaPlanificadaDto'];

/** Comidas del intervalo e `indicadores`: cómo quedarían los grupos al final. */
export type Plan = Esquemas['PlanDto'];

/**
 * Lista de la compra de las comidas planificadas que aún no han pasado (D-076): `secciones` por grupo de alimentos, con
 * los básicos al final, y `noDisponibles`, las comidas que no entran porque su receta ya no está disponible.
 */
export type ListaCompra = Esquemas['ListaCompraDto'];
export type SeccionCompra = Esquemas['SeccionCompraDto'];
/** `total`: la suma de las cantidades, o la cantidad tal cual si la pide una sola comida; `null` si no se puede sumar. */
export type ArticuloCompra = Esquemas['ArticuloCompraDto'];
export type ComidaCompra = Esquemas['ComidaCompraDto'];
export type RecetaConsumida = Esquemas['RecetaConsumidaDto'];
export type IngredienteUsado = Esquemas['IngredienteUsadoDto'];

/** `ingredientesMasUsados` no incluye los grupos básicos (Esenciales y Especias, D-054): puede tener menos de 4. */
export type ResumenAlimentacion = Esquemas['ResumenAlimentacionDto'];

/**
 * Nutrientes de lo comido en los últimos 7 días (D-082): total, media diaria y porcentaje de la ingesta de
 * referencia. `total`, `mediaDiaria` y `referencia` son `null` si ningún consumo tiene estimación.
 */
export type NutricionSemana = Esquemas['NutricionSemanaDto'];

// ---- Administración ----

/**
 * Grupo de alimentos en la administración (D-065). `basico`: no sale en «Ingredientes más usados» (D-054).
 * `ingredientes`: cuántos lo usan; solo se puede borrar si es 0.
 */
export type GrupoAdmin = Esquemas['GrupoAdminDto'];

/** `racionesSemana: null` = sin límite. */
export type GrupoRequest = Esquemas['GrupoRequest'];

/** Fila de la gestión de usuarios. `bloqueado`: no puede iniciar sesión (D-063). */
export type UsuarioAdmin = Esquemas['UsuarioAdminDto'];
