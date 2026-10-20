# Hallazgos de discovery — Día 2

Todos los elementos son candidatos para revisión humana. No se modificó código ni se crearon issues.

### Token legible y sin integridad criptográfica verificable
- Estado: CANDIDATO
- Evidencia: `src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java:13-15` construye el contenido del token y lo codifica en Base64.
- Impacto: El token no muestra protección criptográfica de integridad; el servicio consumidor debe poder validar que identidad y rol no hayan sido alterados.
- Prioridad propuesta: P1 fuera del laboratorio aislado; P2 en el entorno local documentado.
- Contrato potencialmente afectado: Token de respuesta de `POST /auth/login`, consumido por la API de requerimientos.
- Cómo validarlo: En pruebas locales de integración, confirmar que tokens alterados se rechacen y que tokens emitidos válidos se acepten.

### Consulta de credenciales construida mediante concatenación SQL
- Estado: CANDIDATO
- Evidencia: `src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:31-35` concatena los valores de usuario y contraseña en una consulta y luego la ejecuta.
- Impacto: Los datos de entrada pueden cambiar la consulta usada para autenticar.
- Prioridad propuesta: P1 fuera del entorno aislado; P2 en el laboratorio local.
- Contrato potencialmente afectado: Request y resultado de autenticación de `POST /auth/login`.
- Cómo validarlo: En una base local descartable, verificar que los campos se traten como parámetros de datos y que no alteren la estructura de la consulta.

## Revisión humana

Confirmar prioridad y alcance antes de crear requisitos o tareas de implementación.
