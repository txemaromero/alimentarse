import { Routes } from '@angular/router';

/** Área «Mi alimentación» (`/mi-alimentacion/...`), cargada bajo demanda. */
export const RUTAS_ALIMENTACION: Routes = [
  {
    path: '',
    title: 'Mi alimentación',
    loadComponent: () => import('./mi-alimentacion').then((m) => m.MiAlimentacion),
  },
  {
    path: 'historico',
    title: 'Histórico de consumos',
    loadComponent: () => import('./historico').then((m) => m.Historico),
  },
];
