import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { EstadoComponent } from '../../compartido/estado/estado';
import { EmpresaService } from '../empresa.service';
import { Empresa, esVistaProfesor } from '../empresa.model';
import { TarjetaEmpresaComponent } from '../tarjeta-empresa/tarjeta-empresa';
import { CabeceraComponent } from '../../compartido/cabecera/cabecera';

@Component({
  selector: 'app-empresas-listado-page',
  imports: [RouterLink, EstadoComponent, TarjetaEmpresaComponent, CabeceraComponent],
  templateUrl: './empresas-listado-page.html',
})
export class EmpresasListadoPage {
  private readonly empresaService = inject(EmpresaService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);

  protected readonly esVistaProfesor = esVistaProfesor;
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly empresas = signal<Empresa[]>([]);

  /**
   * El filtro de publicación vive en la URL (`?publicada=true|false`) para que
   * el panel pueda enlazar al listado ya filtrado. Pulsar una pastilla navega
   * a la misma ruta, así que hay que leerlo reactivo: el snapshot no cambia.
   */
  private readonly parametros = toSignal(this.route.queryParamMap, { requireSync: true });
  protected readonly filtro = computed(() => this.parametros().get('publicada'));
  /** El alumnado solo recibe empresas publicadas, así que el filtro no le aplica. */
  protected readonly esProfesor = computed(() => this.auth.sesion()?.rol !== 'ALUMNO');

  protected readonly empresasFiltradas = computed(() => {
    const filtro = this.filtro();
    if (filtro === null || !this.esProfesor()) return this.empresas();
    return this.empresas().filter(
      (empresa) => esVistaProfesor(empresa) && empresa.publicada === (filtro === 'true'),
    );
  });

  protected readonly mensajeVacio = computed(() =>
    this.filtro() === null ? 'Todavía no hay empresas.' : 'Ninguna empresa con ese filtro.',
  );

  constructor() {
    void this.cargar();
  }

  private async cargar(): Promise<void> {
    try {
      this.empresas.set(await this.empresaService.listar());
    } catch {
      this.error.set('No se pudieron cargar las empresas.');
    } finally {
      this.cargando.set(false);
    }
  }
}
