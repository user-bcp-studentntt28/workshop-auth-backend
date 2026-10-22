# Definition of Ready - HU-01

Evaluacion del cambio `hu-01-signed-token-claims` al 2026-10-21. La tabla recoge la evidencia disponible para decidir si el trabajo esta listo para implementarse; no declara completada la implementacion funcional.

| Criterio | Estado | Evidencia | Observacion |
|---|---|---|---|
| Emitir token firmado con `sub`, `client`, `iat` y `exp` | BLOQUEADO | [Issue #11](https://github.com/user-bcp-studentntt28/workshop-auth-backend/issues/11) y [signed-auth-tokens/spec.md](../openspec/changes/hu-01-signed-token-claims/specs/signed-auth-tokens/spec.md) lo requieren. | No hay evidencia de implementacion ni PR. |
| Rechazar tokens alterados o expirados y aceptar los validos | BLOQUEADO | Issue #11 y escenarios de [signed-auth-tokens/spec.md](../openspec/changes/hu-01-signed-token-claims/specs/signed-auth-tokens/spec.md). | El reporte disponible cubre caracterizacion, no firma o expiracion. |
| Exigir `client` y conservar su valor exacto | BLOQUEADO | [login-authentication/spec.md](../openspec/changes/hu-01-signed-token-claims/specs/login-authentication/spec.md) lo especifica. | No se implemento ni verifico ese nuevo contrato. |
| Preservar el comportamiento de login caracterizado | CUMPLIDO | [AuthResourceTest.txt](../target/surefire-reports/com.bcp.workshop.auth.AuthResourceTest.txt): 11 tests, 0 fallos, 0 errores. | Solo acredita el baseline existente. |
| Configurar el secreto y evitar filtrarlo en logs | BLOQUEADO | Requisitos en [signed-auth-tokens/spec.md](../openspec/changes/hu-01-signed-token-claims/specs/signed-auth-tokens/spec.md). | No hay pruebas o implementacion funcional que lo acredite. |
| Coherencia de proposal, design y tasks | BLOQUEADO | [proposal.md](../openspec/changes/hu-01-signed-token-claims/proposal.md), [design.md](../openspec/changes/hu-01-signed-token-claims/design.md) y [tasks.md](../openspec/changes/hu-01-signed-token-claims/tasks.md). | Proposal excluye implementar y deja pendiente elegir formato; design decide JWT/HS256. `tasks.md` afirma que la implementacion funcional de HU-01 a HU-05 esta completa, sin evidencia que lo respalde. |
| PR final y checks de GitHub | PENDIENTE DE OTRO GATE | GitHub no muestra un PR para `feature/hu-01` hacia `main`. | Sin PR, no hay checks asociados que evaluar. |
| Uso del Skill solicitado | BLOQUEADO | No se encontro ese Skill dentro de `workshop-auth-backend`. | No se consultaron archivos fuera del repo para respetar el limite de alcance. |
