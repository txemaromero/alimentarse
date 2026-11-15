import AxeBuilder from '@axe-core/playwright';
import { APIRequestContext, Page, expect, test } from '@playwright/test';
import {
  ADMIN_PASSWORD_E2E,
  PASSWORD_E2E,
  recetaPorApi,
  registrarPorApi,
  galeriaPorApi,
  sesionPorApi,
  sufijo,
  tokenDeLogin,
} from './apoyo';

/**
 * Bloque 5 de mejoras (D-075): axe-core (WCAG 2.2 A y AA y buenas prácticas) en todas las pantallas, con datos, con
 * los modales y el menú abiertos, con errores de formulario y en móvil. Lighthouse usa el mismo motor, pero fue una
 * medición puntual (D-059) y no abre modales ni provoca errores; esta prueba se repite en cada cambio.
 */
const NORMAS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa', 'best-practice'];

/** Comprueba la página tal como está, cuando ya no hay nada cargando ni animándose. */
async function sinInfracciones(page: Page, pantalla: string): Promise<void> {
  await expect(page.locator('main .spinner-border')).toHaveCount(0);
  // animate.css: a mitad de un fundido, el contraste de color aún no es el final
  await page.waitForFunction(() =>
    document
      .getAnimations()
      .every((a) => a.playState !== 'running' || a.effect?.getTiming().iterations === Infinity),
  );
  const { violations } = await new AxeBuilder({ page }).withTags(NORMAS).analyze();
  const resumen = violations.map(
    (v) =>
      `${v.id} (${v.impact}): ${v.help}\n    ${v.nodes
        .slice(0, 5)
        .map((n) => n.target.join(' '))
        .join('\n    ')}`,
  );
  expect(resumen, `Infracciones de accesibilidad en ${pantalla}`).toEqual([]);
}

async function recorrer(page: Page, rutas: string[]): Promise<void> {
  for (const ruta of rutas) {
    await test.step(ruta, async () => {
      await page.goto(ruta);
      await expect(page.locator('main h1').first()).toBeVisible();
      await sinInfracciones(page, ruta);
    });
  }
}

/** Abre un modal, lo comprueba y lo cierra con Escape: el foco vuelve al botón que lo abrió. */
async function modal(page: Page, nombre: string, abrir: () => Promise<void>): Promise<void> {
  await test.step(`modal: ${nombre}`, async () => {
    await abrir();
    const dialogo = page.getByRole('dialog');
    await expect(dialogo).toBeVisible();
    await expect(dialogo.locator(':focus')).toHaveCount(1);
    await sinInfracciones(page, `modal ${nombre}`);
    await page.keyboard.press('Escape');
    await expect(page.getByRole('dialog')).toHaveCount(0);
  });
}

/** Usuario con una receta publicada, un «me gusta», un consumo de hoy y una comida planificada para mañana. */
async function usuarioConDatos(request: APIRequestContext) {
  const id = `e2e${sufijo()}`;
  const titulo = `Tortilla ${id}`;
  await registrarPorApi(request, id);
  const token = tokenDeLogin(
    await request.post('/api/auth/login', { data: { usuario: id, password: PASSWORD_E2E } }),
  );
  const receta = await recetaPorApi(request, token, titulo);
  const autorizacion = { Authorization: `Bearer ${token}` };
  expect((await request.post(`/api/recetas/${receta}/like`, { headers: autorizacion })).ok()).toBe(
    true,
  );
  const consumo = await request.post('/api/consumos', {
    headers: autorizacion,
    data: { receta, fecha: null },
  });
  expect(consumo.status(), await consumo.text()).toBe(201);
  const manana = new Date();
  manana.setDate(manana.getDate() + 1);
  manana.setHours(14, 0, 0, 0);
  const plan = await request.post('/api/plan', {
    headers: autorizacion,
    data: { receta, fecha: manana.toISOString() },
  });
  expect(plan.status(), await plan.text()).toBe(201);
  return { id, titulo, receta };
}

test('pantallas sin sesión', async ({ page }) => {
  await recorrer(page, [
    '/login',
    // Vuelta de Google (D-085): sin credenciales de Google ni sesión, sus mensajes de error
    '/login?google=bloqueada',
    '/login/google',
    '/login/google/vincular',
    '/registro/google',
    // El enlace para confirmar el email (D-088), con un token que no vale
    '/confirmar-email?token=no-vale',
    '/registro',
    '/olvido-password',
    '/restablecer-password?token=no-vale',
    '/ayuda',
    '/informacion-legal',
    '/sobre-nosotros',
    '/no-existe',
  ]);

  await test.step('formulario con errores', async () => {
    await page.goto('/registro');
    for (const campo of ['Nombre de usuario', 'Email', 'Contraseña']) {
      const control = page.getByLabel(campo, { exact: true });
      await control.fill('x');
      await control.fill('');
      await control.blur();
    }
    await expect(page.locator('.invalid-feedback').first()).toBeVisible();
    await sinInfracciones(page, 'registro con errores');
  });
});

test('pantallas con sesión y sus modales', async ({ page, request }) => {
  const { id, titulo, receta } = await usuarioConDatos(request);
  const token = await sesionPorApi(page, request, id);

  await recorrer(page, [
    '/',
    '/recetas',
    `/recetas/${receta}`,
    '/recetas/nueva',
    `/recetas/${receta}/editar`,
    '/mis-recetas',
    '/me-gustan',
    '/ingredientes',
    '/que-toca-hoy',
    '/planificador',
    '/planificador/compra',
    '/mi-alimentacion',
    '/mi-alimentacion/historico',
    '/mi-perfil',
  ]);

  await test.step('receta con galería de fotos, en el detalle y en el formulario (D-083)', async () => {
    await galeriaPorApi(request, token, receta);
    await page.goto(`/recetas/${receta}`);
    const galeria = page.locator('app-galeria-fotos');
    await expect(galeria.getByRole('button', { name: /^Ver la foto/ })).toHaveCount(2);
    await sinInfracciones(page, 'detalle con galería');
    await page.goto(`/recetas/${receta}/editar`);
    await expect(page.locator('app-editor-fotos li.foto')).toHaveCount(2);
    await sinInfracciones(page, 'formulario con fotos');
  });

  await test.step('receta ajena con su formulario de valoración y una valoración (D-077)', async () => {
    const otra = `e2eo${sufijo()}`;
    await registrarPorApi(request, otra);
    const tokenOtra = tokenDeLogin(
      await request.post('/api/auth/login', { data: { usuario: otra, password: PASSWORD_E2E } }),
    );
    const ajena = await recetaPorApi(request, tokenOtra, `Receta ajena ${otra}`);
    const valoracion = await request.put(`/api/recetas/${ajena}/valoracion`, {
      headers: { Authorization: `Bearer ${token}` },
      data: { nota: 4, comentario: 'Muy rica.' },
    });
    expect(valoracion.ok(), await valoracion.text()).toBe(true);
    await page.goto(`/recetas/${ajena}`);
    await expect(page.locator('li.valoracion')).toHaveCount(1);
    await sinInfracciones(page, 'receta ajena con valoraciones');
  });

  await test.step('información nutricional con «Cómo se calcula» abierto (D-079)', async () => {
    await page.goto(`/recetas/${receta}`);
    const nutricion = page.locator('app-informacion-nutricional');
    await expect(nutricion.locator('table')).toBeVisible();
    await nutricion.getByText('Cómo se calcula').click();
    await expect(nutricion.locator('details li')).toBeVisible();
    await sinInfracciones(page, 'información nutricional');
  });

  await test.step('menú de usuario abierto', async () => {
    await page.locator('#menu-usuario').click();
    await expect(page.locator('#menu-usuario')).toHaveAttribute('aria-expanded', 'true');
    await sinInfracciones(page, 'menú de usuario');
    await page.keyboard.press('Escape');
  });

  await page.goto('/ingredientes');
  await modal(page, 'nuevo ingrediente', () =>
    page.getByRole('button', { name: 'Nuevo ingrediente' }).click(),
  );
  await page.goto('/planificador');
  await modal(page, 'planificar', () =>
    page
      .getByRole('button', { name: /^Añadir Comida del/ })
      .first()
      .click(),
  );
  await page.goto('/mi-alimentacion/historico');
  await modal(page, 'editar consumo', () =>
    page.getByRole('button', { name: `Editar el consumo de ${titulo}` }).click(),
  );
  await page.goto(`/recetas/${receta}`);
  await modal(page, 'confirmar eliminación', () =>
    page.getByRole('button', { name: 'Eliminar', exact: true }).click(),
  );
  await page.goto('/mi-perfil');
  await modal(page, 'eliminar la cuenta', () =>
    page.getByRole('button', { name: 'Eliminar mi cuenta' }).click(),
  );
  await test.step('perfil cambiando el email (pide la contraseña, D-086)', async () => {
    await page.getByLabel('Email', { exact: true }).fill(`otro-${id}@e2e.local`);
    const password = page.getByLabel('Contraseña actual, para cambiar el email');
    await expect(password).toBeVisible();
    await sinInfracciones(page, 'perfil cambiando el email');
    // El email nuevo espera su confirmación (D-088): se puede cancelar
    await password.fill(PASSWORD_E2E);
    await page.getByRole('button', { name: 'Guardar los datos' }).click();
    await expect(page.getByText('Cambio pendiente: te hemos enviado un enlace a')).toBeVisible();
    await sinInfracciones(page, 'perfil con un cambio de email pendiente');
    await page.getByRole('button', { name: 'Cancelar el cambio' }).click();
    await expect(page.getByText('Cambio pendiente: te hemos enviado un enlace a')).toHaveCount(0);
  });
});

test('administración y sus modales', async ({ page, request }) => {
  const { id, titulo, receta } = await usuarioConDatos(request);
  await sesionPorApi(page, request, 'admin', ADMIN_PASSWORD_E2E);

  await recorrer(page, ['/admin/usuarios', '/admin/grupos', '/admin/recetas-ocultas']);

  await page.goto('/admin/usuarios');
  await page.getByLabel(/^Buscar por usuario/).fill(id);
  await page.getByLabel(/^Buscar por usuario/).press('Enter');
  await expect(page.getByText(`${id}@e2e.local`).first()).toBeVisible();
  await sinInfracciones(page, 'búsqueda de usuarios');

  await page.goto('/admin/grupos');
  await modal(page, 'nuevo grupo', () => page.getByRole('button', { name: 'Nuevo grupo' }).click());
  await page.goto('/ingredientes');
  await modal(page, 'fusionar ingrediente', () =>
    page
      .getByRole('button', { name: /^Fusionar .* con otro$/ })
      .first()
      .click(),
  );
  await modal(page, 'datos nutricionales con un alimento y una pieza (D-080, D-081)', async () => {
    await page
      .getByRole('button', { name: /^Datos nutricionales de / })
      .first()
      .click();
    const campo = page.getByRole('dialog').getByLabel('Alimento de BEDCA');
    await expect(campo).toBeFocused();
    await campo.fill('harina trigo');
    await page
      .getByRole('option', { name: /^Harina de trigo/ })
      .first()
      .click();
    await expect(page.getByRole('dialog').locator('caption')).toBeVisible();
    await page.getByRole('dialog').getByRole('button', { name: 'Añadir una pieza' }).click();
    await expect(page.getByRole('dialog').getByLabel('Pieza 1', { exact: true })).toBeVisible();
  });
  await page.goto(`/recetas/${receta}`);
  await expect(page.getByRole('heading', { name: titulo })).toBeVisible();
  await modal(page, 'ocultar receta', () =>
    page.getByRole('button', { name: 'Ocultar (moderación)' }).click(),
  );
});

test.describe('en móvil', () => {
  test.use({ viewport: { width: 390, height: 844 }, hasTouch: true });

  test('pantallas principales y menú desplegado', async ({ page, request }) => {
    const { id, receta } = await usuarioConDatos(request);
    await sesionPorApi(page, request, id);

    await recorrer(page, [
      '/',
      `/recetas/${receta}`,
      '/que-toca-hoy',
      '/planificador',
      '/mi-alimentacion',
    ]);

    await test.step('menú desplegado', async () => {
      await page.getByRole('button', { name: 'Abrir o cerrar el menú' }).click();
      await expect(page.locator('#menu-usuario')).toBeVisible();
      await sinInfracciones(page, 'menú móvil');
    });
  });
});
