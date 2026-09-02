import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AcordeonComponent } from './acordeon';

@Component({
  imports: [AcordeonComponent],
  template: `<app-acordeon etiqueta="Menú" [(abierto)]="abierto">
    <p>Contenido plegado</p>
  </app-acordeon>`,
})
class Anfitrion {
  readonly abierto = signal(false);
}

describe('acordeón', () => {
  it('el botón abre y cierra, y el estado sube a quien lo usa', () => {
    const fixture = TestBed.createComponent(Anfitrion);
    fixture.detectChanges();
    const boton: HTMLButtonElement = fixture.nativeElement.querySelector('.c-acordeon__boton');
    const contenido: HTMLElement = fixture.nativeElement.querySelector('.c-acordeon__contenido');

    expect(boton.getAttribute('aria-expanded')).toBe('false');
    expect(boton.getAttribute('aria-controls')).toBe(contenido.id);
    expect(contenido.hidden).toBe(true);

    boton.click();
    fixture.detectChanges();

    expect(boton.getAttribute('aria-expanded')).toBe('true');
    expect(contenido.hidden).toBe(false);
    // El `model` deja el estado al alcance de quien lo abre desde fuera.
    expect(fixture.componentInstance.abierto()).toBe(true);

    boton.click();
    fixture.detectChanges();

    expect(fixture.componentInstance.abierto()).toBe(false);
  });
});
