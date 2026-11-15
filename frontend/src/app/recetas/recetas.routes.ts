import { Routes } from '@angular/router';
import { sinCambiosPendientes } from '../core/guards';

/** Área de recetas (`/recetas/...`), cargada bajo demanda. */
export const RUTAS_RECETAS: Routes = [
  {
    path: '',
    title: 'Recetas',
    loadComponent: () => import('./lista-recetas').then((m) => m.ListaRecetas),
  },
  {
    path: 'nueva',
    title: 'Nueva receta',
    canDeactivate: [sinCambiosPendientes],
    loadComponent: () => import('./formulario-receta').then((m) => m.FormularioReceta),
  },
  {
    // El título definitivo (el de la receta) lo pone el propio componente al cargarla
    path: ':id',
    title: 'Receta',
    loadComponent: () => import('./detalle-receta').then((m) => m.DetalleReceta),
  },
  {
    path: ':id/editar',
    title: 'Editar receta',
    canDeactivate: [sinCambiosPendientes],
    loadComponent: () => import('./formulario-receta').then((m) => m.FormularioReceta),
  },
];
