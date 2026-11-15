/** Anchos en los que el servidor sirve cada imagen (D-069). */
const ANCHOS = [400, 800, 1200] as const;

export type AnchoImagen = (typeof ANCHOS)[number];

/** URL de la imagen reducida a ese ancho. */
export function urlImagen(url: string, ancho: AnchoImagen): string {
  return `${url}?ancho=${ancho}`;
}

/** Valor de `srcset`: el navegador elige según el tamaño en pantalla y la densidad del dispositivo. */
export function srcsetImagen(url: string): string {
  return ANCHOS.map((ancho) => `${urlImagen(url, ancho)} ${ancho}w`).join(', ');
}

/** Tipos de imagen que admite el servidor (D-069). */
export const TIPOS_IMAGEN = ['image/jpeg', 'image/png', 'image/webp'];
export const MAX_BYTES_IMAGEN = 5 * 1024 * 1024;

/** Error de la imagen elegida, o null si se puede subir (el backend vuelve a comprobar el contenido real). */
export function errorImagen(fichero: File): string | null {
  if (!TIPOS_IMAGEN.includes(fichero.type)) {
    return 'La imagen debe ser JPEG, PNG o WebP';
  }
  if (fichero.size > MAX_BYTES_IMAGEN) {
    return 'La imagen no puede superar los 5 MB';
  }
  return null;
}
