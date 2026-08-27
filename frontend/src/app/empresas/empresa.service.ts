import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Empresa, EmpresaRequest } from './empresa.model';

@Injectable({ providedIn: 'root' })
export class EmpresaService {
  private readonly http = inject(HttpClient);

  listar(): Promise<Empresa[]> {
    return firstValueFrom(this.http.get<Empresa[]>('/api/empresas'));
  }

  obtener(id: number): Promise<Empresa> {
    return firstValueFrom(this.http.get<Empresa>(`/api/empresas/${id}`));
  }

  crear(request: EmpresaRequest): Promise<Empresa> {
    return firstValueFrom(this.http.post<Empresa>('/api/empresas', request));
  }

  actualizar(id: number, request: EmpresaRequest): Promise<Empresa> {
    return firstValueFrom(this.http.put<Empresa>(`/api/empresas/${id}`, request));
  }

  subirImagen(id: number, fichero: File): Promise<Empresa> {
    const formData = new FormData();
    formData.append('fichero', fichero);
    return firstValueFrom(this.http.post<Empresa>(`/api/empresas/${id}/imagen`, formData));
  }
}
