---
sidebar_position: 2
---

# Contribución

## Ramas

- `desarrollo` es la rama de integración: todo el trabajo (features, fixes) se hace ahí o en una rama propia que se fusiona de vuelta en `desarrollo`.
- `main` es código ya integrado y probado en `desarrollo`, listo para distribuir. La integración va siempre en un solo sentido: `desarrollo` → `main`.
- Ninguna otra rama dispara CI — ver la [matriz de triggers](../operations/ci-cd.md).

## Commits

El histórico sigue [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) (`feat:`, `fix:`, `docs:`, `refactor:`, `ci:`...), en español y en una sola línea. Ejemplos reales del repositorio:

```text
fix(estilos): corregir clases BEM sin bloque raíz
refactor(auth): separar login y registro en componentes propios
ci(docker): publicar imagenes beta y estable en Docker Hub
```

## Pull requests

- Un PR hacia `desarrollo` ejecuta tests (backend y frontend) y el escaneo de seguridad con Trivy.
- Un PR hacia `main` valida que la documentación compila y que ambas imágenes Docker construyen, pero no publica nada.
- Ningún PR publica imágenes Docker ni documentación — eso solo ocurre al fusionar (push directo a la rama destino).

## Antes de abrir un PR

```bash
cd backend && ./mvnw test
cd frontend && pnpm test && pnpm build
```

Ver [Testing](testing.md) para el detalle de qué cubre cada suite y el umbral de cobertura exigido.
