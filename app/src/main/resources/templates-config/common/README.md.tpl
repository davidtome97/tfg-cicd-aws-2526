# {{APP_NAME}} – Configuración CI/CD

Este ZIP incluye archivos de configuración para arrancar el pipeline sin demo-app.

## Qué contiene
- `docker/docker-compose.yml` (profiles: local/remote)
- `.env.example` (variables de ejemplo)
- Plantilla CI/CD (se añadirá según proveedor)

## Variables
- DB_MODE={{DB_MODE}}  (local|remote)
- DB_ENGINE={{DB_ENGINE}} (mysql|postgres|mongo)
- APP_PORT={{APP_PORT}}

Repo: {{REPO_URL}}