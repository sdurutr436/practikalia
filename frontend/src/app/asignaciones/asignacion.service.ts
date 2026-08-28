import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  ActualizarAsignacionRequest,
  Asignacion,
  CrearAsignacionRequest,
  Grado,
  UsuarioResumen,
} from './asignacion.model';

@Injectable({ providedIn: 'root' })
export class AsignacionService {
  private readonly http = inject(HttpClient);

  listarPorEmpresa(empresaId: number): Promise<Asignacion[]> {
    return firstValueFrom(this.http.get<Asignacion[]>(`/api/empresas/${empresaId}/asignaciones`));
  }

  listarPorAlumno(alumnoId: number): Promise<Asignacion[]> {
    return firstValueFrom(this.http.get<Asignacion[]>(`/api/alumnos/${alumnoId}/asignaciones`));
  }

  crear(request: CrearAsignacionRequest): Promise<Asignacion> {
    return firstValueFrom(this.http.post<Asignacion>('/api/asignaciones', request));
  }

  cerrar(id: number, request: ActualizarAsignacionRequest): Promise<Asignacion> {
    return firstValueFrom(this.http.put<Asignacion>(`/api/asignaciones/${id}`, request));
  }

  listarGrados(): Promise<Grado[]> {
    return firstValueFrom(this.http.get<Grado[]>('/api/grados'));
  }

  listarUsuarios(rol: 'ALUMNO' | 'PROFESOR'): Promise<UsuarioResumen[]> {
    return firstValueFrom(this.http.get<UsuarioResumen[]>('/api/usuarios', { params: { rol } }));
  }
}
