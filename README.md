<div align="center">

<img src="./.idea/icon.png" width="128">

# MessageBridge

**Minecraft ↔ Telegram ↔ Discord. Bidirectional. Multi-platform.**

</div>

---

Chat messages, join/leave/death events, and server start/stop — all forwarded to Telegram and Discord in real time. Replies from both platforms appear in Minecraft chat. Zero main-thread blocking.

---

## Features

- **Bidirectional chat** — MC ↔ Telegram ↔ Discord, messages tagged with `[MC]` / `[TG]` / `[DS]`
- **Discord webhooks** — messages show sender avatar and name, not the bot
- **Events** — player join (first-time flag), leave, death, server start/stop
- **Account linking** — `/link` in-game → code → `/link <code>` in TG or Discord
    - Grants a LuckPerms role and a Discord role on link
    - Revokes the LuckPerms role when a player leaves the Discord server
- **Online list** — `/vanilla` (Telegram) or `!vanilla` (Discord) shows current players
- **Proxy support** — HTTP proxy with auth for both bots
- **Hot reload** — `/mbreload` reloads config; bots reconnect automatically

---

## Platform Support

| Platform       | File                           | Minecraft |
|----------------|--------------------------------|-----------|
| Paper / Spigot | `MessageBridge-bukkit-*.jar`   | 1.18+     |
| NeoForge       | `MessageBridge-neoforge-*.jar` | 1.20.1    |
| Forge          | `MessageBridge-forge-*.jar`    | 1.20.1    |

---

## Installation

1. Drop the jar into `plugins/` or `mods/`
2. Start the server — `config.yml` and `translations.yml` are generated
3. Fill in bot tokens and channel IDs
4. `/mbreload` or restart

**Discord:** enable *Message Content Intent* in the Developer Portal; bot needs `Send Messages`, `Manage Channel`, `Read Message History`, `Use Webhooks`.

**Telegram:** make the bot an admin in your group; use `/minfo` in the topic to get `chat_id` and `topic_id`.

---

## Configuration

### `config.yml`

```yaml
jdaConfig:
  token: ""
  activity: "play.example.com"
  channelId: ""
  # proxy:          # uncomment if Discord is blocked in your region
  #   host: "0.0.0.0"
  #   port: 2222
  #   username: "user"
  #   password: "password"

tgConfig:
  token: ""
  chat_id: "-1001234567890"
  topic_id: "12345"
  max_telegram_message_length: 140
  # proxy: ...

displayJoinMessage: true
displayLeaveMessage: true
displayDeathMessage: true

# Remove this block to disable account linking
link:
  linkDiscordRole: "123456789012345678"   # Discord role ID
  linkLuckPermsRole: "verified"           # LuckPerms group
```

---

## Commands

### In-game

| Command     | Permission       |
|-------------|------------------|
| `/mbreload` | `tbridge.reload` |
| `/link`     | -                |

### Bot

| Command        | Platform     | Action                     |
|----------------|--------------|----------------------------|
| `/vanilla`     | Telegram     | List online players        |
| `!vanilla`     | Discord      | List online players        |
| `/link <code>` | TG & Discord | Link Minecraft account     |
| `/minfo`       | Telegram     | Print chat ID and topic ID |

---

<div align="center">
Made with ❤️ by <a href="https://github.com/Astra-Interactive">AstraInteractive</a>
</div>
