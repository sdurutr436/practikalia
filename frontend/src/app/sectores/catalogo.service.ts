import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

/**
 * Una rama del catálogo. El mismo tipo sirve para los tres niveles —sector,
 * actividad principal y etiqueta— porque en el servidor son la misma tabla:
 * lo que distingue a uno de otro es de quién cuelga.
 */
export interface Nodo {
  id: number;
  nombre: string;
  /** Raíz que no es un sector: vale para cualquier empresa (modalidad de trabajo). */
  transversal: boolean;
  hijas: Nodo[];
}

/** Mantenimiento del catálogo de sectores y etiquetas. Solo lo usa /sectores. */
@Injectable({ providedIn: 'root' })
export class CatalogoService {
  private readonly http = inject(HttpClient);

  /** El árbol entero de una vez: son decenas de nodos, no miles. */
  arbol(): Promise<Nodo[]> {
    return firstValueFrom(this.http.get<Nodo[]>('/api/etiquetas/arbol'));
  }

  /** Sin `padreId` nace como sector, o como grupo transversal si `transversal`. */
  crear(nombre: string, padreId: number | null, transversal = false): Promise<Nodo> {
    return firstValueFrom(this.http.post<Nodo>('/api/etiquetas', { nombre, padreId, transversal }));
  }

  renombrar(id: number, nombre: string): Promise<Nodo> {
    return firstValueFrom(this.http.put<Nodo>(`/api/etiquetas/${id}`, { nombre }));
  }

  borrar(id: number): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`/api/etiquetas/${id}`));
  }
}
