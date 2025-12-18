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

<span style="font-weight:bold;color:teal;"> Select installation method </span><span style="font-weight:bold;color:olive;">[1-3]</span>:
</pre>
