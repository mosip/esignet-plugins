# AGENTS.md — mosip-identity-plugin/

Parent guide: [`../AGENTS.md`](../AGENTS.md)

## Purpose

The **production** plugin: integrates eSignet with the real [MOSIP IDA
system](https://github.com/mosip/id-authentication) and eSignet Signup
with the real [MOSIP ID Repository](https://github.com/mosip/id-repository).
This is what actually ships to a live MOSIP deployment — contrast with
`../mock-plugin/`, which is explicitly demo-only.

## Layout

Same two-package-root pattern as `mock-plugin`:

```text
mosip-identity-plugin/src/main/java/
├── io/mosip/esignet/plugin/mosipid/    # esignet-integration-api implementation
│   ├── dto/                              # Ida*Request/Response, AuditRequest/Response, KeyBinding*, etc. (17 DTOs)
│   ├── helper/AuthTransactionHelper.java
│   └── service/
│       ├── HelperService.java
│       ├── IdaAuditPluginImpl.java             # implements `AuditPlugin`
│       ├── IdaAuthenticatorImpl.java             # implements `Authenticator`
│       └── IdaKeyBinderImpl.java                   # implements `KeyBinder`
└── io/mosip/signup/plugin/mosipid/     # signup-integration-api implementation
    ├── dto/                              # Identity*Request/Response, Schema*, RequestWrapper/ResponseWrapper, etc. (13 DTOs)
    ├── service/
    │   ├── IdrepoProfileRegistryPluginImpl.java   # implements `ProfileRegistryPlugin`
    │   └── MockIdentityVerifierPluginImpl.java      # extends `IdentityVerifierPlugin`
    └── util/BiometricUtil.java, ErrorConstants.java, ProfileCacheService.java
```

**Note the name**: `io.mosip.signup.plugin.mosipid.service.MockIdentityVerifierPluginImpl`
is a *mock* identity verifier that ships inside the otherwise-production
`mosip-identity-plugin` module — MOSIP doesn't yet have a real
identity-verification backend integration for signup, so this
"production" plugin still uses a mock for that one piece. Don't assume
everything under this module is a real MOSIP-backed integration; check
which interface/class you're touching.

## Key classes

- **`IdaAuthenticatorImpl implements Authenticator`** — the esignet-side
  KYC auth/exchange/OTP implementation against real IDA services, backed
  by `AuthTransactionHelper` and `HelperService`.
- **`IdaAuditPluginImpl implements AuditPlugin`** — sends audit events to
  MOSIP's Auditmanager service.
- **`IdaKeyBinderImpl implements KeyBinder`** — handles device/key
  binding requests against IDA.
- **`IdrepoProfileRegistryPluginImpl implements ProfileRegistryPlugin`**
  — signup-side profile create/update/fetch against the real MOSIP ID
  Repository (contrast with `mock-plugin`'s
  `MockProfileRegistryPluginImpl`, which fakes this).

## Build & Test Commands

```bash
cd mosip-identity-plugin
mvn clean install
```

Unlike `mock-plugin` and `sunbird-rc-plugin`, this module additionally
uses `maven-assembly-plugin` with a custom `src/assembly.xml`
(`jar-with-runtime-deps` format — plain jar output, `includeBaseDirectory`
false, bundles compiled classes/resources plus the Maven descriptor
metadata) — check `src/assembly.xml` before assuming standard `jar`
packaging behavior for this module specifically.

To build against a specific SNAPSHOT of `esignet-integration-api`/
`signup-integration-api`, see `../AGENTS.md`'s Build & Test Commands
(`mvn clean install -Designet.version=...`).

## Configuration

`src/main/resources/application.properties` ships with defaults for
every configurable property — real overrides happen at the host
application (`esignet-service`/`signup-service`) level via environment
variables. Two properties have **no default and must be supplied by the
deployer**:

- `mosip.ida.client.secret` — generated as part of MOSIP IDA services
  deployment.
- `mosip.esignet.misp.key` — requires onboarding `esignet-service` as a
  MISP (MOSIP Infra Service Provider) partner in MOSIP's Partner
  Management Portal first (see `README.md`'s Prerequisites section).

Dependent MOSIP services (per `README.md`): for the esignet interface —
IDA services, IdRepo services, Authmanager, Auditmanager, File server;
for the signup interface — Kernel-masterdata-service, IdRepo services,
Authmanager, Auditmanager, Idgenerator, Credential-request-generator.

## Agent rules

### Do

1. Keep esignet-side changes under `io.mosip.esignet.plugin.mosipid`
   and signup-side changes under `io.mosip.signup.plugin.mosipid`.
2. Check `src/assembly.xml` before changing this module's packaging —
   it's the only one of the three modules that uses
   `maven-assembly-plugin`.
3. Treat `mosip.ida.client.secret`/`mosip.esignet.misp.key` as
   deployer-supplied — never give them a real default value in
   `application.properties`.

### Do not

1. Do not assume `MockIdentityVerifierPluginImpl` in this module is a
   real MOSIP-backed verifier — it's a mock, despite living in the
   production plugin module.
2. Do not add real credentials/keys to `application.properties`.
3. Do not change `esignet-integration-api`/`signup-integration-api`
   dependency scope away from `provided` (see `../AGENTS.md`).
