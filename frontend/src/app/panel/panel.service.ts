import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

/** Contadores del centro entero: solo el profesorado puede pedirlos (403 si no). */
export interface ResumenCentro {
  empresasPublicadas: number;
  empresasSinPublicar: number;
  alumnadoActivo: number;
  alumnadoSinAsignar: number;
}

@Injectable({ providedIn: 'root' })
export class PanelService {
  private readonly http = inject(HttpClient);

  resumen(): Promise<ResumenCentro> {
    return firstValueFrom(this.http.get<ResumenCentro>('/api/panel/resumen'));
  }
}
