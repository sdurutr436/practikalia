import { Routes } from '@angular/router';
import {
  adminGuard,
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
import { AlumnadoPage } from './alumnado/alumnado-page/alumnado-page';
import { AsignacionesPage } from './asignaciones/asignaciones-page/asignaciones-page';
import { SectoresPage } from './sectores/sectores-page/sectores-page';
import { ProfesoradoPage } from './profesorado/profesorado-page/profesorado-page';

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

  {
    path: 'alumnado',
    component: AlumnadoPage,
    title: 'Alumnado',
    canActivate: [profesorGuard],
  },

  // Una sola pantalla; la pastilla (todas/asignados/sin asignar) viaja en
  // `?estado=`, igual que en alumnado y reseñas.
  {
    path: 'asignaciones',
    component: AsignacionesPage,
    title: 'Asignaciones',
    canActivate: [profesorGuard],
  },

  // El catálogo toca la clasificación de todas las empresas y la afinidad del
  // alumnado, así que es de administradores y no de todo el profesorado.
  {
    path: 'sectores',
    component: SectoresPage,
    title: 'Sectores y etiquetas',
    canActivate: [adminGuard],
  },

  // El profesorado lo consulta cualquier profesor; dar de alta y editar es
  // solo del admin, y eso lo decide el propio backend.
  {
    path: 'profesorado',
    component: ProfesoradoPage,
    title: 'Profesorado',
    canActivate: [profesorGuard],
  },

  // Secciones del menú que todavía no tienen pantalla. Las rutas existen ya
  // para que el menú no cambie de forma según se vayan construyendo: crear
  // una pantalla es cambiar su `component` aquí y nada más.
  //
  // `actividad` está fuera del menú a propósito: la sección no tiene sentido en
  // esta aplicación, pero la ruta sigue respondiendo si se escribe a mano.
  {
    path: 'actividad',
    component: ProximamentePage,
    title: 'Actividad',
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
