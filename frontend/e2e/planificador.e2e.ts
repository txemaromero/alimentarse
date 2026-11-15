import { expect, test } from '@playwright/test';
import { recetaPorApi, registrarPorApi, sesionPorApi, sufijo, aviso } from './apoyo';

/** Bloque 3 de mejoras: planificador semanal (D-068). */
test('planificar comidas, ver cómo quedaría la semana y marcarlas como comidas', async ({
  page,
  request,
}) => {
  const id = `e2e${sufijo()}`;
  const titulo = `Tortilla ${id}`;
  await registrarPorApi(request, id);
  const token = await sesionPorApi(page, request, id);
  // La receta lleva huevos (grupo Huevos, 4 raciones a la semana)
  await recetaPorApi(request, token, titulo);

  await page.goto('/planificador');
  const manana = page.locator('article.card').nth(1);
  const huevos = page.locator('app-indicador-grupo', { hasText: 'Huevos' });

  async function planificar(momento: 'Comida' | 'Cena') {
    await manana.getByRole('button', { name: new RegExp(`^Añadir ${momento} del`) }).click();
    const modal = page.getByRole('dialog');
    const buscador = modal.getByLabel('Busca una receta');
    await expect(buscador).toBeFocused();
    await buscador.fill(id);
    await page.getByRole('option', { name: titulo }).click();
    await expect(page.getByRole('dialog')).toHaveCount(0);
  }

  await test.step('Una comida planificada cuenta en los círculos', async () => {
    await expect(page.getByText('Añade alguna comida')).toBeVisible();
    await planificar('Comida');
    await expect(manana).toContainText(titulo);
    await expect(huevos).toContainText('Raciones 1/4');
  });

  await test.step('La segunda suma y se puede quitar', async () => {
    await planificar('Cena');
    await expect(huevos).toContainText('Raciones 2/4');
    await manana
      .getByRole('button', { name: `Quitar ${titulo} del planificador` })
      .last()
      .click();
    await expect(huevos).toContainText('Raciones 1/4');
  });

  await test.step('La lista de la compra reúne sus ingredientes (D-076)', async () => {
    await page.getByRole('link', { name: 'Lista de la compra' }).click();
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Lista de la compra');
    await expect(page.locator('section[aria-labelledby="titulo-comidas"]')).toContainText(titulo);

    // Lo marcado se recuerda al volver
    await page.getByRole('checkbox', { name: /^Huevos/ }).check();
    await page.reload();
    await expect(page.getByRole('checkbox', { name: /^Huevos/ })).toBeChecked();

    // Imprimir abre el diálogo del navegador, que aquí se sustituye para no bloquear la prueba
    await page.evaluate(() => {
      const ventana = window as unknown as { impresa: boolean };
      ventana.impresa = false;
      window.print = () => (ventana.impresa = true);
    });
    await page.getByRole('button', { name: 'Imprimir o guardar en PDF' }).click();
    expect(await page.evaluate(() => (window as unknown as { impresa: boolean }).impresa)).toBe(
      true,
    );

    await page.getByRole('link', { name: 'Volver al planificador' }).click();
    await expect(manana).toContainText(titulo);
  });

  await test.step('Al comerla pasa a los consumos y sigue contando', async () => {
    await manana.getByRole('button', { name: `Ya la he comido: ${titulo}` }).click();
    await expect(aviso(page, `«${titulo}» añadida a tus consumos.`)).toBeVisible();
    await expect(manana).not.toContainText(titulo);
    await expect(huevos).toContainText('Raciones 1/4');

    await page.goto('/mi-alimentacion');
    await expect(page.locator('section[aria-labelledby="titulo-ultimas"]')).toContainText(titulo);
  });
});
