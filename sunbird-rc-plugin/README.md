# sunbird-rc-plugin

## About
Implementation for all the interfaces defined in esignet-integration-api. This libaray is built as a wrapper for [sunbird-registry-system](sunbird-registry-url) service.

This library should be added as a runtime dependency to [esignet-service](https://github.com/mosip/esignet)

## Configurations required to added in esignet-default.properties

````
mosip.esignet.integration.scan-base-package=io.mosip.esignet.plugin.sunbirdrc
mosip.esignet.integration.authenticator=SunbirdRCAuthenticationService
mosip.esignet.integration.vci-plugin=SunbirdRCVCIssuancePlugin



##--------------------sunbird registry authentication related demo configuration-------------------------##

mosip.esignet.authenticator.sunbird-rc.auth-factor.kbi.individual-id-field='policyNumber'
mosip.esignet.authenticator.sunbird-rc.auth-factor.kbi.field-details={{"id":"policyNumber", "type":"text", "format":""},{"id":"name", "type":"text", "format":""},{"id":"dob", "type":"date", "format":"dd/mm/yyyy"}}
mosip.esignet.authenticator.sunbird-rc.auth-factor.kbi.registry-search-url=http://10.3.148.107/registry/api/v1/Insurance/search
mosip.esignet.authenticator.sunbird-rc.kbi.entity-id-field=osid

##-----------------------------VCI related demo configuration---------------------------------------------##

mosip.esignet.vciplugin.sunbird-rc.issue-credential-url=http://164.52.205.87/credentials/issue 
mosip.esignet.vciplugin.sunbird-rc.supported-credential-types=InsuranceCredential,HealthCardCredential
mosip.esignet.vciplugin.sunbird-rc.credential-type.InsuranceCredential.static-value-map.issuerId=did:web:esignet-mock.dev.mosip.net
mosip.esignet.vciplugin.sunbird-rc.credential-type.InsuranceCredential.template-url=requestTemplete.json
mosip.esignet.vciplugin.sunbird-rc.credential-type.InsuranceCredential.registry-get-url=http://10.3.148.107/api/v1/Insurance/
mosip.esignet.vciplugin.sunbird-rc.credential-type.InsuranceCredential.cred-schema-id=did:schema:1e4d93df-4047-4dd7-8515-9ad46796009f
mosip.esignet.vciplugin.sunbird-rc.credential-type.InsuranceCredential.cred-schema-version=1.0.0
mosip.esignet.vciplugin.sunbird-rc.credential-type.HealthCardCredential.static-value-map.issuerId=did:web:esignet-mock.dev.mosip.net
mosip.esignet.vciplugin.sunbird-rc.credential-type.HealthCardCredential.template-url=requestTemplete.json
mosip.esignet.vciplugin.sunbird-rc.credential-type.HealthCardCredential.registry-get-url=http://10.3.148.107/api/v1/Insurance/
mosip.esignet.vciplugin.sunbird-rc.credential-type.HealthCardCredential.cred-schema-id=did:schema:1e4d93df-4047-4dd7-8515-9ad46796009f
mosip.esignet.vciplugin.sunbird-rc.credential-type.HealthCardCredential.cred-schema-version=1.0.0
````


Add "bindingtransaction" cache name in "mosip.esignet.cache.names" property.

## License
This project is licensed under the terms of [Mozilla Public License 2.0](LICENSE).
This integration plugin is compatible with [Sunbird-RC 1.0.0](https://github.com/Sunbird-RC/sunbird-rc-core/tree/v1.0.0)


