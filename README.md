# Workshop Auth Backend — Java Quarkus

> **Laboratorio intencionalmente vulnerable.** Ejecutar únicamente en local, repositorios privados y ambientes aislados. No desplegar en Internet, CERT o PROD.

Backend Quarkus que autentica usuarios de laboratorio almacenados en SQLite y entrega un token consumido por el frontend Angular y la API Python.

## Ejecución

Requisitos: JDK 21 y Maven 3.9+.

```bash
mvn quarkus:dev
```

Servicio: `http://localhost:8080`

Credenciales locales:

- `student` / `Workshop123!`
- `trainer` / `Trainer123!`

## Endpoints

- `POST /auth/login`
- `POST /auth/logout`
- `GET /q/health`

El login establece la sesión en una cookie `HttpOnly`, `SameSite=Strict` y host-only; el token no se devuelve en el cuerpo. El origen permitido para CORS y para solicitudes que cambian estado se configura con `APP_ALLOWED_ORIGIN` (por defecto `http://localhost:4200`). La cookie usa `Secure` en el perfil `prod`; `SESSION_COOKIE_SECURE` permite configurar el atributo explícitamente. Mantén `Secure` habilitado fuera del desarrollo local HTTP.

## Propósito de la rama `main`

La rama contiene deuda técnica y de seguridad deliberada: SQL concatenado, contraseñas en texto plano, token reversible, secreto hardcodeado, CORS permisivo, método largo, manejo genérico de excepciones y dependencias con CVEs.
