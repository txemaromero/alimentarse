/** Lo marcado en la lista de la compra, en localStorage (D-076). */
export const CLAVE_MARCAS_COMPRA = 'alimentarse.compra';

/**
 * Vacía las respuestas de la API que el service worker guarda para funcionar sin conexión (dataGroups de
 * ngsw-config.json) y lo marcado en la lista de la compra. Se llama al iniciar y al cerrar sesión: nada de eso distingue
 * usuarios, así que en un equipo compartido otra persona podría verlo.
 *
 * Se borran las entradas y no las cachés: el service worker guarda una referencia a cada caché abierta y seguiría
 * usándola hasta reiniciarse.
 */
export async function borrarDatosGuardados(
  almacen: CacheStorage | undefined = globalThis.caches,
): Promise<void> {
  try {
    localStorage.removeItem(CLAVE_MARCAS_COMPRA);
  } catch {
    // Sin acceso al almacenamiento local: no hay nada que borrar
  }
  if (!almacen) {
    return;
  }
  try {
    const nombres = (await almacen.keys()).filter(
      (nombre) => nombre.startsWith('ngsw:') && /:data:[^:]+:cache$/.test(nombre),
    );
    for (const nombre of nombres) {
      const cache = await almacen.open(nombre);
      for (const peticion of await cache.keys()) {
        await cache.delete(peticion);
      }
    }
  } catch {
    // Sin acceso a la Cache Storage (modo privado estricto…): no hay nada que borrar
  }
}
