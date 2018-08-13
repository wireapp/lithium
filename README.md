# Wire™

[![Wire logo](https://github.com/wireapp/wire/blob/master/assets/header-small.png?raw=true)](https://wire.com/jobs/)

## Lithium

[![Build Status](https://travis-ci.org/wireapp/lithium.svg?branch=master)](https://travis-ci.org/wireapp/lithium)

- Lithium is a Wire Services SDK written in Java

## Documentation

- [API Documentation](https://github.com/wireapp/bot-sdk/wiki).

## Other implementations

- [Node.js](https://github.com/wireapp/bot-sdk-node)
- [Beryllium](https://github.com/OmnijarBots/beryllium) Wire Services SDK in Rust

## How to build the project

Requirements:

- Java >1.8 (http://www.oracle.com)
- Maven (https://maven.apache.org)

To build the library, run:

```bash
mvn install
```

## How to register your service with Wire

The `manage.sh` script helps you register as a service provider, create a certificate, and register your service instance.

### Script requirements

- Bash (https://www.gnu.org/software/bash)
- jq (https://stedolan.github.io/jq/)
- cURL (https://curl.haxx.se/)

### How to use the script

In order to register a service, you need to generate a certificate (or bring your own), register as a provider and then register the service.

Using the script:

- Create a new certifiate with `manage.sh new-cert` and follow the instructions.
- Register as a provider with `manage.sh new-provider`. If everything goes well, the response will contain a password and provider ID, and you should get an email. Open the email and follow the link in the email to confirm your identity. You need to do this only once, even when developing. This will save the credentials in the local folder, for further authentication.
- Obtain an authentication token with `manage.sh auth-provider`. The token will need to be refreshed periodically.
- Register a new service with `manage.sh new-service` and follow the instructions.
- Once a server is created, you can update/enable it with `manage.sh update-service` and `manage.sh update-service-conn`.

## Some examples of Wire Services

- [Hello World](https://github.com/wireapp/wire-bot-java)
- [GitHub-bot](https://github.com/wearezeta/github-bot)
- [Alert-bot](https://github.com/wireapp/alert-bot)
- [Anna chatbot](https://github.com/wireapp/anna-bot)
- [Broadcast bot](https://github.com/wireapp/broadcast-bot)
- [Channel-bot](https://github.com/dkovacevic/channel-bot)
- [Don](https://github.com/wireapp/don-bot)
- [s2bot](https://github.com/caura/s2bot)
