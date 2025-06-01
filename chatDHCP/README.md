# DHCP Project

This project implements a simple DHCP (Dynamic Host Configuration Protocol) server and client in Java. The main goal is to study the mechanisms related to the allocation of IP addresses using the DHCP protocol.

## Project Structure

```
dhcp-project
├── src
│   ├── main
│   │   └── java
│   │       ├── dhcp
│   │       │   ├── server
│   │       │   │   ├── DHCPServer.java
│   │       │   │   ├── IPPool.java
│   │       │   │   └── Lease.java
│   │       │   ├── client
│   │       │   │   └── DHCPClient.java
│   │       │   ├── messages
│   │       │   │   ├── DHCPMessage.java
│   │       │   │   ├── DiscoverMessage.java
│   │       │   │   ├── OfferMessage.java
│   │       │   │   ├── RequestMessage.java
│   │       │   │   └── AckMessage.java
│   │       │   └── utils
│   │       │       └── NetworkUtils.java
│   │       └── Main.java
│   └── test
│       └── java
│           └── dhcp
│               ├── DHCPServerTest.java
│               └── DHCPClientTest.java
├── logs
│   ├── server.log
│   └── client.log
├── config
│   └── dhcp.properties
├── pom.xml
└── README.md
```

## Features

- **DHCP Server**: Listens for incoming DHCP messages, processes them, and manages the IP address pool.
- **IP Pool Management**: Handles allocation and deallocation of IP addresses.
- **Lease Management**: Represents leases for IP addresses, including duration and client identifiers.
- **Client Implementation**: Sends DHCP messages (DISCOVER, REQUEST) and processes responses (OFFER, ACK).
- **Logging**: Records server and client activities in log files.
- **Configuration**: Uses a properties file for server configuration.

## Setup Instructions

1. **Clone the Repository**:
   ```
   git clone <repository-url>
   cd dhcp-project
   ```

2. **Build the Project**:
   Use Maven to build the project:
   ```
   mvn clean install
   ```

3. **Run the Server**:
   To start the DHCP server, run:
   ```
   java -cp target/dhcp-project-1.0-SNAPSHOT.jar Main
   ```

4. **Run the Client**:
   To start the DHCP client, run:
   ```
   java -cp target/dhcp-project-1.0-SNAPSHOT.jar dhcp.client.DHCPClient
   ```

## Usage Guidelines

- The server will listen for DHCP DISCOVER messages from clients.
- Clients will send DISCOVER messages to find available DHCP servers and request IP addresses.
- The server will respond with OFFER messages, and clients will confirm with REQUEST messages.
- The server will acknowledge the allocation with ACK messages.

## Logging

Logs for server and client activities can be found in the `logs` directory:
- `server.log`: Contains logs related to server operations.
- `client.log`: Contains logs related to client operations.

## Configuration

The DHCP server configuration can be modified in the `config/dhcp.properties` file. This file includes settings such as the range of IP addresses and lease duration.

## Testing

Unit tests for the server and client functionalities are located in the `src/test/java/dhcp` directory. Use Maven to run the tests:
```
mvn test
```

## Conclusion

This project serves as a practical implementation of the DHCP protocol, providing insights into IP address management and network configuration.