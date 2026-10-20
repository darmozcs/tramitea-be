# tramitea-be

API de [Tramitea](https://github.com/darmozcs/tramitea): catálogo de trámites,
comunidad (comentarios y votos), sugerencias, tareas guardadas y perfil
ciudadano. Reemplaza la capa de mocks del front (`tramitea/src/services/`) con
los mismos contratos de `tramitea/API_REFERENCE.md`.

Spring Boot 3.3 · Java 21 · Postgres (base compartida `nexora`, tablas `tr_*`) ·
Flyway · JWT HS256 propio.

## Autenticación

Hay dos modos, que se eligen con `AUTH_MODE`. El código es el mismo en las
dos ramas; cambia solo la configuración de cada ambiente:

| Modo | Ambiente | Cuentas | Token |
|---|---|---|---|
| `local` (default) | Producción (Railway) | `tr_users` con password BCrypt | JWT HS256 de 30 min que emite este servicio con `AUTH_SECRET` |
| `darmoz` | Testing (server de Darmoz) | darmoz-auth (Application "Tramitea") | Access JWT RS256 de darmoz-auth; acá solo se verifica |

**Modo `darmoz`:**
- Se verifica la firma con la llave pública de darmoz-auth
  (`DARMOZ_AUTH_PUBLIC_KEY_PATH`), `iss=darmoz-auth` y que el claim
  `applicationId` sea `DARMOZ_AUTH_APPLICATION_ID`.
- Los endpoints `/auth/*` de este servicio no existen (404): registro, login,
  refresh y verificación de email los hace el front contra darmoz-auth.
- **Relación con darmoz-auth: `tr_users.id_auth_user`** guarda el `id` de
  `auth_users` (el `sub` del token).
  - `tr_users.id` sigue siendo el id propio de Tramitea, al que apuntan
    tareas, votos y comentarios.
  - El primer request de un usuario crea su fila, o vincula una existente con
    el mismo email.
  - Es una relación lógica, sin FK: en producción `auth_users` no existe en
    esta base.
- El email lo administra darmoz-auth: se sincroniza desde el token, y el que
  llegue en `PATCH /user/profile` se ignora.

**Modo `local`:** el token usa el mismo formato que verifica el middleware
del front (`iss=tramitea`, `aud=tramitea-web`, `sub` y los claims
`firstName`, `lastName` y `email`), así que front y backend comparten el
mismo `AUTH_SECRET`.

- El front guarda el token en la cookie httpOnly `tramitea_session` y llama a
  esta API **solo desde su servidor** (route handlers), reenviando
  `Authorization: Bearer`. El navegador nunca llama directo a esta API, por
  eso no hay CORS.
- (Modo local) La sesión es *sliding*: el front llama a `POST /auth/refresh` cada 5
  minutos. El logout consiste en borrar la cookie (la sesión no tiene estado
  en el servidor).
- Borrar la cuenta elimina la fila de `tr_users`. Los tokens que todavía no
  vencieron pasan a responder `401`.

## Endpoints

Base (solo en la red Docker interna): `http://tramitea-be:8080/tramitea-api`.
No tiene router en Traefik.

| Método | Ruta | Auth | Notas |
|---|---|---|---|
| POST | `/auth/register` (solo modo local) | — | `{firstName, lastName, email, password}` (password de 8 a 72 caracteres) → `201 {user, token, expiresIn}`. `409 email-already-registered`, `400 invalid-payload` |
| POST | `/auth/sign-in` (solo modo local) | — | `{email, password}` → `{user, token, expiresIn}`. `401 invalid-credentials` |
| POST | `/auth/refresh` (solo modo local) | sí | Token nuevo con los datos actuales del usuario |
| GET | `/procedures` | — | Filtros: `searchTerm`, `categoryIds`, `institutionIds`, `groups`, `methods` (listas separadas por coma), `isFreeOnly`, `sortBy` (`RELEVANCE`, `MOST_VIEWED`, `PRICE_ASC`, `RECENTLY_UPDATED`), `page` (1-based), `pageSize` (máx. 100). Devuelve `PaginatedResponse<Procedure>` |
| GET | `/procedures?ids=a,b` | — | `Procedure[]`; los ids inexistentes se omiten |
| GET | `/procedures/popular?limit=N` | — | Por `viewCount` desc (máx. 50) |
| GET | `/procedures/categories` | — | Activas, por `displayOrder` |
| GET | `/procedures/institutions` | — | |
| GET | `/procedures/{slug}` | — | Slug `es` o `en`. Suma una visita. `404` si no existe |
| POST | `/procedures/suggestions` | opcional | `201`. `400 invalid-suggestion-payload` |
| GET | `/procedures/{id}/comments` | — | Por `createdAt` asc |
| POST | `/procedures/{id}/comments` | sí | `{content, parentId?}` → `201` |
| GET | `/procedures/{id}/usefulness` | — | Número de votos `USEFUL` |
| POST | `/procedures/{id}/vote` | sí | `{value}`. Un voto por usuario (votar de nuevo lo cambia). Devuelve el conteo actualizado |
| GET | `/user/saved-tasks` | sí | Por `createdAt` desc |
| GET | `/user/saved-tasks/{procedureId}/is-saved` | sí | `boolean` |
| POST | `/user/saved-tasks` | sí | `{procedureId}`. Idempotente |
| PATCH | `/user/saved-tasks/{taskId}` | sí | `{status}`. `404` si no es del usuario |
| PATCH | `/user/saved-tasks/{taskId}/notes` | sí | `{notes}`; vacía = se borra |
| DELETE | `/user/saved-tasks/{taskId}` | sí | `204`, idempotente |
| GET | `/user/profile` | sí | |
| PATCH | `/user/profile` | sí | `400 invalid-profile-payload`, `409 email-already-registered` (el email también es el de login) |
| PATCH | `/user/profile/notification-preferences` | sí | Objeto completo |
| DELETE | `/user/profile` | sí | `204`. Borra la cuenta, sus tareas y sus votos, y anonimiza sus comentarios |

Diferencias con los mocks del front:

- Donde el mock devolvía `null` (slug o tarea inexistente) la API responde `404`.
- Los errores siguen el formato de darmoz-auth: `{timestamp, status, error, message}`. Los códigos que el front usa para decidir qué mostrar van en `error`.
- Los campos opcionales se omiten en vez de enviarse como `null`. Las fechas van en ISO 8601 UTC y `department` con el código de dos letras (`"MO"`).

## Desarrollo local

```bash
# Postgres local en localhost:5432 (base/usuario/password: nexora)
./mvnw spring-boot:run
./mvnw test        # Testcontainers: necesita Docker
```

Si la JVM local no puede bajar dependencias (proxy que intercepta TLS),
compilar y testear dentro del contenedor de Maven:

```bash
docker run --rm -v "$PWD:/build" -v tramitea-be-m2:/root/.m2 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
  -w /build maven:3.9.9-eclipse-temurin-21 mvn -B test
```

Sin `AUTH_SECRET` usa el mismo secreto de desarrollo que el front (`pnpm dev`), así que en local los dos se entienden sin configurar nada.

## Catálogo

`V2__seed_catalog.sql` se generó a partir de los mocks del front
(`tramitea/src/mocks`). Como las migraciones solo avanzan, los cambios al
catálogo van en migraciones nuevas (`V3__...`).

## Deploy

Ver [deploy/RUNBOOK.md](deploy/RUNBOOK.md) y la guía general
`C:\Darmoz\DEPLOY_GUIDE.md` (secciones 3, 4b y 4c).
