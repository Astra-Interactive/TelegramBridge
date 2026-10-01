# Discord setup

[Telegram](telegram.md) · [Proxy](proxy.md) · [Commands](commands.md) ·
[Troubleshooting](troubleshooting.md) · [Limitations](limitations.md)

MessageBridge relays chat between Minecraft and one Discord text channel. It works through a bot that you create.
Chat messages from Minecraft are sent through a webhook, so they show the player's name and avatar.

## Quick start

Type these commands in the server console. In the console, commands have no `/`.
In game, add `/` in front and `--unsafe` after the token (see [Set the token](#2-set-the-token)).

1. Open the [Developer Portal](https://discord.com/developers/applications), create an application,
   open **Bot**, press **Reset Token** and copy the token. On the same page turn on **Message Content Intent**.
2. Run `mb discord token <token>`
3. Run `mb discord invite`, open the link and add the bot to your Discord server.
4. Run `mb discord bind`. Send the `!bind <code>` it shows into the channel you want to bridge.
5. Run `mb discord check` and fix anything it marks as an error.

`mb discord` without arguments shows these steps and the current status.

Discord is blocked in some countries, for example in Russia. If `mb discord check` cannot reach Discord,
set up an HTTP proxy first: [proxy.md](proxy.md).

## 1. Create the bot

1. Open <https://discord.com/developers/applications> and press **New Application**.
2. Enter a name, e.g. `My Server`, accept the terms and press **Create**.
3. Open the **Bot** tab.
4. Press **Reset Token**, confirm and copy the token. Discord shows it only once.
5. On the same tab, under **Privileged Gateway Intents**, turn on **Message Content Intent** and press **Save Changes**.
   Without it Discord refuses the connection with close code 4014.

Keep the token private. Anyone who has it controls your bot.
If it leaked, press **Reset Token** again and set the new token. The old one stops working.

## 2. Set the token

From the console or RCON:

```
mb discord token MTEx...
```

From the game:

```
/mb discord token MTEx... --unsafe
```

The server writes every command a player types into `logs/latest.log`, so the token ends up in the log.
`--unsafe` confirms that you accept this. The console does not need it.

You can also put the token into `config.yml` (`jdaConfig.token`) and run `mb reload`.

## 3. Add the bot to your server

Run `mb discord invite`. It prints a link that adds the bot with every permission it needs.
Open it, choose your Discord server and press **Authorize**. The link is ready once the bot has connected.

The permissions and why they are needed:

| Permission           | Why                                                                                    |
|----------------------|----------------------------------------------------------------------------------------|
| View Channel         | To see the channel                                                                     |
| Send Messages        | Server start and stop, replies to commands                                             |
| Read Message History | Replies to commands                                                                    |
| Embed Links          | Join, leave and death messages are embeds                                              |
| Manage Webhooks      | Chat messages are sent through the webhook `BRIDGE_HOOK_<channelId>` with player names and avatars |
| Manage Channel       | The number of players online is shown in the channel topic                              |
| Manage Roles         | Only for the role given on [account linking](#account-linking)                         |

If the channel is private or has its own permission settings, add the bot or its role to the channel
and allow the same permissions there.

To make the link by hand: Developer Portal → your application → **OAuth2** → **URL Generator** →
scope `bot` → tick the permissions above → open the generated URL.

## 4. Choose the channel

### With a bind code (recommended)

1. Run `mb discord bind`. It shows a code, e.g. `48213907`.
2. Send `!bind 48213907` into the channel you want to bridge.
3. The plugin saves `channelId` and tells you what it bound.

- The code works once and is valid for 10 minutes.
- The one who sends the code needs the **Manage Channel** permission in this channel.

### By hand

1. In Discord open **User Settings** → **Advanced** and turn on **Developer Mode**.
2. Right-click the channel → **Copy Channel ID**.
3. Run `mb discord channel 123456789012345678`

The channel must be a normal text channel of a server where the bot is.

## 5. Check the setup

`mb discord check` checks the token, the connection, the channel, the permissions of the bot,
and sends a test message. Every line that failed says what to do.
`mb status` shows both bots, the channel, the proxy and the last error of sending a message.

If something does not work, see [troubleshooting.md](troubleshooting.md).

## Bot status

`mb discord activity play.example.com` makes the bot show **Playing play.example.com**.

## Proxy

Discord works only through an HTTP proxy:

```
mb discord proxy http 127.0.0.1 10809
mb discord proxy off
```

With a login and password, add them at the end, and in game also `--unsafe`. Details in [proxy.md](proxy.md).

## Account linking

1. The player runs `/link` in game and gets a code.
2. The player sends `/link <code>` as a normal message into the bridged channel,
   or sends only the code to the bot in a direct message.

To give roles on linking, fill the `link` block in `config.yml` and run `mb reload`:

```yaml
link:
  linkDiscordRole: "123456789012345678"   # Discord role id
  linkLuckPermsRole: "verified"           # LuckPerms group
```

- To copy a role id, turn on Developer Mode, open **Server Settings** → **Roles**, press **...** next to the role
  → **Copy Role ID**.
- The bot needs **Manage Roles**, and its own role must be higher in the list than the role it gives.
  Drag the bot's role above it in **Server Settings** → **Roles**.
- `link: null` gives no roles.

A player removes the link with `/unlink`. An admin with `tbridge.unlink.player` can run `/unlink <player>`.

## Settings

The settings are in `plugins/MessageBridge/config.yml` on Paper and Spigot,
and in `config/MessageBridge/config.yml` on Forge and NeoForge, in the `jdaConfig` block.

| Key         | Default | What it is                                  | Command                          |
|-------------|---------|---------------------------------------------|----------------------------------|
| `token`     | `""`    | Bot token. Empty turns Discord off          | `mb discord token`               |
| `activity`  | `""`    | Status of the bot, "Playing ..."            | `mb discord activity`            |
| `channelId` | `""`    | Id of the text channel                      | `mb discord channel`, `bind`     |
| `proxy`     | `null`  | HTTP proxy, see [proxy.md](proxy.md)        | `mb discord proxy`               |

Example:

```yaml
jdaConfig:
  token: "MTEx..."
  activity: "play.example.com"
  channelId: "123456789012345678"
  proxy: null
```

The plugin rewrites `config.yml` on every load. Comments you add are lost, only the built-in comments stay.

## Bot commands in Discord

| Command        | Where                  | What it does                                |
|----------------|------------------------|---------------------------------------------|
| `!vanilla`     | The bridged channel    | Lists the players online                    |
| `/link <code>` | The bridged channel    | Links a Minecraft account                   |
| `<code>`       | Direct message         | Links a Minecraft account                   |
| `!bind <code>` | The channel to bridge  | Binds the channel. The code comes from `mb discord bind` |

## Things to know

- Messages of bots and webhooks are not relayed to Minecraft.
- One server relays one channel.
- If the link role is higher than the bot's role, Discord does not let the bot give it.

More in [limitations.md](limitations.md).
