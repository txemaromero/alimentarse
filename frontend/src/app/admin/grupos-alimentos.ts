import { NgTemplateOutlet } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faLayerGroup, faPenToSquare, faPlus, faTrashCan } from '@fortawesome/free-solid-svg-icons';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { confirmar } from '../compartido/modal-confirmacion';
import { RequiereConexion } from '../compartido/requiere-conexion';
import { CatalogoApi, textoRaciones } from '../core/catalogo-api';
import { GrupoAdmin } from '../core/modelos';
import { Notificaciones } from '../core/notificaciones';
import { AdminApi } from './admin-api';
import { abrirModalGrupo } from './modal-grupo';

/**
 * Grupos de alimentos (D-065): sus raciones semanales, si son básicos y cuántos ingredientes tienen. Se crean, se
 * editan y, si no tienen ingredientes, se borran.
 */
@Component({
  selector: 'app-grupos-alimentos',
  imports: [FaIconComponent, NgTemplateOutlet, RequiereConexion],
  template: `
    <section class="container pb-5">
      <h1 class="titulo-seccion">Grupos de alimentos</h1>
      <p class="text-body-secondary">
        Las raciones por semana deciden los indicadores de «Mi alimentación» y qué recetas salen en
        «¿Qué toca hoy?». Un cambio se aplica al momento a todos los usuarios.
      </p>
      <div class="d-flex justify-content-end mb-3">
        <button type="button" class="btn btn-primary" appRequiereConexion (click)="nuevo()">
          <fa-icon [icon]="iconos.nuevo" class="me-1" />Nuevo grupo
        </button>
      </div>

      @if (fallo()) {
        <div class="alert alert-danger d-flex align-items-center gap-3" role="alert">
          <span class="flex-grow-1">No se han podido cargar los grupos.</span>
          <button type="button" class="btn btn-sm btn-outline-danger" (click)="cargar()">
            Reintentar
          </button>
        </div>
      } @else if (grupos(); as lista) {
        <div class="card d-none d-md-block">
          <div class="table-responsive">
            <table class="table table-hover align-middle mb-0">
              <caption class="visually-hidden">
                Grupos de alimentos con sus raciones semanales
              </caption>
              <thead>
                <tr>
                  <th scope="col">Grupo</th>
                  <th scope="col">Raciones por semana</th>
                  <th scope="col">Básico</th>
                  <th scope="col" class="text-end">Ingredientes</th>
                  <th scope="col" class="text-end">
                    <span class="visually-hidden">Acciones</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                @for (grupo of lista; track grupo.id) {
                  <tr>
                    <td class="fw-semibold">{{ grupo.nombre }}</td>
                    <td>{{ raciones(grupo) }}</td>
                    <td>{{ grupo.basico ? 'Sí' : 'No' }}</td>
                    <td class="text-end">{{ grupo.ingredientes }}</td>
                    <td class="text-end text-nowrap">
                      <ng-container
                        [ngTemplateOutlet]="acciones"
                        [ngTemplateOutletContext]="{ $implicit: grupo }"
                      />
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </div>
        <ul class="list-unstyled d-grid gap-2 d-md-none">
          @for (grupo of lista; track grupo.id) {
            <li class="card card-body d-flex flex-row align-items-center gap-3">
              <div class="flex-grow-1">
                <strong>{{ grupo.nombre }}</strong>
                <span class="d-block small">{{ raciones(grupo) }}</span>
                <span class="d-block small text-body-secondary">
                  {{ grupo.ingredientes }}
                  {{ grupo.ingredientes === 1 ? 'ingrediente' : 'ingredientes' }}
                  @if (grupo.basico) {
                    · básico
                  }
                </span>
              </div>
              <div class="text-nowrap">
                <ng-container
                  [ngTemplateOutlet]="acciones"
                  [ngTemplateOutletContext]="{ $implicit: grupo }"
                />
              </div>
            </li>
          }
        </ul>
      } @else {
        <div class="text-center py-5">
          <span class="spinner-border text-secondary" role="status">
            <span class="visually-hidden">Cargando los grupos…</span>
          </span>
        </div>
      }
    </section>

    <ng-template #acciones let-grupo>
      <button
        type="button"
        class="btn btn-sm btn-outline-secondary me-1"
        title="Editar"
        [attr.aria-label]="'Editar ' + grupo.nombre"
        appRequiereConexion
        (click)="editar(grupo)"
      >
        <fa-icon [icon]="iconos.editar" />
      </button>
      <button
        type="button"
        class="btn btn-sm btn-outline-danger"
        [title]="grupo.ingredientes > 0 ? 'Tiene ingredientes: no se puede borrar' : 'Borrar'"
        [attr.aria-label]="'Borrar ' + grupo.nombre"
        [appRequiereConexion]="grupo.ingredientes > 0"
        (click)="borrar(grupo)"
      >
        <fa-icon [icon]="iconos.borrar" />
      </button>
    </ng-template>
  `,
})
export class GruposAlimentos implements OnInit {
  private readonly api = inject(AdminApi);
  private readonly catalogo = inject(CatalogoApi);
  private readonly modales = inject(NgbModal);
  private readonly notificaciones = inject(Notificaciones);

  readonly grupos = signal<GrupoAdmin[] | null>(null);
  readonly fallo = signal(false);

  protected readonly iconos = {
    nuevo: faPlus,
    editar: faPenToSquare,
    borrar: faTrashCan,
    vacio: faLayerGroup,
  };

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.fallo.set(false);
    this.api.grupos().subscribe({
      next: (grupos) => this.grupos.set(grupos),
      error: (error: HttpErrorResponse) => {
        this.fallo.set(true);
        if (error.status === 403) {
          this.notificaciones.error(
            'Ya no tienes permisos de administrador. Vuelve a iniciar sesión.',
          );
        }
      },
    });
  }

  raciones(grupo: GrupoAdmin): string {
    const texto = textoRaciones(grupo);
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  }

  nuevo(): void {
    abrirModalGrupo(this.modales).subscribe((grupo) => {
      this.notificaciones.exito(`Grupo «${grupo.nombre}» creado.`);
      this.alCambiar();
    });
  }

  editar(grupo: GrupoAdmin): void {
    abrirModalGrupo(this.modales, grupo).subscribe((editado) => {
      this.notificaciones.exito(`Grupo «${editado.nombre}» guardado.`);
      this.alCambiar();
    });
  }

  borrar(grupo: GrupoAdmin): void {
    if (grupo.ingredientes > 0) {
      return;
    }
    confirmar(this.modales, {
      titulo: 'Borrar el grupo',
      mensaje: `¿Seguro que quieres borrar el grupo «${grupo.nombre}»? No tiene ningún ingrediente.`,
      aceptar: 'Sí, borrar',
      accion: () => this.api.borrarGrupo(grupo.id),
    }).subscribe(() => {
      this.notificaciones.exito(`Grupo «${grupo.nombre}» borrado.`);
      this.alCambiar();
    });
  }

  /** Recarga la lista y hace que el resto de pantallas vuelvan a pedir los grupos. */
  private alCambiar(): void {
    this.catalogo.invalidarGrupos();
    this.cargar();
  }
}
