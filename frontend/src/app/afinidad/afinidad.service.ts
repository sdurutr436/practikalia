import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AfinidadListado } from './afinidad.model';

@Injectable({ providedIn: 'root' })
export class AfinidadService {
  private readonly http = inject(HttpClient);

  propia(): Promise<AfinidadListado> {
    return firstValueFrom(this.http.get<AfinidadListado>('/api/empresas/afinidad'));
  }

  deAlumno(alumnoId: number): Promise<AfinidadListado> {
    return firstValueFrom(this.http.get<AfinidadListado>(`/api/alumnos/${alumnoId}/afinidad`));
  }
}
