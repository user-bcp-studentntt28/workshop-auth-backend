# Proposal

## Why

El [Issue #11 - HU-01](https://github.com/user-bcp-studentntt28/workshop-auth-backend/issues/11) exige tokens firmados, claims minimos y expiracion verificable para que los consumidores puedan confiar en la identidad y vigencia recibidas. El [contrato del Dia 1](../../../docs/ENDPOINTS.md) documenta un token reversible que incluye el secreto; corregirlo importa sin alterar innecesariamente el resto del login.

## What Changes

### Alcance comprometido

- Emitir tokens firmados con `sub`, `client`, `iat` y `exp`, sin secreto embebido.
- **BREAKING**: exigir `client` string no vacio ni compuesto solo de espacios; su ausencia o invalidez produce 400. Decision confirmada por el solicitante: conservar exactamente el valor recibido, sin normalizarlo ni agregar una lista de clientes permitidos.
- **BREAKING**: sustituir el contenido Base64 legacy de `token`. Los consumidores no deben depender de su representacion anterior; su adaptacion en otros repositorios queda fuera del trabajo.
- Preservar 3600 segundos de vigencia y exigir coherencia de `expiresIn` con `exp - now`. Convencion verificable propuesta: segundos Unix UTC y tolerancia absoluta maxima de 1 segundo, con `now` observado al emitir la respuesta, no al recibirla por red.
- Declarar invalido un token al alcanzar `exp`, sin periodo de gracia.
- Documentar `AUTH_TOKEN_SECRET`; impedir el uso de un secreto por defecto fuera del laboratorio y evitar tokens, secretos o contrasenas en logs.
- Exigir evidencia futura de login valido, credenciales invalidas, claims, firma, expiracion y logs redactados.

### Comportamiento que se preserva

- `POST /auth/login` sigue siendo publico y JSON. Para solicitudes validas conserva 200 y los campos `token`, `expiresIn` entero, `user.username`, `user.displayName` escapado en HTML, `user.role` y `debug` con `authenticated-with-legacy-flow`.
- Se conservan los errores 400 de request, username y password; el 401 de credenciales invalidas y su mensaje; y el contrato 500 documentado, sin aprovechar este cambio para reformarlo.
- `GET /q/health`, persistencia, roles y autenticacion de usuarios existentes no cambian. Las preguntas abiertas del baseline sobre JSON malformado, Content-Type y contenido de health checks no se convierten en nuevas garantias.

### Fuera de alcance

- Implementar codigo, ejecutar refactors, crear endpoints de validacion, refresh, logout o revocacion, cambiar duracion, roles, contrasenas, SQL, CORS o manejo general de errores.
- Leer o modificar otros repositorios y adaptar sus consumidores.
- Elegir formato de token, algoritmo, HMAC frente a biblioteca JWT, dependencias, capas o mecanismo de validacion. El Issue exige documentar esa decision posteriormente; queda expresamente pendiente.
- Crear `design.md` o `tasks.md`. Esta entrega comprende solo propuesta y specs, no habilita implementacion.

## Capabilities

### New Capabilities

- `login-authentication`: contrato observable del login, validacion de client y compatibilidad con las respuestas del Dia 1.
- `signed-auth-tokens`: autenticidad, claims, vigencia, configuracion del secreto y confidencialidad de logs.

### Modified Capabilities

Ninguna: no existen specs principales en `openspec/specs/`. Se usa ADDED para incorporar contratos a OpenSpec, no para declarar que todos estos comportamientos sean nuevos.

## Impact

- Fuentes: Issue #11, sin comentarios al consultarlo, y contrato del Dia 1. El Issue dice que ya existen HMAC y `AUTH_TOKEN_SECRET`; el codigo local inspeccionado y el baseline muestran Base64 con secreto hardcodeado. Se registra la discrepancia y se exige el resultado del Issue, sin asumir esa implementacion previa ni decidir una alternativa.
- Superficies afectadas en una implementacion futura: login, emision/validacion de tokens, configuracion y documentacion del secreto, pruebas y evidencia redactada. No se presupone un endpoint que consuma tokens para comprobar expiracion.
- Riesgos de compatibilidad: clientes que omitan `client` recibiran 400; quienes interpreten el token legacy necesitaran adaptacion. Solo esas diferencias se autorizan por el Issue y la confirmacion del solicitante.
- La evidencia de aceptacion debera cubrir los escenarios de ambas specs y la tolerancia temporal declarada. Esta entrega no ejecuta ni acredita esas pruebas funcionales.
