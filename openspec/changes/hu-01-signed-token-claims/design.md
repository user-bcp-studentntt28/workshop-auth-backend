# Design

## Context

### Resumen para cualquier lector

El login entrega una credencial temporal, llamada **token**. Hoy esa credencial puede leerse y contiene el secreto del servidor: codificar informacion en Base64 no la protege. El cambio propone entregar una credencial con una **firma digital**, equivalente a un sello que permite detectar modificaciones, y una fecha de vencimiento comprobable.

La firma no oculta el contenido: el usuario y la aplicacion solicitante seguiran siendo legibles. Nunca se incluira la contrasena ni el secreto. El usuario conservara el mismo resultado de login, salvo los dos cambios aprobados: debe enviar `client` y recibira un token con otro formato.

Fuentes y orden de autoridad:

- [Propuesta aprobada](proposal.md), con la motivacion del [Issue #11](https://github.com/user-bcp-studentntt28/workshop-auth-backend/issues/11).
- [Contrato de login](specs/login-authentication/spec.md) y [garantias del token](specs/signed-auth-tokens/spec.md).
- [Contrato del Dia 1](../../../docs/ENDPOINTS.md), para el comportamiento que se conserva.
- [Reglas del repositorio](../../../REGLAS.md), para caracterizacion, separacion de responsabilidades y cobertura.

Las exclusiones de diseno y tareas de la propuesta describen la entrega inicial. Este documento corresponde a la siguiente fase, autorizada expresamente; no cambia las specs, no crea tareas ni autoriza implementar.

### Situacion tecnica observada

El proyecto usa Java 21, Quarkus 3.27.4, REST con Jackson para JSON, SQLite, JUnit 5, REST Assured y JaCoCo, segun [pom.xml](../../../pom.xml).

El flujo actual es:

`JSON -> AuthResource -> LegacyDatabase -> LegacyTokenUtil -> respuesta JSON`

- [AuthResource](../../../src/main/java/com/bcp/workshop/auth/AuthResource.java) valida username/password, busca al usuario, crea el token y arma la respuesta. Tiene un manejo general de excepciones que forma parte del baseline.
- [LoginRequest](../../../src/main/java/com/bcp/workshop/auth/LoginRequest.java) acepta `client` como string, pero no lo utiliza.
- [LegacyTokenUtil](../../../src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java) concatena usuario, rol, expiracion y un secreto hardcodeado; luego aplica Base64. No firma ni verifica tokens.
- [LegacyDatabase](../../../src/main/java/com/bcp/workshop/auth/LegacyDatabase.java) consulta SQLite y usa `printStackTrace()` en fallos. Una excepcion de SQL puede arrastrar datos sensibles: esas salidas tambien deben considerarse para cumplir la spec de logs.
- [La configuracion](../../../src/main/resources/application.properties) no declara `AUTH_TOKEN_SECRET`.
- [La prueba existente](../../../src/test/java/com/bcp/workshop/auth/AuthResourceTest.java) solo comprueba un login exitoso, username y presencia del token.

La afirmacion del Issue sobre HMAC existente no coincide con este codigo. No hay una implementacion HMAC que conservar: hay que introducir la firma. No se inspeccionaron consumidores ni otros repositorios.

## Goals / Non-Goals

**Goals**

- Aislar la creacion y comprobacion del token para poder probar firma y vencimiento sin crear una API nueva.
- Mantener el contrato HTTP protegido mientras se sustituyen las partes inseguras del token.
- Hacer deterministas las pruebas de tiempo: que no dependan de esperar una hora ni de la velocidad de la maquina.
- Rechazar configuraciones inseguras antes de atender logins y producir diagnosticos sin secretos.
- Permitir cambios pequenos y revisables, con evidencia antes y despues de alterar autenticacion.

**Non-Goals**

- No agregar autenticacion global de Quarkus, nuevos endpoints, permisos, refresh, revocacion ni sesiones persistidas.
- No transformar toda la aplicacion en una arquitectura nueva ni crear una interfaz por cada clase.
- No cambiar consultas SQL, almacenamiento de contrasenas, CORS, usuarios, roles, duracion ni formato general de errores.
- No corregir toda la observabilidad: solo las salidas que pueden filtrar datos del flujo comprometido.
- No desplegar el laboratorio en Internet, CERT o produccion. Mejorar el token no vuelve seguro al resto del sistema.

## Contratos protegidos

Las specs contienen los escenarios completos; esta tabla indica como protegerlos durante el cambio.

| Contrato | Proteccion de diseno |
|---|---|
| Login publico y JSON | No instalar filtros que exijan un token para llamar a login ni cambiar rutas o Content-Type. |
| 200 con token, expiresIn, user y debug | Conservar nombres, tipos y valores; `expiresIn` sigue siendo entero 3600. |
| Datos de user | Usar username/role persistidos y el mismo escape HTML de displayName. |
| 400 de request, username y password | Mantener textos y precedencia de las validaciones actuales; validar client despues. |
| 401 por credenciales incorrectas | Conservar mensaje y ausencia de token; no reinterpretar fallos de credenciales como errores JWT. |
| 500 del baseline | Mantener message/exception para errores internos; no registrar automaticamente mensajes sensibles. |
| client exacto | Verificar que sea string no blanco y transportar el valor sin trim ni normalizacion. |
| Tiempo | `exp = iat + 3600`; tolerancia de respuesta de 1 segundo, sin gracia al vencer. |
| Health y configuracion ajena al token | No modificar su seguridad, checks, CORS ni persistencia. |

Solo son rupturas autorizadas el `client` ahora obligatorio y el contenido del token. La precedencia en entradas con varios errores preserva request, username y password antes de client; no se cambia por conveniencia del refactor.

## Componentes afectados

Los nombres nuevos de esta tabla describen responsabilidades, no exigen una estructura extensa de archivos.

| Componente | Cambio acotado previsto |
|---|---|
| AuthResource | Conservar HTTP y el armado compatible de respuestas; realizar validaciones de entrada y delegar el caso de login. |
| LoginRequest | Representar client como nodo JSON de Jackson para distinguir un string de numeros, booleanos, arrays y objetos sin conversion implicita. Username y password no cambian. |
| Servicio de login pequeno | Coordinar busqueda del usuario y emision. Reutilizar LegacyDatabase como acceso a datos, sin una nueva capa de repositorios ni reescritura de SQL. |
| Servicio de tokens | Reemplazar el uso de LegacyTokenUtil; emitir y verificar JWT con una biblioteca y un reloj sustituible en pruebas. |
| Configuracion de tokens | Leer y validar el secreto al iniciar, sin valores predeterminados. Centralizar la duracion fija, no convertirla en una nueva opcion configurable. |
| LegacyDatabase, solo diagnosticos | Sustituir las dos impresiones de excepciones por eventos sanitizados que no incluyan SQL, credenciales, mensajes de excepcion ni cadenas de causas sensibles. Conservar el comportamiento de acceso a datos de esta HU. |
| Dependencias y pruebas | Declarar la biblioteca JWT y ampliar pruebas unitarias, HTTP, configuracion y captura de logs. |
| Documentacion | Explicar configuracion y nuevo contrato, conservando identificable el baseline del Dia 1 y sin publicar tokens o secretos reales. |

No dejar una ruta alternativa que siga emitiendo tokens legacy. Si se retira LegacyTokenUtil, debe hacerse despues de comprobar y reemplazar todos sus usos locales.

## Decisions

### 1. JWT estandar con biblioteca; firma HMAC-SHA-256

**Decision:** utilizar JWT en formato compacto firmado mediante HS256, con la biblioteca `jose4j` declarada directamente en Maven. Usar la version compatible administrada por el BOM de Quarkus cuando exista; verificar la resolucion antes de cambiar dependencias. No agregar una extension que proteja automaticamente endpoints.

JWT define como organizar los datos del token; HMAC define como firmarlos con un secreto compartido. No son alternativas excluyentes. Esta decision satisface la comparacion del Issue: se conserva la simplicidad de HMAC como algoritmo, pero se adopta una biblioteca JWT para el formato y la verificacion, en vez de programarlos manualmente.

**Motivo:** la biblioteca resuelve serializacion, firma y lectura estandar sin introducir infraestructura de certificados ni un nuevo formato propio.

**Alternativas consideradas:**

- Base64 legacy: descartado; no ofrece autenticidad y revela el secreto.
- HMAC mediante JCA y formato propio: evita una dependencia, pero obliga a mantener formato, parsing y reglas criptograficas propias.
- Extension JWT de autenticacion completa: innecesaria para emitir tokens y comprobarlos localmente; agrega superficie de configuracion y posible impacto en endpoints publicos.
- Firma asimetrica con clave publica/privada: separa quien firma de quien verifica, pero requiere gestion de claves no pedida por esta HU.

La futura resolucion Maven debe confirmar compatibilidad y controles de dependencias, sin silenciarlos. Si no es compatible, se revisara esta decision antes de implementar, no se cambiara de biblioteca silenciosamente.

### 2. Claims minimos y verificacion local explicita

El payload incluira `sub`, `client`, `iat` y `exp`. No se incorporan contrasenas, secreto, displayName ni nuevos claims de autorizacion. El rol permanece en `user.role`; no se promete compatibilidad con el rol concatenado en el token anterior.

La comprobacion local del servicio de tokens:

- Exige firma y algoritmo HS256 fijado por configuracion del verificador, nunca elegido libremente por el header recibido; rechaza `none` y algoritmos distintos.
- Exige los cuatro claims y sus tipos; sub/client no blancos, iat/exp enteros y duracion de 3600 segundos.
- Verifica autenticidad antes de confiar en los datos y evalua vencimiento contra el reloj del servidor.
- Devuelve un resultado distinguible para token invalido o expirado; no los convierte en login exitoso ni expone el token en diagnosticos.

La comprobacion es una operacion interna utilizada por las pruebas de aceptacion; no hay un endpoint consumidor que proteger en esta HU. No se aceptan tokens legacy ni se agrega deteccion automatica de formatos.

**Alternativa descartada:** limitar las pruebas a decodificar el payload. Leer los datos no comprueba firma ni expiracion.

### 3. Un reloj UTC controlable y una sola duracion

Inyectar un `java.time.Clock`, con UTC en ejecucion normal y tiempo fijado en pruebas. Crear iat y exp desde una misma lectura, despues de autenticar al usuario; una sola constante representa los 3600 segundos y alimenta tambien expiresIn.

El servicio devolvera el token y su duracion juntos, evitando que la capa HTTP repita valores independientes. La respuesta se construye inmediatamente despues de emitir el token, sin consultas de base de datos ni operaciones de red intermedias.

La verificacion usa `now >= exp` como expirado, sin la tolerancia de reloj que algunas bibliotecas activan por defecto. El chequeo temporal se hara explicitamente con el Clock si la API de la biblioteca no permite introducirlo con esa precision. No se desactiva por ello la verificacion criptografica.

La prueba de coherencia observara el momento de entrega de la respuesta por el servidor, antes del transporte, con ese reloj controlado. Verificara diferencias de 0 y 1 segundo aceptadas y 2 segundos rechazadas. Una demora real de mas de 1 segundo entre emision y entrega incumple la spec: no se compensa cambiando expiresIn, extendiendo exp ni usando la hora del navegador.

**Alternativas descartadas:** llamadas independientes a la hora del sistema, pruebas con sleep o aplicar la tolerancia de expiresIn como gracia de expiracion. Son fragiles o cambian el contrato.

### 4. Client estricto sin cambiar globalmente Jackson

El nodo JSON de client se valida en el limite HTTP: debe ser textual y no blanco. Solo despues se convierte a String para el servicio. Se responde 400 con message no vacio y sin token; para ausencia, null o blanco el mensaje identifica client.

Esto evita que Jackson convierta un numero o booleano a texto antes de validar. No se modifica la conversion de strings global, el manejo de propiedades desconocidas ni el comportamiento de username/password.

**Alternativas descartadas:** validar solo el String actual, que puede haber perdido su tipo JSON original; o instalar un mapper global de errores, que podria alterar respuestas ajenas a client.

### 5. Secreto explicito y fallo temprano

Resolver `AUTH_TOKEN_SECRET` una vez mediante la configuracion de Quarkus y validar de forma anticipada, antes de atender solicitudes. Exigir configuracion explicita en todos los perfiles, incluido laboratorio: la spec permite eliminar el secreto predeterminado y esta opcion evita distinguir entornos con heuristicas.

Documentar que el valor se interpreta como bytes UTF-8, no como Base64 decodificado. Para HS256 debe tener al menos 32 bytes y generarse con alta entropia; la longitud sola no garantiza seguridad. No recortar ni transformar un secreto valido. Ausencia, blanco o material insuficiente causan error de configuracion seguro, no un secreto de respaldo. Esta restriccion criptografica no agrega campos al login.

El diagnostico identifica `AUTH_TOKEN_SECRET` y el motivo general sin imprimir el valor, su representacion, su hash ni la excepcion cruda del proveedor de configuracion. Leerlo inicialmente como valor opcional permite producir un mensaje controlado; las pruebas de arranque comprobaran tambien los logs del runtime para evitar filtraciones antes de la validacion propia.

Las pruebas usan secretos sinteticos exclusivos de test, nunca el secreto hardcodeado anterior ni secretos reales. No se versionan archivos locales con secretos.

**Alternativas descartadas:** fallback solo para dev, que puede activarse accidentalmente, y fallo al primer login, que descubre tarde una configuracion que ya era invalida.

### 6. Diagnosticos por evento, no por volcado de datos

Registrar identificador de evento, resultado y categoria de fallo con mensajes controlados; no imprimir requests, headers Authorization, respuestas con token, SQL ni objetos de excepcion que puedan contenerlos. No agregar nuevos logs con username o client por defecto.

Para fallos de datos o token, evitar trasladar mensajes sensibles de bibliotecas a nuevas excepciones que lleguen al HTTP 500 legacy. Conservar los errores internos del baseline sin agregar mas detalle. No se incorpora un catch general nuevo ni un retorno de exito de respaldo.

La sustitucion acotada de `printStackTrace()` en LegacyDatabase es necesaria para la confidencialidad de esta HU, no un refactor de persistencia. La evidencia resumira escenario y resultado; una captura redactada no sustituye verificar que el log original ya carecia de secretos.

**Alternativas descartadas:** borrar todos los logs, que oculta fallos; o confiar exclusivamente en una expresion regular aplicada despues de registrar datos, que puede dejar filtraciones.

## Pruebas de caracterizacion y validacion

**Caracterizacion** significa registrar mediante pruebas lo que ya funciona antes de cambiarlo. No prueba que el sistema sea seguro: evita perder comportamiento por accidente.

### Antes de tocar token o flujo de autenticacion

1. Ampliar la prueba HTTP existente para ambos usuarios del laboratorio: 200 publico, tipos y valores de expiresIn, user y debug, sin fijar el token completo ni su timestamp.
2. Cubrir request nulo y username/password ausentes, nulos, vacios y blancos, verificando los mensajes exactos y su precedencia.
3. Cubrir usuario inexistente y password incorrecta con los 401 exactos, sin token.
4. Comprobar escape HTML y roles usando datos sinteticos aislados. Para 500 con/sin mensaje, usar una instancia controlada del resource y un sustituto de LegacyDatabase que lance la excepcion; no provocar fallos en la base compartida.
5. Confirmar que health sigue siendo publico y conserva su estructura general, sin fijar el contenido dinamico de checks.
6. Registrar como caracterizacion transitoria que hoy client es opcional/ignorado y el token tiene estructura legacy. Esas dos pruebas se reemplazaran expresamente por las expectativas aprobadas, no se conservaran como requisito ni se perpetuara el secreto legacy en nuevos fixtures.

Separar pruebas unitarias del resource de las HTTP: solo las HTTP acreditan la deserializacion JSON real. Usar SQLite temporal para las pruebas con persistencia; no modificar [la base de laboratorio](../../../data/workshop.db). Configurar AUTH_DB_URL antes de inicializar la aplicacion de prueba, porque hoy se captura en un campo estatico.

### Despues del cambio, con trazabilidad a las specs

| Grupo | Evidencia necesaria |
|---|---|
| Client | Ausente, null, vacio, blanco, numero, booleano, array y objeto producen 400; string con espacios/mayusculas llega intacto al claim; dos solicitudes no comparten client por error. |
| Claims | sub persistido, client exacto, iat/exp enteros y exp - iat = 3600; payload sin secreto ni contrasena. |
| Firma | Token original valido; alteracion individual de cada claim o firma rechazada; algoritmo no permitido rechazado. Verificacion adicional con API de biblioteca configurada desde test, sin reutilizar solo el emisor como oraculo. |
| Tiempo | Clock fijo en exp - 1, exp y exp + 1; diferencia de expiresIn de 0/1/2 segundos; ausencia de gracia y sleeps. |
| Configuracion | Arranque con secreto sintetico valido; rechazo de ausente, vacio, blanco o insuficiente, sin fallback ni filtracion en diagnosticos. |
| Logs | Capturar logger y salida estandar/error del proceso en exito, credenciales incorrectas, client invalido, expiracion, error de datos, configuracion y excepcion controlada. Buscar valores sinteticos completos y sus codificaciones reversibles, incluidos tokens y contrasenas. |
| Compatibilidad | Volver a ejecutar toda la caracterizacion protegida y revisar que solo cambian las dos expectativas autorizadas. |

Utilizar JUnit 5, REST Assured y JaCoCo existentes. Solo si es imprescindible para una prueba concreta se agregara soporte de test; no instalar un framework nuevo por defecto.

Comandos previstos para implementacion: `mvn "-Dtest=AuthResourceTest" test` para la caracterizacion inicial, selectores equivalentes para los nuevos tests y `mvn verify` al integrar. La meta es al menos 85% del codigo modificado, con reporte revisado y sin silenciar los controles existentes. Estos comandos no se ejecutan ni se presentan como aprobados en esta entrega documental.

## Risks / Trade-offs

- **Tokens antiguos dejan de servir** -> No hay doble aceptacion. Coordinar el corte y requerir nuevo login; las adaptaciones de otros repositorios son una dependencia externa, no trabajo de este cambio.
- **Client obligatorio rompe solicitudes anteriores** -> Comunicar el requisito y probarlo antes de habilitar el cambio. No introducir un valor por defecto que contradiga el claim solicitado.
- **Una clave HMAC compartida permite firmar a quien la conoce** -> Distribuirla solo a quien corresponda. Si se necesita verificacion sin poder emitir, sera otro cambio hacia firma asimetrica.
- **JWT firmado sigue siendo legible** -> Mantener payload minimo; no prometer cifrado ni adjuntar datos personales adicionales.
- **Validacion fuerte del secreto impide arrancar instalaciones antiguas** -> Preparar primero la variable y comprobar arranque con datos sinteticos; fallo explicito en lugar de tokens inseguros.
- **Demoras, desfases de reloj o tolerancias de biblioteca** -> Clock UTC, limites exactos y pruebas del instante de respuesta; no suavizar el vencimiento para que pase una prueba.
- **Menos detalle de excepciones en logs** -> Eventos categorizados conservan el diagnostico basico sin exponer SQL ni secretos; el detalle sensible no se considera evidencia aceptable.
- **Deuda deliberada del laboratorio permanece** -> SQL concatenado, passwords persistidas sin hash y 500 con detalles siguen siendo riesgos conocidos, no corregidos por la firma del token ni una autorizacion de despliegue.
- **Dependencia criptografica nueva** -> Compatibilidad con Java/Quarkus y revision de dependencias antes de integrar; mantener su declaracion explicita y actualizable.

## Migration Plan

### Estrategia incremental

Cada etapa futura deja un cambio revisable; ninguna se ejecuta como parte de este documento.

1. **Fijar la red de seguridad:** agregar y ejecutar caracterizacion antes de refactorizar. Confirmar fixtures y logs aislados.
2. **Aislar responsabilidades:** separar coordinacion de login de HTTP sin cambiar aun su comportamiento; conservar LegacyDatabase. Volver a ejecutar caracterizacion.
3. **Preparar el token nuevo:** incorporar biblioteca, configuracion, reloj y servicio de tokens con pruebas unitarias. Preparar el secreto explicitamente antes del arranque que lo exija. No habilitar una emision publica que no cumpla aun client.
4. **Integrar el cambio observable completo:** activar juntos validacion de client, emision firmada y respuesta compatible; retirar la ruta legacy y limitar diagnosticos sensibles. Sustituir solo las caracterizaciones de las dos rupturas autorizadas y ejecutar todos los escenarios.
5. **Validar y documentar:** ejecutar pruebas y cobertura, revisar logs redactados y documentar JWT/HS256, secreto, nuevo login y procedimiento de corte. Mantener identificable el contrato historico del Dia 1.

No hay migracion de tablas ni de cuentas. Antes de usar la version nueva en el entorno aislado autorizado, comprobar arranque, health y login con secreto sintetico. No publicar esta version si falta la coordinacion externa necesaria; no inspeccionar ni modificar consumidores desde esta HU.

### Rollback

En esta fase solo se agrega este documento: revertirlo no afecta ejecucion ni datos.

Para una futura implementacion:

- Si falla la configuracion o el login, detener la emision y corregir o restaurar una version firmante previamente validada. No activar un fallback de Base64 ni reutilizar el secreto legacy.
- En el primer despliegue no existe una version anterior segura de firma. Volver al binario del Dia 1 reintroduciria la vulnerabilidad y no es un rollback seguro: mantener el servicio detenido hasta corregir. Una restauracion del laboratorio legacy requeriria una decision explicita y aislamiento, no se automatiza aqui.
- La base no cambia; no necesita una migracion inversa. No borrar cuentas ni datos para recuperar el servicio.
- Si cambia el secreto o se invalida la version emisora, exigir nuevos logins. No garantizar continuidad de tokens emitidos durante una version defectuosa ni aceptar ambos formatos.
- Ante sospecha de filtracion, detener emision, reemplazar el secreto y descartar tokens afectados. Como no existe revocacion central en esta HU, no prometer invalidacion inmediata en consumidores externos que conserven la clave anterior.
