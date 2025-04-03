# UltimateBan

A comprehensive punishment management system for Minecraft servers with advanced features beyond what typical ban plugins offer.

## Features

- Multiple punishment types: ban, tempban, mute, tempmute, warn, kick
- Support for appeals system with staff review
- Discord webhook integration for punishment notifications
- Customizable GUI for issuing punishments
- Database support (SQLite, MySQL, MongoDB)
- Offline player punishment support
- IP ban and alt account detection
- Detailed punishment history
- Fake punishment announcements for staff testing
- Warning threshold actions
- Customizable messages and formats

## Installation

1. Download the latest version of UltimateBan from [GitHub Releases](https://github.com/yourusername/UltimateBan/releases) or build from source.
2. Place the JAR file in your server's `plugins` folder.
3. Start or restart your server.
4. Configure the plugin by editing the config.yml file in the plugins/UltimateBan directory.

## Building from Source

### Requirements
- Java JDK 8 or higher
- Maven

### Build Instructions
1. Clone the repository: `git clone https://github.com/yourusername/UltimateBan.git`
2. Navigate to the project directory: `cd UltimateBan`
3. Build the project: `mvn clean package`
4. The compiled JAR will be in the `target` directory.

## Setting Up a Test Server

If you want to test the plugin locally before deploying it to a production server, follow these steps:

### Option 1: Using Spigot BuildTools (Recommended)

1. **Download BuildTools**:
   - Visit [Spigot BuildTools](https://www.spigotmc.org/wiki/buildtools/)
   - Download BuildTools.jar

2. **Run BuildTools to create a server JAR**:
   ```
   java -jar BuildTools.jar --rev 1.16.5
   ```
   (Replace 1.16.5 with your desired version)

3. **Create a server directory**:
   ```
   mkdir TestServer
   cd TestServer
   ```

4. **Copy the server JAR**:
   - Copy the spigot-1.16.5.jar file to your TestServer directory

5. **Create a start script**:
   - For Windows, create a file named `start.bat` with:
     ```
     @echo off
     java -Xmx2G -jar spigot-1.16.5.jar nogui
     pause
     ```
   - For Linux/Mac, create a file named `start.sh` with:
     ```
     #!/bin/bash
     java -Xmx2G -jar spigot-1.16.5.jar nogui
     ```

6. **Start the server**:
   - Run the start script
   - Accept the EULA by editing eula.txt and changing `eula=false` to `eula=true`
   - Start the server again

7. **Install the plugin**:
   - Copy your UltimateBan.jar to the `plugins` folder
   - Restart the server

### Option 2: Using Paper (Easier)

1. **Download Paper**:
   - Visit [Paper downloads](https://papermc.io/downloads)
   - Download your desired version

2. **Create a server directory**:
   ```
   mkdir TestServer
   cd TestServer
   ```

3. **Copy the server JAR**:
   - Copy the paper-1.16.5-XXX.jar file to your TestServer directory

4. **Create a start script** (same as Option 1)

5. **Start the server** (same as Option 1)

6. **Install the plugin** (same as Option 1)

## Testing the Plugin

After setting up your test server and installing the plugin:

1. **Check if the plugin loaded**:
   - Look for "[UltimateBan] UltimateBan v1.0.0 has been enabled!" in the console

2. **Test basic commands**:
   - `/ban testplayer Hacking`
   - `/tempban testplayer 1d Test reason`
   - `/mute testplayer Spamming`
   - `/tempmute testplayer 30m Bad language`
   - `/warn testplayer Breaking rules`
   - `/kick testplayer Testing`

3. **Test the appeal system**:
   - Ban yourself temporarily with `/tempban YourName 10m Testing`
   - Rejoin and use `/appeal This is a test appeal`
   - On an admin account, use `/reviewappeal` to see and manage appeals

4. **Test the GUI system**:
   - Type `/punish playername` to open the punishment GUI

## Permissions

- `ultimateban.ban` - Permission to permanently ban players
- `ultimateban.tempban` - Permission to temporarily ban players
- `ultimateban.unban` - Permission to unban players
- `ultimateban.mute` - Permission to permanently mute players
- `ultimateban.tempmute` - Permission to temporarily mute players
- `ultimateban.unmute` - Permission to unmute players
- `ultimateban.warn` - Permission to warn players
- `ultimateban.kick` - Permission to kick players
- `ultimateban.appeal` - Permission to appeal punishments (default: true)
- `ultimateban.reviewappeal` - Permission to review punishment appeals
- `ultimateban.fakeban` - Permission to fake ban players
- `ultimateban.punish` - Permission to use the punishment GUI
- `ultimateban.bypass.ban` - Immunity to bans
- `ultimateban.bypass.tempban` - Immunity to temporary bans
- `ultimateban.bypass.mute` - Immunity to mutes
- `ultimateban.bypass.tempmute` - Immunity to temporary mutes
- `ultimateban.bypass.kick` - Immunity to kicks
- `ultimateban.bypass.warn` - Immunity to warnings
- `ultimateban.bypass.override` - Ability to bypass a player's immunity

## Configuration

The plugin is highly configurable. Check the config.yml file for all available options.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

If you need help with the plugin, join our Discord server: [Your Discord Server Link]

## Contributing

1. Fork the repository
2. Create your feature branch: `git checkout -b feature/amazing-feature`
3. Commit your changes: `git commit -m 'Add some amazing feature'`
4. Push to the branch: `git push origin feature/amazing-feature`
5. Open a Pull Request 