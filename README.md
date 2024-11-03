# Java Authentication (With OAuth)

A simple Java application demonstrating OAuth authentication. This project sets up a local server to handle OAuth callbacks and exchanges authorization codes for access tokens.

## Table of Contents

- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Setup](#setup)
- [Usage](#usage)
- [License](#license)

## Overview

This project demonstrates how to perform OAuth authentication using Java. It includes setting up a local HTTP server to capture OAuth callbacks and exchanging authorization codes for access tokens.

## Prerequisites

- Java 21 or later
- Maven
- An OAuth provider with client credentials

## Setup

1. **Clone the repository:**

   ```bash
   git clone https://github.com/markheramis/java-Authentication-With-OAuth2.git
   cd java-Authentication-With-OAuth2
   ```

2. **Build the project:**

   ```bash
   mvn clean install
   ```

## Usage

1. **Run the application:**

   ```bash
   mvn exec:java
   ```

2. **Follow the prompts:**

   - Enter your OAuth Client ID when prompted.
   - Enter the Authorization URL provided by your OAuth provider.
   - Enter the Token URL provided by your OAuth provider.
   - The application will open the authorization URL in your default browser.
   - After authorizing, the OAuth provider will redirect to the local server, which will capture the callback.

3. **Check the console output:**

   - The application will print the access token response to the console.


## Note

This project is a simple example and may need to be adapted to fit your specific OAuth provider's requirements. currently, it is set up to work with the [Laravel OAuth2 Server](https://github.com/markheramis/Laravel-OAuth-Server) project.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
