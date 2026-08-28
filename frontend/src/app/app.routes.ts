import { Routes } from '@angular/router';
import { autenticadoGuard, cambioContrasenaPendienteGuard, profesorGuard } from './auth/auth.guards';
import { LoginPage } from './auth/login-page/login-page';
import { CambiarContrasenaPage } from './auth/cambiar-contrasena-page/cambiar-contrasena-page';
import { EmpresasListadoPage } from './empresas/empresas-listado-page/empresas-listado-page';
import { EmpresaDetallePage } from './empresas/empresa-detalle-page/empresa-detalle-page';
import { EmpresaFormularioPage } from './empresas/empresa-formulario-page/empresa-formulario-page';
import { AsignacionFormularioPage } from './asignaciones/asignacion-formulario-page/asignacion-formulario-page';
import { AlumnoAsignacionesPage } from './asignaciones/alumno-asignaciones-page/alumno-asignaciones-page';

export const routes: Routes = [
  { path: 'login', component: LoginPage },
  {
    path: 'cambiar-contrasena',
    component: CambiarContrasenaPage,
    canActivate: [cambioContrasenaPendienteGuard],
  },
  { path: 'empresas', component: EmpresasListadoPage, canActivate: [autenticadoGuard] },
  // Rutas literales antes de ':id' — si no, 'nueva' se interpretaría como un id.
  { path: 'empresas/nueva', component: EmpresaFormularioPage, canActivate: [profesorGuard] },
  { path: 'empresas/:id/editar', component: EmpresaFormularioPage, canActivate: [profesorGuard] },
  {
    path: 'empresas/:empresaId/asignaciones/nueva',
    component: AsignacionFormularioPage,
    canActivate: [profesorGuard],
  },
  { path: 'empresas/:id', component: EmpresaDetallePage, canActivate: [autenticadoGuard] },
  { path: 'alumnos/:alumnoId/asignaciones', component: AlumnoAsignacionesPage, canActivate: [profesorGuard] },
  { path: '', pathMatch: 'full', redirectTo: 'empresas' },
  { path: '**', redirectTo: 'empresas' },
];
