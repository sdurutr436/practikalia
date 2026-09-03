import { DOCUMENT } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

/** Lo público de la configuración: nunca la whitelist, que es dato personal. */
export interface Centro {
  nombre: string;
  logo: string | null;
}

/** Una fila de la whitelist de correos permitidos. */
export interface CorreoPermitido {
  id: number;
  correo: string;
}

/**
 * Configuración de la instancia. El nombre y el logo se comparten desde esta
 * misma señal con el acceso, el marco y el título de la pestaña: los tres
 * tienen que pintar lo mismo. `cargar()` la pide una sola vez, al arrancar de
 * verdad — la dispara `app.config.ts`, no el constructor de este servicio: si
 * arrancara aquí, cualquier test que inyecte este servicio (o el `TitleStrategy`,
 * que lo inyecta en cada navegación) dispararía la petición sin poder
 * responderla, y ninguno de los flujos existentes la espera.
 *
 * La whitelist va aparte, sin señal compartida: solo la pide la pantalla de
 * configuración, y solo si quien mira es admin.
 */
@Injectable({ providedIn: 'root' })
export class CentroService {
  private readonly http = inject(HttpClient);
  private readonly document = inject(DOCUMENT);

  /** Por defecto mientras no llega la respuesta, o si el centro no está configurado. */
  readonly centro = signal<Centro>({ nombre: 'Practikalia', logo: null });

  async cargar(): Promise<void> {
    try {
      const centro = await firstValueFrom(this.http.get<Centro>('/api/centro'));
      this.centro.set(centro);
      if (centro.logo) {
        this.actualizarFavicon(centro.logo);
      }
    } catch {
      // Sin respuesta se queda 'Practikalia' y el favicon de siempre: nada se rompe.
    }
  }

  async actualizar(nombre: string): Promise<Centro> {
    const centro = await firstValueFrom(this.http.put<Centro>('/api/centro', { nombre }));
    this.centro.set(centro);
    return centro;
  }

  async subirLogo(fichero: File): Promise<Centro> {
    const cuerpo = new FormData();
    cuerpo.append('fichero', fichero);
    const centro = await firstValueFrom(this.http.post<Centro>('/api/centro/logo', cuerpo));
    this.centro.set(centro);
    if (centro.logo) {
      this.actualizarFavicon(centro.logo);
    }
    return centro;
  }

  listarCorreos(): Promise<CorreoPermitido[]> {
    return firstValueFrom(this.http.get<CorreoPermitido[]>('/api/correos-permitidos'));
  }

  crearCorreo(correo: string): Promise<CorreoPermitido> {
    return firstValueFrom(this.http.post<CorreoPermitido>('/api/correos-permitidos', { correo }));
  }

  borrarCorreo(id: number): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`/api/correos-permitidos/${id}`));
  }

  private actualizarFavicon(logo: string): void {
    this.document.querySelector<HTMLLinkElement>('link[rel="icon"]')?.setAttribute('href', logo);
  }
}
