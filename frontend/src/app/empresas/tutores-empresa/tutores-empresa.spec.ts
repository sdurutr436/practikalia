import { Component, inject } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { filaTutor, TutoresEmpresaComponent } from './tutores-empresa';

@Component({
  imports: [ReactiveFormsModule, TutoresEmpresaComponent],
  template: `<app-tutores-empresa [tutores]="tutores" />`,
})
class Anfitrion {
  private readonly fb = inject(NonNullableFormBuilder);
  readonly tutores = this.fb.array([filaTutor(this.fb)]);
}

describe('TutoresEmpresaComponent', () => {
  it('añade una fila y nunca deja quitar la última', () => {
    const fixture = TestBed.createComponent(Anfitrion);
    fixture.detectChanges();
    const raiz = fixture.nativeElement as HTMLElement;
    const { tutores } = fixture.componentInstance;

    expect(tutores.length).toBe(1);
    expect(raiz.querySelector<HTMLButtonElement>('.c-tutores__quitar')?.disabled).toBe(true);

    raiz.querySelector<HTMLButtonElement>('app-boton button')?.click();
    fixture.detectChanges();
    expect(tutores.length).toBe(2);
    expect(raiz.querySelector<HTMLButtonElement>('.c-tutores__quitar')?.disabled).toBe(false);

    raiz.querySelectorAll<HTMLButtonElement>('.c-tutores__quitar')[0].click();
    fixture.detectChanges();
    expect(tutores.length).toBe(1);

    // La última no se puede quitar: el backend exige al menos un tutor.
    raiz.querySelector<HTMLButtonElement>('.c-tutores__quitar')?.click();
    fixture.detectChanges();
    expect(tutores.length).toBe(1);
  });
});
