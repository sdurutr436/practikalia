import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

/**
 * Alta de alumno. `contrasenaTemporal` llega en claro y **solo en esta
 * respuesta**: si no se anota aquí, no hay forma de recuperarla.
 */
export interface AlumnoCreado {
  id: number;
  correo: string;
  rol: 'ALUMNO';
  contrasenaTemporal: string;
}

@Injectable({ providedIn: 'root' })
export class AlumnadoService {
  private readonly http = inject(HttpClient);

  crear(correo: string): Promise<AlumnoCreado> {
    return firstValueFrom(this.http.post<AlumnoCreado>('/api/usuarios', { correo, rol: 'ALUMNO' }));
  }
}
