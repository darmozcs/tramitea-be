# Runbook de deploy — tramitea-be

## Ambientes

| Ambiente | Rama | Dónde | Cómo se deploya |
|---|---|---|---|
| **Testing** | `develop` | Server de Darmoz (`darmoz@192.168.1.23`), contenedor `tramitea-be` | `.github/workflows/deploy-testing.yml`: test → imagen en GHCR → `docker compose up` con el runner self-hosted |
| **Producción** | `main` | Railway, proyecto "quiero tramitar", servicio `tramitea-be` + `Postgres` | Integración de Railway con GitHub: cada push a `main` construye el `Dockerfile` y deploya. La config está en `railway.json` (builder Dockerfile, healthcheck) |

Flujo: trabajar en ramas de feature → PR a `develop` (se prueba en testing) →
PR `develop` → `main` (release a producción).

**Auth por ambiente:** testing usa `AUTH_MODE=darmoz` (cuentas en darmoz-auth,
relacionadas por `tr_users.id_auth_user`; necesita `DARMOZ_AUTH_PUBLIC_KEY_PATH`
y `DARMOZ_AUTH_APPLICATION_ID`). Producción usa `AUTH_MODE=local` (o lo deja sin
definir), con cuentas propias y `AUTH_SECRET`.

Cada ambiente tiene su propia base. Los usuarios y
las sesiones de testing no sirven en producción.

### Producción (Railway)

- Variables del servicio (cargadas con la CLI, ver skill `railway`):
  `SPRING_DATASOURCE_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}`,
  `SPRING_DATASOURCE_USERNAME=${{Postgres.PGUSER}}`,
  `SPRING_DATASOURCE_PASSWORD=${{Postgres.PGPASSWORD}}` y `AUTH_SECRET`.
- Estado: `railway service status --service tramitea-be`. Logs: `railway logs --deployment --service tramitea-be`.
- Rollback: en la web de Railway, *Deployments* → deploy anterior → *Redeploy*.

## Testing (server de Darmoz)

Mismo servidor (`darmoz@192.168.1.23`), mismo Postgres compartido (`nexora`)
y mismas redes (`proxy`/`data`) que el resto de los servicios. Diferencias con
guardias-sa-be:

- **Sin router de Traefik**: la API no se publica en internet. Solo la
  consume el contenedor del front (`tramitea`) por la red `proxy`, como
  `http://tramitea-be:8080/tramitea-api`.
- **Sin llave pública de darmoz-auth**: el servicio emite sus propios tokens
  HS256 con `AUTH_SECRET`, que tiene que ser **el mismo** en el `.env` del
  front.

### Primer deploy en testing

1. `mkdir -p /opt/infra/services/tramitea-be`
2. Copiar `deploy/compose.yaml` → `/opt/infra/services/tramitea-be/compose.yaml`.
3. Copiar `deploy/.env.example` → `/opt/infra/services/tramitea-be/.env`,
   completar y `chmod 600`. `AUTH_SECRET` se genera una sola vez
   (`openssl rand -base64 48`) y se copia igual al `.env` del front.
4. Runner self-hosted (`~/actions-runner-tramitea-be`, ya registrado). Para
   instalarlo como servicio hace falta sudo:
   ```bash
   cd ~/actions-runner-tramitea-be && sudo ./svc.sh install darmoz && sudo ./svc.sh start
   ```
5. `git push` a `develop` → CI: test → build & push a GHCR → deploy al server.
6. Si `docker compose pull` da `denied`: hacer público el paquete en
   `github.com/darmozcs?tab=packages` → `tramitea-be` → Package settings → Public.
7. Verificar desde adentro del contenedor (no hay ruta pública):
   ```bash
   docker exec tramitea-be wget -qO- http://localhost:8080/tramitea-api/actuator/health
   docker logs tramitea-be --tail 50
   ```

## Base de datos

No hay que crear nada a mano. En el primer arranque Flyway aplica
`V1__init_schema.sql` (tablas `tr_*`) y `V2__seed_catalog.sql` (catálogo
inicial: 8 categorías, 10 organismos, 14 trámites, comentarios y votos de
ejemplo), con historial propio en `tr_flyway_schema_history`.

```bash
docker exec postgres psql -U nexora nexora -c "select version, description, success from tr_flyway_schema_history"
```

Backup antes de cualquier migración destructiva (ver DEPLOY_GUIDE §2.8).

## Rollback (testing)

Fijar `APP_IMAGE` en el `.env` del server a un tag `:<sha>` anterior (cada
build de CI publica `latest` y `${{ github.sha }}`), luego:
```bash
docker compose pull && docker compose up -d
```

## Rotar AUTH_SECRET (testing)

Cambiarlo en los dos `.env` (tramitea-be y tramitea) y reiniciar ambos
contenedores. Todas las sesiones abiertas se invalidan: los usuarios tienen
que volver a iniciar sesión.
