# compass-identity-plugin

## About
Implementation for all the interfaces defined in esignet-integration-api.

This library should be added as a runtime dependency to [esignet-service](https://github.com/mosip/esignet)

## Configurations required to be added / updated in esignet-default.properties

````
## Compass plugin configuration

mosip.esignet.integration.scan-base-package=io.compass.esignet.plugin
mosip.esignet.integration.authenticator=CompassAuthenticationService
mosip.esignet.integration.key-binder=CompassKeyBindingWrapperService
mosip.esignet.integration.vci-plugin=NoOpVCIssuancePlugin


## Compass mock OTP configuration
mosip.esignet.compass.authenticator.otp-value=111111


## Update below cache related configuration with "kycauth" and "challengehash" cache name

mosip.esignet.cache.size={'clientdetails' : 200, 'preauth': 200, 'authenticated': 200, 'authcodegenerated': 200, 'userinfo': 200, \
   'linkcodegenerated' : 500, 'linked': 200 , 'linkedcode': 200, 'linkedauth' : 200 , 'consented' :200, 'vcissuance':100, \
  'apiRateLimit' : 500, 'blocked': 500,'kycauth': 500,'challengehash': 500 }

mosip.esignet.cache.expire-in-seconds={'clientdetails' : 86400, 'preauth': 180, 'authenticated': ${mosip.esignet.authentication-expire-in-secs}, \
  'authcodegenerated': 60, 'userinfo': ${mosip.esignet.access-token-expire-seconds}, 'linkcodegenerated' : ${mosip.esignet.link-code-expire-in-secs}, \
  'linked': 60 , 'linkedcode': ${mosip.esignet.link-code-expire-in-secs}, 'linkedauth' : ${mosip.esignet.authentication-expire-in-secs}, \
  'consented': 120, 'vcissuance': ${mosip.esignet.access-token-expire-seconds}, 'apiRateLimit' : 180, 'blocked': 300, 'kycauth':1800, ,'challengehash': 1800}

mosip.esignet.cache.names=clientdetails,preauth,authenticated,authcodegenerated,userinfo,linkcodegenerated,linked,linkedcode,\
  linkedauth,consented,vcissuance,apiRateLimit,blocked,kycauth,challenghash

## Compass identity endpoint configuration, update the API credentials based on the environment



````

## License
This project is licensed under the terms of [Mozilla Public License 2.0](LICENSE).
This integration plugin is compatible with [Sunbird-RC 1.0.0](https://github.com/Sunbird-RC/sunbird-rc-core/tree/v1.0.0)


