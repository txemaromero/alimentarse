import { Route, Routes } from '@angular/router';
import { routes } from './app.routes';

interface Hoja {
  camino: string;
  ruta: Route;
}

/** Todas las rutas que pintan una pantalla, también las de los `loadChildren`, con su camino completo. */
async function hojas(rutas: Routes, padre = ''): Promise<Hoja[]> {
  const resultado: Hoja[] = [];
  for (const ruta of rutas) {
    const camino = [padre, ruta.path].filter(Boolean).join('/');
    if (ruta.children) {
      resultado.push(...(await hojas(ruta.children, camino)));
    } else if (ruta.loadChildren) {
      resultado.push(...(await hojas((await ruta.loadChildren()) as Routes, camino)));
    } else if (ruta.redirectTo === undefined) {
      resultado.push({ camino: `/${camino}`, ruta });
    }
  }
  return resultado;
}

describe('rutas', () => {
  // Un import diferido roto solo se notaría al abrir esa pantalla en el navegador
  it('cada pantalla carga su componente', async () => {
    const pantallas = await hojas(routes);

    expect(pantallas.length).toBeGreaterThan(20);
    for (const { camino, ruta } of pantallas) {
      const componente = ruta.loadComponent ? await ruta.loadComponent() : ruta.component;
      expect(componente, camino).toBeTypeOf('function');
    }
    // Importa todas las pantallas: con la cobertura activada tarda más que los 5 s por defecto
  }, 30_000);

  it('cada pantalla tiene título para la pestaña', async () => {
    for (const { camino, ruta } of await hojas(routes)) {
      expect(ruta.title, camino).toBeTruthy();
    }
  });

  it('las de administración cuelgan del guard de administrador', () => {
    const conSesion = routes.find((r) => r.path === '' && r.children);
    const admin = conSesion?.children?.find((r) => r.path === 'admin');

    expect(conSesion?.canActivateChild?.length).toBe(1);
    expect(admin?.canActivateChild?.length).toBe(1);
    expect(admin?.children?.map((r) => r.path)).toEqual([
      '',
      'usuarios',
      'grupos',
      'recetas-ocultas',
    ]);
  });
});
