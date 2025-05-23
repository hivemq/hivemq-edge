# @percy/cli-app

Percy CLI commands for running Percy with native apps.

## Commands
<!-- commands -->
* [`percy app:exec`](#percy-appexec)

### `percy app:exec`

Start and stop Percy around a supplied command for native apps

```
Usage:
  $ percy app:exec [options] -- <command>

Subcommands:
  app:exec:start [options]  Starts a locally running Percy process for native apps
  app:exec:stop [options]   Stops a locally running Percy process
  app:exec:ping [options]   Pings a locally running Percy process
  help [command]            Display command help

Options:
  --parallel                Marks the build as one of many parallel builds
  --partial                 Marks the build as a partial build

Percy options:
  -c, --config <file>       Config file path
  -d, --dry-run             Print snapshot names only
  -P, --port [number]       Local CLI server port (default: 5338)

Global options:
  -v, --verbose             Log everything
  -q, --quiet               Log errors only
  -s, --silent              Log nothing
  -h, --help                Display command help
```
<!-- commandsstop -->
