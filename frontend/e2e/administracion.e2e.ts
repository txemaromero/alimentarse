import { expect, test } from '@playwright/test';
import {
  ADMIN_PASSWORD_E2E,
  PASSWORD_E2E,
  iniciarSesion,
  registrarPorApi,
  sufijo,
  aviso,
} from './apoyo';

/**
 * Flujo de administración (12): el administrador da el rol a un usuario, edita un ingrediente y le asigna un alimento
 * de BEDCA (D-080).
 */
test('el administrador da el rol de administrador y edita un ingrediente', async ({
  page,
  request,
  browser,
}) => {
  const id = `e2e${sufijo()}`;
  const ingrediente = `Ingrediente ${id}`;
  await registrarPorApi(request, id);

  await test.step('Inicio de sesión del administrador', async () => {
    await iniciarSesion(page, 'admin', ADMIN_PASSWORD_E2E);
    await expect(page.locator('#menu-admin')).toBeVisible();
  });

  await test.step('Dar el rol de administrador', async () => {
    await page.locator('#menu-admin').click();
    await page.getByRole('link', { name: 'Usuarios' }).click();
    await expect(page.getByRole('heading', { name: 'Gestión de usuarios' })).toBeVisible();
    await page.getByLabel('Buscar por usuario, nombre, apellidos o email').fill(id);
    await page.getByRole('button', { name: 'Buscar' }).click();
    const interruptor = page.locator(`table [aria-label="Administrador: ${id}"]`);
    await expect(interruptor).toHaveAttribute('aria-pressed', 'false');
    await interruptor.click();
    await expect(interruptor).toHaveAttribute('aria-pressed', 'true');
    await expect(aviso(page, `${id} ya es administrador.`)).toBeVisible();
  });

  await test.step('El usuario, al volver a entrar, es administrador', async () => {
    const contexto = await browser.newContext();
    const otra = await contexto.newPage();
    await iniciarSesion(otra, id, PASSWORD_E2E);
    await expect(otra.locator('#menu-admin')).toBeVisible();
    await contexto.close();
  });

  await test.step('Crear un ingrediente', async () => {
    await page.getByRole('link', { name: 'Ingredientes' }).click();
    await page.getByRole('button', { name: 'Nuevo ingrediente' }).click();
    const modal = page.getByRole('dialog');
    await modal.getByLabel('Nombre').fill(ingrediente);
    await modal.getByLabel('Grupo de alimentos').selectOption({ label: 'Fruta (sin límite)' });
    await modal.getByRole('button', { name: 'Guardar' }).click();
    await expect(aviso(page, `Ingrediente «${ingrediente}» creado.`)).toBeVisible();
    await expect(page.getByRole('dialog')).toHaveCount(0);
  });

  await test.step('Editarlo como administrador', async () => {
    await page.getByRole('searchbox', { name: 'Nombre' }).fill(id);
    await page.getByRole('button', { name: 'Filtrar' }).click();
    const tarjeta = page.locator('.tarjeta-ingrediente', { hasText: ingrediente });
    await expect(tarjeta).toContainText('Fruta');
    await tarjeta.getByRole('button', { name: `Editar ${ingrediente}` }).click();

    const modal = page.getByRole('dialog');
    await expect(modal.getByRole('heading', { name: 'Editar ingrediente' })).toBeVisible();
    await modal.getByLabel('Nombre').fill(`${ingrediente} editado`);
    await modal.getByLabel('Grupo de alimentos').selectOption({ label: 'Verduras (sin límite)' });
    await modal.getByRole('button', { name: 'Guardar' }).click();
    await expect(page.getByRole('dialog')).toHaveCount(0);

    const editada = page.locator('.tarjeta-ingrediente', { hasText: `${ingrediente} editado` });
    await expect(editada).toContainText('Verduras');
  });

  await test.step('Darle un alimento de BEDCA y el peso de una pieza (D-080, D-081)', async () => {
    const tarjeta = page.locator('.tarjeta-ingrediente', { hasText: `${ingrediente} editado` });
    await expect(tarjeta).toContainText('Sin datos nutricionales');
    await tarjeta
      .getByRole('button', { name: `Datos nutricionales de ${ingrediente} editado` })
      .click();

    const modal = page.getByRole('dialog');
    const campo = modal.getByLabel('Alimento de BEDCA');
    await expect(campo).toBeFocused();
    await campo.fill('espinaca cruda');
    await page.getByRole('option', { name: /Espinaca, picada, congelada, cruda/ }).click();
    await expect(modal.locator('caption')).toContainText('Espinaca, picada, congelada, cruda');
    await modal.getByRole('button', { name: 'Añadir una pieza' }).click();
    await modal.getByLabel('Pieza 1', { exact: true }).fill('manojo');
    await modal.getByLabel('Gramos de la pieza 1').fill('250');
    await modal.getByRole('button', { name: 'Guardar' }).click();
    await expect(page.getByRole('dialog')).toHaveCount(0);
    await expect(tarjeta).toContainText('BEDCA: Espinaca, picada, congelada, cruda');

    // Al volver a abrirlo, están el alimento y la pieza
    await tarjeta
      .getByRole('button', { name: `Datos nutricionales de ${ingrediente} editado` })
      .click();
    await expect(modal.getByLabel('Alimento de BEDCA')).toHaveValue(
      'Espinaca, picada, congelada, cruda',
    );
    await expect(modal.getByLabel('Pieza 1', { exact: true })).toHaveValue('manojo');
    await modal.getByRole('button', { name: 'Cancelar' }).click();
    await expect(page.getByRole('dialog')).toHaveCount(0);
  });
});
