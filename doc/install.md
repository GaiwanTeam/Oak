# Installing Oak

Oak is distributed as a OCI-compatible container (podman, docker), through our
Forgejo instance. See [Packages: Oak](https://git.gaiwan.co/gaiwan/-/packages/container/oak/snapshot)

The following tags are available:

- `git.gaiwan.co/gaiwan/oak:latest` - latest stable release
- `git.gaiwan.co/gaiwan/oak:snapshot` - built directly from `main`, may be unstable
- `git.gaiwan.co/gaiwan/oak:v0.5.23` - tags pinned to a fixed version

Oak has two additional dependencies: PostgreSQL as its primary data store, and
Redis to maintain session information, so in a typical setup you run three
separate containers: `oak`, `postgres`, and `redis`.

You can run the container any way you like. We're big fan of Podman Quadlets,
since they provide a way to declaratively set up containers and manage them
through standard systemd tools. The install script can set these up for you, or
alternatively will create a `docker-compose.yml`. For detailed instructions on
how to set things up with Podman, skip to "Manual setup with Podman Quadlets".
If you don't need help running a container, you can move on to [Configuring
Oak](configuring.md).

## Install Script

To make this easy for you we provide an install script, which lets you choose
between Podman Quadlets (rootless), Podman Quadlets (root), or Docker Compose.
Linux users who want to evaluate Oak on their own machine should use rootless
quadlets. When setting up a server, either rootless or rootfull quadlets are
suitable. On Mac OS X only Docker Compose is supported. Windows user can use
either method through WSL2.

```shell
bash <(curl -sL https://git.gaiwan.co/gaiwan/Oak/raw/branch/main/install.sh)
```

The script will ask you some questions, create the necessary files, and provide
instructions on how to start things from there.

<pre>
<span style="font-weight:bold;color:teal;">┌────────────────────────────────────────┐</span>
<span style="font-weight:bold;color:teal;">│</span>        🌳 <span style="font-weight:bold;color:green;">Oak IAM Installer</span> 🌳         <span style="font-weight:bold;color:teal;">│</span>
<span style="font-weight:bold;color:teal;">└────────────────────────────────────────┘</span>

<span style="font-weight:bold;color:olive;">Select installation method:</span>

  1) <span style="font-weight:bold;color:purple;">Podman</span> (Rootless)
     ↪ ~/.config/containers/systemd/
     ↪ Runs with user privileges

  2) <span style="font-weight:bold;color:red;">Podman</span> (Root)
     ↪ /etc/containers/systemd/
     ↪ System-wide service (requires sudo)

  3) <span style="font-weight:bold;color:blue;">Docker Compose</span>
     ↪ docker-compose.yml in current directory
     ↪ Traditional container orchestration

<span style="font-weight:bold;color:teal;"> Select installation method </span><span style="font-weight:bold;color:olive;">[1-3]</span>: 1

<span style="font-weight:bold;color:purple;">📋 Configuration Summary:</span>
<span style="font-weight:bold;color:olive;">Will write:</span>
   - <span style="font-weight:bold;color:teal;">/home/arne/.config/containers/systemd/oak-net.network</span>
   - <span style="font-weight:bold;color:teal;">/home/arne/.config/containers/systemd/oak-postgres.container</span>
   - <span style="font-weight:bold;color:teal;">/home/arne/.config/containers/systemd/oak-redis.container</span>
   - <span style="font-weight:bold;color:teal;">/home/arne/.config/containers/systemd/oak.container</span>

<span style="font-weight:bold;color:olive;">Configuration:</span>
   - HTTP Port: 4800
   - Postgres DB: oak
   - Postgres User: oak
   - Postgres Password: [hidden]
   - Postgres Image: docker.io/postgres:17
   - Redis Image: docker.io/redis:8.2.1-alpine
   - Oak Image: git.gaiwan.co/gaiwan/oak:latest

<span style="font-weight:bold;color:teal;"> Continue to create these files? [y/N]<span> y

<span style="font-weight:bold;color:blue;">📦 Pulling latest image with podman...</span>

Generating Quadlets in /home/arne/.config/containers/systemd...
   ✏️ <span style="font-weight:bold;color:teal;">Generating /home/arne/.config/containers/systemd/oak-net.network</span>
   ✏️ <span style="font-weight:bold;color:teal;">Generating /home/arne/.config/containers/systemd/oak-postgres.container</span>
   ✏️ <span style="font-weight:bold;color:teal;">Generating /home/arne/.config/containers/systemd/oak-redis.container</span>
   ✏️ <span style="font-weight:bold;color:teal;">Generating /home/arne/.config/containers/systemd/oak.container</span>

<span style="font-weight:bold;color:green;">╔══════════════════════════════════════════════╗</span>
<span style="font-weight:bold;color:green;">║</span>  ✅ <span style="font-weight:bold;color:green;">Successfully generated quadlet files!</span>    <span style="font-weight:bold;color:green;">║</span>
<span style="font-weight:bold;color:green;">╚══════════════════════════════════════════════╝</span>

<span style="font-weight:bold;color:blue;">🚀 To start rootless podman services:</span>
   <span style="font-weight:bold;color:teal;">systemctl --user daemon-reload
   systemctl --user enable --now oak.container</span>
</pre>

### Command Line Options

The installer supports full CLI flags for automation and scripting. All
configuration options are available as flags that bypass the interactive menu.

#### `--quadlet-rootless`

Skip the interactive menu and immediately generate rootless Podman quadlets in
`~/.config/containers/systemd/`. This creates user-level systemd services that
run without root privileges.

#### `--quadlet-root`

Skip the interactive menu and generate root-level Podman quadlets in
`/etc/containers/systemd/`. This creates system-wide systemd services that
typically require sudo privileges. Ideal for production server deployments.

#### `--docker-compose`

Skip the interactive menu and create a `docker-compose.yml` file in the current
directory. This works with both Docker Desktop and Podman Docker compatibility
mode.

#### `--snapshot`

Install the development snapshot (`git.gaiwan.co/gaiwan/oak:snapshot`) instead
of the latest stable release. Snapshot builds track the most recent changes on
the main development branch, which may include new features or bug fixes, but
could also be unstable.

#### `--postgres-img IMAGE`

Override the default PostgreSQL container image. Useful for using specific
versions, optimized images, or internal registries. The downloader will respect
the provided image name exactly as given.

#### `--redis-img IMAGE`

Override the default Redis container image. Supports any OCI-compatible Redis
image including Alpine variants, custom registries, or specific version tags.

#### `--postgres-user USER` (default: `oak`)

Set the PostgreSQL username that Oak will use to connect to the database. This
gets passed down to the PostgreSQL container as `POSTGRES_USER` which means it
overrides the default "postgres" super-user.

#### `--postgres-password PASS` (default: `oak`)

Set the PostgreSQL password for the database user. This gets added to the
quadlet files for Oak and PostgreSQL. Ensure this is a strong password for
production deployments.

#### `--postgres-db-name NAME` (default: `oak`)

Specify the PostgreSQL database name that Oak will use to store all persistent
data. The PostgreSQL container will create this database on startup, Oak will
create the necessary tables and columns when it starts.


#### `--http-port PORT` (default: 4800)

Configure the HTTP port where Oak will listen for connections. This both
instructs Oak on which port it should listen, and maps the container port to
`localhost` so it is accessible from the host. In production setups you would
typically use Nginx/Caddy/Traefik as a reverse proxy, redirecting traffic to
this port.

#### `--oak-img IMAGE` (default: `git.gaiwan.co/gaiwan/oak:latest` or `snapshot`)  

Override the Oak container image entirely. This flag takes precedence over
`--snapshot` and allows using custom builds, private registries, or specific
version tags. Useful for testing custom builds or using internal mirrors.

#### `--help`

Display comprehensive help information.

## Manual setup with Podman Quadlets

Previous sections explained how to use the installer script. If you prefer to
set things up yourself, or want to understand what the installer script does,
then this section is for you.

Podman Quadlets are systemd service files that describe containers. They allow
Podman containers to integrate seamlessly with systemd, providing services that
start on boot, restart on failure, and can be managed with standard `systemctl`
commands.

These files either go into `~/.config/containers/systemd/`, for a "rootless"
setup (running as your regular user), or into `/etc/containers/systemd`, to run
as root.

### Creating Quadlets Manually

If you prefer to create the quadlets yourself or need custom configurations
beyond what the installer provides, here are the individual quadlet files you
need:

#### 1. Network Quadlet (`oak-net.network`)

Creates a dedicated Docker bridge network for Oak services. This isn't strictly
necessary, but it makes things easier since podman doesn't have DNS resolution
of container names on its default network.

```ini
[Unit]
Description=Oak network

[Network]
NetworkName=oak-net
Driver=bridge

[Install]
WantedBy=multi-user.target default.target
```

**Location:**
- Rootless: `~/.config/containers/systemd/oak-net.network`
- Root: `/etc/containers/systemd/oak-net.network`

#### 2. PostgreSQL Quadlet (`oak-postgres.container`)

Runs PostgreSQL with persistent storage.

```ini
[Unit]
Description=Postgres 17 Database Service

[Container]
Image=docker.io/postgres:17
ContainerName=oak-postgres
Environment=POSTGRES_PASSWORD="your-password"
Environment=POSTGRES_USER=oak
Environment=POSTGRES_DB=oak
Volume=oak_data:/var/lib/postgresql/data:Z
Network=oak-net

[Service]
Restart=always

[Install]
WantedBy=multi-user.target default.target
```

#### 3. Redis Quadlet (`oak-redis.container`)

Provides caching and session storage.

```ini
[Unit]
Description=Redis

[Container]
Image=docker.io/redis:8.2.1-alpine
ContainerName=oak-redis
Network=oak-net

[Service]
Restart=always

[Install]
WantedBy=multi-user.target default.target
```

#### 4. Oak IAM Quadlet (`oak.container`)

The main Oak application container. 

```ini
[Unit]
Description=Oak IAM
Requires=oak-postgres.service oak-redis.service oak-net-network.service
After=oak-postgres.service oak-redis.service oak-net-network.service

[Container]
Image=git.gaiwan.co/gaiwan/oak:latest
ContainerName=oak
Environment=OAK__ENV=prod
Environment=OAK__DB__URL=jdbc:postgresql://oak-postgres:5432/oak
Environment=OAK__DB__USERNAME=oak
Environment=OAK__DB__PASSWORD="your-password"
Environment=OAK__REDIS__HOST=oak-redis
Environment=OAK__HTTP__PORT=4800
Network=oak-net
PublishPort=4800:4800

[Service]
Restart=always

[Install]
WantedBy=multi-user.target default.target
```

### Starting / Enabling the Services

Once you have the `.container`/`.network` files in place, they need to be
converted to systemd `.service` files. `systemctl daemon-reload` handles that.

```bash
systemctl --user daemon-reload  # rootless
# or
sudo systemctl daemon-reload    # root
```

Now you can start the Oak service, it will automatically start its dependencies,
so you don't need to manually start redis/postgres.

```bash
systemctl --user enable --now oak.service  # rootless
# or
sudo systemctl enable --now oak.service    # root
```

Some useful commands. All of these will need either `--user` (rootless) or `sudo` (root).

- `systemctl start <service>` - starts the service once
- `systemctl enable <service>` - starts the service and enables it, so it automatically starts at boot
- `systemctl stop <service>` - stop the service
- `systemctl restart <service>` - restart the service
- `journalctl -u <service>` - see logs for a given service
- `journalctl -f -u <service>` - "follow" (stream) the logs for a given service

## Cleaning up Podman setup

If you want to remove Oak, or clean things up so you can start from a clean
slate, these are the steps.

### Stop and disable the systemd services

For rootless:

```shell
systemctl --user disable --now oak.service
systemctl --user disable --now oak-postgres.service
systemctl --user disable --now oak-redis.service
systemctl --user disable --now oak-net-network.service
```

For root:

```shell
sudo systemctl disable --now oak.service
sudo systemctl disable --now oak-postgres.service
sudo systemctl disable --now oak-redis.service
sudo systemctl disable --now oak-net-network.service
```

### Remove the quadlet files

Rootless

```shell
rm ~/.config/containers/systemd/oak.container \
   ~/.config/containers/systemd/oak-net.network \
   ~/.config/containers/systemd/oak-postgres.container \
   ~/.config/containers/systemd/oak-redis.container
```

Root

```shell
sudo rm \
   /etc/containers/systemd/oak.container \
   /etc/containers/systemd/oak-net.network \
   /etc/containers/systemd/oak-postgres.container \
   /etc/containers/systemd/oak-redis.container
```

### Reload systemd services

Rootless

```shell
systemctl --user daemon-reload
```

Root

```shell
sudo systemctl daemon-reload
```

### Remove the persistent volume 

Rootless

```shell
podman volume rm oak_data
```

Root

```shell
sudo podman volume rm oak_data
```
