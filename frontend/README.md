# Frontend de Alimentarse

Aplicación Angular 22 (componentes standalone, zoneless y tests con Vitest). La puesta en marcha completa está en el [README raíz](../README.md).

| Comando                                   | Qué hace                                                                                     |
| ----------------------------------------- | -------------------------------------------------------------------------------------------- |
| `npm start`                               | Servidor de desarrollo en http://localhost:4200, con proxy de `/api` a http://localhost:8080 |
| `npm test`                                | Tests unitarios (Vitest, una sola pasada)                                                    |
| `npm run lint`                            | ESLint                                                                                       |
| `npm run format` / `npm run format:check` | Prettier                                                                                     |
| `npm run build`                           | Build de producción en `../backend/src/main/resources/static`                                |
