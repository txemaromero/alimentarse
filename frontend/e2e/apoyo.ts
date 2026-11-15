import { APIRequestContext, APIResponse, Locator, Page, expect, test } from '@playwright/test';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

/** Contraseña del administrador inicial en la base de datos de las pruebas (perfil e2e). */
export const ADMIN_PASSWORD_E2E = process.env['E2E_ADMIN_PASSWORD'] ?? 'admin-e2e-1234';
export const PASSWORD_E2E = 'clave-e2e-1234';

/** Identificador único por ejecución, para que las pruebas no choquen entre sí. */
export function sufijo(): string {
  return `${Date.now().toString(36)}${Math.floor(Math.random() * 1000)}`;
}

/** Alta de un usuario por la API (para las pruebas que no prueban el registro). */
export async function registrarPorApi(request: APIRequestContext, id: string): Promise<void> {
  const respuesta = await request.post('/api/auth/registro', {
    data: {
      id,
      nombre: 'Prueba',
      apellido1: 'E2E',
      apellido2: null,
      email: `${id}@e2e.local`,
      password: PASSWORD_E2E,
    },
  });
  expect(respuesta.status(), await respuesta.text()).toBe(201);
}

/** JWT de la cookie de sesión de un login, para usarlo como `Authorization: Bearer` en las llamadas por la API. */
export function tokenDeLogin(respuesta: APIResponse): string {
  const cookie = respuesta
    .headersArray()
    .find((h) => h.name.toLowerCase() === 'set-cookie' && h.value.startsWith('toca_sesion='));
  expect(cookie, 'cookie de sesión').toBeTruthy();
  return cookie!.value.split(';')[0].substring('toca_sesion='.length);
}

/** Inicia sesión por la API y deja en la página la cookie de sesión y los datos del usuario. Devuelve el JWT. */
export async function sesionPorApi(
  page: Page,
  request: APIRequestContext,
  usuario: string,
  password = PASSWORD_E2E,
): Promise<string> {
  const respuesta = await request.post('/api/auth/login', { data: { usuario, password } });
  expect(respuesta.status(), await respuesta.text()).toBe(200);
  const token = tokenDeLogin(respuesta);
  await page.context().addCookies([
    {
      name: 'toca_sesion',
      value: token,
      domain: new URL(test.info().project.use.baseURL!).hostname,
      path: '/api',
      httpOnly: true,
      sameSite: 'Strict',
    },
  ]);
  await page.addInitScript(
    (valor) => {
      if (!localStorage.getItem('alimentarse.sesion')) {
        localStorage.setItem('alimentarse.sesion', valor);
      }
    },
    JSON.stringify(await respuesta.json()),
  );
  return token;
}

/** Inicia sesión desde la pantalla de login. */
export async function iniciarSesion(page: Page, usuario: string, password: string): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('Usuario o email').fill(usuario);
  await page.locator('#login-password').fill(password);
  await page.getByRole('button', { name: 'Iniciar sesión' }).click();
  await expect(page).toHaveURL(/\/$/);
}

/** Id de un ingrediente por su nombre exacto. */
export async function idIngrediente(
  request: APIRequestContext,
  token: string,
  nombre: string,
): Promise<number> {
  const respuesta = await request.get('/api/ingredientes/sugerencias', {
    params: { q: nombre },
    headers: { Authorization: `Bearer ${token}` },
  });
  const lista = (await respuesta.json()) as { id: number; nombre: string }[];
  const ingrediente = lista.find((i) => i.nombre === nombre);
  expect(ingrediente, `ingrediente ${nombre}`).toBeTruthy();
  return ingrediente!.id;
}

/** Crea y publica una receta por la API; devuelve su id. */
export async function recetaPorApi(
  request: APIRequestContext,
  token: string,
  titulo: string,
): Promise<number> {
  const autorizacion = { Authorization: `Bearer ${token}` };
  const receta = {
    titulo,
    instrucciones: '<p>Mezclar todo.</p><ol><li>Batir</li><li>Hornear 30 minutos</li></ol>',
    tipo: 1,
    tiempo: 30,
    comensales: 4,
    dificultad: 'Fácil',
    ingredientes: [{ ingrediente: await idIngrediente(request, token, 'Huevos'), cantidad: '3' }],
  };
  const alta = await request.post('/api/recetas', {
    headers: autorizacion,
    multipart: {
      receta: {
        name: 'receta.json',
        mimeType: 'application/json',
        buffer: Buffer.from(JSON.stringify(receta)),
      },
    },
  });
  expect(alta.status(), await alta.text()).toBe(201);
  const id = (await alta.json()).id as number;
  const publicar = await request.patch(`/api/recetas/${id}/publicada`, {
    headers: autorizacion,
    data: { publicada: true },
  });
  expect(publicar.status()).toBe(200);
  return id;
}

/**
 * Filtra un listado de recetas por título desde su panel de filtros (acordeón plegado).
 *
 * Hay que esperar a que el acordeón termine de abrirse: mientras se anima solo tiene la clase `collapsing`, con la
 * altura creciendo desde 0 y `overflow: hidden`, así que el botón «Filtrar» queda recortado y el clic no le llega
 * aunque Playwright lo dé por hecho. Al acabar, ng-bootstrap le pone `collapse show`.
 */
export async function filtrarRecetasPorTitulo(page: Page, titulo: string): Promise<void> {
  const filtros = page.locator('app-filtros-recetas');
  await filtros.getByRole('button', { name: /Filtros/ }).click();
  await expect(filtros.locator('.accordion-collapse')).toHaveClass(/(^|\s)show(\s|$)/);
  await filtros.getByLabel('Título', { exact: true }).fill(titulo);
  await filtros.getByRole('button', { name: 'Filtrar' }).click();
}

/**
 * Un aviso (toast) por su texto. Hay que buscarlo así: el mismo texto está también en una región viva oculta, la que
 * lo anuncia a los lectores de pantalla (D-075).
 */
export function aviso(page: Page, texto: string | RegExp): Locator {
  return page.locator('ngb-toast').filter({ hasText: texto });
}

/** Ancho del documento: si supera el de la ventana, hay scroll horizontal. */
export async function anchoDelDocumento(page: Page): Promise<number> {
  return page.evaluate(() => document.documentElement.scrollWidth);
}

/** Una foto para subir: una de las imágenes de demostración del backend (D-083). */
export function fotoDePrueba(
  nombre: 'demo-tortilla.jpg' | 'demo-lentejas.jpg' | 'demo-batido.jpg',
) {
  return {
    name: nombre,
    mimeType: 'image/jpeg',
    buffer: readFileSync(
      resolve(__dirname, '../../backend/src/main/resources/demo/imagenes', nombre),
    ),
  };
}

/**
 * Pone a la receta una galería de dos fotos por la API (D-083): una petición por foto, porque cada una solo lleva un
 * fichero. La segunda lleva descripción.
 */
export async function galeriaPorApi(
  request: APIRequestContext,
  token: string,
  receta: number,
): Promise<void> {
  const autorizacion = { Authorization: `Bearer ${token}` };
  const detalle = await (
    await request.get(`/api/recetas/${receta}`, { headers: autorizacion })
  ).json();
  const datos = (fotos: unknown[]) => ({
    titulo: detalle.titulo,
    instrucciones: detalle.instrucciones,
    tipo: detalle.tipo.id,
    tiempo: detalle.tiempo,
    comensales: detalle.comensales,
    dificultad: detalle.dificultad,
    ingredientes: detalle.ingredientes.map((i: { id: number; cantidad: string }) => ({
      ingrediente: i.id,
      cantidad: i.cantidad,
    })),
    fotos,
  });
  const subir = async (fotos: unknown[], fichero: ReturnType<typeof fotoDePrueba>) => {
    const respuesta = await request.put(`/api/recetas/${receta}`, {
      headers: autorizacion,
      multipart: {
        receta: {
          name: 'receta.json',
          mimeType: 'application/json',
          buffer: Buffer.from(JSON.stringify(datos(fotos))),
        },
        fotos: fichero,
      },
    });
    expect(respuesta.status(), await respuesta.text()).toBe(200);
    return respuesta.json();
  };
  const conUna = await subir(
    [{ existente: null, nueva: 0, descripcion: null }],
    fotoDePrueba('demo-tortilla.jpg'),
  );
  await subir(
    [
      { existente: conUna.fotos[0].nombre, nueva: null, descripcion: null },
      { existente: null, nueva: 0, descripcion: 'Las lentejas servidas' },
    ],
    fotoDePrueba('demo-lentejas.jpg'),
  );
}
