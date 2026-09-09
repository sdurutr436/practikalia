---
sidebar_position: 3
---

# Frontend

Angular 22, TypeScript, pnpm. Componentes standalone con selector `app-*` (convención habitual de Angular) organizados por dominio bajo `frontend/src/app/` (`alumnado/`, `empresas/`, `auth/`, `compartido/`, ...) — el mismo criterio de "por dominio, no por capa" que el backend.

## Sin CSS por componente

Ningún componente Angular tiene `styleUrls` ni un `.scss` propio. Todo el CSS vive centralizado en `frontend/src/styles/`, organizado con **ITCSS** (Inverted Triangle CSS): las capas se cargan en orden de especificidad creciente, de lo más genérico a lo más concreto.

```text
frontend/src/styles/
├── settings/     # tokens: color, tipografía, espaciado, z-index, radios...
├── tools/        # mixins y funciones Sass, sin salida CSS propia
├── generic/      # reset y estilos base sin clase
├── elements/     # selectores de elemento HTML sin clase (h1, a, button...)
├── objects/      # patrones de layout reutilizables (prefijo o-)
├── components/   # componentes visuales con identidad propia (prefijo c-)
└── utilities/    # una sola responsabilidad, máxima especificidad (prefijo u-)
```

Las clases siguen **BEM** dentro de esa jerarquía (`c-tarjeta__titulo--destacado`), y los prefijos `o-`/`c-`/`u-` dejan ver a qué capa de ITCSS pertenece una clase con solo mirar su nombre en el HTML.

## Tokens completos, no solo lo que se usa hoy

Al traducir un valor de diseño a `settings/` (un color, una escala tipográfica, una sombra) se traduce la hoja completa, no únicamente los valores que el componente de turno necesita en ese momento — evita tokens a medias que alguien tiene que completar después bajo presión.

## Testing

Vitest a través del builder nativo de Angular (`@angular/build:unit-test`), con jsdom — no necesita un navegador real ni modo headless de Chrome. Ver [Testing](../development/testing.md).
