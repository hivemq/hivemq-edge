# @percy/cli-build

Commands for interacting with Percy builds

## Commands
<!-- commands -->
* [`percy build:finalize`](#percy-buildfinalize)
* [`percy build:wait`](#percy-buildwait)
* [`percy build:id`](#percy-buildid)

### `percy build:finalize`

Finalize parallel Percy builds

```
Usage:
  $ percy build:finalize [options]

Global options:
  -v, --verbose  Log everything
  -q, --quiet    Log errors only
  -s, --silent   Log nothing
  -h, --help     Display command help
```

### `percy build:wait`

Wait for a build to be finished

```
Usage:
  $ percy build:wait [options]

Options:
  -b, --build <id>       Build ID
  -p, --project <slug>   Build project slug, requires '--commit'
  -c, --commit <sha>     Build commit sha, requires '--project'
  -t, --timeout <ms>     Timeout before exiting without updates, defaults to 10 minutes
  -i, --interval <ms>    Interval at which to poll for updates, defaults to 10 second
  -f, --fail-on-changes  Exit with an error when diffs are found
  --pass-if-approved     Doesn't Exit with an error if the build is approved, requires '--fail-on-changes'

Global options:
  -v, --verbose          Log everything
  -q, --quiet            Log errors only
  -s, --silent           Log nothing
  -h, --help             Display command help

Examples:
  $ percy build:wait --build 2222222
  $ percy build:wait --project org/project --commit HEAD
```

### `percy build:id`

Prints the build ID from a locally running Percy process

```
Usage:
  $ percy build:id [options]

Percy options:
  -P, --port [number]  Local CLI server port (default: 5338)

Global options:
  -v, --verbose        Log everything
  -q, --quiet          Log errors only
  -s, --silent         Log nothing
  -h, --help           Display command help
```
<!-- commandsstop -->
