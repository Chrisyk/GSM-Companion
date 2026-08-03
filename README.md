# Game Sentence Miner Companion (GSMC)

https://github.com/user-attachments/assets/5bb27a24-785c-4efc-a4c7-2a6f3c89c25a

## What it Is

GSMC is an Android Application that acts as a remote client for [Game Sentence Miner](https://github.com/bpwhelan/GameSentenceMiner).
It connects to a remote instance of GSM/Yomitan/AnkiConnect via a private peer-to-peer mesh or a private address.

It makes mining sentences while streaming (through Moonlight/Apollo) more intuitive via a dedicated UI, whilst maintaining a single source of truth (host).

All data, such as dictionaries and Anki fields, is fetched and sent directly to the host, ensuring same-card consistency across local and remote sessions.

## Features

- Full text-hooker functionality through WebSockets
- Full Yomitan dictionary/parser integration with custom HTML tag handling
- Remote Anki note adding with duplicate checking
- Open remote browser GUI for existing note
- Anki fields and ports configuration
- Health check for GSM, AnkiConnect, and Yomitan

## Installation

The application requires a remote connection to the main host; this means many default ports (127.0.0.1) will not be accessible remotely and need to be changed.

**Gateway IP**: Before starting, ensure you have either a private address if you intend to play on the same home network, or a private peer-to-peer mesh service such as TailScale or Twingate if you intend to play from a remote network.

### Automated (Port Proxy Manager)

Port Proxy Manager will forward any gateway's request to the host pc's localhost. It uses netsh to configure TCP port forwarding and redirect incoming network traffic from one IP address port to another. Warning: port forwarding and firewall rules require elevated administrator privileges.

1. Install the .exe file from [releases](https://github.com/Chrisyk/GSM-Companion/releases)
2. Set up the gateway(s) and ports
3. Click Apply Proxies

### Manual

#### GSM
1. On the main GSM Settings Menu
2. Under Advanced->Local Host Bind Address
3. Change the value to that of the Gateway IP

#### Anki
1. Under Tools->Addons
2. Ensure you have the latest version of [Anki Connect](https://ankiweb.net/shared/info/2055492159) installed
3. Click on Extensions and Configs
4. Change "WebBindAddress" to that of the Gateway IP

#### Yomitan
1. Ensure you have Yomitan installed on your browser of choice
2. Clone the [Yomitan-API Repository](https://github.com/yomidevs/yomitan-api)
3. Follow the installation instructions until you have the API running on 127.0.0.1
4. Once finished, open "yomitan_api.py" and change the "ADDR = 127.0.0.1" variable to your GatewayIP
5. Restart your browser

Once finished, open the GSMC app and set the Default Gateway to your GatewayIP. 

Verify the connections work by going into the "Health Check" tab. Finally, configure your Anki Model (same as GSM) and Fields.
