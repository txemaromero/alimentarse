import { expect, test } from '@playwright/test';
import {
  ADMIN_PASSWORD_E2E,
  PASSWORD_E2E,
  aviso,
  filtrarRecetasPorTitulo,
  iniciarSesion,
  recetaPorApi,
  registrarPorApi,
  sesionPorApi,
  sufijo,
  tokenDeLogin,
} from './apoyo';

/** Bloque 6: valoraciones de las recetas (D-077). */
test('valorar una receta ajena, cambiar la nota, la moderación y borrarla', async ({
  page,
  request,
  browser,
}) => {
  const id = sufijo();
  const autora = `e2ea${id}`;
  const lectora = `e2el${id}`;
  const titulo = `Bizcocho ${id}`;
  await registrarPorApi(request, autora);
  const token = tokenDeLogin(
    await request.post('/api/auth/login', { data: { usuario: autora, password: PASSWORD_E2E } }),
  );
  const receta = await recetaPorApi(request, token, titulo);
  await registrarPorApi(request, lectora);
  await sesionPorApi(page, request, lectora);

  const seccion = page.locator('app-valoraciones');
  const estrella = (n: number) => seccion.locator(`label[for="valoracion-nota-${n}"]`);

  await test.step('Valorar con nota y comentario', async () => {
    await page.goto(`/recetas/${receta}`);
    await expect(seccion).toContainText('Aún no tiene valoraciones.');
    await expect(seccion.getByRole('heading', { name: 'Valora esta receta' })).toBeVisible();
    await estrella(4).click();
    await expect(seccion.getByRole('radio', { name: '4 estrellas' })).toBeChecked();
    await seccion.getByLabel('Comentario').fill('Esponjoso y nada seco.');
    await seccion.getByRole('button', { name: 'Guardar valoración' }).click();

    await expect(aviso(page, 'Valoración guardada.')).toBeVisible();
    await expect(seccion.locator('.resumen')).toContainText('4,0');
    await expect(seccion.locator('li.valoracion')).toContainText('Esponjoso y nada seco.');
    await expect(seccion.getByRole('heading', { name: 'Tu valoración' })).toBeVisible();
  });

  await test.step('Cambiarla sustituye la anterior', async () => {
    await estrella(5).click();
    await seccion.getByRole('button', { name: 'Guardar valoración' }).click();
    await expect(seccion.locator('.resumen')).toContainText('5,0');
    await expect(seccion.locator('li.valoracion')).toHaveCount(1);
  });

  await test.step('La tarjeta muestra la nota media', async () => {
    await page.goto('/recetas');
    await filtrarRecetasPorTitulo(page, titulo);
    // Hasta que llega el resultado filtrado se ven las tarjetas de antes (y una comprobación estricta no espera)
    await expect(page.locator('app-tarjeta-receta')).toHaveCount(1);
    await expect(page.locator('app-tarjeta-receta .valoracion')).toContainText('5,0 (1)');
  });

  await test.step('La administración la oculta', async () => {
    const contexto = await browser.newContext();
    const admin = await contexto.newPage();
    await iniciarSesion(admin, 'admin', ADMIN_PASSWORD_E2E);
    await admin.goto(`/recetas/${receta}`);
    await admin.getByRole('button', { name: `Ocultar la valoración de ${lectora}` }).click();
    await expect(aviso(admin, 'ya no cuenta en la media')).toBeVisible();
    await expect(admin.locator('app-valoraciones')).toContainText('Aún no tiene valoraciones.');
    await expect(admin.locator('li.valoracion')).toContainText('Oculta por la administración');
    await contexto.close();
  });

  await test.step('Su autora lo ve y la borra', async () => {
    await page.goto(`/recetas/${receta}`);
    await expect(seccion.locator('form .alert-warning')).toContainText(
      'La administración ha ocultado tu valoración',
    );
    await seccion.getByRole('button', { name: 'Borrar mi valoración' }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Borrar' }).click();
    await expect(page.getByRole('dialog')).toHaveCount(0);
    await expect(aviso(page, 'Valoración borrada.')).toBeVisible();
    await expect(seccion.getByRole('heading', { name: 'Valora esta receta' })).toBeVisible();
  });
});
