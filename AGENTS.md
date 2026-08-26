# AGENTS.md

## Repository Overview

Java runtime-dependency plugins for [esignet](https://github.com/mosip/esignet)
and [esignet-signup](https://github.com/mosip/esignet-signup). Each plugin
implements `esignet-integration-api` and/or `signup-integration-api` and is
bundled as a runtime jar into the `esignet-with-plugins` image.

No root aggregator `pom.xml` — three independent Maven modules, each built,
versioned, and published on its own:

| Module | Purpose |
|---|---|
| [`mock-plugin`](mock-plugin/README.md) ([`AGENTS.md`](mock-plugin/AGENTS.md)) | Mock IDA-backed implementation. **Not for production.** |
| [`mosip-identity-plugin`](mosip-identity-plugin/README.md) ([`AGENTS.md`](mosip-identity-plugin/AGENTS.md)) | Production plugin — real MOSIP IDA/ID Repository. |
| [`sunbird-rc-plugin`](sunbird-rc-plugin/README.md) ([`AGENTS.md`](sunbird-rc-plugin/AGENTS.md)) | Sunbird-RC registry as an eSignet authenticator/VCI plugin. |

Each module's own `README.md` is the source of truth for its configuration
properties, dependent services, and DB entries — not repeated here.

## Technology Stack

- Java 21 (`java.version`, `maven.compiler.source/target` in every `pom.xml`)
- Apache Maven, no wrapper committed — use a local `mvn`
- Test: JUnit via `maven-surefire-plugin` 3.1.2; coverage via
  `jacoco-maven-plugin` 0.8.14
- Packaging: plain `jar`; `mosip-identity-plugin` additionally uses
  `maven-assembly-plugin` (`src/assembly.xml`)
- Published to Maven Central / Sonatype OSSRH snapshots

## Build & Test Commands

Build from inside the module directory — there's no repo-root build. Each
module's `maven-gpg-plugin` binds `sign` to the `verify` phase (which
`install` runs through), so pass `-Dgpg.skip=true` locally unless you have a
signing key:

```shell
cd mock-plugin && mvn clean install -Dgpg.skip=true
cd mosip-identity-plugin && mvn clean install -Dgpg.skip=true
cd sunbird-rc-plugin && mvn clean install -Dgpg.skip=true
```

```shell
cd mock-plugin
mvn test                                      # module tests
mvn test -Dtest=MockAuthenticationServiceTest # single class
```

To build against a SNAPSHOT of `esignet-integration-api`/`signup-integration-api`
(`provided`-scope, resolved from the OSSRH snapshot repo):

```shell
cd mosip-identity-plugin
mvn clean install -Dgpg.skip=true -Designet.version=1.6.0-SNAPSHOT
```

## Configuration

No secrets or local-override property files are checked in. Each module ships
one `src/main/resources/application.properties` with defaults; real overrides
happen at the host service (esignet-service/signup-service) via env vars.
`mosip-identity-plugin` additionally requires `mosip.ida.client.secret` and
`mosip.esignet.misp.key`, supplied only by the deployer — see
[mosip-identity-plugin/README.md](mosip-identity-plugin/README.md). Never
commit real values for these or any Nexus/OSSRH/GPG release-workflow
credentials — GitHub Actions secrets only.

## Project Structure Notes

- `.github/workflows/push-trigger.yml` — on push to `master`/`develop`/`1.*`/
  `MOSIP*`/`release*` and on PR, builds all three modules independently
  (`mosip/kattu` reusable workflows). Publish to Nexus + Sonar analysis only
  fire on non-PR, non-release pushes off `master`.
- `.github/workflows/codeql.yml` — CodeQL on push/PR to `develop` + weekly.

## Development Workflow

1. Branch from `develop`.
2. Change one module at a time — they don't depend on each other.
3. Run that module's `mvn clean install -Dgpg.skip=true` (or `mvn test`)
   before opening a PR.
4. New classes go under the module's existing package root
   (`io.mosip.esignet.plugin.<module>` or `io.mosip.signup.plugin.<module>`).

## Pull Request Guidelines

- Target `develop`; sign off commits (`git commit -s`, DCO required).
- Reference the tracking issue (`MOSIP-xxxxx` or GitHub issue URL) per
  existing commit-history convention.
- CI runs the module build + CodeQL automatically — fix failures/alerts
  before requesting review.

## Repository-Specific Considerations

- `esignet-integration-api`/`signup-integration-api` are `provided`-scope
  (supplied by the host service at runtime, not bundled) — keep new
  dependencies out of `compile` scope unless they must ship in the jar.
- Maven's CLI parser accepts `-D` before or after the goal; this repo's
  convention is after the goal (unlike a plain `java -jar` command, where
  `-D` must precede `-jar`).

## Agent rules

### Do

1. Work inside one module per change; run its `mvn clean install
   -Dgpg.skip=true` / `mvn test` before calling a change complete.
2. Read the target module's own `README.md` before touching its config.
3. Keep new provider classes in the matching existing package root.

### Do not

1. Don't assume a root `pom.xml` exists, or add cross-module dependencies.
2. Don't commit secrets/credentials into `application.properties` or
   workflow files.
3. Don't point production-facing changes at `mock-plugin` — demo-only.
4. Don't move `esignet-integration-api`/`signup-integration-api` off
   `provided` scope without a specific reason.
