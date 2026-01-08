services:
  app:
    image: ${IMAGE_URI:-local/{{APP_NAME}}:dev}
    container_name: {{APP_NAME}}-app
    ports:
      - "${APP_PORT:-{{APP_PORT}}}:{{APP_PORT}}"
    environment:
      - DB_MODE=${DB_MODE:-{{DB_MODE}}}
      - DB_ENGINE=${DB_ENGINE:-{{DB_ENGINE}}}
      - DB_PORT=${DB_PORT:-}
      - DB_NAME=${DB_NAME:-demo}
      - DB_SSLMODE=${DB_SSLMODE:-disable}
      - DB_HOST=${DB_HOST:-}
      - DB_USER=${DB_USER:-}
      - DB_PASSWORD=${DB_PASSWORD:-}
      - DB_URI=${DB_URI:-}
    networks:
      - demo-net

networks:
  demo-net:
    name: {{APP_NAME}}_demo-net