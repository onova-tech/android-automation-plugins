# Android Automation Plugins

Plugins for the [Android Automation Agent](https://github.com/onova-tech/android-automation-app),
shared libraries, and **`agp`**, the tool that validates, builds, signs and tests plugin packages
(`.agp`).

A plugin is a folder of YAML files (manifest, commands, skills, flows, targets, screens, interrupt
rules, texts per language, replay tests). It contains no code: everything it can do is a built-in
action of the base app. The package format and rules are specified in the app repository:
[spec 002](https://github.com/onova-tech/android-automation-app/blob/main/specs/002-plugin-packages/spec.md)
and its [package format](https://github.com/onova-tech/android-automation-app/blob/main/specs/002-plugin-packages/contracts/package-format.md).

## Layout

| Path | Content |
|------|---------|
| `plugins/<id>/` | One plugin per folder (example: `whatsapp`) |
| `libraries/<id>/` | Shared libraries vendored into plugins at build time (example: `android-common`) |
| `agp/` | The `agp` command-line tool |
| `engine/` | Git submodule of the app repository; `agp` is built from its `core`, so plugins are checked with exactly the engine the phone runs |
| `scripts/check-plugins.sh` | Validates, replay-tests and builds every plugin (what CI runs) |

## Getting started

Requirements: JDK 17 and git.

```bash
git clone --recurse-submodules https://github.com/onova-tech/android-automation-plugins.git
cd android-automation-plugins
./gradlew :agp:installDist
scripts/check-plugins.sh            # every plugin: validate, test, build into build/packages/
```

Or download `agp-<version>.zip` from the [releases](https://github.com/onova-tech/android-automation-plugins/releases),
unzip it and run `bin/agp`.

### Devcontainer and devices

The devcontainer has only the `adb` client. It reaches the phone or emulator through the **adb
server on the host** (`ADB_SERVER_SOCKET=tcp:host.docker.internal:5037` in
`.devcontainer/devcontainer.json`). By default that server listens on `127.0.0.1` only, so start
it on all interfaces, **on the host**:

```bash
adb kill-server
adb -a nodaemon server start      # foreground; or `adb -a start-server` in the background
```

Then, inside the container, `adb devices` lists the host's devices (e.g. `emulator-5554`). If it
does not, allow TCP port 5037 from the Docker network in the host firewall.

With `-a`, anyone on your local network can reach the adb server: use it on a trusted network
only, and restart it without `-a` (`adb kill-server && adb start-server`) when you are done.

## agp

```bash
AGP=agp/build/install/agp/bin/agp
$AGP validate plugins/whatsapp --libs libraries
$AGP test     plugins/whatsapp --libs libraries                 # replay tests/ against fixtures/
$AGP build    plugins/whatsapp --libs libraries -o build/whatsapp.agp [--key ~/keys/me.key]
$AGP inspect  build/whatsapp.agp
$AGP targets  plugins/whatsapp screen.xml --libs libraries --lang pt
```

`agp targets` checks a plugin against a real screen: `adb shell uiautomator dump /sdcard/s.xml && adb pull /sdcard/s.xml`.
Redact dumps of personal apps before committing them as fixtures.

### Signing

Packages are signed like APKs. Unsigned packages install with a warning; plugins that operate
financial apps or hold secrets need a key the phone owner trusts.

```bash
$AGP keygen -o ~/keys/me.key --name "Me"          # passphrase from AGP_KEY_PASSWORD or the terminal
$AGP build plugins/whatsapp --libs libraries -o build/whatsapp.agp --key ~/keys/me.key
$AGP verify build/whatsapp.agp                    # prints the signer's fingerprint
$AGP fingerprint ~/keys/me.pub                    # share this so users can trust the key
```

Never commit private keys (`*.key` is ignored). Release packages built by CI are **unsigned**.

## Updating the engine

The engine version is the commit `engine/` points to. After a change in the app repository:

```bash
git -C engine fetch origin main && git -C engine checkout origin/main
git add engine && git commit -m "build: update engine"
```

Dependabot also proposes engine updates weekly. CI runs every plugin against the new engine
before it can be merged.

## CI and releases

Every pull request builds `agp` and runs `scripts/check-plugins.sh`. Every merge to `main`
publishes a release with `agp-<version>.zip`, the plugin packages and `SHA256SUMS`.
