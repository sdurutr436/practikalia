import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

/** Fila del listado de profesorado. */
export interface Profesor {
  id: number;
  nombre: string | null;
  apellido1: string | null;
  apellido2: string | null;
  dni: string | null;
  correo: string;
  /** Administrador del centro. Viaja sobre el rol PROFESOR, no es un rol aparte. */
  esAdmin: boolean;
  /** La clase que tutoriza, o `null` si no tutoriza ninguna. */
  clase: { id: number; nombre: string } | null;
  /** Cuántos alumnos tiene como tutor de prácticas. */
  alumnosPractica: number;
}

export interface PaginaProfesores {
  contenido: Profesor[];
  pagina: number;
  tamano: number;
  total: number;
  paginas: number;
}

/** Ficha del modal. La misma para dar de alta y para editar. */
export interface FichaProfesorRequest {
  nombre: string;
  apellido1: string;
  apellido2: string | null;
  dni: string;
  correo: string;
  /** La clase de la que es tutor. Se la quita a quien la tuviera. */
  gradoId: number | null;
  esAdmin: boolean;
  /** Alumnos que pasan a tenerle como tutor de prácticas. Solo añade. */
  alumnosPractica: number[];
}

@Injectable({ providedIn: 'root' })
export class ProfesoradoService {
  private readonly http = inject(HttpClient);

  /** `conClase` a `null` para la pastilla «Todos». */
  listar(conClase: boolean | null, pagina: number, tamano: number): Promise<PaginaProfesores> {
    const params: Record<string, string | number | boolean> = { pagina, tamano };
    if (conClase !== null) {
      params['conClase'] = conClase;
    }
    return firstValueFrom(this.http.get<PaginaProfesores>('/api/profesores', { params }));
  }

  /** Alta a mano: nace confirmada y su contraseña es el DNI sin la letra. */
  crear(ficha: FichaProfesorRequest): Promise<Profesor> {
    return firstValueFrom(this.http.post<Profesor>('/api/profesores', ficha));
  }

  editar(id: number, ficha: FichaProfesorRequest): Promise<Profesor> {
    return firstValueFrom(this.http.put<Profesor>(`/api/profesores/${id}`, ficha));
  }
}
