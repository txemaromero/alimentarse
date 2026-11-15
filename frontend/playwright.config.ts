import { defineConfig } from '@playwright/test';
import { existsSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { ADMIN_PASSWORD_E2E } from './e2e/apoyo';

/**
 * Pruebas de extremo a extremo (12): los flujos principales contra la aplicación empaquetada.
 *
 * Por defecto arrancan el JAR (backend/target/alimentarse.jar, con el frontend dentro: «mvnw -Pfrontend package») en
 * el puerto 8081 con el perfil «e2e», que VACÍA la base de datos al arrancar. Por eso usa la de los tests del backend
 * (toca_test, o TEST_DB_URL del .env) o la que diga E2E_DB_URL, nunca la de desarrollo.
 *
 * Con E2E_URL se prueban contra una aplicación ya arrancada (sin vaciar nada).
 */

/** Variables del .env de la raíz (formato .properties sencillo). */
function leerEnv(ruta: string): Record<string, string> {
  if (!existsSync(ruta)) {
    return {};
  }
  return Object.fromEntries(
    readFileSync(ruta, 'utf8')
      .split(/\r?\n/)
      .filter((linea) => /^[A-Z0-9_]+=/.test(linea))
      .map((linea) => {
        const igual = linea.indexOf('=');
        return [linea.slice(0, igual), linea.slice(igual + 1).trim()];
      }),
  );
}

const env = leerEnv(join(__dirname, '..', '.env'));
const PUERTO = process.env['E2E_PUERTO'] ?? '8081';
const URL_EXTERNA = process.env['E2E_URL'];
/** La de los tests del backend: TEST_DB_URL o, si no está en el .env, el mismo valor por defecto que application-test.yml. */
const BASE_DATOS =
  process.env['E2E_DB_URL'] ?? env['TEST_DB_URL'] ?? 'jdbc:mysql://localhost:3307/toca_test';
const JAVA = process.env['JAVA_HOME']
  ? `"${join(process.env['JAVA_HOME'], 'bin', 'java')}"`
  : 'java';
/** En Windows se usa el Edge instalado; en otros sistemas, el Chromium de Playwright. */
const CANAL = process.env['E2E_NAVEGADOR'] ?? (process.platform === 'win32' ? 'msedge' : undefined);

export default defineConfig({
  testDir: './e2e',
  testMatch: '**/*.e2e.ts',
  // Los flujos comparten la base de datos: uno detrás de otro
  workers: 1,
  fullyParallel: false,
  timeout: 90_000,
  expect: { timeout: 10_000 },
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: URL_EXTERNA ?? `http://localhost:${PUERTO}`,
    channel: CANAL,
    locale: 'es-ES',
    timezoneId: 'Europe/Madrid',
    viewport: { width: 1280, height: 900 },
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  webServer: URL_EXTERNA
    ? undefined
    : {
        command: `${JAVA} -jar backend/target/alimentarse.jar`,
        cwd: join(__dirname, '..'),
        // 401 sin sesión: la aplicación ya responde
        url: `http://localhost:${PUERTO}/api/grupos`,
        timeout: 180_000,
        reuseExistingServer: false,
        stdout: 'ignore',
        stderr: 'pipe',
        env: {
          SPRING_PROFILES_ACTIVE: 'e2e',
          SERVER_PORT: PUERTO,
          DB_URL: BASE_DATOS,
          APP_BASE_URL: `http://localhost:${PUERTO}`,
          ADMIN_INITIAL_PASSWORD: ADMIN_PASSWORD_E2E,
          UPLOAD_DIR: 'backend/target/uploads-e2e',
        },
      },
});
