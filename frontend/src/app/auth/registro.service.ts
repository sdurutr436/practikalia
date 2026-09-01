import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface GradoOpcion {
  id: number;
  nombre: string;
}

export interface RegistroRequest {
  nombre: string;
  apellido1: string;
  apellido2: string | null;
  dni: string;
  gradoId: number;
  correo: string;
  web: string;
}

@Injectable({ providedIn: 'root' })
export class RegistroService {
  private readonly http = inject(HttpClient);

  // ponytail: ni el listado público de grados ni el alta quedan todavía
  // expuestos en el backend — ver
  // docs/prompts/backend/phases/fase18_autoregistro_alumnos.md.
  listarGrados(): Promise<GradoOpcion[]> {
    return firstValueFrom(this.http.get<GradoOpcion[]>('/api/grados/publico'));
  }

  registrar(datos: RegistroRequest): Promise<void> {
    return firstValueFrom(this.http.post<void>('/api/auth/registro', datos));
  }
}
