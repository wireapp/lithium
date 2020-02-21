# Wire™

[![Wire logo](https://github.com/wireapp/wire/blob/master/assets/header-small.png?raw=true)](https://wire.com/jobs/)

## Lithium

[![Build Status](https://travis-ci.org/wireapp/lithium.svg?branch=master)](https://travis-ci.org/wireapp/lithium)

- Lithium is Wire Services SDK written in Java

## How to use it to build your bots?
- In your `pom.xml`:
```
<repositories>
    <repository>
        <id>lithium</id>
        <url>https://packagecloud.io/dkovacevic/lithium/maven2</url>
        <releases>
            <enabled>true</enabled>
        </releases>
    </repository>
</repositories>
<dependencies>
    <dependency>
        <groupId>com.wire.bots</groupId>
        <artifactId>lithium</artifactId>
        <version>2.34.5</version>
    </dependency>
<dependencies>
```

### Tutorial:
- [Echo Bot](https://github.com/wireapp/echo-bot)

## Bot API Documentation

- [API Documentation](https://github.com/wireapp/bot-sdk/wiki).

## How to build the project

Requirements:

- [Java >1.8](http://www.oracle.com)
- [Maven](https://maven.apache.org)
- [Cryptobox4j](https://github.com/wireapp/cryptobox4j)

To build the library, run:

```bash
mvn install
```

## How to register your service with Wire

The `manage.sh` script helps you register as a service provider, create a certificate, and register your service instance.

### Script requirements

- [Bash](https://www.gnu.org/software/bash)
- [jq](https://stedolan.github.io/jq/)
- [cURL](https://curl.haxx.se/)

### How to use the script

In order to register a service, you need to generate a certificate (or bring your own), register as a provider and then register the service.

Using the `manage.sh` script:

- Register as a provider with `manage.sh new-provider`. If everything goes well, the response will contain a password and provider ID, and you should get an email. Open the email and follow the link in the email to confirm your identity. You need to do this only once, even when developing multiple services. This will save the credentials in the local folder, for further authentication.
- If you don't have a certificate already, create a new certificate with `manage.sh new-cert` and follow the instructions. This needs to match the certificate that is used for the SSL termination on your service.
- Deploy your service and make it accessible by public IP, using HTTPS and the certificate you created at step one.
- Obtain an authentication token with `manage.sh auth-provider`. This is a temporary token to perform authenticated requests, and will need to be refreshed periodically if you don't use the script for more than 10 minutes.
- Register a new service with `manage.sh new-service` and enter the required information. Make sure the base URL is an `https` URL. You will receive an service auth token.
- Once a server is created, you can update it with `manage.sh update-service`. 
- Edit the YAML configuration file of your service and add the service token you received at the previous step.
- (Re)-start the service with the new configuration file.
- Activate the service with `manage.sh update-service-conn` to make it _enabled_

## Use Hello World sample service as your first service

- [Hello World](https://github.com/wireapp/echo-bot)

## Other examples of Wire Services

- [Hello World](https://github.com/wireapp/echo-bot)
- [GitHub-bot](https://github.com/wearezeta/github-bot)
- [GitLab-bot](https://github.com/wireapp/gitlab)
- [Alert-bot](https://github.com/wireapp/alert-bot)
- [Texas Holdem](https://github.com/dkovacevic/holdem)
- [Broadcast-bot](https://github.com/wireapp/broadcast-bot)
- [Channel-bot](https://github.com/dkovacevic/channel-bot)
- [Don](https://github.com/wireapp/don-bot)
- [Recording-bot](https://github.com/wireapp/recording-bot)

## Other implementations of Bot API

- [Node.js](https://github.com/wireapp/bot-sdk-node) Wire Services SDK in Node.js
- [Beryllium](https://github.com/OmnijarBots/beryllium) Wire Services SDK in Rust
