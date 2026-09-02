import { Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { EstadoComponent } from '../../compartido/estado/estado';
import { EmpresaService } from '../empresa.service';
import { Empresa, esVistaProfesor } from '../empresa.model';
import { TarjetaEmpresaComponent } from '../tarjeta-empresa/tarjeta-empresa';
import { CabeceraComponent } from '../../compartido/cabecera/cabecera';
import { IconoComponent } from '../../compartido/icono/icono';

/** Sin acentos y en minúsculas: buscar "diseno" tiene que encontrar "Diseño". */
function normalizar(texto: string): string {
  return texto
    .toLowerCase()
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '');
}

/** Todo lo que se puede escribir para dar con una empresa, en un solo texto. */
function textoBuscable(empresa: Empresa): string {
  return normalizar(
    [empresa.nombre, empresa.descripcion, empresa.sector.nombre, ...empresa.etiquetas.map((e) => e.nombre)]
      .filter(Boolean)
      .join(' '),
  );
}

@Component({
  selector: 'app-empresas-listado-page',
  imports: [RouterLink, EstadoComponent, TarjetaEmpresaComponent, CabeceraComponent, IconoComponent],
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

  private readonly entrada = viewChild.required<ElementRef<HTMLInputElement>>('entrada');
  protected readonly buscando = signal(false);
  protected readonly busqueda = signal('');

  protected readonly empresasFiltradas = computed(() => {
    const filtro = this.filtro();
    const porPublicacion =
      filtro === null || !this.esProfesor()
        ? this.empresas()
        : this.empresas().filter(
            (empresa) => esVistaProfesor(empresa) && empresa.publicada === (filtro === 'true'),
          );

    // Cada palabra suelta tiene que aparecer en algún sitio de la empresa, en
    // cualquier orden: "web ondara" encuentra "Grupo Ondara" del sector "web".
    // ponytail: sin tolerancia a erratas; si hace falta, aquí entra una
    // distancia de edición sobre cada palabra.
    const palabras = normalizar(this.busqueda().trim()).split(/\s+/).filter(Boolean);
    if (palabras.length === 0) return porPublicacion;
    return porPublicacion.filter((empresa) => {
      const texto = textoBuscable(empresa);
      return palabras.every((palabra) => texto.includes(palabra));
    });
  });

  protected readonly mensajeVacio = computed(() => {
    if (this.busqueda().trim()) return 'Ninguna empresa coincide con la búsqueda.';
    return this.filtro() === null ? 'Todavía no hay empresas.' : 'Ninguna empresa con ese filtro.';
  });

  constructor() {
    void this.cargar();
  }

  /** La lupa despliega el campo y le da el foco; cerrarla limpia la búsqueda. */
  protected alternarBusqueda(): void {
    if (this.buscando()) {
      this.cerrarBusqueda();
      return;
    }
    this.buscando.set(true);
    this.entrada().nativeElement.focus();
  }

  protected cerrarBusqueda(): void {
    this.buscando.set(false);
    this.busqueda.set('');
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
