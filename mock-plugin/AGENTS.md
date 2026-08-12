# AGENTS.md — mock-plugin/

Parent guide: [`../AGENTS.md`](../AGENTS.md)

## Purpose

Implements interfaces from **both** `esignet-integration-api` and
`signup-integration-api`, backed by the [Mock IDA
system](https://github.com/mosip/esignet-mock-services/tree/master/mock-identity-system)
instead of a real MOSIP ID Authentication service. **Development/demo
use only — not for production.** See `../AGENTS.md`'s
Repository-Specific Considerations for why.

## Layout

Two separate package roots exist side by side in this module — one per
consumer:

```text
mock-plugin/src/main/java/
├── io/mosip/esignet/plugin/mock/       # esignet-integration-api implementation
│   ├── dto/                              # Kyc*RequestDto/ResponseDto (V2/V3 variants), LanguageValue
│   └── service/
│       ├── MockAuthenticationService.java      # implements `Authenticator`
│       ├── MockHelperService.java
│       └── MockKeyBindingWrapperService.java
└── io/mosip/signup/plugin/mock/        # signup-integration-api implementation
    ├── dto/                              # BiometricData, LanguageValue, MockIdentityRequest/Response, MockScene, MockUserStory
    ├── service/MockProfileRegistryPluginImpl.java   # implements `ProfileRegistryPlugin`
    ├── verifier/MockIdentityVerifierPluginImpl.java  # extends `IdentityVerifierPlugin`
    └── util/ErrorConstants.java
```

`src/test/java` mirrors both package roots with one test class per main
service class (5 test files total).

## Key classes

- **`MockAuthenticationService implements Authenticator`** — the
  esignet-side plugin. Public API: `doKycAuth`, `doKycExchange`,
  `sendOtp`, `isSupportedOtpChannel`, `getAllKycSigningCertificates`,
  plus the newer `doKycAuth`/`doVerifiedKycExchange` overloads that take
  `boolean claimsMetadataRequired`/`VerifiedKycExchangeDto`. If you add a
  new `Authenticator` method (from an `esignet-integration-api` version
  bump), implement it here.
- **`MockProfileRegistryPluginImpl implements ProfileRegistryPlugin`** —
  the signup-side plugin: `validate`, `createProfile`, `updateProfile`,
  `getProfileCreateUpdateStatus`, `getProfile`, `isMatch`,
  `getUISpecification`.
- **`MockIdentityVerifierPluginImpl extends IdentityVerifierPlugin`** —
  drives a scripted mock identity-verification flow using
  `MockScene`/`MockUserStory` fixtures rather than a real biometric
  verification backend.

## Build & Test Commands

```bash
cd mock-plugin
mvn clean install
mvn test
mvn test -Dtest=MockAuthenticationServiceTest
```

## Configuration

`src/main/resources/application.properties` ships with default values
for every property this plugin needs — see `README.md`'s Configuration
section. Real overrides happen at the host application
(`esignet-service`) level via environment variables, not by editing
this file. `README.md` also documents two `key_policy_def` DB rows
(`MOCK_AUTHENTICATION_SERVICE`, `MOCK_BINDING_SERVICE`) that must exist
in the target `mosip_esignet` database, and a required
`"bindingtransaction"` entry in `mosip.esignet.cache.names`.

## Agent rules

### Do

1. Keep esignet-side changes under `io.mosip.esignet.plugin.mock` and
   signup-side changes under `io.mosip.signup.plugin.mock` — don't mix
   the two package roots even though they live in one module.
2. Add a matching test class under `src/test/java` mirroring the
   package you changed.
3. Update `README.md`'s DB-entries section if a change requires a new
   `key_policy_def` row or cache-name entry.

### Do not

1. Do not treat this module as production-ready — it's explicitly
   development/demo-only (see `../AGENTS.md`).
2. Do not add real credentials/keys to `application.properties` — every
   value here is a documented default meant to be overridden by the
   host application.
