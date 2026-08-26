# AGENTS.md — sunbird-rc-plugin/

Parent guide: [`../AGENTS.md`](../AGENTS.md)

## Purpose

Implements **only** `esignet-integration-api` (unlike `mock-plugin` and
`mosip-identity-plugin`, which implement both esignet- and
signup-integration-api) — wraps the
[Sunbird-RC](https://github.com/Sunbird-RC/sunbird-rc-core) registry
system as an eSignet authenticator/VCI plugin, compatible with
Sunbird-RC 1.0.0. The smallest of the three modules by a wide margin.

## Layout

```text
sunbird-rc-plugin/src/
├── main/java/io/mosip/esignet/plugin/sunbirdrc/
│   ├── dto/RegistrySearchRequestDto.java
│   └── service/SunbirdRCAuthenticationService.java   # implements `Authenticator`
└── test/java/io/mosip/esignet/plugin/sunbirdrc/service/
    └── SunbirdRCAuthenticaionServiceTest.java          # note the misspelling ("Authenticaion") in the actual filename
```

Only 2 main-source files and 1 test file — this is a single-class
plugin, not a multi-service module like the other two.

## Key class

**`SunbirdRCAuthenticationService implements Authenticator`** — public
API: `initialize()`, `doKycAuth`, `doKycExchange`,
`buildKycDataBasedOnPolicy`, `sendOtp`, `isSupportedOtpChannel`,
`getAllKycSigningCertificates`, plus a static `b64Encode(String)`
helper. `doKycAuth` performs a registry search against Sunbird-RC using
`RegistrySearchRequestDto`.

## Build & Test Commands

```bash
cd sunbird-rc-plugin
mvn clean install -Dgpg.skip=true
mvn test
```

## Configuration

Unlike the other two modules, this one's README documents configuration
as properties the **host application** (`esignet-service`'s
`esignet-default.properties`) must set, not just a local
`application.properties` with defaults:

```properties
mosip.esignet.integration.scan-base-package=io.mosip.esignet.plugin.sunbirdrc
mosip.esignet.integration.authenticator=SunbirdRCAuthenticationService
mosip.esignet.integration.vci-plugin=SunbirdRCVCIssuancePlugin
```

Plus demo KBI (knowledge-based identity) auth-factor configuration for
the Sunbird registry (`individual-id-field`, `field-details`) — see
`README.md` for the full property set. Never commit real registry
endpoints or credentials into these examples.

## Agent rules

### Do

1. Keep this module's single-responsibility scope — it only implements
   `Authenticator`-family interfaces for Sunbird-RC, not the full
   `esignet-integration-api`/`signup-integration-api` surface the other
   two modules cover.
2. Check `README.md` for the exact `esignet-default.properties` keys
   required in the host application when documenting configuration.

### Do not

1. Do not assume this module implements `signup-integration-api` — it
   doesn't; only `esignet-integration-api`.
