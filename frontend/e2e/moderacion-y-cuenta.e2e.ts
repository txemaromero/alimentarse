import { expect, test } from '@playwright/test';
import {
  ADMIN_PASSWORD_E2E,
  PASSWORD_E2E,
  iniciarSesion,
  recetaPorApi,
  registrarPorApi,
  sesionPorApi,
  sufijo,
  aviso,
} from './apoyo';

/** Bloque 2 de mejoras: moderación (D-063). */
test('el administrador oculta una receta y bloquea a su autor', async ({
  page,
  request,
  browser,
}) => {
  const autor = `e2e${sufijo()}`;
  const titulo = `Receta dudosa ${autor}`;
  await registrarPorApi(request, autor);
  const contextoAutor = await browser.newContext();
  const paginaAutor = await contextoAutor.newPage();
  const token = await sesionPorApi(paginaAutor, request, autor);
  const receta = await recetaPorApi(request, token, titulo);

  await test.step('El administrador la oculta con un motivo', async () => {
    await iniciarSesion(page, 'admin', ADMIN_PASSWORD_E2E);
    await page.goto(`/recetas/${receta}`);
    await page.getByRole('button', { name: 'Ocultar (moderación)' }).click();
    const modal = page.getByRole('dialog');
    await modal.getByLabel('Motivo').fill('Publicidad encubierta');
    await modal.getByRole('button', { name: 'Ocultar' }).click();
    await expect(page.getByRole('dialog')).toHaveCount(0);
    await expect(page.getByText('Motivo: Publicidad encubierta')).toBeVisible();
  });

  await test.step('Su autor la ve, con el motivo', async () => {
    await paginaAutor.goto(`/recetas/${receta}`);
    await expect(paginaAutor.getByText('La administración ha ocultado esta receta.')).toBeVisible();
    await expect(paginaAutor.getByText('Motivo: Publicidad encubierta')).toBeVisible();
  });

  await test.step('El administrador bloquea al autor', async () => {
    await page.locator('#menu-admin').click();
    await page.getByRole('link', { name: 'Usuarios' }).click();
    await page.getByLabel('Buscar por usuario, nombre, apellidos o email').fill(autor);
    await page.getByRole('button', { name: 'Buscar' }).click();
    const bloqueo = page.locator(`table [aria-label="Bloqueado: ${autor}"]`);
    await bloqueo.click();
    await expect(bloqueo).toHaveAttribute('aria-pressed', 'true');
  });

  await test.step('La sesión abierta del autor se cierra al momento', async () => {
    await paginaAutor.goto('/mis-recetas');
    await expect(paginaAutor).toHaveURL(/\/login/);
    // Con aviso(): el texto también está en la región viva del Anunciador, y getByText puede dar dos elementos
    await expect(
      aviso(paginaAutor, 'Tu cuenta está bloqueada por la administración.'),
    ).toBeVisible();
  });
  await contextoAutor.close();
});

/** Bloque 2 de mejoras: «me gusta», eliminar una receta y darse de baja (D-061, D-062 y D-064). */
test('un usuario guarda una receta, elimina la suya y se da de baja', async ({ page, request }) => {
  const id = `e2e${sufijo()}`;
  const titulo = `Mi receta ${id}`;
  await registrarPorApi(request, id);
  const token = await sesionPorApi(page, request, id);
  const receta = await recetaPorApi(request, token, titulo);

  await test.step('«Me gusta» y «Recetas que me gustan»', async () => {
    await page.goto(`/recetas/${receta}`);
    const corazon = page.getByRole('button', { name: 'Me gusta' });
    await corazon.click();
    await expect(corazon).toHaveAttribute('aria-pressed', 'true');
    await page.goto('/me-gustan');
    await expect(page.getByRole('heading', { name: 'Recetas que me gustan' })).toBeVisible();
    await expect(page.locator('app-tarjeta-receta', { hasText: titulo })).toBeVisible();
  });

  await test.step('Eliminar la receta desde «Mis recetas»', async () => {
    await page.goto('/mis-recetas');
    await page.getByRole('button', { name: `Eliminar ${titulo}` }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Sí, eliminar' }).click();
    await expect(aviso(page, `«${titulo}» se ha eliminado.`)).toBeVisible();
    await expect(page.locator('app-tarjeta-receta', { hasText: titulo })).toHaveCount(0);
  });

  await test.step('Descargar los datos', async () => {
    await page.goto('/mi-perfil');
    const [descarga] = await Promise.all([
      page.waitForEvent('download'),
      page.getByRole('button', { name: 'Descargar mis datos' }).click(),
    ]);
    expect(descarga.suggestedFilename()).toBe(`alimentarse-${id}.json`);
  });

  await test.step('Darse de baja', async () => {
    await page.getByRole('button', { name: 'Eliminar mi cuenta' }).click();
    const modal = page.getByRole('dialog');
    const campo = modal.getByLabel('Escribe tu contraseña para confirmarlo');
    // Con el foco puesto (ngbAutofocus), el formulario del modal ya está listo y no borra lo escrito
    await expect(campo).toBeFocused();
    await campo.fill(PASSWORD_E2E);
    await expect(campo).toHaveValue(PASSWORD_E2E);
    await modal.getByRole('button', { name: 'Eliminar mi cuenta' }).click();
    await expect(page).toHaveURL(/\/login/);
    await expect(aviso(page, 'Tu cuenta se ha eliminado.')).toBeVisible();
  });

  const login = await request.post('/api/auth/login', {
    data: { usuario: id, password: PASSWORD_E2E },
  });
  expect(login.status()).toBe(401);
});
