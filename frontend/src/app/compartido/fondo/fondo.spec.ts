import { TestBed } from '@angular/core/testing';
import { FondoComponent } from './fondo';

describe('FondoComponent', () => {
  beforeEach(() => TestBed.configureTestingModule({ imports: [FondoComponent] }));

  // Si .c-fondo vuelve a un <div> interno, el anfitrión queda `display: inline`
  // y dentro de un contenedor grid pasa a ser un ítem más de la rejilla: ocupa
  // celda y empuja al resto de la página hacia abajo.
  it('saca del flujo al anfitrión, no a un hijo', () => {
    const fixture = TestBed.createComponent(FondoComponent);
    fixture.detectChanges();

    expect([...fixture.nativeElement.classList]).toContain('c-fondo');
    expect(fixture.nativeElement.querySelector('.c-fondo')).toBeNull();
    expect(fixture.nativeElement.getAttribute('aria-hidden')).toBe('true');
    expect(fixture.nativeElement.querySelectorAll('.c-fondo__forma').length).toBe(12);
  });
});
