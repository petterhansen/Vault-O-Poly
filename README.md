# Vault-O-Poly

A Fallout-themed Monopoly game built in Java with multiplayer support, casino minigames, and a retro terminal aesthetic.

![Vault-O-Poly](https://img.shields.io/badge/Java-17+-blue) ![Status](https://img.shields.io/badge/status-educational-yellow)

## 🎮 Features

### Core Gameplay
- **Classic Monopoly Mechanics**: Buy properties, collect rent, build improvements
- **Fallout Theme**: Wasteland locations, bottle caps currency, and post-apocalyptic flavor
- **S.P.E.C.I.A.L. Stats**: Each player has randomized RPG stats that affect gameplay
  - **Charisma**: Reduces rent paid to other players (2.5% per point)
  - **Intelligence**: Increases chance of bonus resource production from properties
- **Resource System**: Collect Water, Food, Power, and Scrap Materials
- **Property Development**: Upgrade properties using caps and resources (3 improvement levels)
- **Event Cards**: Random Wasteland events that can help or hinder players

### Multiplayer
- **Network Play**: Host or join games over LAN/Internet
- **Tunnel Support**: Built-in support for Playit.gg, Ngrok, and other tunneling services
- **Session Codes**: Easy-to-share encoded connection codes
- **Real-time Sync**: Board state, player stats, and property ownership synchronized across clients
- **Persistent Chat**: Rich HTML chat with GIF support and multimedia content

### Casino Minigames
- **Four Games**: Coinflip, Blackjack, Baccarat, and Dice
- **Dynamic Currency**: Each game session randomly assigns resource types as currency
- **Mr. House Theme**: New Vegas-inspired orange/gold aesthetic
- **Persistent Configuration**: Casino currencies remain consistent throughout the game session

### Media Features
- **GIF Conversion**: Automatically converts video URLs to GIFs using FFmpeg
- **WebP Support**: Converts WebP images to PNG for compatibility
- **Embedded Media Server**: Hosts converted media for multiplayer clients
- **Smart Caching**: Reuses previously converted files to save bandwidth

### UI/UX
- **Pip-Boy Style**: Retro green terminal aesthetic inspired by Fallout
- **Radio Player**: Integrated music player with Galaxy News Radio, Radio New Vegas and Enclave Radio stations
- **Full Logging**: Comprehensive game event tracking with history
- **Properties Window**: View all board properties with images, descriptions and mortgage management
- **Dark Theme**: Easy on the eyes for extended play sessions

## 🚀 Getting Started

### Prerequisites
- **Java 17 or higher**
- **FFmpeg** (required for GIF conversion feature)
  - Download from [ffmpeg.org](https://ffmpeg.org/download.html)
  - Place `ffmpeg.exe` in `tools/` directory (create if needed)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/vault-o-poly.git
   cd vault-o-poly
   ```

2. **Build with Maven**
   ```bash
   mvn clean package
   ```

3. **Run the game**
   ```bash
   java -jar target/vault-o-poly-1.0.jar
   ```

### Quick Start

#### Local Game
1. Click **"Start Local Game"**
2. Choose number of players (2-4)
3. Enter player names
4. Roll dice to begin!

#### Host Multiplayer
1. Click **"Host Network Game"**
2. (Optional) Enter external tunnel details if using Playit.gg/Ngrok
3. Choose to start fresh or load a saved game
4. Share the generated Session Code with friends
5. Wait for players to join

#### Join Multiplayer
1. Click **"Join Network Game"**
2. Select **"Join by Session ID"**
3. Enter the host's Session Code
4. Wait for the game to sync

## 🎯 How to Play

### The Basics
- **Objective**: Be the last player standing by bankrupting your opponents
- **Turn Structure**:
  1. Collect passive resources from owned properties
  2. Roll dice to move around the board
  3. Land on spaces to buy properties or pay rent
  4. (Optional) Improve properties or trade with other players

### Property Management
- **Buying**: Land on unowned property and choose to purchase
- **Improving**: Own all properties in a color group to build improvements
  - Costs: Caps + Scrap Materials
  - Each level increases rent significantly
- **Mortgaging**: Temporarily mortgage properties for emergency caps
  - Receive 50% of purchase price
  - Un-mortgage for 55% of purchase price

### Special Spaces
- **START**: Collect 200 caps when passing
- **Resource Fields**: Gain random resources (Water, Food, Power, Scrap)
- **Wasteland/Vault-Tec**: Draw event cards
- **Jail**: Pay 50 caps, use card, or roll doubles to escape
- **Casino**: Type `/casino` in chat to play minigames

### Trading
1. Click **"Trade"** button during your turn
2. Select trading partner
3. Build your offer (caps, resources, properties)
4. Partner builds counter-offer
5. Both players must accept the deal

### Commands
- `/roll` - Roll dice (only on your turn)
- `/casino` - Open casino minigames
- `/gif [url]` - Convert and share video/image URLs
- `/w [player] [message]` - Whisper to specific player
- `/me [action]` - Roleplay action text
- `/radio` or `/music` - Open radio tuner
- `/help` - Show all commands

## 🛠️ Configuration

### Radio Stations
Create `radio.json` in the game directory to add custom stations:

```json
[Galaxy News Radio]
https://example.com/song1.mp3
https://example.com/song2.mp3

[Radio New Vegas]
https://example.com/another-song.mp3
```

### Network Settings
For hosting over the internet:
1. Use [Playit.gg](https://playit.gg) for easiest setup (recommended)
2. Or forward ports **10365** (game) and **10366** (media) on your router
3. Enter external IP/domain when prompted during host setup

### Cache Management
- **Settings → Cache Maintenance** to clear temporary files
- Video cache: `%TEMP%/vop_*`
- GIF cache: `%USERPROFILE%/VaultOPolyCache/gifs/`

## 🔧 Advanced Features

### Save System
- **Encrypted Saves**: Game states are AES-256 encrypted
- **Save Viewer**: Standalone tool to inspect `.vop` save files
  ```bash
  java -cp target/vault-o-poly-1.0.jar tools.SaveViewer
  ```

### GIF Conversion
The game can convert videos to GIFs:
```
/gif [url] [start_time] [duration]
/gif https://example.com/video.webm 5 10
```
- Supports: MP4, WebM, MOV, AVI, MKV
- Auto-caches to avoid re-conversion
- Host approval required for client requests

### Debug Commands (Host Only)
- `/setcaps [player] [amount]` - Set player's caps
- `/addcaps [player] [amount]` - Add caps to player
- `/setowner [position] [player]` - Transfer property ownership
- `/setres [player] [type] [amount]` - Set resource amount
- `/teleport [player] [position]` - Move player to board position

## 📁 Project Structure

```
src/main/java/
├── board/          # Board and field types
├── config/         # JSON loaders and constants
├── game/           # Core game logic and networking
├── mechanics/      # Game systems (trading, casino, etc.)
├── players/        # Player data and S.P.E.C.I.A.L. stats
├── radio/          # Music player system
├── resources/      # Resource management
├── ui/             # Swing UI components
└── util/           # Helpers and utilities

src/main/resources/
├── board.json      # Board layout definition
├── icons/          # UI icons
├── sounds/         # Sound effects
└── tokens/         # Player token images
```

## 🐛 Troubleshooting

### "FFmpeg not found" error
- Download FFmpeg and place `ffmpeg.exe` in `tools/` directory
- Or disable GIF conversion by not using `/gif` command

### Connection issues
- **LAN**: Ensure firewall allows Java on port 10365
- **Internet**: Use Playit.gg tunnel or port forward 10365-10366
- **Behind VPN**: May need to enter external address manually when hosting

### Game crashes on save/load
- Ensure you have write permissions in game directory
- Try clearing save files in case of corruption

### Chat images not loading
- Check if URL is publicly accessible
- Some formats (AVIF, TIFF) are not supported
- Use `/gif` command to convert unsupported formats

## 🙏 Acknowledgments

- **Fallout Series** by Bethesda/Obsidian - for inspiration and theme
- **[Fallout.wiki](https://fallout.wiki)** - for Property images and Property descriptions
- **FlatLaf** - Modern look and feel for Swing
- **Gson** - JSON parsing
- **Archive.org** - Default radio station tracks
- **Playit.gg** - Easy tunneling solution for multiplayer

## ⚠️ Disclaimer

This is a **personal educational project** created for learning purposes, specifically to explore multiplayer networking concepts in Java. This project is:

- **Not for commercial use** - This is a hobby/learning project with no commercial intent
- **Fan-made content** - This project is inspired by the Fallout universe and Monopoly game mechanics, but is not affiliated with, endorsed by, or connected to Bethesda Softworks, Obsidian Entertainment, Hasbro, or any other rights holders
- **Educational purposes only** - Created purely as a learning exercise in game development and network programming
- **No warranty** - Provided "as is" without any guarantees or support
- **Trademark acknowledgment** - "Fallout," "Vault-Tec," and related terms are trademarks of Bethesda Softworks. "Monopoly" is a trademark of Hasbro. This project uses these themes for educational purposes only.

**If you are a rights holder and have concerns about this project, please contact me and I will address them promptly.**

## 📖 Learning Resources

This project demonstrates:
- **Socket Programming**: Client-server architecture with TCP sockets
- **Object Serialization**: Network message passing with Java serialization
- **Thread Management**: Concurrent client handling and UI updates
- **Swing GUI**: Building complex desktop applications with Java Swing
- **State Synchronization**: Keeping multiplayer game states consistent
- **Media Streaming**: Hosting and serving converted media files
- **Encryption**: AES-256 encryption for save files

Feel free to explore the code and use it as a reference for your own learning!

---

**War. War never changes. But Monopoly... Monopoly just got a whole lot more radioactive.** ☢️
