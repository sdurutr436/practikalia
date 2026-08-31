import { Routes } from '@angular/router';
import {
  alumnoGuard,
  autenticadoGuard,
  cambioContrasenaPendienteGuard,
  profesorGuard,
} from './auth/auth.guards';
import { LoginPage } from './auth/login-page/login-page';
import { CambiarContrasenaPage } from './auth/cambiar-contrasena-page/cambiar-contrasena-page';
import { ProximamentePage } from './marco/proximamente-page';
import { PanelPage } from './panel/panel-page';
import { EmpresasListadoPage } from './empresas/empresas-listado-page/empresas-listado-page';
import { EmpresaDetallePage } from './empresas/empresa-detalle-page/empresa-detalle-page';
import { EmpresaFormularioPage } from './empresas/empresa-formulario-page/empresa-formulario-page';
import { AsignacionFormularioPage } from './asignaciones/asignacion-formulario-page/asignacion-formulario-page';
import { AlumnoAsignacionesPage } from './asignaciones/alumno-asignaciones-page/alumno-asignaciones-page';
import { ReviewFormularioPage } from './reviews/review-formulario-page/review-formulario-page';
import { ReviewsPendientesPage } from './reviews/reviews-pendientes-page/reviews-pendientes-page';
import { MisInteresesPage } from './intereses/mis-intereses-page/mis-intereses-page';
import { MisEtiquetasPage } from './perfil/mis-etiquetas-page/mis-etiquetas-page';
import { AfinidadPage } from './afinidad/afinidad-page/afinidad-page';

export const routes: Routes = [
  // El marco (menú lateral + barra superior) lo pone app.ts alrededor del
  // router-outlet; login y cambio de contraseña se pintan sin él.
  { path: 'login', component: LoginPage },
  {
    path: 'cambiar-contrasena',
    component: CambiarContrasenaPage,
    canActivate: [cambioContrasenaPendienteGuard],
  },
  { path: 'panel', component: PanelPage, canActivate: [autenticadoGuard] },
  { path: 'empresas', component: EmpresasListadoPage, canActivate: [autenticadoGuard] },
  { path: 'mis-intereses', component: MisInteresesPage, canActivate: [alumnoGuard] },
  { path: 'mis-etiquetas', component: MisEtiquetasPage, canActivate: [alumnoGuard] },
  { path: 'mi-afinidad', component: AfinidadPage, canActivate: [alumnoGuard] },
  // Rutas literales antes de ':id' — si no, 'nueva' se interpretaría como un id.
  { path: 'empresas/nueva', component: EmpresaFormularioPage, canActivate: [profesorGuard] },
  {
    path: 'empresas/:id/editar',
    component: EmpresaFormularioPage,
    canActivate: [profesorGuard],
  },
  {
    path: 'empresas/:empresaId/asignaciones/nueva',
    component: AsignacionFormularioPage,
    canActivate: [profesorGuard],
  },
  { path: 'empresas/:id', component: EmpresaDetallePage, canActivate: [autenticadoGuard] },
  {
    path: 'alumnos/:alumnoId/asignaciones',
    component: AlumnoAsignacionesPage,
    canActivate: [profesorGuard],
  },
  { path: 'alumnos/:alumnoId/afinidad', component: AfinidadPage, canActivate: [profesorGuard] },
  {
    path: 'reviews/pendientes',
    component: ReviewsPendientesPage,
    canActivate: [profesorGuard],
  },
  { path: 'reviews/nueva', component: ReviewFormularioPage, canActivate: [autenticadoGuard] },
  {
    path: 'reviews/:id/editar',
    component: ReviewFormularioPage,
    canActivate: [autenticadoGuard],
  },

  // Secciones del menú que todavía no tienen pantalla. Las rutas existen ya
  // para que el menú no cambie de forma según se vayan construyendo: crear
  // una pantalla es cambiar su `component` aquí y nada más.
  { path: 'alumnado', component: ProximamentePage, canActivate: [profesorGuard] },
  { path: 'asignaciones', component: ProximamentePage, canActivate: [profesorGuard] },
  { path: 'sectores', component: ProximamentePage, canActivate: [profesorGuard] },
  { path: 'actividad', component: ProximamentePage, canActivate: [profesorGuard] },
  { path: 'profesorado', component: ProximamentePage, canActivate: [profesorGuard] },
  { path: 'mis-resenas', component: ProximamentePage, canActivate: [alumnoGuard] },
  { path: 'mi-empresa', component: ProximamentePage, canActivate: [alumnoGuard] },
  { path: 'mis-documentos', component: ProximamentePage, canActivate: [alumnoGuard] },
  { path: 'configuracion', component: ProximamentePage, canActivate: [autenticadoGuard] },

  { path: '', pathMatch: 'full', redirectTo: 'empresas' },
  { path: '**', redirectTo: 'empresas' },
];
