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
- **Discord webhooks** — messages show the player's name and avatar, not the bot
- **Telegram topics** — a whole group, one topic of a group with topics, or a reply thread
- **Replies** — a Telegram or Discord reply reads `[TG] Alex ↪ Steve: …` in game; hover `↪ Steve` to read the
  replied message. Change the look with `chat.to_minecraft_reply` in `translation/main.yml`
- **Events** — player join (first-time flag), leave, death, server start/stop
- **Setup from the console or in game** — `/mb` commands set tokens, chats and proxies;
  a one-time bind code captures the chat, topic or channel id; `check` finds what is wrong and links to the docs
- **Account linking** — `/link` in game → code → `/link <code>` in Telegram or Discord;
  grants a LuckPerms group and a Discord role, and takes the group away when the player leaves the Discord server
- **Online list** — `/vanilla` (Telegram) or `!vanilla` (Discord)
- **Proxy support** — HTTP with auth for both bots, SOCKS5 for Telegram, own Bot API server for Telegram
- **Safe reload** — a `config.yml` with an error is not applied, the working settings stay

---

## Platform Support

| Platform       | File                           | Minecraft |
|----------------|--------------------------------|-----------|
| Paper / Spigot | `MessageBridge-bukkit-*.jar`   | 1.18+     |
| NeoForge       | `MessageBridge-neoforge-*.jar` | 1.21.1    |
| Forge          | `MessageBridge-forge-*.jar`    | 1.21.1    |

---

## Quick Start

1. Drop the jar into `plugins/` (Paper) or `mods/` (Forge, NeoForge) and start the server.
2. Create a Telegram bot in [@BotFather](https://t.me/BotFather) and a Discord bot in the
   [Developer Portal](https://discord.com/developers/applications) (turn on *Message Content Intent*).
3. In the server console: `mb telegram token <token>` and `mb discord token <token>`.
4. Telegram: make the bot an admin of your group, run `mb telegram bind` and send `/bind <code>` into the group.
   Discord: run `mb discord invite` to add the bot, then `mb discord bind` and send `!bind <code>` into the channel.
5. Run `mb telegram check` and `mb discord check`.

No restart is needed. In Russia and other places where Telegram or Discord is blocked, set up a [proxy](docs/proxy.md) first.

---

## Documentation

| Topic           | Page                                           |
|-----------------|------------------------------------------------|
| Telegram setup  | [docs/telegram.md](docs/telegram.md)           |
| Discord setup   | [docs/discord.md](docs/discord.md)             |
| Proxy           | [docs/proxy.md](docs/proxy.md)                 |
| Commands        | [docs/commands.md](docs/commands.md)           |
| Troubleshooting | [docs/troubleshooting.md](docs/troubleshooting.md) |
| Limitations     | [docs/limitations.md](docs/limitations.md)     |

---

## Configuration

`plugins/MessageBridge/config.yml` on Paper, `config/MessageBridge/config.yml` on Forge and NeoForge.
Every setting has a comment in the file. The `/mb` commands change it for you; after editing it by hand, run `mb reload`.

```yaml
jdaConfig:
  token: ""
  activity: "play.example.com"
  channelId: "123456789012345678"
  proxy: null                 # or: { type: "HTTP", host: "127.0.0.1", port: 10809 }

tgConfig:
  token: ""
  chat_id: "-1001234567890"
  topic_id: ""                # empty: no topic or the General topic
  max_telegram_message_length: 90
  display_name_regex: ".*"
  proxy: null                 # or: { type: "SOCKS5", host: "127.0.0.1", port: 10808 }
  api_url: ""                 # empty: https://api.telegram.org

displayJoinMessage: true
displayLeaveMessage: true
displayDeathMessage: true

link:                         # roles given on account linking; null gives none
  linkDiscordRole: "123456789012345678"   # Discord role ID
  linkLuckPermsRole: "verified"           # LuckPerms group
```

---

## Commands

In the console, type commands without `/`. Full list: [docs/commands.md](docs/commands.md).

| Command                          | Permission              |                                              |
|----------------------------------|-------------------------|----------------------------------------------|
| `/mb status`                     | `tbridge.setup`         | Status of both bots and the last error       |
| `/mb telegram`, `/mb discord`    | `tbridge.setup`         | Step-by-step guide, setup and `check`        |
| `/mb reload`                     | `tbridge.reload`        | Reload the config                            |
| `/link`, `/unlink`               | -                       | Link or unlink your account                  |
| `/unlink <player>`               | `tbridge.unlink.player` | Unlink another player                        |

| Bot command    | Platform          | Action                                      |
|----------------|-------------------|---------------------------------------------|
| `/vanilla`     | Telegram          | List online players                         |
| `!vanilla`     | Discord           | List online players                         |
| `/link <code>` | Telegram, Discord | Link a Minecraft account                    |
| `/minfo`       | Telegram          | Print the chat ID, topic ID and chat type   |

`/linkminecraft <code>` and `/linkmc <code>` work the same as `/link <code>`.

---

## 💜 Support Us

If our projects help you, consider supporting their development.

<table>
<tr>
<td align="center" width="130">
<img src="https://cdn.simpleicons.org/bitcoin/F7931A" width="25" alt="BTC"/><br/>
<sub><b>Bitcoin</b></sub>
</td>
<td>

```text
bc1q9a8dr55jgfae0mhevw3vvczegjv0khfp0ngrnv
```

</td>
</tr>
<tr>
<td align="center" width="130">
<img src="https://cdn.simpleicons.org/ethereum/627EEA" width="25" alt="ETH"/><br/>
<sub><b>Ethereum</b></sub>
</td>
<td>

```text
0x0BaAeEA44Ce08c8DC139224ff57563695B30d423
```

</td>
</tr>
<tr>
<td align="center" width="130">
<img src="https://cdn.simpleicons.org/boosty/F15F2C" width="25" alt="Boosty"/><br/>
<sub><b>Boosty</b></sub>
</td>
<td align="center">
<a href="https://boosty.to/empireprojekt/donate">
<img width="70%" src="https://img.shields.io/badge/Donate-Boosty-F15F2C?style=for-the-badge&logo=boosty&logoColor=white" alt="Donate on Boosty"/>
</a>
</td>
</tr>
</table>

---

<div align="center">
Made with ❤️ by <a href="https://github.com/Astra-Interactive">AstraInteractive</a>
</div>
