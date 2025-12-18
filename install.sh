#!/bin/bash

##############################################################
#
# Oak IAM Installer Script
#
# This script provides automated installation of the Oak Identity and Access
# Management (IAM) server using container orchestration solutions.
#
# Features:
# - Automatic PostgreSQL and Redis setup with persistent storage
# - Three installation methods: Podman (rootless/root) or Docker Compose
# - Configurable environment variables for customization
# - Optional development snapshot builds
# - Systemd integration via Podman Quadlets
#
# Environment variables (all optional):
# - POSTGRES_IMG: PostgreSQL container image (default: docker.io/postgres:17)
# - REDIS_IMG: Redis container image (default: docker.io/redis:8.2.1-alpine)
# - POSTGRES_USER: Database username (default: oak)
# - POSTGRES_PASSWORD: Database password (default: oak)
# - POSTGRES_DB_NAME: Database name (default: oak)
# - HTTP_PORT: Oak HTTP server port (default: 4800)
# - OAK_IMG: Oak container image (automatically set based on --snapshot flag)
#
# Usage:
#   ./install.sh [OPTIONS]
#
# Options:
#   --snapshot    Install development snapshot instead of latest stable
#   --help        Show this help message
#
##############################################################

# Environment variables which can be set from the outside, with default values

POSTGRES_IMG=${POSTGRES_IMG:-"docker.io/postgres:17"}
REDIS_IMG=${REDIS_IMG:-"docker.io/redis:8.2.1-alpine"}
POSTGRES_USER=${POSTGRES_USER:-"oak"}
#POSTGRES_PASSWORD=${POSTGRES_PASSWORD:-$(LC_ALL=C tr -dc 'A-Za-z0-9!#$%&()*+,-.;<=>@^_{|}~' </dev/urandom | head -c 40; echo)}
POSTGRES_PASSWORD=${POSTGRES_PASSWORD:-"oak"}
POSTGRES_DB_NAME=${POSTGRES_DB_NAME:-"oak"}
HTTP_PORT=${HTTP_PORT:-"4800"}

# Command line flags

USE_SNAPSHOT=false

for arg in "$@"; do
    case $arg in
        --snapshot)
            USE_SNAPSHOT=true
            shift
            ;;
        --help)
            echo "Oak IAM - Identity and Access Management Installer"
            echo ""
            echo "Usage: ./install.sh [OPTIONS]"
            echo ""
            echo "DESCRIPTION:"
            echo "  Installs Oak IAM server with automatic PostgreSQL and Redis setup."
            echo "  Supports Podman Quadlets (rootless/root) and Docker Compose."
            echo ""
            echo "INSTALLATION METHODS:"
            echo "  1) Podman Quadlets (Rootless) - ~/.config/containers/systemd/"
            echo "  2) Podman Quadlets (Root) - /etc/containers/systemd/"
            echo "  3) Docker Compose - docker-compose.yml in current directory"
            echo ""
            echo "OPTIONS:"
            echo "  --snapshot    Install development snapshot instead of latest stable"
            echo "  --help        Display this help message and exit"
            echo ""
            echo "ENVIRONMENT VARIABLES:"
            echo "  POSTGRES_IMG       PostgreSQL container image (default: docker.io/postgres:17)"
            echo "  REDIS_IMG          Redis container image (default: docker.io/redis:8.2.1-alpine)"
            echo "  POSTGRES_USER      Database username (default: oak)"
            echo "  POSTGRES_PASSWORD  Database password (default: oak)"
            echo "  POSTGRES_DB_NAME   Database name (default: oak)"
            echo "  HTTP_PORT          Oak HTTP server port (default: 4800)"
            echo "  OAK_IMG            Oak container image (overrides --snapshot flag)"
            echo ""
            echo "EXAMPLES:"
            echo "  ./install.sh              # Interactive installation with defaults"
            echo "  POSTGRES_PASSWORD=secret ./install.sh  # Custom password"
            echo "  ./install.sh --snapshot   # Install development build"
            exit 0
            ;;
        *)
            ;;
    esac
done

if [ "$USE_SNAPSHOT" = true ]; then
    OAK_IMG=${OAK_IMG:-"git.gaiwan.co/gaiwan/oak:snapshot"}
else
    OAK_IMG=${OAK_IMG:-"git.gaiwan.co/gaiwan/oak:latest"}
fi

write_file() {
    local content="$1"
    local filepath="$2"
    local sudo_required="$3" # "true" or "false"

    echo "--- Generating $filepath ---"
    if [ "$sudo_required" = "true" ]; then
        echo "$content" | sudo tee "$filepath" > /dev/null
        if [ $? -ne 0 ]; then
            echo "Error: Failed to write file using sudo. Check permissions."
            exit 1
        fi
    else
        echo "$content" > "$filepath"
    fi
}

echo "========================================"
echo "             Oak Installer"
echo "========================================"

echo "Select how to install Oak:"
echo "1) Podman (Rootless Quadlets: ~/.config/containers/systemd)"
echo "2) Podman (Root Quadlets: /etc/containers/systemd)"
echo "3) Docker Compose (docker-compose.yml in current directory)"
read -r -p "Enter choice [1-3]: " choice

if [[ ! "$choice" =~ ^[1-3]$ ]]; then
    echo "Invalid selection. Exiting."
    exit 1
fi

DB_CONN_URL="postgres://${POSTGRES_USER}:${POSTGRES_PASSWORD}@postgres:5432/${POSTGRES_DB_NAME}"

case $choice in
  1|2)
    SUDO_REQUIRED="false"
    if [ "$choice" == "1" ]; then
        TARGET_DIR="$HOME/.config/containers/systemd"
        mkdir -p "$TARGET_DIR"
    else
        TARGET_DIR="/etc/containers/systemd"
        SUDO_REQUIRED="true"
        echo "Attempting to create $TARGET_DIR with sudo..."
        # Create directory using sudo if necessary
        sudo mkdir -p "$TARGET_DIR" 2>/dev/null
    fi

    echo ""
    echo "Generating Quadlets in $TARGET_DIR..."

    # --- Oak Network Quadlet ---
    OAK_NETWORK_QUADLET=$(cat <<EOF
[Unit]
Description=Oak network

[Network]
NetworkName=oak-net
Driver=bridge

[Install]
WantedBy=multi-user.target default.target
EOF
)
    write_file "$OAK_NETWORK_QUADLET" "$TARGET_DIR/oak-net.network" "$SUDO_REQUIRED"

    POSTGRES_QUADLET=$(cat <<EOF
[Unit]
Description=Postgres 17 Database Service

[Container]
Image=$POSTGRES_IMG
ContainerName=oak-postgres
Environment=POSTGRES_PASSWORD="${POSTGRES_PASSWORD}"
Environment=POSTGRES_USER=${POSTGRES_USER}
Environment=POSTGRES_DB=${POSTGRES_DB_NAME}
Volume=oak_data:/var/lib/postgresql/data:Z
Network=oak-net

[Service]
Restart=always

[Install]
WantedBy=multi-user.target default.target
EOF
)
    write_file "$POSTGRES_QUADLET" "$TARGET_DIR/oak-postgres.container" "$SUDO_REQUIRED"

    # --- Redis Quadlet Content ---
    REDIS_QUADLET=$(cat <<EOF
[Unit]
Description=Redis

[Container]
Image=$REDIS_IMG
ContainerName=oak-redis
Network=oak-net

[Service]
Restart=always

[Install]
WantedBy=multi-user.target default.target
EOF
)
    write_file "$REDIS_QUADLET" "$TARGET_DIR/oak-redis.container" "$SUDO_REQUIRED"

    # --- Oak Quadlet Content ---
    OAK_QUADLET=$(cat <<EOF
[Unit]
Description=Oak IAM
Requires=oak-postgres.service oak-redis.service oak-net-network.service
After=oak-postgres.service oak-redis.service oak-net-network.service

[Container]
Image=$OAK_IMG
ContainerName=oak
Environment=OAK__ENV=prod
Environment=OAK__DB__URL=jdbc:postgresql://oak-postgres:5432/${POSTGRES_DB_NAME}
Environment=OAK__DB__USERNAME=${POSTGRES_USER}
Environment=OAK__DB__PASSWORD="${POSTGRES_PASSWORD}"
Environment=OAK__REDIS__HOST=oak-redis
Environment=OAK__HTTP__PORT=${HTTP_PORT}
Network=oak-net
PublishPort=${HTTP_PORT}:${HTTP_PORT}

[Service]
Restart=always

[Install]
WantedBy=multi-user.target default.target
EOF
)
    write_file "$OAK_QUADLET" "$TARGET_DIR/oak.container" "$SUDO_REQUIRED"

    echo ""
    echo "========================================================="
    if [ "$choice" == "1" ]; then
      echo "QUADLETS GENERATED (Rootless)."
      echo "To enable and start services, run the following commands:"
      echo "$ systemctl --user daemon-reload"
      echo "$ systemctl --user enable --now oak.container"
    else
      echo "QUADLETS GENERATED (Root)."
      echo "To enable and start services, run the following commands:"
      echo "$ sudo systemctl daemon-reload"
      echo "$ sudo systemctl enable --now oak.container"
    fi
    echo "========================================================="
    ;;

  3)
    echo ""
    echo "Generating docker-compose.yml in current directory..."

    # --- Docker Compose Content ---
    COMPOSE_YML=$(cat <<EOF
 version: '3.8'
 services:
   oak-postgres:
     image: $POSTGRES_IMG
     restart: always
     environment:
       POSTGRES_PASSWORD: $POSTGRES_PASSWORD
       POSTGRES_USER: $POSTGRES_USER
       POSTGRES_DB: $POSTGRES_DB_NAME
    volumes:
      - oak_data:/var/lib/postgresql/data

  oak-redis:
    image: $REDIS_IMG
    restart: always

  oak:
    image: $OAK_IMG
    container_name: oak
    restart: always
    environment:
      OAK__ENV: prod
      OAK__DB__URL: jdbc:postgresql://oak-postgres:5432/${POSTGRES_DB_NAME}
      OAK__DB__USERNAME: ${POSTGRES_USER}
      OAK__DB__PASSWORD: ${POSTGRES_PASSWORD}
      OAK__REDIS__HOST: oak-redis

volumes:
  oak_data:
EOF
)
    write_file "$COMPOSE_YML" "docker-compose.yml" "false"

    echo ""
    echo "========================================================="
    echo "DOCKER-COMPOSE.YML GENERATED."
    echo "To build and run the services, execute:"
    echo "$ docker compose up -d"
    echo "========================================================="
    ;;

  *)
    # Should be caught by the initial check, but included for completeness
    echo "Invalid selection."
    exit 1
    ;;
esac
