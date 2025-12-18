# Installing Oak IAM

Oak is distributed as a OCI-compatible container (podman, docker), through our
Forgejo instance. See [Packages: Oak](https://git.gaiwan.co/gaiwan/-/packages/container/oak/snapshot)

The following tags are available:

- `git.gaiwan.co/gaiwan/oak:latest` - latest stable release
- `git.gaiwan.co/gaiwan/oak:snapshot` - built directly from `main`, may be unstable
- `git.gaiwan.co/gaiwan/oak:v0.5.23` - tags pinned to a fixed version

Oak has two additional dependencies: PostgreSQL as its primary data store, and
Redis to maintain session information, so in a typical setup you run three
separate containers: `oak`, `postgres`, and `redis`.

To make this easy for you we provide an install script, which lets you choose
between Podman Quadlets (rootless), Podman Quadlets (root), or Docker Compose.
Linux users who want to evaluate Oak on their own machine should use rootless
quadlets. When setting up a server, either rootless or rootfull quadlets are
suitable. On Mac OS X only Docker Compose is supported.

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

## Command Line Options

The installer supports full command line interface for automation and scripting. All configuration options are available as flags that bypass the interactive menu.

### `--quadlet-rootless`

Skip the interactive menu and immediately generate rootless Podman quadlets in `~/.config/containers/systemd/`. This creates user-level systemd services that run without root privileges. Perfect for personal development environments or when you don't have sudo access.

### `--quadlet-root`

Skip the interactive menu and generate root-level Podman quadlets in `/etc/containers/systemd/`. This creates system-wide systemd services that typically require sudo privileges. Ideal for production server deployments.

### `--docker-compose`

Skip the interactive menu and create a `docker-compose.yml` file in the current directory. This works with both Docker Desktop and Podman Docker compatibility mode. The only option supported on macOS due to systemd limitations.

### `--snapshot`

Install the development snapshot (`git.gaiwan.co/gaiwan/oak:snapshot`) instead of the latest stable release. This pulls the most recent build directly from the main branch, which may include new features or bug fixes, but could also be unstable.

### `--postgres-img IMAGE`

Override the default PostgreSQL container image. Useful for using specific versions, optimized images, or internal registries. The downloader will respect the provided image name exactly as given.

### `--redis-img IMAGE`

Override the default Redis container image. Supports any OCI-compatible Redis image including Alpine variants, custom registries, or specific version tags.

### `--postgres-user USER` (default: `oak`)

Set the PostgreSQL username that Oak will use to connect to the database. This gets passed down to the PostgreSQL container as `POSTGRES_USER` which means it overrides the default "root" user.

### `--postgres-password PASS` (default: `oak`)

Set the PostgreSQL password for the database user. This gets added to the quadlet files for Oak and PostgreSQL. Ensure this is a strong password for production deployments.

### `--postgres-db-name NAME` (default: `oak`)

Specify the PostgreSQL database name that Oak will use to store all persistent data. This creates a dedicated database within PostgreSQL rather than using the default database.

### `--http-port PORT` (default: 4800)

Configure the HTTP port where Oak will listen for connections. Useful when port 4800 is already in use or when running multiple Oak instances on the same machine. This both instructs Oak on which port it should listen, and maps that port to `localhost` so it is accessible.

### `--oak-img IMAGE` (default: `git.gaiwan.co/gaiwan/oak:latest` or `snapshot`)  

Override the Oak container image entirely. This flag takes precedence over `--snapshot` and allows using custom builds, private registries, or specific version tags. Useful for testing custom builds or using internal mirrors.

### `--help`

Display comprehensive help information including all available options, usage patterns, and example commands.


