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
import { ReviewsPage } from './reviews/reviews-page/reviews-page';
import { MisInteresesPage } from './intereses/mis-intereses-page/mis-intereses-page';
import { MisEtiquetasPage } from './perfil/mis-etiquetas-page/mis-etiquetas-page';
import { AfinidadPage } from './afinidad/afinidad-page/afinidad-page';
import { AlumnoFormularioPage } from './alumnado/alumno-formulario-page/alumno-formulario-page';
import { AlumnadoPage } from './alumnado/alumnado-page/alumnado-page';

export const routes: Routes = [
  // El marco (menú lateral + barra superior) lo pone app.ts alrededor del
  // router-outlet; login y cambio de contraseña se pintan sin él.
  { path: 'login', component: LoginPage, title: 'Acceso' },
  {
    path: 'cambiar-contrasena',
    component: CambiarContrasenaPage,
    title: 'Cambiar contraseña',
    canActivate: [cambioContrasenaPendienteGuard],
  },
  {
    path: 'panel',
    component: PanelPage,
    title: 'Panel principal',
    canActivate: [autenticadoGuard],
  },
  {
    path: 'empresas',
    component: EmpresasListadoPage,
    title: 'Repositorio de empresas',
    canActivate: [autenticadoGuard],
  },
  {
    path: 'mis-intereses',
    component: MisInteresesPage,
    title: 'Mis intereses',
    canActivate: [alumnoGuard],
  },
  {
    path: 'mis-etiquetas',
    component: MisEtiquetasPage,
    title: 'Mis etiquetas',
    canActivate: [alumnoGuard],
  },
  {
    path: 'mi-afinidad',
    component: AfinidadPage,
    title: 'Mi afinidad',
    canActivate: [alumnoGuard],
  },
  // Rutas literales antes de ':id' — si no, 'nueva' se interpretaría como un id.
  {
    path: 'empresas/nueva',
    component: EmpresaFormularioPage,
    title: 'Nueva empresa',
    canActivate: [profesorGuard],
  },
  {
    path: 'empresas/:id/editar',
    title: 'Editar empresa',
    component: EmpresaFormularioPage,
    canActivate: [profesorGuard],
  },
  {
    path: 'empresas/:empresaId/asignaciones/nueva',
    title: 'Nueva asignación',
    component: AsignacionFormularioPage,
    canActivate: [profesorGuard],
  },
  {
    path: 'empresas/:id',
    component: EmpresaDetallePage,
    title: 'Ficha de empresa',
    canActivate: [autenticadoGuard],
  },
  {
    path: 'alumnos/nuevo',
    component: AlumnoFormularioPage,
    title: 'Nuevo alumno',
    canActivate: [profesorGuard],
  },
  {
    path: 'alumnos/:alumnoId/asignaciones',
    title: 'Asignaciones del alumnado',
    component: AlumnoAsignacionesPage,
    canActivate: [profesorGuard],
  },
  {
    path: 'alumnos/:alumnoId/afinidad',
    component: AfinidadPage,
    title: 'Afinidad del alumnado',
    canActivate: [profesorGuard],
  },
  // Una sola pantalla; la pastilla (pendientes/aprobadas/rechazadas) viaja en
  // `?estado=`, igual que `?publicada=` en el listado de empresas.
  {
    path: 'reviews',
    title: 'Reseñas',
    component: ReviewsPage,
    canActivate: [profesorGuard],
  },
  {
    path: 'reviews/nueva',
    component: ReviewFormularioPage,
    title: 'Nueva reseña',
    canActivate: [autenticadoGuard],
  },
  {
    path: 'reviews/:id/editar',
    title: 'Editar reseña',
    component: ReviewFormularioPage,
    canActivate: [autenticadoGuard],
  },

  // Secciones del menú que todavía no tienen pantalla. Las rutas existen ya
  // para que el menú no cambie de forma según se vayan construyendo: crear
  // una pantalla es cambiar su `component` aquí y nada más.
  {
    path: 'alumnado',
    component: AlumnadoPage,
    title: 'Alumnado',
    canActivate: [profesorGuard],
  },
  {
    path: 'asignaciones',
    component: ProximamentePage,
    title: 'Asignaciones',
    canActivate: [profesorGuard],
  },
  {
    path: 'sectores',
    component: ProximamentePage,
    title: 'Sectores y etiquetas',
    canActivate: [profesorGuard],
  },
  {
    path: 'actividad',
    component: ProximamentePage,
    title: 'Actividad',
    canActivate: [profesorGuard],
  },
  {
    path: 'profesorado',
    component: ProximamentePage,
    title: 'Profesorado',
    canActivate: [profesorGuard],
  },
  {
    path: 'mis-resenas',
    component: ProximamentePage,
    title: 'Mis reseñas',
    canActivate: [alumnoGuard],
  },
  {
    path: 'mi-empresa',
    component: ProximamentePage,
    title: 'Mi empresa',
    canActivate: [alumnoGuard],
  },
  {
    path: 'mis-documentos',
    component: ProximamentePage,
    title: 'Mis documentos',
    canActivate: [alumnoGuard],
  },
  {
    path: 'configuracion',
    component: ProximamentePage,
    title: 'Configuración',
    canActivate: [autenticadoGuard],
  },

  { path: '', pathMatch: 'full', redirectTo: 'empresas' },
  { path: '**', redirectTo: 'empresas' },
];
