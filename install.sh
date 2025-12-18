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
# - Command line flags for all configuration options
# - Optional development snapshot builds
# - Systemd integration via Podman Quadlets
#
# Usage:
#   ./install.sh [OPTIONS]
#
# Options:
#   --snapshot                   Install development snapshot instead of latest stable
#   --quadlet-rootless           Skip menu and use rootless Podman quadlets
#   --quadlet-root               Skip menu and use root Podman quadlets
#   --docker-compose             Skip menu and use Docker Compose
#   --postgres-img IMG           PostgreSQL container image (default: docker.io/postgres:17)
#   --redis-img IMG              Redis container image (default: docker.io/redis:8.2.1-alpine)
#   --postgres-user USER         Database username (default: oak)
#   --postgres-password PASS     Database password (default: oak)
#   --postgres-db-name NAME      Database name (default: oak)
#   --http-port PORT             Oak HTTP server port (default: 4800)
#   --oak-img IMG                Oak container image (overrides --snapshot flag)
#   --help                       Show this help message
#
##############################################################

# Default values
POSTGRES_IMG="docker.io/postgres:17"
REDIS_IMG="docker.io/redis:8.2.1-alpine"
POSTGRES_USER="oak"
POSTGRES_PASSWORD="oak"
POSTGRES_DB_NAME="oak"
HTTP_PORT="4800"
OAK_IMG=""
USE_SNAPSHOT=false
INSTALL_TYPE=""  # "quadlet-rootless", "quadlet-root", "docker-compose"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --snapshot)
            USE_SNAPSHOT=true
            shift
            ;;
        --postgres-img)
            POSTGRES_IMG="$2"
            shift 2
            ;;
        --redis-img)
            REDIS_IMG="$2"
            shift 2
            ;;
        --postgres-user)
            POSTGRES_USER="$2"
            shift 2
            ;;
        --postgres-password)
            POSTGRES_PASSWORD="$2"
            shift 2
            ;;
        --postgres-db-name)
            POSTGRES_DB_NAME="$2"
            shift 2
            ;;
        --http-port)
            HTTP_PORT="$2"
            shift 2
            ;;
        --oak-img)
            OAK_IMG="$2"
            shift 2
            ;;
        --quadlet-rootless)
            INSTALL_TYPE="quadlet-rootless"
            shift
            ;;
        --quadlet-root)
            INSTALL_TYPE="quadlet-root"
            shift
            ;;
        --docker-compose)
            INSTALL_TYPE="docker-compose"
            shift
            ;;
        --help)
            echo "Oak IAM - Identity and Access Management Installer"
            echo ""
            echo "Usage: ./install.sh [OPTIONS]"
            echo ""
            echo $'\033[1mDESCRIPTION:\033[0m'
            echo "  Installs Oak IAM server with automatic PostgreSQL and Redis setup."
            echo "  Supports Podman Quadlets (rootless/root) and Docker Compose."
            echo ""
            echo $'\033[1mINSTALLATION METHODS:\033[0m'
            echo "  1) Podman Quadlets (Rootless) - ~/.config/containers/systemd/"
            echo "  2) Podman Quadlets (Root) - /etc/containers/systemd/"
            echo "  3) Docker Compose - docker-compose.yml in current directory"
            echo ""
            echo $'\033[1mOPTIONS:\033[0m'
            echo "  --snapshot                   Install development snapshot instead of latest stable"
            echo "  --postgres-img IMG           PostgreSQL container image (default: docker.io/postgres:17)"
            echo "  --redis-img IMG              Redis container image (default: docker.io/redis:8.2.1-alpine)"
            echo "  --postgres-user USER         Database username (default: oak)"
            echo "  --postgres-password PASS     Database password (default: oak)"
            echo "  --postgres-db-name NAME      Database name (default: oak)"
            echo "  --http-port PORT             Oak HTTP server port (default: 4800)"
            echo "  --oak-img IMG                Oak container image (overrides --snapshot flag)"
            echo "  --quadlet-rootless           Skip menu and use rootless Podman quadlets"
            echo "  --quadlet-root               Skip menu and use root Podman quadlets"
            echo "  --docker-compose              Skip menu and use Docker Compose"
            echo "  --help                       Display this help message and exit"
            echo ""
            echo $'\033[1mEXAMPLES:\033[0m'
            echo "  ./install.sh                                           # Interactive installation with defaults"
            echo "  ./install.sh --postgres-password secret               # Custom password"
            echo "  ./install.sh --snapshot --postgres-user myuser        # Development build with custom username"
            echo "  ./install.sh --docker-compose                         # Skip menu, use Docker Compose"
            echo "  ./install.sh --quadlet-rootless --http-port 8080      # Skip menu, use rootless Podman"
            exit 0
            ;;
        *)
            echo "Error: Unknown option $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Environment variables can still override command line flags
POSTGRES_IMG=${POSTGRES_IMG:-$POSTGRES_IMG}
REDIS_IMG=${REDIS_IMG:-$REDIS_IMG}
POSTGRES_USER=${POSTGRES_USER:-$POSTGRES_USER}
POSTGRES_PASSWORD=${POSTGRES_PASSWORD:-$POSTGRES_PASSWORD}
POSTGRES_DB_NAME=${POSTGRES_DB_NAME:-$POSTGRES_DB_NAME}
HTTP_PORT=${HTTP_PORT:-$HTTP_PORT}

# Set OAK_IMG based on --snapshot flag if not explicitly provided
if [ -z "$OAK_IMG" ]; then
    if [ "$USE_SNAPSHOT" = true ]; then
        OAK_IMG="git.gaiwan.co/gaiwan/oak:snapshot"
    else
        OAK_IMG="git.gaiwan.co/gaiwan/oak:latest"
    fi
fi

write_file() {
    local content="$1"
    local filepath="$2"
    local sudo_required="$3" # "true" or "false"

    echo -e "   ✏️ \033[1;36mGenerating $filepath\033[0m"
    if [ "$sudo_required" = "true" ]; then
        if ! echo "$content" | sudo tee "$filepath" > /dev/null; then
            echo "Error: Failed to write file using sudo. Check permissions."
            exit 1
        fi
    else
        echo "$content" > "$filepath"
    fi
}

# Skip menu if installation type specified via flags
if [ -n "$INSTALL_TYPE" ]; then
    echo "Using installation method: $INSTALL_TYPE"
    choice=""
    case $INSTALL_TYPE in
        quadlet-rootless) choice=1 ;;
        quadlet-root) choice=2 ;;
        docker-compose) choice=3 ;;
    esac
else
    echo -e "\033[1;36m┌────────────────────────────────────────┐\033[0m"
    echo -e "\033[1;36m│\033[0m        🌳 \033[1;32mOak IAM Installer\033[0m 🌳         \033[1;36m│\033[0m"
    echo -e "\033[1;36m└────────────────────────────────────────┘\033[0m"
    echo
    echo -e "\033[1;33mSelect installation method:\033[0m"
    echo
    echo -e "  1) \033[1;35mPodman\033[0m (Rootless)"
    echo -e "     ↪ ~/.config/containers/systemd/"
    echo -e "     ↪ Runs with user privileges"
    echo
    echo -e "  2) \033[1;31mPodman\033[0m (Root)"
    echo -e "     ↪ /etc/containers/systemd/"
    echo -e "     ↪ System-wide service (requires sudo)"
    echo
    echo -e "  3) \033[1;34mDocker Compose\033[0m"
    echo -e "     ↪ docker-compose.yml in current directory"
    echo -e "     ↪ Traditional container orchestration"
    echo
    read -r -p $'\033[1;36m Select installation method \033[1;33m[1-3]\033[0m: ' choice
fi

case $choice in
  1|2)
    echo -e "\033[1;34m📦 Pulling latest image with podman...\033[0m"
    podman pull "$OAK_IMG"
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
    echo -e "\033[1;32m╔══════════════════════════════════════════════╗\033[0m"
    echo -e "\033[1;32m║\033[0m  ✅ \033[1;32mSuccessfully generated quadlet files!\033[0m    \033[1;32m║\033[0m"
    echo -e "\033[1;32m╚══════════════════════════════════════════════╝\033[0m"
    echo
    if [ "$choice" == "1" ]; then
      echo -e "\033[1;34m🚀 To start rootless podman services:\033[0m"
      echo -e "   \033[1;36msystemctl --user daemon-reload"
      echo -e "   \033[1;36msystemctl --user enable --now oak.container\033[0m"
    else
      echo -e "\033[1;34m🚀 To start root podman services:\033[0m"
      echo -e "   \033[1;36msudo systemctl daemon-reload"
      echo -e "   \033[1;36msudo systemctl enable --now oak.container\033[0m"
    fi
    echo
    ;;

  3)
    echo -e "\033[1;34m📦 Pulling latest image with docker...\033[0m"
    docker pull "$OAK_IMG"
    echo ""
    echo "Generating docker-compose.yml in current directory..."

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
    echo -e "\n\033[1;32m╔════════════════════════════════════════════╗\033[0m"
    echo -e "\033[1;32m║\033[0m  ✅ \033[1;32mSuccessfully generated docker-compose.yml!\033[0m  \033[1;32m║\033[0m"
    echo -e "\033[1;32m╚════════════════════════════════════════════╝\033[0m"
    echo
    echo -e "\033[1;34m🐳 To start the services:\033[0m"
    echo -e "   \033[1;36mdocker compose up -d\033[0m"
    echo
    ;;

  *)
    # Should be caught by the initial check, but included for completeness
    echo "Invalid selection."
    exit 1
    ;;
esac
