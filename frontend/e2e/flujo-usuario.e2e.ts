import { expect, test } from '@playwright/test';
import { PASSWORD_E2E, filtrarRecetasPorTitulo, iniciarSesion, sufijo, aviso } from './apoyo';

/**
 * Flujo principal de un usuario (12): registro → login → crear receta → publicar → «me gusta» → consumir →
 * Mi alimentación → la recomendación excluye el grupo agotado.
 *
 * La receta lleva chocolate negro (grupo Dulces, 1 ración a la semana): tras consumirla una vez, Dulces queda
 * agotado y «Qué toca hoy» deja de recomendarla.
 */
test('de registrarse a que la recomendación excluya un grupo agotado', async ({ page }) => {
  const id = `e2e${sufijo()}`;
  const titulo = `Bizcocho de chocolate ${id}`;

  await test.step('Registro', async () => {
    await page.goto('/registro');
    await page.getByLabel('Nombre de usuario').fill(id);
    await page.getByLabel('Nombre', { exact: true }).fill('Ana');
    await page.getByLabel('Primer apellido').fill('Prueba');
    await page.getByLabel('Email').fill(`${id}@e2e.local`);
    await page.locator('#registro-password').fill(PASSWORD_E2E);
    await page.getByLabel('Repite la contraseña').fill(PASSWORD_E2E);
    await page.getByRole('button', { name: 'Registrarse' }).click();
    await expect(page).toHaveURL(/\/login$/);
    await expect(aviso(page, 'Cuenta creada, Ana.')).toBeVisible();
  });

  await test.step('Inicio de sesión', async () => {
    await iniciarSesion(page, id, PASSWORD_E2E);
    await expect(page.getByRole('heading', { name: '¿Qué toca hoy?' })).toBeVisible();
    await expect(page.locator('#menu-usuario')).toContainText('Bienvenido, Ana');

    // La sesión va en una cookie que el JavaScript no puede leer (D-070)
    const cookie = (await page.context().cookies()).find((c) => c.name === 'toca_sesion');
    expect(cookie?.httpOnly).toBe(true);
    expect(cookie?.sameSite).toBe('Strict');
    expect(await page.evaluate(() => localStorage.getItem('alimentarse.sesion'))).not.toContain(
      cookie!.value,
    );
  });

  await test.step('Crear receta', async () => {
    await page.locator('#menu-usuario').click();
    await page.getByRole('link', { name: 'Mis recetas' }).click();
    await page.getByRole('link', { name: 'Nueva receta' }).click();
    await expect(page.getByRole('heading', { name: 'Nueva receta' })).toBeVisible();

    await page.getByLabel('Título', { exact: true }).fill(titulo);
    for (const [ingrediente, cantidad] of [
      ['Chocolate negro', '150 gramos'],
      ['Huevos', '4 unidades'],
    ]) {
      await page.getByLabel('Ingrediente', { exact: true }).fill(ingrediente.slice(0, 6));
      await page.getByRole('option', { name: ingrediente }).first().click();
      await page.getByLabel('Cantidad').fill(cantidad);
      await page.getByRole('button', { name: 'Añadir', exact: true }).click();
    }
    await expect(page.locator('.chip')).toHaveCount(2);

    await page.locator('.editor-html-contenido').click();
    await page.keyboard.type('Fundir el chocolate y mezclarlo con los huevos batidos.');
    await page.getByLabel('Tipo').selectOption({ label: 'Postre' });
    await page.getByLabel('Tiempo (min)').fill('40');
    await page.getByLabel('Comensales').fill('6');
    await page.locator('label[for="receta-dificultad-1"]').click();

    await page.getByRole('button', { name: 'Guardar' }).click();
    await expect(page).toHaveURL(/\/recetas\/\d+$/);
    await expect(page.getByRole('heading', { level: 1 })).toHaveText(titulo);
    await expect(page.getByText('No publicada')).toBeVisible();
  });
  const urlReceta = page.url();

  await test.step('Publicar', async () => {
    await page.getByRole('button', { name: 'Publicar' }).click();
    await expect(page.getByRole('button', { name: 'Dejar de publicar' })).toBeVisible();
  });

  await test.step('Se recomienda mientras Dulces no está agotado', async () => {
    await page.goto('/que-toca-hoy');
    await filtrarRecetasPorTitulo(page, id);
    await expect(page.locator('app-tarjeta-receta')).toHaveCount(1);
    await expect(page.locator('app-tarjeta-receta')).toContainText(titulo);
  });

  await test.step('Me gusta', async () => {
    await page.goto(urlReceta);
    const corazon = page.locator('aside .boton-me-gusta');
    await corazon.click();
    await expect(corazon).toHaveAttribute('aria-pressed', 'true');
    await expect(page.getByText('1 persona le ha dado «me gusta»')).toBeVisible();
  });

  await test.step('Añadir a consumidos', async () => {
    // Antes de comerla se avisa de cómo quedarían las raciones (D-067)
    const impacto = page.locator('.impacto');
    await expect(impacto).toContainText('Dulces: de 0/1 a 1/1');
    await expect(impacto).toContainText('llegarás al límite');
    await page.getByRole('button', { name: 'Añadir a consumidos' }).click();
    await expect(aviso(page, /Añadida a tus consumos el/)).toBeVisible();
    await expect(impacto).toContainText('te pasarás del límite');
  });

  await test.step('Mi alimentación', async () => {
    await page.locator('#menu-usuario').click();
    await page.getByRole('link', { name: 'Mi alimentación' }).click();
    const ultimas = page.locator('section[aria-labelledby="titulo-ultimas"]');
    await expect(ultimas).toContainText(titulo);
    await expect(ultimas).toContainText('Receta consumida el');
    const dulces = page.locator('app-indicador-grupo', { hasText: 'Dulces' });
    await expect(dulces).toContainText('Raciones 1/1');
    await expect(dulces).toContainText('límite alcanzado');
  });

  await test.step('La recomendación excluye el grupo agotado', async () => {
    await page.goto('/que-toca-hoy');
    await expect(page.locator('.alert-info')).toContainText(
      'Grupos completados esta semana: Dulces.',
    );
    await filtrarRecetasPorTitulo(page, id);
    await expect(
      page.getByText('Ninguna receta recomendada coincide con los filtros.'),
    ).toBeVisible();
    await expect(page.locator('app-tarjeta-receta')).toHaveCount(0);
  });

  await test.step('Cerrar sesión borra la cookie', async () => {
    await page.locator('#menu-usuario').click();
    await page.getByRole('button', { name: 'Cerrar sesión' }).click();
    await expect(page).toHaveURL(/\/login/);
    expect((await page.context().cookies()).some((c) => c.name === 'toca_sesion')).toBe(false);
  });
});
