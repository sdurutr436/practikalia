import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { EmpresaService } from '../empresa.service';
import { Empresa, esVistaProfesor } from '../empresa.model';

@Component({
  selector: 'app-empresas-listado-page',
  imports: [RouterLink],
  templateUrl: './empresas-listado-page.html',
})
export class EmpresasListadoPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly empresaService = inject(EmpresaService);

  protected readonly esVistaProfesor = esVistaProfesor;
  protected readonly sesion = this.auth.sesion;
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

  protected async cerrarSesion(): Promise<void> {
    await this.auth.logout();
    await this.router.navigate(['/login']);
  }
}
