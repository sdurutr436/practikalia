import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { EmpresaService } from '../empresa.service';
import { Empresa, esVistaProfesor } from '../empresa.model';

@Component({
  selector: 'app-empresa-detalle-page',
  imports: [RouterLink],
  templateUrl: './empresa-detalle-page.html',
})
export class EmpresaDetallePage {
  private readonly route = inject(ActivatedRoute);
  private readonly empresaService = inject(EmpresaService);

  protected readonly esVistaProfesor = esVistaProfesor;
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly empresa = signal<Empresa | null>(null);

  constructor() {
    void this.cargar();
  }

  private async cargar(): Promise<void> {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    try {
      this.empresa.set(await this.empresaService.obtener(id));
    } catch (e) {
      this.error.set(
        e instanceof HttpErrorResponse && e.status === 404
          ? 'Esta empresa no existe, o no está publicada.'
          : 'No se pudo cargar la empresa.',
      );
    } finally {
      this.cargando.set(false);
    }
  }
}
