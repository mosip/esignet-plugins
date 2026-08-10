# AGENTS.md

## Repository Overview

This repository hosts the Java runtime-dependency plugins used by
[esignet](https://github.com/mosip/esignet) and
[esignet-signup](https://github.com/mosip/esignet-signup). Each plugin
implements the interfaces defined in `esignet-integration-api` and/or
`signup-integration-api` and is bundled as a runtime jar into the eSignet
service images. MOSIP publishes two image flavors: a base `esignet` image and
an `esignet-with-plugins` image that bundles the plugins from this repo.

There is no root aggregator `pom.xml` — the repository is a flat collection
of three independent Maven modules, each built and published on its own by
CI:

| Module | Purpose |
|---|---|
| [`mock-plugin`](mock-plugin/README.md) | Implementation for use with the [Mock IDA system](https://github.com/mosip/esignet-mock-services/tree/master/mock-identity-system). Development/demo use only — not for production. |
| [`mosip-identity-plugin`](mosip-identity-plugin/README.md) | Integrates eSignet with the [MOSIP IDA system](https://github.com/mosip/id-authentication) and esignet-signup with [MOSIP ID Repository](https://github.com/mosip/id-repository). This is the production plugin. |
| [`sunbird-rc-plugin`](sunbird-rc-plugin/README.md) | Wraps the [Sunbird-RC](https://github.com/Sunbird-RC/sunbird-rc-core) registry system as an eSignet authenticator/VCI plugin (compatible with Sunbird-RC 1.0.0). |

Each module's own `README.md` is the authoritative source for that module's
configuration properties, dependent services, and database entries — this
file does not repeat that detail, only points to it.

## Technology Stack

- Language: Java 21 (`java.version`, `maven.compiler.source/target` are all
  `21` in every module's `pom.xml`)
- Build tool: Apache Maven (no Maven Wrapper committed — use a locally
  installed `mvn`)
- Test: JUnit via `maven-surefire-plugin` (version `3.1.2`), coverage via
  `jacoco-maven-plugin` (version `0.8.14`) in each module
- Packaging: plain `jar` for `mock-plugin` and `sunbird-rc-plugin`;
  `mosip-identity-plugin` additionally uses `maven-assembly-plugin` with
  `src/assembly.xml`
- Artifacts published to Maven Central / Sonatype OSSRH snapshots
  (`https://central.sonatype.com/repository/maven-snapshots`)

## Build & Test Commands

Each module is built independently — there is no parent `pom.xml` to build
all three from the repo root. Run Maven from inside the module directory you
are changing:

```shell
cd mock-plugin
mvn clean install
```

```shell
cd mosip-identity-plugin
mvn clean install
```

```shell
cd sunbird-rc-plugin
mvn clean install
```

Run only the tests for a module:

```shell
cd mock-plugin
mvn test
```

Run a single test class:

```shell
cd mock-plugin
mvn test -Dtest=MockAuthenticationServiceTest
```

To build against a specific SNAPSHOT version of `esignet-integration-api` /
`signup-integration-api` (both are `provided`-scope dependencies resolved
from the OSSRH snapshot repository declared in each module's `pom.xml`),
override the version property before the goal, not after the module path —
system properties (`-D...`) must precede the goal on the Maven command line:

```shell
cd mosip-identity-plugin
mvn -Designet.version=1.6.0-SNAPSHOT clean install
```

## Configuration

There are no secrets or local-override property files checked into this
repository. Each module ships one `src/main/resources/application.properties`
with default values for its own plugin; there is no
`application-local.properties` or similar override file in any module.
Configuration works by env-var overrides at the host application
(esignet-service / signup-service) level, as described in each module's
README:

- `mock-plugin/src/main/resources/application.properties`
- `mosip-identity-plugin/src/main/resources/application.properties`
- `sunbird-rc-plugin/src/main/resources/application.properties`

`mosip-identity-plugin` additionally requires values that have no default and
must be supplied by the deployer: `mosip.ida.client.secret` and
`mosip.esignet.misp.key` (see
[mosip-identity-plugin/README.md](mosip-identity-plugin/README.md)).

Never commit real values for these, or for any Nexus/OSSRH/GPG credentials
used by the release workflow (see below) — they are supplied only as GitHub
Actions secrets.

## Project Structure Notes

- `mock-plugin/`, `mosip-identity-plugin/`, `sunbird-rc-plugin/` — the three
  independent plugin modules described above. Each has its own `pom.xml`,
  `README.md`, `src/main/java`, `src/main/resources`, and `src/test/java`.
- `.github/workflows/push-trigger.yml` — on every push to `master`, `develop`,
  `1.*`, `MOSIP*`, or `release*`, and on PR open/reopen/sync, builds all three
  modules independently (one `build-maven-<module>` job per module) using the
  shared `mosip/kattu` reusable workflows. On non-PR, non-release pushes off
  `master`, each module is also published to Nexus and Sonar-analyzed.
- `.github/workflows/codeql.yml` — runs CodeQL static analysis (Java/Kotlin)
  on pushes and PRs targeting `develop`, plus a weekly schedule.
- No root `pom.xml`: do not try to build "the whole repo" with one Maven
  invocation from the repo root — it will not find a project there.

## Development Workflow

1. Fork the repository and clone your fork.
2. Branch from `develop` (the active integration branch; CI, including
   CodeQL, targets `develop`).
3. Make changes inside the one module your change concerns. Cross-module
   changes are rare because the modules do not depend on each other.
4. Run `mvn clean install` (or at least `mvn test`) inside that module
   before opening a PR.
5. Keep new/changed classes under the module's existing package roots
   (`io.mosip.esignet.plugin.<module>` for esignet-integration-api
   implementations, `io.mosip.signup.plugin.<module>` for
   signup-integration-api implementations — see `mock-plugin` and
   `mosip-identity-plugin` for examples of both).

## Pull Request Guidelines

- Target the `develop` branch.
- Reference the tracking issue (e.g. `MOSIP-xxxxx` or a GitHub issue URL) in
  the PR title/description, following the existing commit history convention
  in this repo (see `git log`).
- Sign off commits (`git commit -s`) — MOSIP requires a DCO sign-off line
  matching the committer's real identity.
- Expect CI to run the module-specific Maven build and CodeQL scan
  automatically; a failing build or new CodeQL alert should be fixed before
  requesting review.
- Do not add a root `pom.xml` or otherwise couple the three modules together
  unless that is explicitly the goal of the change — they are deliberately
  independent, versioned and released separately.

## Repository-Specific Considerations

- `mock-plugin` is explicitly documented as **not for production use** — it
  exists to exercise eSignet against the Mock IDA system. Do not point
  production-facing changes at it; use `mosip-identity-plugin` instead.
- Each module declares its own `<version>` in its `pom.xml` and is released
  independently to Sonatype/Maven Central — bumping one module's version does
  not affect the others.
- `esignet-integration-api` and `signup-integration-api` are consumed as
  `provided`-scope dependencies (i.e. supplied by the host esignet/esignet-
  signup service at runtime, not bundled into the plugin jar). Keep new
  dependencies out of `compile` scope unless they genuinely need to ship
  inside the plugin jar.
- The publish/Sonar/Nexus jobs in `push-trigger.yml` only run for non-PR,
  non-release pushes off `master` — a plain PR only triggers the build (and
  CodeQL) jobs, not publishing.

## Agent rules

### Do

1. Work inside a single module directory (`mock-plugin`, `mosip-identity-plugin`, or `sunbird-rc-plugin`) per change, and run that module's own `mvn clean install` / `mvn test` before proposing a change as complete.
2. Read the target module's own `README.md` before changing its configuration or documenting new properties — it is the source of truth for that module's setup.
3. Keep new provider/service implementation classes in the correct existing package (`io.mosip.esignet.plugin.<module>` or `io.mosip.signup.plugin.<module>`) matching the interface being implemented.
4. Target the `develop` branch for new branches and PRs.
5. Place any `-D` system property before the Maven goal on the command line (e.g. `mvn -Dproperty=value clean install`), never after.

### Do not

1. Do not assume a root `pom.xml` exists — there isn't one, so do not attempt a repo-root Maven build or add cross-module dependencies between the three plugins.
2. Do not commit real secrets, license keys, or credentials (e.g. `mosip.esignet.misp.key`, `mosip.ida.client.secret`, OSSRH/GPG/Sonar tokens) into any `application.properties` or workflow file.
3. Do not treat `mock-plugin` as production-ready — it is documented as development/demo-only.
4. Do not change `esignet-integration-api` / `signup-integration-api` dependency scope away from `provided` without a specific reason — they are supplied by the host service at runtime.
5. Do not skip running the affected module's tests locally before opening a PR just because CI will also run them.
