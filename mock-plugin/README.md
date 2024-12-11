# Mock plugin

## About

Implementation for all the interfaces defined in esignet-integration-api and signup-integration-api. Mock plugin is built to use eSignet with [Mock IDA system](https://github.com/mosip/esignet-mock-services/tree/master/mock-identity-system)

This library should be added as a runtime dependency to [esignet-service](https://github.com/mosip/esignet) for development purpose only.

**Note**: This is not production use implementation.

## Configurations

Refer [application.properties](src/main/resources/application.properties) for all the configurations required to use this plugin implementation. All the properties 
are set with default values. If required values can be overridden in the host application by setting them as environment variable. Refer [esignet-service](https://github.com/mosip/esignet)
docker-compose file to see how the configuration property values can be changed.

Add "bindingtransaction" cache name in "mosip.esignet.cache.names" property.

## Databases
Below two entries need to be added in mosip_esignet.key_policy_def table.

```
INSERT INTO KEY_POLICY_DEF(APP_ID,KEY_VALIDITY_DURATION,PRE_EXPIRE_DAYS,ACCESS_ALLOWED,IS_ACTIVE,CR_BY,CR_DTIMES) VALUES('MOCK_AUTHENTICATION_SERVICE', 1095, 50, 'NA', true, 'mosipadmin', now());

INSERT INTO KEY_POLICY_DEF(APP_ID,KEY_VALIDITY_DURATION,PRE_EXPIRE_DAYS,ACCESS_ALLOWED,IS_ACTIVE,CR_BY,CR_DTIMES) VALUES('MOCK_BINDING_SERVICE', 1095, 50, 'NA', true, 'mosipadmin', now());
```

## License
This project is licensed under the terms of [Mozilla Public License 2.0](LICENSE).
