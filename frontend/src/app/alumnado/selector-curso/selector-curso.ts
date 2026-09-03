import { Component, computed, inject, input, output, signal } from '@angular/core';
import { FormControl } from '@angular/forms';
import { DesplegableComponent } from '../../compartido/desplegable/desplegable';
import { AlumnadoService } from '../alumnado.service';

/**
 * Selector de curso académico: «2026/2027». Es el mismo en el filtro de
 * asignaciones y en la ficha de alumno, así que el catálogo y la forma de
 * escribirlo viven aquí y no en cada pantalla.
 *
 * Con `vacio` puesto, la lista lleva delante esa opción y no se elige nada por
 * su cuenta (la ficha, donde el curso es opcional). Sin él se preselecciona el
 * curso en marcha, que es lo que el backend lista cuando no se le pide otro.
 */
@Component({
  selector: 'app-selector-curso',
  imports: [DesplegableComponent],
  template: `
    <app-desplegable
      [id]="id()"
      [etiqueta]="etiqueta()"
      [opciones]="opciones()"
      [control]="control()"
      [valor]="valorEfectivo()"
      (cambia)="cambia.emit($event)"
    />
  `,
  host: { class: 'u-contenidos' },
})
export class SelectorCursoComponent {
  readonly control = input<FormControl | null>(null);
  readonly valor = input<number | null>(null);
  readonly id = input<string>();
  readonly etiqueta = input('Curso académico');
  /** Rótulo de la opción «ninguno»; sin él, el curso en marcha va preseleccionado. */
  readonly vacio = input<string | null>(null);
  readonly cambia = output<string>();

  private readonly alumnado = inject(AlumnadoService);
  private readonly cursos = signal<number[]>([]);
  private readonly actual = signal<number | null>(null);

  protected readonly opciones = computed(() => [
    ...(this.vacio() === null ? [] : [{ valor: '', etiqueta: this.vacio()! }]),
    ...this.cursos().map((curso) => ({ valor: curso, etiqueta: `${curso}/${curso + 1}` })),
  ]);

  protected readonly valorEfectivo = computed(
    () => this.valor() ?? (this.vacio() === null ? this.actual() : null),
  );

  constructor() {
    void this.cargar();
  }

  private async cargar(): Promise<void> {
    try {
      const { actual, cursos } = await this.alumnado.listarCursos();
      this.actual.set(actual);
      this.cursos.set(cursos);
    } catch {
      // Sin catálogo el selector queda vacío; el resto de la pantalla sigue.
    }
  }
}
