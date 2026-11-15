import { expect, test } from '@playwright/test';
import {
  PASSWORD_E2E,
  fotoDePrueba,
  recetaPorApi,
  registrarPorApi,
  sesionPorApi,
  sufijo,
  tokenDeLogin,
} from './apoyo';

/** Bloque 6: la galería de fotos de una receta (D-083). */
test('añadir fotos y un vídeo a una receta, ordenarlas y verlas en la galería', async ({
  page,
  request,
}) => {
  const id = `e2eg${sufijo()}`;
  const titulo = `Galería ${id}`;
  await registrarPorApi(request, id);
  const token = tokenDeLogin(
    await request.post('/api/auth/login', { data: { usuario: id, password: PASSWORD_E2E } }),
  );
  const receta = await recetaPorApi(request, token, titulo);
  await sesionPorApi(page, request, id);

  await test.step('Añadir dos fotos y subir la segunda para que sea la principal', async () => {
    await page.goto(`/recetas/${receta}/editar`);
    await expect(page.getByLabel('Título', { exact: true })).toHaveValue(titulo);
    await page
      .locator('#receta-fotos')
      .setInputFiles([fotoDePrueba('demo-tortilla.jpg'), fotoDePrueba('demo-batido.jpg')]);
    const fotos = page.locator('app-editor-fotos li.foto');
    await expect(fotos).toHaveCount(2);
    await expect(fotos.first()).toContainText('Principal');

    await page.getByLabel('Descripción de la foto 2').fill('El batido servido');
    await page.getByRole('button', { name: 'Subir la foto 2' }).click();
    // Ya no se puede subir más: el foco pasa a «Bajar» de la misma foto
    await expect(page.getByRole('button', { name: 'Bajar la foto 1' })).toBeFocused();
    await expect(page.getByLabel('Descripción de la foto 1')).toHaveValue('El batido servido');
    await page.getByLabel('Vídeo').fill('https://www.youtube.com/watch?v=e2e');

    await page.getByRole('button', { name: 'Guardar', exact: true }).click();
    await expect(page).toHaveURL(new RegExp(`/recetas/${receta}$`));
  });

  await test.step('Verlas en el detalle', async () => {
    const galeria = page.locator('app-galeria-fotos');
    const foto = galeria.locator('.marco-imagen img');
    await expect(foto).toHaveAttribute('alt', 'El batido servido');
    await expect(galeria.getByRole('button', { name: /^Ver la foto/ })).toHaveCount(2);

    await galeria.getByRole('button', { name: 'Foto siguiente' }).click();
    await expect(foto).toHaveAttribute('alt', `${titulo}: foto 2 de 2`);
    await expect(galeria.getByRole('button', { name: 'Ver la foto 2 de 2' })).toHaveAttribute(
      'aria-current',
      'true',
    );

    const video = page.getByRole('link', { name: /^Ver el vídeo en YouTube/ });
    await expect(video).toHaveAttribute('href', 'https://www.youtube.com/watch?v=e2e');
    await expect(video).toHaveAttribute('target', '_blank');

    // Se sirven recodificadas a JPEG (D-069)
    const src = (await foto.getAttribute('src'))!;
    expect((await request.get(src)).headers()['content-type']).toBe('image/jpeg');
  });

  await test.step('La principal es la de la tarjeta', async () => {
    const detalle = await (
      await request.get(`/api/recetas/${receta}`, {
        headers: { Authorization: `Bearer ${token}` },
      })
    ).json();
    expect(detalle.imagen).toBe(detalle.fotos[0].url);

    await page.goto('/mis-recetas');
    const tarjeta = page.locator('app-tarjeta-receta', { hasText: titulo });
    await expect(tarjeta.locator('img')).toHaveAttribute(
      'src',
      new RegExp(detalle.fotos[0].nombre),
    );
  });
});
