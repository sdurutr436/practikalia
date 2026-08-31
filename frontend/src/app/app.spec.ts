import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { App } from './app';
import { AuthService } from './auth/auth.service';
import { ProximamentePage } from './marco/proximamente-page';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([
          { path: 'login', component: ProximamentePage },
          { path: 'empresas', component: ProximamentePage },
        ]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('envuelve las páginas con el marco, salvo las de sesión', async () => {
    const router = TestBed.inject(Router);
    TestBed.inject(AuthService).sesion.set({
      id: 1,
      correo: 'quien@centro.es',
      rol: 'ALUMNO',
      esAdmin: false,
      debeCambiarContrasena: false,
    });
    const fixture = TestBed.createComponent(App);

    await router.navigateByUrl('/login');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-marco')).toBeNull();

    await router.navigateByUrl('/empresas');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-marco')).not.toBeNull();
  });
});
