import { Component, inject, signal } from '@angular/core';
import { EstadoComponent } from '../../compartido/estado/estado';
import { EmpresaService } from '../empresa.service';
import { Empresa, esVistaProfesor } from '../empresa.model';
import { TarjetaEmpresaComponent } from '../tarjeta-empresa/tarjeta-empresa';
import { CabeceraComponent } from '../../compartido/cabecera/cabecera';

@Component({
  selector: 'app-empresas-listado-page',
  imports: [EstadoComponent, TarjetaEmpresaComponent, CabeceraComponent],
  templateUrl: './empresas-listado-page.html',
})
export class EmpresasListadoPage {
  private readonly empresaService = inject(EmpresaService);

  protected readonly esVistaProfesor = esVistaProfesor;
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly empresas = signal<Empresa[]>([]);

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
