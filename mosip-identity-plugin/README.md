# MOSIP Identity plugin

## About

Implementation of all the interfaces defined in esignet-integration-api and signup-integration-api. This plugin is created to integrate eSignet with [MOSIP IDA system](https://github.com/mosip/id-authentication) 
and eSignet-signup with [MOSIP ID repository](https://github.com/mosip/id-repository)

This library should be added as a runtime dependency to [esignet-service](https://github.com/mosip/esignet) and [signup-service](https://github.com/mosip/esignet-signup).

## Dependent MOSIP services for esignet interface implementation
* IDA services
* IdRepo services
* Authmanager service
* Auditmanager service
* File server

## Dependent MOSIP services for esignet-signup interface implementation
* Kernel-masterdata-service
* IdRepo services
* Authmanager service
* Auditmanager service
* Idgenerator service
* Credential-request-generator service

## Prerequisites

1. Onboard esignet-service as a MISP(MOSIP Infra Service Provider) partner in MOSIP's Partner management portal. 
2. Update the MISP license key in `mosip.esignet.misp.key` property.

## Configurations 

Refer [application.properties](src/main/resources/application.properties) for all the configurations required to use this plugin implementation. All the properties
are set with default values. If required values can be overridden in the host application by setting them as environment variable. Refer [esignet-service](https://github.com/mosip/esignet)
docker-compose file to see how the configuration property values can be changed.

Properties without any default value are:
* mosip.ida.client.secret (generated as part of MOSIP IDA services deployment)
* mosip.esignet.misp.key

## License
This project is licensed under the terms of [Mozilla Public License 2.0](LICENSE).
