import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Interes, Interesado } from './interes.model';

@Injectable({ providedIn: 'root' })
export class InteresService {
  private readonly http = inject(HttpClient);

  marcar(empresaId: number): Promise<void> {
    return firstValueFrom(this.http.put<void>(`/api/empresas/${empresaId}/interes`, null));
  }

  desmarcar(empresaId: number): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`/api/empresas/${empresaId}/interes`));
  }

  listarInteresados(empresaId: number): Promise<Interesado[]> {
    return firstValueFrom(this.http.get<Interesado[]>(`/api/empresas/${empresaId}/interesados`));
  }

  listarPorAlumno(alumnoId: number): Promise<Interes[]> {
    return firstValueFrom(this.http.get<Interes[]>(`/api/alumnos/${alumnoId}/intereses`));
  }
}
