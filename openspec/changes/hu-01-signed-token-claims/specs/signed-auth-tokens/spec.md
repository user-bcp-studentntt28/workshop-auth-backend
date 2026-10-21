# Spec Delta

## Purpose

Establecer las garantias verificables de identidad, autenticidad, vigencia y confidencialidad de los tokens emitidos por el login de HU-01, sin prescribir su formato ni biblioteca.

Fuentes: [Issue #11](https://github.com/user-bcp-studentntt28/workshop-auth-backend/issues/11) y [propuesta](../../proposal.md). El token reversible del [Dia 1](../../../../../docs/ENDPOINTS.md) se reemplaza expresamente por exigencia del Issue. Los escenarios de validacion describen el resultado comprobable de verificar un token, no un nuevo endpoint HTTP ni un mecanismo tecnico.

## ADDED Requirements

### Requirement: Claims minimos identifican usuario y cliente

Cada token emitido SHALL contener al menos sub, client, iat y exp. sub SHALL ser el username persistido del usuario autenticado; client SHALL ser el string valido recibido sin modificaciones. iat y exp SHALL ser enteros de segundos Unix UTC que representen emision y expiracion, con exp = iat + 3600.

#### Scenario: Claims de un login valido

- **WHEN** el usuario student inicia sesion correctamente con client `angular-workshop`
- **THEN** el token contiene sub `student` y client `angular-workshop`
- **AND** iat y exp son enteros de segundos Unix UTC y exp - iat es 3600
- **AND** iat corresponde al instante de emision, con una diferencia maxima de 1 segundo respecto del reloj controlado de la comprobacion

#### Scenario: Client se obtiene de cada solicitud

- **WHEN** el mismo usuario inicia sesion correctamente, primero con client `angular-workshop` y despues con client `" Other-Client "`
- **THEN** cada token contiene el client exacto de su propia solicitud
- **AND** el segundo token no reutiliza, normaliza ni sustituye el client del primero

### Requirement: Firma comprobable sin secreto embebido

El token SHALL estar firmado y permitir comprobar su autenticidad. Una alteracion de los claims o la firma SHALL invalidarlo. El token no SHALL contener AUTH_TOKEN_SECRET ni una representacion reversible de ese secreto. El formato, algoritmo y biblioteca quedan pendientes de diseno.

#### Scenario: Token original autentico

- **WHEN** se verifica un token recien emitido con el material de verificacion correspondiente y antes de exp
- **THEN** la comprobacion confirma su autenticidad y vigencia

#### Scenario: Claims o firma alterados

- **WHEN** se altera sub, client, iat o exp sin generar una nueva firma valida, o se altera la firma de un token emitido
- **THEN** cada token alterado se considera invalido
- **AND** la alteracion no otorga una identidad, cliente ni vigencia nuevos

#### Scenario: Token sin secreto recuperable

- **WHEN** se inspecciona un token emitido con un secreto de prueba conocido
- **THEN** no aparece el secreto ni puede recuperarse de sus campos mediante una decodificacion reversible
- **AND** el token no conserva la concatenacion legacy de identidad, rol, expiracion y secreto

### Requirement: Expiracion estricta y observable

Un token con firma y claims validos SHALL considerarse vigente solo si now < exp, y expirado cuando now >= exp. La comparacion SHALL usar segundos Unix UTC. La tolerancia de expiresIn no SHALL ampliar la validez del token; no se introduce periodo de gracia.

#### Scenario: Antes del limite

- **WHEN** se verifica un token autentico con un reloj controlado en now = exp - 1 segundo
- **THEN** se considera vigente

#### Scenario: En el limite de expiracion

- **WHEN** se verifica el mismo token autentico con un reloj controlado en now = exp
- **THEN** se considera expirado y no valido para autenticar

#### Scenario: Despues del limite

- **WHEN** se verifica el token autentico con un reloj controlado en now = exp + 1 segundo
- **THEN** se considera expirado y no valido para autenticar

### Requirement: ExpiresIn coherente con la vigencia declarada

El login SHALL devolver expiresIn entero 3600 y cumplir abs(expiresIn - (exp - now)) <= 1 segundo. now SHALL ser el instante UTC de emision de la respuesta, observado con un reloj controlado, no el de recepcion por red. Esta tolerancia cubre precision temporal, no una extension de exp.

#### Scenario: Coherencia temporal dentro de la tolerancia

- **WHEN** un login valido emite respuesta y se observa now en ese instante mediante reloj controlado
- **THEN** expiresIn es el entero 3600
- **AND** la diferencia absoluta entre expiresIn y exp - now es menor o igual a 1 segundo
- **AND** exp - iat sigue siendo exactamente 3600

#### Scenario: Desfase que incumple el contrato

- **WHEN** al comprobar una respuesta se observa una diferencia absoluta de 2 segundos entre expiresIn y exp - now en el instante de emision
- **THEN** esa respuesta incumple el criterio de aceptacion aunque tenga firma valida
- **AND** la latencia de transporte no se usa para modificar ni justificar la tolerancia

### Requirement: Configuracion explicita del secreto fuera del laboratorio

La configuracion SHALL documentar AUTH_TOKEN_SECRET, su obligatoriedad fuera del laboratorio y ejemplos sin secretos reales. Fuera del laboratorio no SHALL usarse un secreto por defecto; si falta un valor no vacio, no SHALL emitirse token y SHALL existir un diagnostico explicito sin revelar secretos.

#### Scenario: Secreto configurado

- **WHEN** se ejecuta una comprobacion aislada con AUTH_TOKEN_SECRET no vacio provisto explicitamente y un login valido
- **THEN** se puede emitir un token firmado conforme a esta spec sin depender del secreto hardcodeado legacy

#### Scenario: Secreto ausente fuera del laboratorio

- **WHEN** se comprueba un entorno no marcado explicitamente como laboratorio con AUTH_TOKEN_SECRET ausente, vacio o compuesto solo de espacios
- **THEN** cada caso impide emitir tokens usando un secreto por defecto
- **AND** se informa un error de configuracion que identifica AUTH_TOKEN_SECRET sin exponer su valor
- **AND** no se prescribe si la deteccion ocurre al iniciar o al solicitar login

#### Scenario: Documentacion de limites del laboratorio

- **WHEN** se revisa la documentacion de configuracion entregada con la futura implementacion
- **THEN** explica como proporcionar AUTH_TOKEN_SECRET sin publicar su valor
- **AND** cualquier secreto por defecto que se mantenga queda identificado como exclusivo de laboratorio, nunca como configuracion valida fuera de el
- **AND** no se presenta HU-01 como autorizacion para desplegar el laboratorio en produccion

### Requirement: Logs y evidencia sin datos sensibles de autenticacion

El servicio no SHALL registrar tokens, secretos ni contrasenas, en claro o mediante representaciones reversibles, en rutas exitosas o fallidas. La evidencia de logs SHALL estar redactada y permitir comprobar el resultado del flujo sin mostrar esos valores.

#### Scenario: Logs de login exitoso

- **WHEN** se realiza un login correcto con credenciales y secreto sinteticos conocidos y se inspeccionan los logs emitidos
- **THEN** no contienen el token devuelto, el secreto configurado ni la contrasena, ni representaciones reversibles de esos valores

#### Scenario: Logs de fallos y expiracion

- **WHEN** se ejercitan client invalido, credenciales incorrectas, token expirado, configuracion de secreto invalida y un error interno controlado
- **THEN** los logs de cada caso, incluidos mensajes de excepcion que se registren, no exponen tokens, secretos ni contrasenas
- **AND** la evidencia compartida esta redactada sin ocultar el resultado de la comprobacion

### Requirement: Evidencia de aceptacion trazable al Issue

La entrega futura SHALL aportar resultados comprobables de login valido, credenciales invalidas, validacion de client, claims, firma, expiracion, tolerancia temporal y logs redactados. SHALL documentar posteriormente la decision HMAC frente a biblioteca JWT; esta spec no la toma ni exige una alternativa.

#### Scenario: Revision de evidencia de HU-01

- **WHEN** se evalua la futura implementacion contra el Issue #11
- **THEN** existen resultados de pruebas para esos comportamientos, incluidos now = exp y el umbral temporal de 1 segundo
- **AND** se aporta documentacion de AUTH_TOKEN_SECRET y evidencia de logs sin valores sensibles
- **AND** se registra la decision tecnica sobre HMAC frente a biblioteca JWT con su justificacion, tomada en una fase posterior a esta propuesta
