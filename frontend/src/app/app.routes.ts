import { Routes } from '@angular/router';
import { adminGuard, authGuard, invitadoGuard, sinCambiosPendientes } from './core/guards';

export const routes: Routes = [
  // Autenticación (sin sesión)
  {
    // Vuelta de Google con la sesión iniciada (D-085). Sin invitadoGuard: la sesión de la cookie es la que vale
    path: 'login/google',
    title: 'Entrar con Google',
    loadComponent: () => import('./auth/login-google').then((m) => m.LoginGoogle),
  },
  {
    // El email de Google es el de una cuenta sin vincular: se vincula con su contraseña (D-086)
    path: 'login/google/vincular',
    title: 'Vincular tu cuenta con Google',
    loadComponent: () => import('./auth/vincular-google').then((m) => m.VincularGoogle),
  },
  {
    path: 'registro/google',
    title: 'Crear una cuenta con Google',
    loadComponent: () => import('./auth/registro-google').then((m) => m.RegistroGoogle),
  },
  {
    path: 'login',
    title: 'Iniciar sesión',
    canActivate: [invitadoGuard],
    loadComponent: () => import('./auth/login').then((m) => m.Login),
  },
  {
    path: 'registro',
    title: 'Registro',
    canActivate: [invitadoGuard],
    loadComponent: () => import('./auth/registro').then((m) => m.Registro),
  },
  {
    path: 'olvido-password',
    title: 'Recuperar la contraseña',
    canActivate: [invitadoGuard],
    loadComponent: () => import('./auth/olvido-password').then((m) => m.OlvidoPassword),
  },
  {
    // Sin invitadoGuard: el enlace del correo debe funcionar aunque haya una sesión abierta
    path: 'restablecer-password',
    title: 'Nueva contraseña',
    loadComponent: () => import('./auth/restablecer-password').then((m) => m.RestablecerPassword),
  },

  {
    // El enlace del correo para confirmar el email (D-088): sin guard, puede abrirse con o sin sesión
    path: 'confirmar-email',
    title: 'Confirmar el email',
    loadComponent: () => import('./auth/confirmar-email').then((m) => m.ConfirmarEmail),
  },

  // Páginas informativas (públicas, D-045)
  {
    path: 'ayuda',
    title: 'Ayuda',
    loadComponent: () => import('./paginas/ayuda').then((m) => m.Ayuda),
  },
  {
    path: 'informacion-legal',
    title: 'Información legal',
    loadComponent: () => import('./paginas/informacion-legal').then((m) => m.InformacionLegal),
  },
  {
    path: 'sobre-nosotros',
    title: 'Sobre nosotros',
    loadComponent: () => import('./paginas/sobre-nosotros').then((m) => m.SobreNosotros),
  },

  // Con sesión
  {
    path: '',
    canActivateChild: [authGuard],
    children: [
      {
        path: '',
        pathMatch: 'full',
        title: 'Inicio',
        loadComponent: () => import('./inicio/inicio').then((m) => m.Inicio),
      },
      {
        path: 'recetas',
        loadChildren: () => import('./recetas/recetas.routes').then((m) => m.RUTAS_RECETAS),
      },
      {
        path: 'mis-recetas',
        title: 'Mis recetas',
        loadComponent: () => import('./recetas/mis-recetas').then((m) => m.MisRecetas),
      },
      {
        path: 'me-gustan',
        title: 'Recetas que me gustan',
        loadComponent: () => import('./recetas/me-gustan').then((m) => m.MeGustan),
      },
      {
        path: 'ingredientes',
        title: 'Ingredientes',
        loadComponent: () =>
          import('./ingredientes/lista-ingredientes').then((m) => m.ListaIngredientes),
      },
      {
        path: 'que-toca-hoy',
        title: 'Qué toca hoy',
        loadComponent: () => import('./alimentacion/que-toca-hoy').then((m) => m.QueTocaHoy),
      },
      {
        path: 'planificador',
        title: 'Planificador',
        loadComponent: () => import('./alimentacion/planificador').then((m) => m.Planificador),
      },
      {
        path: 'planificador/compra',
        title: 'Lista de la compra',
        loadComponent: () => import('./alimentacion/lista-compra').then((m) => m.ListaDeLaCompra),
      },
      // Nombre anterior de la pantalla (D-066): los enlaces guardados siguen funcionando
      { path: 'que-como-hoy', redirectTo: 'que-toca-hoy' },
      {
        path: 'mi-alimentacion',
        loadChildren: () =>
          import('./alimentacion/alimentacion.routes').then((m) => m.RUTAS_ALIMENTACION),
      },
      {
        path: 'mi-perfil',
        title: 'Mi perfil',
        canDeactivate: [sinCambiosPendientes],
        loadComponent: () => import('./perfil/mi-perfil').then((m) => m.MiPerfil),
      },
      {
        path: 'admin',
        canActivateChild: [adminGuard],
        children: [
          { path: '', pathMatch: 'full', redirectTo: 'usuarios' },
          {
            path: 'usuarios',
            title: 'Gestión de usuarios',
            loadComponent: () => import('./admin/gestion-usuarios').then((m) => m.GestionUsuarios),
          },
          {
            path: 'grupos',
            title: 'Grupos de alimentos',
            loadComponent: () => import('./admin/grupos-alimentos').then((m) => m.GruposAlimentos),
          },
          {
            path: 'recetas-ocultas',
            title: 'Recetas ocultas',
            loadComponent: () => import('./admin/recetas-ocultas').then((m) => m.RecetasOcultas),
          },
        ],
      },
    ],
  },

  {
    path: '**',
    title: 'Página no encontrada',
    loadComponent: () => import('./paginas/no-encontrada').then((m) => m.NoEncontrada),
  },
];
