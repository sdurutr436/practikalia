import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideHttpClient, withInterceptors, withXsrfConfiguration } from '@angular/common/http';
import { TitleStrategy, provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { authInterceptor } from './auth/auth.interceptor';
import { CentroService } from './centro/centro.service';
import { TituloPractikalia } from './compartido/titulo';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    { provide: TitleStrategy, useClass: TituloPractikalia },
    provideHttpClient(
      // Cookie XSRF-TOKEN de Spring → cabecera X-XSRF-TOKEN en cada mutación.
      withXsrfConfiguration({ cookieName: 'XSRF-TOKEN', headerName: 'X-XSRF-TOKEN' }),
      withInterceptors([authInterceptor]),
    ),
    // Sin `return`: no bloquea el arranque. El nombre y el logo se quedan en
    // su valor de respaldo hasta que la petición resuelve.
    provideAppInitializer(() => void inject(CentroService).cargar()),
  ],
};
