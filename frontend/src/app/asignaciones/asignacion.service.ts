import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { PaginaAlumnos } from '../alumnado/alumnado.service';

/** Lo que guarda una fila de la pantalla de asignaciones. */
export interface AsignarEmpresaRequest {
  empresaId: number;
  /** El profesor que le tutoriza las prácticas. */
  tutorCentroId: number | null;
  /** Quién le tutoriza en la empresa; `null` para dejarle sin ninguno. */
  tutorEmpresaId: number | null;
}

/** Criterios del listado de asignaciones; todos opcionales menos la página. */
export interface ConsultaAlumnado {
  anio?: number | null;
  gradoId?: number | null;
  texto?: string | null;
  asignado?: boolean | null;
  pagina: number;
  tamano: number;
}
import {
  ActualizarAsignacionRequest,
  ActualizarGradoRequest,
  Asignacion,
  CrearAsignacionRequest,
  Grado,
  TasaContratacion,
  UsuarioGrado,
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

  /**
   * El alumnado de un curso académico, que es lo que lista la pantalla de
   * asignaciones. Sin `anio`, el curso en marcha; `asignado` a `null` para la
   * pastilla «Todas».
   */
  listarAlumnadoDelCurso(consulta: ConsultaAlumnado): Promise<PaginaAlumnos> {
    let params = new HttpParams().set('pagina', consulta.pagina).set('tamano', consulta.tamano);
    if (consulta.anio != null) params = params.set('anio', consulta.anio);
    if (consulta.gradoId != null) params = params.set('gradoId', consulta.gradoId);
    if (consulta.texto) params = params.set('texto', consulta.texto);
    if (consulta.asignado != null) params = params.set('asignado', consulta.asignado);
    return firstValueFrom(this.http.get<PaginaAlumnos>('/api/alumnos/curso', { params }));
  }

  /**
   * Pone empresa y tutores a un alumno: crea su asignación, o corrige la que
   * tenga abierta. Sin `tutorCentroId` manda el tutor de su clase.
   */
  asignar(alumnoId: number, request: AsignarEmpresaRequest): Promise<Asignacion> {
    return firstValueFrom(
      this.http.put<Asignacion>(`/api/alumnos/${alumnoId}/asignacion`, request),
    );
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

  actualizarGrado(alumnoId: number, request: ActualizarGradoRequest): Promise<UsuarioGrado> {
    return firstValueFrom(this.http.put<UsuarioGrado>(`/api/usuarios/${alumnoId}/grado`, request));
  }

  tasaContratacion(empresaId: number): Promise<TasaContratacion> {
    return firstValueFrom(
      this.http.get<TasaContratacion>(`/api/empresas/${empresaId}/tasa-contratacion`),
    );
  }
}
