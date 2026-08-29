import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Etiqueta } from '../empresas/empresa.model';

@Injectable({ providedIn: 'root' })
export class PerfilService {
  private readonly http = inject(HttpClient);

  listarEtiquetas(): Promise<Etiqueta[]> {
    return firstValueFrom(this.http.get<Etiqueta[]>('/api/etiquetas'));
  }

  obtenerEtiquetas(alumnoId: number): Promise<Etiqueta[]> {
    return firstValueFrom(this.http.get<Etiqueta[]>(`/api/usuarios/${alumnoId}/etiquetas`));
  }

  actualizarEtiquetas(alumnoId: number, etiquetaIds: number[]): Promise<Etiqueta[]> {
    return firstValueFrom(
      this.http.put<Etiqueta[]>(`/api/usuarios/${alumnoId}/etiquetas`, { etiquetaIds }),
    );
  }
}
