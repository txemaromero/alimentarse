import { expect, test } from '@playwright/test';
import {
  ADMIN_PASSWORD_E2E,
  anchoDelDocumento,
  recetaPorApi,
  registrarPorApi,
  sesionPorApi,
  sufijo,
  tokenDeLogin,
} from './apoyo';

/**
 * Revisión de responsive (12): ninguna pantalla tiene scroll horizontal a 375, 768 y 1280 px. Se adjunta una captura
 * de cada una al informe (playwright-report). Con las pantallas añadidas después, en el bloque de cierre.
 */
const ANCHOS = [375, 768, 1280];
const PUBLICAS = [
  '/login',
  '/registro',
  '/olvido-password',
  '/ayuda',
  '/informacion-legal',
  // Acceso con Google (D-085, D-086), sin nada pendiente: sus mensajes
  '/login?google=otra-cuenta',
  '/login/google',
  '/login/google/vincular',
  '/registro/google',
  '/confirmar-email?token=no-vale',
];
const DE_ADMINISTRACION = ['/admin/usuarios', '/admin/grupos', '/admin/recetas-ocultas'];

test.describe('responsive', () => {
  let usuario: string;
  let receta: number;

  test.beforeAll(async ({ request }) => {
    usuario = `e2e${sufijo()}`;
    await registrarPorApi(request, usuario);
    const login = await request.post('/api/auth/login', {
      data: { usuario, password: 'clave-e2e-1234' },
    });
    const token = tokenDeLogin(login);
    receta = await recetaPorApi(request, token, `Tortilla responsive ${usuario}`);
    const consumo = await request.post('/api/consumos', {
      headers: { Authorization: `Bearer ${token}` },
      data: { receta },
    });
    expect(consumo.status()).toBe(201);
  });

  for (const ancho of ANCHOS) {
    test(`sin scroll horizontal a ${ancho} px`, async ({ page, request }, info) => {
      await page.setViewportSize({ width: ancho, height: 800 });

      for (const ruta of PUBLICAS) {
        await page.goto(ruta);
        await page.waitForLoadState('networkidle');
        expect(await anchoDelDocumento(page), `${ruta} a ${ancho} px`).toBeLessThanOrEqual(ancho);
      }

      await sesionPorApi(page, request, usuario);
      const privadas = [
        '/',
        '/recetas',
        `/recetas/${receta}`,
        '/recetas/nueva',
        '/mis-recetas',
        '/me-gustan',
        '/ingredientes',
        '/que-toca-hoy',
        '/planificador',
        '/planificador/compra',
        '/mi-alimentacion',
        '/mi-alimentacion/historico',
        '/mi-perfil',
      ];
      for (const ruta of privadas) {
        await page.goto(ruta);
        await page.waitForLoadState('networkidle');
        await expect(page.locator('main h1').first()).toBeVisible();
        expect(await anchoDelDocumento(page), `${ruta} a ${ancho} px`).toBeLessThanOrEqual(ancho);
        await info.attach(`${ancho}px ${ruta}`, {
          body: await page.screenshot({ fullPage: true }),
          contentType: 'image/png',
        });
      }

      await sesionPorApi(page, request, 'admin', ADMIN_PASSWORD_E2E);
      for (const ruta of DE_ADMINISTRACION) {
        await page.goto(ruta);
        await page.waitForLoadState('networkidle');
        await expect(page.locator('main h1').first()).toBeVisible();
        expect(await anchoDelDocumento(page), `${ruta} a ${ancho} px`).toBeLessThanOrEqual(ancho);
        await info.attach(`${ancho}px ${ruta}`, {
          body: await page.screenshot({ fullPage: true }),
          contentType: 'image/png',
        });
      }
    });
  }
});
