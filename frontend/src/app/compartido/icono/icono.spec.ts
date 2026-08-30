import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { IconoComponent } from './icono';

@Component({
  imports: [IconoComponent],
  template: `<app-icono nombre="correo" />`,
})
class AnfitrionCorreo {}

@Component({
  imports: [IconoComponent],
  template: `<app-icono nombre="interesActivo" />`,
})
class AnfitrionInteresActivo {}

describe('IconoComponent', () => {
  it('renderiza un path distinto por cada nombre', () => {
    const correo = TestBed.createComponent(AnfitrionCorreo);
    correo.detectChanges();
    const interesActivo = TestBed.createComponent(AnfitrionInteresActivo);
    interesActivo.detectChanges();

    const dCorreo = correo.nativeElement.querySelector('path').getAttribute('d');
    const dInteres = interesActivo.nativeElement.querySelector('path').getAttribute('d');

    expect(dCorreo).toBeTruthy();
    expect(dInteres).toBeTruthy();
    expect(dCorreo).not.toEqual(dInteres);
  });
});
