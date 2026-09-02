import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

/**
 * Alta suelta de alumno. `contrasenaTemporal` llega en claro y **solo en esta
 * respuesta**: si no se anota aquí, no hay forma de recuperarla. (Las cuentas
 * importadas por CSV no pasan por aquí: nacen con el DNI de contraseña.)
 */
export interface AlumnoCreado {
  id: number;
  correo: string;
  rol: 'ALUMNO';
  contrasenaTemporal: string;
}

/** Fila del listado de alumnado. */
export interface Alumno {
  id: number;
  nombre: string | null;
  apellido1: string | null;
  apellido2: string | null;
  dni: string | null;
  correo: string;
  grado: { id: number; nombre: string } | null;
  anio: number | null;
  /** `false` = pendiente de confirmar por el centro. */
  activo: boolean;
  empresaId: number | null;
  empresaNombre: string | null;
}

export interface PaginaAlumnos {
  contenido: Alumno[];
  pagina: number;
  tamano: number;
  total: number;
  paginas: number;
}

export interface EditarAlumnoRequest {
  nombre: string;
  apellido1: string;
  apellido2: string | null;
  dni: string;
  correo: string;
  gradoId: number | null;
  anio: number | null;
}

@Injectable({ providedIn: 'root' })
export class AlumnadoService {
  private readonly http = inject(HttpClient);

  crear(correo: string): Promise<AlumnoCreado> {
    return firstValueFrom(this.http.post<AlumnoCreado>('/api/usuarios', { correo, rol: 'ALUMNO' }));
  }

  /** `activo` a `null` para la pastilla «Todos». */
  listar(activo: boolean | null, pagina: number, tamano: number): Promise<PaginaAlumnos> {
    const params: Record<string, string | number | boolean> = { pagina, tamano };
    if (activo !== null) {
      params['activo'] = activo;
    }
    return firstValueFrom(this.http.get<PaginaAlumnos>('/api/alumnos', { params }));
  }

  editar(id: number, request: EditarAlumnoRequest): Promise<Alumno> {
    return firstValueFrom(this.http.put<Alumno>(`/api/alumnos/${id}`, request));
  }

  /** Confirma la cuenta. No devuelve contraseña: la inicial es el DNI del alumno. */
  confirmar(id: number): Promise<void> {
    return firstValueFrom(this.http.put<void>(`/api/usuarios/${id}/activar`, {}));
  }

  importar(fichero: File): Promise<{ creados: number }> {
    const cuerpo = new FormData();
    cuerpo.append('fichero', fichero);
    return firstValueFrom(this.http.post<{ creados: number }>('/api/alumnos/importar', cuerpo));
  }

  plantillaCsv(): Promise<Blob> {
    return firstValueFrom(this.http.get('/api/alumnos/plantilla.csv', { responseType: 'blob' }));
  }
}
