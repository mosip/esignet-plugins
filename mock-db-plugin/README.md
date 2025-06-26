# Mock DB plugin

## About

Implementation for all the interfaces defined in esignet-integration-api. Mock DB plugin is built to use eSignet with any database

This library should be added as a runtime dependency to [esignet-service](https://github.com/mosip/esignet) for development purpose only.

**Note**: This is not production use implementation.

## Configurations

Refer [application.properties](src/main/resources/application.properties) for all the configurations required to use this plugin implementation. All the properties 
are set with default values. If required values can be overridden in the host application by setting them as environment variable. Refer [esignet-service](https://github.com/mosip/esignet)
docker-compose file to see how the configuration property values can be changed.

Add "bindingtransaction" cache name in "mosip.esignet.cache.names" property.

## Databases
You have to create a new database, table and some user details entries as well.

```
-- Step 1: Create Database
CREATE DATABASE IF NOT EXISTS mock_db;

-- Step 2: Create User and Grant Privileges
CREATE USER IF NOT EXISTS 'mock_user'@'localhost' IDENTIFIED BY 'SecureP@ss123';
GRANT ALL PRIVILEGES ON mock_db.* TO 'mock_user'@'localhost';
FLUSH PRIVILEGES;

-- Step 3: Use the Database
USE mock_db;

-- Step 4: Create Table user_detail
CREATE TABLE IF NOT EXISTS user_detail (
    id VARCHAR(12) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    dob DATE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL
);

-- Step 5: Insert Sample Data
INSERT INTO user_detail (id, name, dob, email) VALUES
('3453434553', 'Alice Johnson', '1990-05-14', 'alice@example.com'),
('2583148061', 'Bob Smith', '1985-09-23', 'bob@example.com'),
('9834544352', 'Charlie Brown', '1992-07-11', 'charlie@example.com'),
('5236574533', 'Diana Ross', '1988-12-30', 'diana@example.com');
```

## License
This project is licensed under the terms of [Mozilla Public License 2.0](LICENSE).
