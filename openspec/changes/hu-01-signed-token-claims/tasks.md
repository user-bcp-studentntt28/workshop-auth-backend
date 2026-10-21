# Tasks

El [Issue #11 - HU-01](https://github.com/user-bcp-studentntt28/workshop-auth-backend/issues/11) es la unidad de trabajo del producto. Este archivo contiene exclusivamente el checklist de caracterizacion previa del cambio OpenSpec `hu-01-signed-token-claims`; no crea historias nuevas ni amplia el alcance.

Fuentes aprobadas: [proposal.md](proposal.md), [login-authentication](specs/login-authentication/spec.md), [signed-auth-tokens](specs/signed-auth-tokens/spec.md) y [design.md](design.md). Se preserva el [contrato del Dia 1](../../../docs/ENDPOINTS.md) salvo las dos rupturas autorizadas: client obligatorio y nuevo contenido del token.

Este checklist registra exclusivamente la caracterizacion previa a la implementacion funcional de HU-01. Las ocho tareas estan completadas con evidencia en [docs/TESTING.md](../../../docs/TESTING.md); el 8/8 se refiere solo a ese trabajo y significa que la implementacion funcional de HU-01 a HU-05 estan completas.

Este checklist registra la proteccion del comportamiento existente antes de modificar autenticacion. Cada tarea se marca solo con evidencia real.

Todo trabajo, prueba, dato temporal y operacion Git/GitHub se limita a workshop-auth-backend, sobre feature/hu-01. No acceder a consumidores de otros repositorios. No crear endpoints, cambiar SQL, passwords persistidas, CORS, roles o errores generales, ni efectuar un rediseno global. Usar las herramientas de pruebas existentes; agregar soporte de test solo si una prueba concreta lo requiere.

## 1. Caracterizacion antes del cambio funcional

- [x] 1.1 Preparar fixtures de prueba con SQLite temporal dentro del proyecto y configurar AUTH_DB_URL antes de inicializar Quarkus; verificar que la prueba existente pasa y que [data/workshop.db](../../../data/workshop.db) conserva su hash antes/despues.
- [x] 1.2 Ampliar AuthResourceTest para login publico de student y trainer; verificar 200 JSON, token string no vacio, expiresIn entero 3600, username/role/displayName persistidos y debug exacto, sin fijar el token completo ni su timestamp.
- [x] 1.3 Agregar casos de request nulo y username/password ausentes, null, vacios y blancos, incluidos errores simultaneos; verificar los 400, mensajes exactos, ausencia de token y precedencia request -> username -> password.
- [x] 1.4 Agregar casos de usuario inexistente y password incorrecta; verificar 401, mensaje `Invalid user <username> or password` y ausencia de token con entradas estructuralmente validas.
- [x] 1.5 Agregar fixtures de nombre con HTML y pruebas unitarias controladas del resource para RuntimeException con/sin mensaje; verificar escape de displayName, rol sin cambios y los cuerpos 500 del baseline sin provocar fallos en la base compartida.
- [x] 1.6 Agregar caracterizacion de GET /q/health sin Authorization; verificar acceso publico y estructura status/checks sin fijar checks dinamicos ni introducir nuevas garantias de estados no observados.
- [x] 1.7 Caracterizar transitoriamente client opcional/ignorado y estructura del token legacy; verificar que esos casos pasan hoy, sin copiar el secreto hardcodeado en nuevos fixtures y dejandolos identificados como las dos expectativas que se reemplazaran.
- [x] 1.8 Ejecutar `mvn "-Dtest=AuthResourceTest" test` y los selectores de las pruebas unitarias de caracterizacion agregadas; registrar comandos, cantidades y resultados aprobados como punto de comparacion antes de cualquier refactor funcional.

