# MOSIP Identity plugin

## About

Implementation for all the interfaces defined in esignet-integration-api. This plugin is built to use eSignet with [MOSIP IDA system](https://github.com/mosip/id-authentication)

This library should be added as a runtime dependency to [esignet-service](https://github.com/mosip/esignet).

## Prerequisites

1. Onboard esignet-service as a MISP(MOSIP Infra Service Provider) partner in MOSIP's Partner management portal. 
2. Update the MISP license key in `mosip.esignet.misp.key` property.

## Configurations

Refer [application.properties](src/main/resources/application.properties) for all the configurations required to use this plugin implementation.

## License
This project is licensed under the terms of [Mozilla Public License 2.0](LICENSE).
