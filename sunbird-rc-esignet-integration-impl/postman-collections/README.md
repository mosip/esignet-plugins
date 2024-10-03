
# [eSignet](https://docs.esignet.io/overview) Collection

This folder contains Postman collection with requests for creating and updating OIDC clients, performing authentication using KBA flow using esignet.

## Usage

One can [import](https://learning.postman.com/docs/getting-started/importing-and-exporting/importing-and-exporting-overview/ "Postman Docs") the following collections and the corresponding environment files in postman

KBA flow:

* [DCL Collection](./esignet-with-Sunbird-RC.postman_collection.json "Postman Collection")
* [DCL Environment](./esignet-with-Sunbird-RC.postman_environment.json "Environment")



## Prerequisites for KBA Flow

For prerequisites and postman collection for a sample usecase from Sunbird RC can be found here (https://github.com/Sunbird-RC/demo-mosip-rc)

## Crypto Operations

This collection utilizes the [postman util lib](https://joolfe.github.io/postman-util-lib/ "Postman Util Library") for performing crypto operations like

* Key Pair Generation
* Signing
* Client Assertion

## Contributing

Pull requests are welcome. For major changes, please open an issue first
to discuss what you would like to change.