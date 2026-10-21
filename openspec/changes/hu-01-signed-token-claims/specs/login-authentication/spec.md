# Spec Delta

## Purpose

Definir el contrato observable de POST /auth/login para HU-01, preservando las respuestas del Dia 1 salvo la obligatoriedad de client y el nuevo contenido del token.

Fuentes: [Issue #11](https://github.com/user-bcp-studentntt28/workshop-auth-backend/issues/11), [baseline del Dia 1](../../../../../docs/ENDPOINTS.md) y [propuesta con la decision de client](../../proposal.md). Estos requisitos se incorporan como ADDED porque no existe una spec principal previa.

## ADDED Requirements

### Requirement: Login publico y respuesta exitosa compatibles

El servicio SHALL aceptar POST /auth/login sin autenticacion previa, con Content-Type application/json. Para credenciales correctas y client valido SHALL responder 200 con token string no vacio, expiresIn entero 3600, user y debug, conservando la semantica del Dia 1 salvo el contenido legacy del token.

#### Scenario: Login valido sin token previo

- **WHEN** se envia un login sin Authorization, con las credenciales vigentes del usuario de laboratorio student y client `angular-workshop`
- **THEN** la respuesta es 200 y JSON, con un token firmado conforme a la spec signed-auth-tokens
- **AND** expiresIn es el entero 3600
- **AND** user.username es `student`, user.displayName es `Workshop Student` y user.role es `STUDENT`
- **AND** debug es `authenticated-with-legacy-flow`

#### Scenario: Preservar datos persistidos y escape HTML

- **WHEN** un usuario existente con rol y displayName que contiene caracteres HTML inicia sesion con credenciales correctas y client valido
- **THEN** user.username y user.role coinciden con los valores persistidos
- **AND** user.displayName conserva el escape HTML del Dia 1, por ejemplo `<Trainer>` se devuelve como `&lt;Trainer&gt;`
- **AND** no se exige Authorization ni se cambia el rol para emitir el token

### Requirement: Client obligatorio y conservado sin normalizacion

El servicio SHALL exigir client string con al menos un caracter no blanco y SHALL rechazar su ausencia, null, string vacio, solo espacios o un valor JSON no string con 400 y un mensaje JSON no vacio. El client valido SHALL conservarse exactamente en el claim client, sin trim ni conversion de mayusculas.

#### Scenario: Client ausente o vacio

- **WHEN** username y password son validos y client se omite, es null, `""` o `"   "`
- **THEN** cada caso recibe 400 con un campo message string no vacio que identifica client como invalido
- **AND** la respuesta no contiene token

#### Scenario: Client con tipo incorrecto

- **WHEN** username y password son validos y el JSON contiene client numerico, booleano, array u objeto
- **THEN** cada caso recibe 400 con un campo message string no vacio y no se emite token
- **AND** esos valores no se convierten implicitamente a string para autenticar

#### Scenario: Client valido con espacios y mayusculas

- **WHEN** las credenciales son correctas y client es `" Angular-Workshop "`
- **THEN** el login responde 200
- **AND** el claim client es exactamente `" Angular-Workshop "`
- **AND** no se aplica una lista de clientes permitidos no exigida por el Issue

### Requirement: Validaciones existentes de request y credenciales

El servicio SHALL preservar los errores 400 documentados para request nulo y username o password nulos, vacios o en blanco. SHALL conservar el campo message y los textos del Dia 1; no SHALL emitir token ante estas entradas.

#### Scenario: Request nulo

- **WHEN** POST /auth/login recibe un request nulo
- **THEN** responde 400 con `{"message":"Request is null"}` y sin token

#### Scenario: Username invalido

- **WHEN** password y client son validos y username es null, `""` o `"   "`
- **THEN** cada caso responde 400 con `{"message":"Username is required"}` y sin token

#### Scenario: Password invalida

- **WHEN** username y client son validos y password es null, `""` o `"   "`
- **THEN** cada caso responde 400 con `{"message":"Password is required"}` y sin token

### Requirement: Credenciales invalidas conservan el contrato de error

Con username, password y client estructuralmente validos, el servicio SHALL responder 401 si no existe el usuario o la contrasena no coincide. SHALL mantener el mensaje del Dia 1 `Invalid user <username> or password` y no SHALL emitir token.

#### Scenario: Contrasena incorrecta

- **WHEN** se solicita login con username `student`, una contrasena incorrecta y client valido
- **THEN** responde 401 con `{"message":"Invalid user student or password"}`
- **AND** no devuelve token

#### Scenario: Usuario inexistente

- **WHEN** se solicita login con username `unknown-user`, inexistente en los datos de prueba, password no vacia y client valido
- **THEN** responde 401 con `{"message":"Invalid user unknown-user or password"}`
- **AND** no devuelve token

### Requirement: Errores internos mantienen el baseline

Ante una excepcion no controlada en el login, el servicio SHALL conservar el 500 con los campos message y exception documentados el Dia 1, sin token. Reformar esta exposicion de detalles internos queda fuera de HU-01; la ausencia de valores sensibles en logs se exige por separado.

#### Scenario: Excepcion sin mensaje durante el login

- **WHEN** una solicitud valida provoca una RuntimeException sin mensaje durante el flujo
- **THEN** responde 500 con `{"message":"Unknown error","exception":"java.lang.RuntimeException"}`
- **AND** no devuelve token

#### Scenario: Excepcion con mensaje no sensible

- **WHEN** una solicitud valida provoca una RuntimeException con mensaje de prueba `Test failure`
- **THEN** responde 500 con `{"message":"Test failure","exception":"java.lang.RuntimeException"}`
- **AND** no devuelve token
