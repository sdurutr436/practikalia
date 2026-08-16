import { Routes } from '@angular/router';
import { autenticadoGuard, cambioContrasenaPendienteGuard } from './auth/auth.guards';
import { LoginPage } from './auth/login-page/login-page';
import { CambiarContrasenaPage } from './auth/cambiar-contrasena-page/cambiar-contrasena-page';
import { GeneralPage } from './general-page/general-page';

export const routes: Routes = [
  { path: 'login', component: LoginPage },
  {
    path: 'cambiar-contrasena',
    component: CambiarContrasenaPage,
    canActivate: [cambioContrasenaPendienteGuard],
  },
  { path: '', pathMatch: 'full', component: GeneralPage, canActivate: [autenticadoGuard] },
  { path: '**', redirectTo: '' },
];
