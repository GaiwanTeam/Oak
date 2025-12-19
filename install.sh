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
#   --config-file FILE           Path to custom oak.edn configuration file
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
CONFIG_FILE=""  # Path to custom oak.edn configuration file

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
        --config-file)
            CONFIG_FILE="$2"
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
            echo "  --config-file FILE           Path to custom oak.edn configuration file"
            echo "  --quadlet-rootless           Skip menu and use rootless Podman quadlets"
            echo "  --quadlet-root               Skip menu and use root Podman quadlets"
            echo "  --docker-compose             Skip menu and use Docker Compose"
            echo "  --help                       Display this help message and exit"
            echo ""
            echo $'\033[1mEXAMPLES:\033[0m'
            echo "  ./install.sh                                          # Interactive installation with defaults"
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

# Helper function to format config file path for different formats
format_config_volume() {
    local path="$1"
    local format="$2"  # "quadlet-rootless", "quadlet-root", "docker-compose"

    # Expand ~ to home directory
    path=$(echo "$path" | sed "s|^~|$HOME|")

    case "$format" in
        "quadlet-rootless")
            # Replace home directory with %h
            local formatted_path=$(echo "$path" | sed "s|$HOME|%h|")
            echo "Volume=${formatted_path}:/etc/oak.edn:Z"
            ;;
        "quadlet-root")
            # Use path as-is
            echo "Volume=${path}:/etc/oak.edn:Z"
            ;;
        "docker-compose")
            # Replace home directory with ${HOME}
            local formatted_path=$(echo "$path" | sed "s|$HOME|\${HOME}|")
            echo "      - \"${formatted_path}:/etc/oak.edn:ro\""
            ;;
    esac
}

# Helper function to format commented config line
format_config_comment() {
    local format="$1"  # "quadlet-rootless", "quadlet-root", "docker-compose"

    case "$format" in
        "quadlet-rootless")
            echo "# Volume=%h/path/to/oak.edn:/etc/oak.edn:Z"
            ;;
        "quadlet-root")
            echo "# Volume=/etc/oak.edn:/etc/oak.edn:Z"
            ;;
        "docker-compose")
            echo "    # volumes:"
            echo "    #   - \"${HOME}/path/to/oak.edn:/etc/oak.edn:ro\""
            ;;
    esac
}

# Prompt function with consistent coloring
prompt() {
    local message="$1"
    local options="$2"
    local result_var="$3"
    read -r -p $'\033[1;36m'"$message"$'\033[0m \033[1;33m'"$options"$'\033[0m: ' "$result_var"
}

# Yellow message
heading() {
    local message="$1"
    echo -e "\033[1;33m$message\033[0m"
}

# Blue message
progress() {
    local message="$1"
    echo -e "\033[1;34m$message\033[0m"
}

# Create a file with the given contents, optionally using `sudo`. If the file
# exists, the difference is shown, and the user is asked how to proceed.
write_file() {
    local content="$1"
    local filepath="$2"
    local sudo_required="$3" # "true" or "false"

    # Check if file exists and compare contents
    if [ "$sudo_required" = "true" ]; then
        if sudo [ -f "$filepath" ]; then
            local existing_content
            existing_content=$(sudo cat "$filepath")
            if [ "$content" = "$existing_content" ]; then
                return 0
            fi

            echo ""
            heading "⚠️ File already exists: $filepath"
            diff -u --color=auto <(echo "$existing_content") <(echo "$content")
            echo ""
            prompt "File already exists, overwrite?" "[y/N/q]" overwrite_choice
            case $overwrite_choice in
                [Yy])
                    if ! echo "$content" | sudo tee "$filepath" > /dev/null; then
                        echo "Error: Failed to write file using sudo. Check permissions."
                        exit 1
                    fi
                    ;;
                q)
                    echo "Aborting."
                    ;;
                *)
                    echo "⏭️  Skipping existing file: $filepath"
                    return 0
                    ;;
            esac
        else
            echo -e "   ✏️ \033[1;36m$filepath\033[0m"
            if ! echo "$content" | sudo tee "$filepath" > /dev/null; then
                echo "Error: Failed to write file using sudo. Check permissions."
                exit 1
            fi
        fi
    else
        if [ -f "$filepath" ]; then
            local existing_content
            existing_content=$(cat "$filepath")
            if [ "$content" = "$existing_content" ]; then
                return 0
            fi

            echo ""
            heading "⚠️ File already exists: $filepath"
            diff -u --color=auto <(echo "$existing_content") <(echo "$content")
            echo ""
            prompt "File already exists, overwrite?" "[y/N/q]" overwrite_choice
            case $overwrite_choice in
                [Yy])
                    echo "$content" > "$filepath"
                    ;;
                q)
                    echo "Aborting."
                    exit 0
                    ;;
                *)
                    echo "⏭️  Skipping existing file: $filepath"
                    return 0
                    ;;
            esac
        else
            echo -e "   ✏️ \033[1;36m$filepath\033[0m"
            echo "$content" > "$filepath"
        fi
    fi
}

# Detect OS and handle macOS limitations
detect_os() {
    case "$(uname -s)" in
        Darwin*)
            if [ -z "$INSTALL_TYPE" ] || [ "$INSTALL_TYPE" != "docker-compose" ]; then
                heading "OS X detected: only --docker-compose option is supported"
                heading "Podman/systemd quadlets are not available on macOS"
                if [ -z "$INSTALL_TYPE" ]; then
                    prompt "Continue to create a docker-compose.yml?" "[y/N]" confirm
                    if [[ "$confirm" =~ ^[Yy]$ ]]; then
                        INSTALL_TYPE="docker-compose"
                    else
                        echo "Exiting."
                        exit 0
                    fi
                else
                    echo -e "\033[1;31m✗ OS X only supports --docker-compose\033[0m"
                    exit 1
                fi
            fi
            ;;
        *)
            # Linux - full functionality available
            ;;
    esac
}

# Check OS and handle accordingly
detect_os

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
    echo -e '\033[32m  ____        _      _____           _        _ _           \033[0m'
    echo -e '\033[32m / __ \      | |    |_   _|         | |      | | |          \033[0m'
    echo -e '\033[32m| |  | | __ _| | __   | |  _ __  ___| |_ __ _| | | ___ _ __ \033[0m'
    echo -e '\033[32m| |  | |/ _` | |/ /   | | | ._ \/ __| __/ _` | | |/ _ \ .__|\033[0m'
    echo -e '\033[32m| |__| | (_| |   <   _| |_| | | \__ \ || (_| | | |  __/ |   \033[0m'
    echo -e '\033[32m \____/ \__,_|_|\_\ |_____|_| |_|___/\__\__,_|_|_|\___|_|    \033[0m'
   echo
    heading "Select installation method:"
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
    prompt "Select installation method" "[1-3]" choice
fi

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
    heading "📋 Configuration Summary:"
    echo ""
    heading "Will pull:"
    echo -e "   📦️ \033[1;36m${OAK_IMG}\033[0m"
    heading "Will write:"
    echo -e "   ✏️ \033[1;36m./docker-compose.yml\033[0m"
    heading "Configuration:"
    echo "   - HTTP Port: $HTTP_PORT"
    echo "   - Postgres DB: $POSTGRES_DB_NAME"
    echo "   - Postgres User: $POSTGRES_USER"
    echo "   - Postgres Password: [hidden]"
    echo "   - Postgres Image: $POSTGRES_IMG"
    echo "   - Redis Image: $REDIS_IMG"
    echo "   - Oak Image: $OAK_IMG"
    echo ""
    prompt "Continue to create this file?" "[y/N]" confirm
    if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
        echo "Installation cancelled."
        exit 0
    fi

    progress "📦 Pulling latest image with podman..."
    podman pull "$OAK_IMG"

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

    # Prepare config file volume for quadlets
    CONFIG_VOLUME_LINE=""
    if [ -n "$CONFIG_FILE" ]; then
        expanded_path=$(echo "$CONFIG_FILE" | sed "s|^~|$HOME|")
        if [ "$choice" == "1" ]; then
            # rootless quadlet: replace home with %h
            formatted_path=$(echo "$expanded_path" | sed "s|$HOME|%h|")
            CONFIG_VOLUME_LINE="Volume=$(echo "$formatted_path"):/etc/oak.edn:Z"
        else
            # root quadlet: use path as-is
            CONFIG_VOLUME_LINE="Volume=$(echo "$expanded_path"):/etc/oak.edn:Z"
        fi
    else
        if [ "$choice" == "1" ]; then
            CONFIG_VOLUME_LINE="# Volume=%h/path/to/oak.edn:/etc/oak.edn:Z"
        else
            CONFIG_VOLUME_LINE="# Volume=/path/to/oak.edn:/etc/oak.edn:Z"
        fi
    fi

    OAK_QUADLET=$(cat <<EOF
[Unit]
Description=Oak IAM
Requires=oak-postgres.service oak-redis.service oak-net-network.service
After=oak-postgres.service oak-redis.service oak-net-network.service

[Container]
Image=$OAK_IMG
ContainerName=oak
Environment=OAK__DB__URL=jdbc:postgresql://oak-postgres:5432/${POSTGRES_DB_NAME}
Environment=OAK__DB__USERNAME=${POSTGRES_USER}
Environment=OAK__DB__PASSWORD="${POSTGRES_PASSWORD}"
Environment=OAK__REDIS__HOST=oak-redis
Environment=OAK__HTTP__PORT=${HTTP_PORT}
Network=oak-net
PublishPort=${HTTP_PORT}:${HTTP_PORT}
${CONFIG_VOLUME_LINE}

[Service]
Restart=always

[Install]
WantedBy=multi-user.target default.target
EOF
)
    write_file "$OAK_QUADLET" "$TARGET_DIR/oak.container" "$SUDO_REQUIRED"

    echo ""
    echo -e "✅ \033[1;32mSuccessfully generated quadlet files!\033[0m"
    echo
    if [ "$choice" == "1" ]; then
      progress "🚀 To start rootless podman services:"
      echo -e "   \033[1;36msystemctl --user daemon-reload"
      echo -e "   \033[1;36msystemctl --user start --now oak.service\033[0m"
      progress "⚠️ To make sure containers restart on boot:"
      echo -e "   \033[1;36mloginctl enable-linger $USER\033[0m"
    else
      progress "🚀 To start root podman services:"
      echo -e "   \033[1;36msudo systemctl daemon-reload"
      echo -e "   \033[1;36msudo systemctl start --now oak.service\033[0m"
    fi
    echo -e "\033[1;34mVisit your new Oak installation at\n   \033[1;36mhttp://127.0.0.1:$HTTP_PORT\033[0m"
    echo -e "\033[1;34mNext step, Setup users and Applications\n   \033[1;36mhttps://git.gaiwan.co/gaiwan/Oak/src/branch/main/doc/setup.md\033[0m"
    echo -e "\033[1;34mConfiguration instructions, including outgoing email (SMTP) \n   \033[1;36mhttps://git.gaiwan.co/gaiwan/Oak/src/branch/main/doc/configuring.md\033[0m"
    echo
    ;;

  3)
    progress "📦 Pulling latest image with docker..."
    docker pull "$OAK_IMG"
    echo ""
    heading "📋 Configuration Summary:"
    heading "Will write:"
    echo -e "   ✏️ \033[1;36m./docker-compose.yml\033[0m"
    echo ""
    heading "Configuration:"
    echo "   - HTTP Port: $HTTP_PORT"
    echo "   - Postgres DB: $POSTGRES_DB_NAME"
    echo "   - Postgres User: $POSTGRES_USER"
    echo "   - Postgres Password: [hidden]"
    echo "   - Postgres Image: $POSTGRES_IMG"
    echo "   - Redis Image: $REDIS_IMG"
    echo "   - Oak Image: $OAK_IMG"
    echo ""
    prompt "Continue to create this file?" "[y/N]" confirm

    if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
        echo "Installation cancelled."
        exit 0
    fi

    echo ""
    progress "✏ Generating docker-compose.yml in current directory..."
    echo ""

    # Prepare config file volume for docker-compose
    if [ -n "$CONFIG_FILE" ]; then
        expanded_path=$(echo "$CONFIG_FILE" | sed "s|^~|$HOME|")
        formatted_path=$(echo "$expanded_path" | sed "s|$HOME|\${HOME}|g")
        volume_line="    volumes:
      - \"${formatted_path}:/etc/oak.edn:ro\""
    else
        volume_line="    # volumes:
    #   - \"\${HOME}/path/to/oak.edn:/etc/oak.edn:ro\""
    fi

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
    ports:
      - "4800:4800"

  oak-redis:
    image: $REDIS_IMG
    restart: always

  oak:
    image: $OAK_IMG
    container_name: oak
    restart: always
    environment:
      OAK__DB__URL: jdbc:postgresql://oak-postgres:5432/${POSTGRES_DB_NAME}
      OAK__DB__USERNAME: ${POSTGRES_USER}
      OAK__DB__PASSWORD: ${POSTGRES_PASSWORD}
      OAK__REDIS__HOST: oak-redis
$volume_line

volumes:
  oak_data:
EOF
)
    write_file "$COMPOSE_YML" "docker-compose.yml" "false"

    echo ""
    progress "✅ Successfully generated docker-compose.yml!"
    echo
    progress "🐳 To start the services:"
    echo -e "   \033[1;36mdocker compose up -d\033[0m"
    echo
    echo -e "\033[1;34mVisit your new Oak installation at\n   \033[1;36mhttp://127.0.0.1:$HTTP_PORT\033[0m"
    echo -e "\033[1;34mNext step, Setup users and Applications\n   \033[1;36mhttps://git.gaiwan.co/gaiwan/Oak/src/branch/main/doc/setup.md\033[0m"
    echo -e "\033[1;34mConfiguration instructions, including outgoing email (SMTP) \n   \033[1;36mhttps://git.gaiwan.co/gaiwan/Oak/src/branch/main/doc/configuring.md\033[0m"
    echo
    ;;

  *)
    # Should be caught by the initial check, but included for completeness
    echo "Invalid selection."
    exit 1
    ;;
esac
