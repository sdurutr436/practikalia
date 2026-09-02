import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ConsultaEmpresas, Empresa, EmpresaRequest, PaginaEmpresas } from './empresa.model';

@Injectable({ providedIn: 'root' })
export class EmpresaService {
  private readonly http = inject(HttpClient);

  listar(consulta: ConsultaEmpresas = {}): Promise<PaginaEmpresas> {
    let params = new HttpParams();
    if (consulta.texto) params = params.set('texto', consulta.texto);
    if (consulta.publicada != null) params = params.set('publicada', consulta.publicada);
    if (consulta.sectorId != null) params = params.set('sectorId', consulta.sectorId);
    for (const id of consulta.etiquetaIds ?? []) params = params.append('etiquetaIds', id);
    if (consulta.pagina) params = params.set('pagina', consulta.pagina);
    if (consulta.tamano != null) params = params.set('tamano', consulta.tamano);
    return firstValueFrom(this.http.get<PaginaEmpresas>('/api/empresas', { params }));
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
