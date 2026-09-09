---
sidebar_position: 1
---

# Testing

## Backend

```bash
cd backend
./mvnw test
```

JUnit 5 + Spring Boot Test (unitarios y de integración), corriendo sobre **H2 en memoria** — no hace falta Postgres real ni Docker levantado para ejecutar los tests. JaCoCo mide la cobertura y falla el build si baja del **80% de líneas** (`jacoco:check` en `backend/pom.xml`).

## Frontend

```bash
cd frontend
pnpm test
```

Vitest a través del builder nativo de Angular, con jsdom (sin navegador real). Para ver la cobertura en local:

```bash
pnpm exec ng test --coverage
```

El CI de `desarrollo` exige que la cobertura de líneas no baje del **80%** (ver [CI/CD](../operations/ci-cd.md)); si tu cambio la reduce, el job `frontend-tests` fallará.

:::tip
jsdom no implementa todo lo que hace un navegador real — por ejemplo, `Element.scrollIntoView` no existe. Si un test lanza un error de un método "not implemented", comprueba si el propio código de producción puede protegerse con encadenamiento opcional (`?.`) en vez de asumir que el método siempre existe.
:::

## Qué no hay todavía

No hay tests end-to-end ni de integración con navegador real. El build de producción del frontend (`pnpm build`) se ejecuta en CI como comprobación adicional, ya que no hay un paso de lint/type-check separado expuesto como script de npm.
