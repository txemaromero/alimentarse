import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faMagnifyingGlass } from '@fortawesome/free-solid-svg-icons';
import {
  NgbHighlight,
  NgbTypeahead,
  NgbTypeaheadSelectItemEvent,
} from '@ng-bootstrap/ng-bootstrap';
import {
  Observable,
  catchError,
  debounceTime,
  distinctUntilChanged,
  map,
  of,
  switchMap,
} from 'rxjs';
import { RecetaResumen, SugerenciaReceta } from '../core/modelos';
import { Sesion } from '../core/sesion';
import { RecetasApi } from '../recetas/recetas-api';
import { TarjetaReceta } from '../recetas/tarjeta-receta';

interface Seccion {
  recetas: RecetaResumen[] | null;
  fallo: boolean;
}

/** Portada: buscador con autocompletado, recetas mejor valoradas y últimas añadidas. */
@Component({
  selector: 'app-inicio',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    NgbTypeahead,
    NgbHighlight,
    FaIconComponent,
    TarjetaReceta,
  ],
  templateUrl: './inicio.html',
  styleUrl: './inicio.scss',
})
export class Inicio implements OnInit {
  private readonly api = inject(RecetasApi);
  private readonly router = inject(Router);
  protected readonly sesion = inject(Sesion);

  readonly busqueda = new FormControl<SugerenciaReceta | string | null>(null);
  /** Con [formGroup] el envío lo gestiona Angular (ngSubmit) y no recarga la página. */
  readonly buscador = new FormGroup({ texto: this.busqueda });
  readonly mejorValoradas = signal<Seccion>({ recetas: null, fallo: false });
  readonly ultimas = signal<Seccion>({ recetas: null, fallo: false });

  protected readonly iconoBuscar = faMagnifyingGlass;
  protected readonly tituloSugerencia = (s: SugerenciaReceta | string | null) =>
    typeof s === 'string' ? s : (s?.titulo ?? '');

  readonly sugerir = (texto$: Observable<string>): Observable<SugerenciaReceta[]> =>
    texto$.pipe(
      debounceTime(250),
      map((texto) => (typeof texto === 'string' ? texto.trim() : '')),
      distinctUntilChanged(),
      switchMap((texto) =>
        texto.length < 2 ? of([]) : this.api.sugerencias(texto).pipe(catchError(() => of([]))),
      ),
    );

  ngOnInit(): void {
    this.api.mejorValoradas(3).subscribe({
      next: (recetas) => this.mejorValoradas.set({ recetas, fallo: false }),
      error: () => this.mejorValoradas.set({ recetas: null, fallo: true }),
    });
    this.api.ultimas(3).subscribe({
      next: (recetas) => this.ultimas.set({ recetas, fallo: false }),
      error: () => this.ultimas.set({ recetas: null, fallo: true }),
    });
  }

  /** Elegir una sugerencia lleva a su detalle. */
  abrir(evento: NgbTypeaheadSelectItemEvent<SugerenciaReceta>): void {
    evento.preventDefault();
    this.router.navigate(['/recetas', evento.item.id]);
  }

  /** Buscar sin elegir sugerencia lleva al listado filtrado por título. */
  buscar(): void {
    const valor = this.busqueda.value;
    if (valor && typeof valor === 'object') {
      this.router.navigate(['/recetas', valor.id]);
      return;
    }
    const texto = (valor ?? '').trim();
    this.router.navigate(['/recetas'], texto ? { queryParams: { titulo: texto } } : {});
  }

  /** Un «me gusta» cambia la receta en las dos secciones (puede estar en ambas). */
  actualizar(receta: RecetaResumen): void {
    const cambiar = (s: Seccion): Seccion => ({
      ...s,
      recetas: s.recetas?.map((r) => (r.id === receta.id ? receta : r)) ?? null,
    });
    this.mejorValoradas.update(cambiar);
    this.ultimas.update(cambiar);
  }
}
