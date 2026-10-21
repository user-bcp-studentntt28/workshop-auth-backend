# Testing

Este documento registra la estrategia y evidencia de pruebas de caracterizacion
de HU-01 antes de implementar cambios funcionales. El alcance actual cubre las
tareas 1.1 a 1.8 de `openspec/changes/hu-01-signed-token-claims/tasks.md`.

## HU-01: aislamiento del baseline (tarea 1.1)

Las pruebas HTTP de `AuthResourceTest` caracterizan:

- `logsInWithWorkshopUser` y `logsInWithTrainerAndPreservesPersistedFields`:
	login publico de student y trainer, JSON 200, token no vacio, `expiresIn`
	entero 3600, username, displayName, role y marca debug.
- `rejectsNullRequestWithBaselineError`,
	`rejectsMissingOrInvalidUsernameBeforeOtherFields` y
	`rejectsMissingOrInvalidPasswordAfterUsername`: errores 400 exactos para
	request, username y password nulos, ausentes, vacios o blancos; comprueban
	que no se devuelve token y preservan la precedencia de validacion.
- `invalidCredentialCasesKeepUnauthorizedContract`: errores 401 y mensajes
	exactos para password incorrecta y usuario inexistente, sin token.
- `healthIsPublicAndKeepsBaselineStructure`: GET `/q/health` sin credenciales
	conserva el estado 200 y los campos `status` y `checks`, sin fijar checks
	dinamicos.
- `characterizesIgnoredClientAndLegacyTokenLayout`: client sigue siendo
	opcional/ignorado en el baseline y el token conserva cuatro componentes
	decodificables. El test no copia ni imprime el secreto legacy.

Las pruebas directas `preservesEscapingAndRoleForSyntheticDisplayName`,
`preservesInternalErrorBodyWhenFailureHasNoMessage` y
`preservesInternalErrorBodyWhenFailureHasMessage` usan una instancia aislada
de `AuthResource` con una `LegacyDatabase` sustituida. Protegen el escape HTML,
el rol persistido y los cuerpos 500 del baseline sin provocar fallos sobre la
base de datos compartida.

`LegacyDatabase` captura `AUTH_DB_URL` durante la inicializacion de la clase.
Por eso, la variable debe establecerse antes de iniciar Quarkus. Las pruebas
usan una SQLite temporal bajo `target/characterization`, no la base compartida
`data/workshop.db`.

Desde la raiz de `workshop-auth-backend`, en PowerShell:

```powershell
New-Item -ItemType Directory -Force target\characterization | Out-Null
$env:AUTH_DB_URL = 'jdbc:sqlite:./target/characterization/workshop.db'
mvn "-Dtest=AuthResourceTest" "-Dquarkus.http.test-port=0" test
Remove-Item Env:AUTH_DB_URL
```

El puerto de test aleatorio evita el conflicto observado con el puerto 8081.
La variable `AUTH_DB_URL` solo afecta al proceso de Maven y debe establecerse
antes de que Quarkus cargue `LegacyDatabase`.

### Evidencia registrada

- Fecha: 2026-10-21.
- Comando: `mvn "-Dtest=AuthResourceTest" "-Dquarkus.http.test-port=0" test`, con `AUTH_DB_URL=jdbc:sqlite:./target/characterization/workshop.db` establecido antes de iniciar Maven.
- Resultado anterior, solo con el test preexistente: 1 test ejecutado, 0 fallos, 0 errores, 0 omitidos.
- Resultado de la suite ampliada: 11 tests ejecutados, 0 fallos, 0 errores, 0 omitidos.
- SHA-256 de `data/workshop.db` antes y despues de la suite ampliada: `03CDF1B7DB25C444357343403B199CAB21663BF860D41ED958C19A09C1ED747F` en ambos casos.
- Alcance: caracterizacion pre-cambio; no se implementa HU-01 ni se modifica el comportamiento funcional.