import { Component, OnInit, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import {
  faAppleWhole,
  faCodeMerge,
  faFilter,
  faPenToSquare,
  faPlus,
  faRotateLeft,
  faSeedling,
  faTrashCan,
} from '@fortawesome/free-solid-svg-icons';
import { AdminApi } from '../admin/admin-api';
import { confirmar } from '../compartido/modal-confirmacion';
import { RequiereConexion } from '../compartido/requiere-conexion';
import { abrirModalFusionar } from './modal-fusionar';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { crearListado } from '../compartido/listado';
import { Paginacion } from '../compartido/paginacion';
import { CatalogoApi, textoRaciones } from '../core/catalogo-api';
import { Ingrediente } from '../core/modelos';
import { Notificaciones } from '../core/notificaciones';
import { Sesion } from '../core/sesion';
import { FiltroIngredientes, IngredientesApi } from './ingredientes-api';
import { abrirModalDatosNutricionales } from './modal-datos-nutricionales';
import { abrirModalIngrediente } from './modal-ingrediente';

/**
 * Ingredientes (6.8): filtros por nombre y grupo, alta para todos y, solo para ADMIN, edición, fusión de duplicados,
 * borrado de los que no usa ninguna receta (D-065) y su alimento de BEDCA, con un filtro de los que no tienen (D-080).
 */
@Component({
  selector: 'app-lista-ingredientes',
  imports: [ReactiveFormsModule, FaIconComponent, Paginacion, RequiereConexion],
  templateUrl: './lista-ingredientes.html',
  styles: `
    .tarjeta-ingrediente .card-title {
      font-size: 1rem;
    }
  `,
})
export class ListaIngredientes implements OnInit {
  private readonly api = inject(IngredientesApi);
  private readonly modales = inject(NgbModal);
  private readonly notificaciones = inject(Notificaciones);
  private readonly admin = inject(AdminApi);
  protected readonly sesion = inject(Sesion);

  readonly listado = crearListado<Ingrediente, FiltroIngredientes>('ingredientes', (filtro, p) =>
    this.api.listar(filtro, p),
  );
  readonly grupos = toSignal(inject(CatalogoApi).grupos$, { initialValue: [] });
  readonly formulario = inject(NonNullableFormBuilder).group({
    nombre: [''],
    grupo: new FormControl<number | null>(null),
    sinAlimento: [false],
  });

  protected readonly textoRaciones = textoRaciones;
  protected readonly iconos = {
    filtrar: faFilter,
    restablecer: faRotateLeft,
    nuevo: faPlus,
    editar: faPenToSquare,
    fusionar: faCodeMerge,
    borrar: faTrashCan,
    nutricion: faAppleWhole,
    vacio: faSeedling,
  };

  ngOnInit(): void {
    const filtro = this.listado.filtro();
    this.formulario.reset({
      nombre: filtro.nombre ?? '',
      grupo: filtro.grupo ?? null,
      sinAlimento: filtro.sinAlimento ?? false,
    });
    this.listado.iniciar();
  }

  filtrar(): void {
    const { nombre, grupo, sinAlimento } = this.formulario.getRawValue();
    const filtro: FiltroIngredientes = {};
    if (nombre.trim()) filtro.nombre = nombre.trim();
    if (grupo) filtro.grupo = Number(grupo);
    if (sinAlimento && this.sesion.esAdmin()) filtro.sinAlimento = true;
    this.listado.filtrar(filtro);
  }

  restablecer(): void {
    this.formulario.reset({ nombre: '', grupo: null, sinAlimento: false });
    this.listado.filtrar({});
  }

  nuevo(): void {
    abrirModalIngrediente(this.modales, {
      nombreInicial: this.formulario.controls.nombre.value,
    }).subscribe((creado) => {
      this.notificaciones.exito(`Ingrediente «${creado.nombre}» creado.`);
      this.listado.recargar();
    });
  }

  fusionar(ingrediente: Ingrediente): void {
    abrirModalFusionar(this.modales, ingrediente).subscribe((resultado) => {
      const recetas =
        resultado.recetas === 1
          ? '1 receta actualizada'
          : `${resultado.recetas} recetas actualizadas`;
      this.notificaciones.exito(
        `«${ingrediente.nombre}» se ha fusionado con «${resultado.ingrediente.nombre}»: ${recetas}.`,
      );
      this.listado.recargar();
    });
  }

  borrar(ingrediente: Ingrediente): void {
    confirmar(this.modales, {
      titulo: 'Borrar el ingrediente',
      mensaje:
        `¿Seguro que quieres borrar «${ingrediente.nombre}»? Solo se puede si ninguna receta lo ` +
        'usa; si es un duplicado, fusiónalo con el bueno.',
      aceptar: 'Sí, borrar',
      accion: () => this.admin.borrarIngrediente(ingrediente.id),
    }).subscribe(() => {
      this.notificaciones.exito(`Ingrediente «${ingrediente.nombre}» borrado.`);
      this.listado.recargar();
    });
  }

  /** El alimento de BEDCA y los pesos de las piezas con los que se calcula su información nutricional (D-080). */
  datosNutricionales(ingrediente: Ingrediente): void {
    abrirModalDatosNutricionales(this.modales, ingrediente).subscribe((actualizado) => {
      this.notificaciones.exito(
        actualizado.alimento
          ? `Datos nutricionales de «${actualizado.nombre}» guardados: usa «${actualizado.alimento}».`
          : `Datos nutricionales de «${actualizado.nombre}» guardados: no tiene alimento.`,
      );
      if (this.listado.filtro().sinAlimento) {
        this.listado.recargar();
      } else {
        this.listado.reemplazar(actualizado, (a, b) => a.id === b.id);
      }
    });
  }

  editar(ingrediente: Ingrediente): void {
    abrirModalIngrediente(this.modales, { ingrediente }).subscribe((editado) => {
      this.notificaciones.exito(`Ingrediente «${editado.nombre}» guardado.`);
      this.listado.reemplazar(editado, (a, b) => a.id === b.id);
    });
  }
}
