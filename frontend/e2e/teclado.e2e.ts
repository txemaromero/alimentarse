import { expect, test } from '@playwright/test';
import { PASSWORD_E2E, registrarPorApi, sufijo } from './apoyo';

/** Bloque 5 de mejoras (D-075): la aplicación se puede usar solo con el teclado y el foco va donde se espera. */
test('uso solo con teclado', async ({ page, request }) => {
  const id = `e2e${sufijo()}`;
  await registrarPorApi(request, id);

  await test.step('«Saltar al contenido» es lo primero y lleva al contenido', async () => {
    await page.goto('/login');
    await page.keyboard.press('Tab');
    const saltar = page.getByRole('button', { name: 'Saltar al contenido' });
    await expect(saltar).toBeFocused();
    await expect(saltar).toBeVisible();
    await page.keyboard.press('Enter');
    await expect(page.locator('main#contenido')).toBeFocused();
  });

  await test.step('al iniciar sesión, el foco pasa al título de la portada', async () => {
    await page.getByLabel('Usuario o email').focus();
    await page.keyboard.type(id);
    await page.locator('#login-password').focus();
    await page.keyboard.type(PASSWORD_E2E);
    await page.keyboard.press('Enter');
    await expect(page).toHaveURL(/\/$/);
    await expect(page.locator('main h1')).toBeFocused();
  });

  await test.step('un enlace del menú lleva el foco al título de la nueva pantalla', async () => {
    await page.locator('nav').getByRole('link', { name: 'Qué toca hoy' }).focus();
    await page.keyboard.press('Enter');
    await expect(page).toHaveURL(/\/que-toca-hoy$/);
    await expect(page.getByRole('heading', { level: 1 })).toBeFocused();
  });

  await test.step('un modal retiene el foco y lo devuelve al cerrarse', async () => {
    await page.goto('/ingredientes');
    const nuevo = page.getByRole('button', { name: 'Nuevo ingrediente' });
    await nuevo.focus();
    await page.keyboard.press('Enter');
    const dialogo = page.getByRole('dialog');
    await expect(dialogo).toBeVisible();
    for (let i = 0; i < 8; i++) {
      await page.keyboard.press('Tab');
      await expect(dialogo.locator(':focus')).toHaveCount(1);
    }
    await page.keyboard.press('Escape');
    await expect(page.getByRole('dialog')).toHaveCount(0);
    await expect(nuevo).toBeFocused();
  });

  await test.step('el menú de usuario se abre, se recorre y se cierra con el teclado', async () => {
    const menu = page.locator('#menu-usuario');
    await menu.focus();
    await page.keyboard.press('Enter');
    await expect(menu).toHaveAttribute('aria-expanded', 'true');
    await page.keyboard.press('ArrowDown');
    await expect(page.locator('[ngbdropdownitem]:focus, .dropdown-item:focus')).toHaveCount(1);
    await page.keyboard.press('Escape');
    await expect(menu).toHaveAttribute('aria-expanded', 'false');
    await expect(menu).toBeFocused();
  });
});
