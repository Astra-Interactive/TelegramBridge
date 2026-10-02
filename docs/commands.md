# Commands

[Telegram](telegram.md) · [Discord](discord.md) · [Proxy](proxy.md) ·
[Troubleshooting](troubleshooting.md) · [Limitations](limitations.md)

## Where to type them

- **Server console or RCON:** without `/`, e.g. `mb telegram check`. Best for tokens and passwords.
- **In game:** with `/`, e.g. `/mb telegram check`.

Settings changed with `/mb` are saved into `config.yml` and applied right away.
The texts of the `/mb` commands are in `translation/onboarding.yml`, the texts of linking accounts in
`translation/link.yml`, the other texts in `translation/main.yml`.

### `--unsafe`

The server writes every command a player types into `logs/latest.log`.
So in game, a command with a token or a proxy password must end with `--unsafe`:

```
/mb telegram token 123456789:AAHd3... --unsafe
/mb discord proxy http proxy.example.com 3128 user password --unsafe
```

Without it the command is refused and nothing is saved. The console and RCON do not need `--unsafe`.

## Permissions

| Permission              | Gives                                                   | Default |
|-------------------------|---------------------------------------------------------|---------|
| `tbridge.setup`         | `/mb status`, `/mb telegram ...`, `/mb discord ...`     | op      |
| `tbridge.reload`        | `/mb reload`                                            | op      |
| `tbridge.unlink.player` | `/unlink <player>`: remove the link of another player   | op      |

## `/mb`

| Command       | What it does                                                                                |
|---------------|---------------------------------------------------------------------------------------------|
| `/mb`         | Help                                                                                        |
| `/mb status`  | Status of both bots, the chat and the channel, the proxy, the last error of sending a message |
| `/mb reload`  | Reloads `config.yml` and the `translation` folder. Tells you if `config.yml` has an error    |

## `/mb telegram`

| Command                                                                     | What it does                                   |
|-----------------------------------------------------------------------------|------------------------------------------------|
| `/mb telegram`                                                              | Step-by-step guide and the current status      |
| `/mb telegram token <token> [--unsafe]`                                     | Sets the bot token                             |
| `/mb telegram chat <chat_id>`                                               | Sets the group                                 |
| `/mb telegram topic <topic_id\|none>`                                       | Sets the topic, `none` for no topic            |
| `/mb telegram proxy <http\|socks5> <host> <port> [username] [password] [--unsafe]` | Sets a proxy, see [proxy.md](proxy.md) |
| `/mb telegram proxy off`                                                    | Removes the proxy                              |
| `/mb telegram api-url <url\|default>`                                       | Sets an own Bot API server, `default` for `api.telegram.org` |
| `/mb telegram bind`                                                         | Gives a code to bind a group, see below        |
| `/mb telegram check`                                                        | Checks the token, the bot, privacy mode, the chat, the rights, the topic and sends a test message |

`/mb telegram bind` shows a code. Send `/bind <code>` into the group, inside the topic if you use topics.
The code is valid for 10 minutes, and the one who sends it must be an admin of the group.
Details in [telegram.md](telegram.md#4-choose-the-chat-and-the-topic).

## `/mb discord`

| Command                                                               | What it does                                          |
|-----------------------------------------------------------------------|-------------------------------------------------------|
| `/mb discord`                                                         | Step-by-step guide and the current status             |
| `/mb discord token <token> [--unsafe]`                                | Sets the bot token                                    |
| `/mb discord channel <channel_id>`                                    | Sets the channel                                      |
| `/mb discord activity <text>`                                         | Sets the status of the bot, "Playing <text>"          |
| `/mb discord proxy http <host> <port> [username] [password] [--unsafe]` | Sets an HTTP proxy, see [proxy.md](proxy.md)        |
| `/mb discord proxy off`                                               | Removes the proxy                                     |
| `/mb discord bind`                                                    | Gives a code to bind a channel, see below             |
| `/mb discord invite`                                                  | Link that adds the bot to a Discord server with the permissions it needs |
| `/mb discord check`                                                   | Checks the token, the connection, the channel, the permissions and sends a test message |

`/mb discord bind` shows a code. Send `!bind <code>` into the channel.
The code is valid for 10 minutes, and the one who sends it needs the Manage Channel permission.
Details in [discord.md](discord.md#4-choose-the-channel).

## Player commands

| Command            | What it does                                                            |
|--------------------|-------------------------------------------------------------------------|
| `/link`            | Gives a code to link a Telegram or Discord account                      |
| `/unlink`          | Removes the link of your account                                        |
| `/unlink <player>` | Removes the link of another player. Needs `tbridge.unlink.player`       |

## Bot commands

Telegram:

| Command        | Where              | What it does                                                   |
|----------------|--------------------|----------------------------------------------------------------|
| `/vanilla`     | The bridged chat   | Lists the players online                                       |
| `/link <code>` | The bridged chat   | Links a Minecraft account, the code comes from `/link` in game |
| `/minfo`       | Any chat           | Replies with the chat id, the topic id and the chat type       |
| `/bind <code>` | The chat to bridge | Binds the chat, the code comes from `/mb telegram bind`        |

Telegram commands also work with the bot's name, e.g. `/vanilla@my_server_chat_bot`.
`/linkminecraft <code>` and `/linkmc <code>` work the same as `/link <code>`.

Discord:

| Command        | Where                 | What it does                                             |
|----------------|-----------------------|----------------------------------------------------------|
| `!vanilla`     | The bridged channel   | Lists the players online                                 |
| `/link <code>` | The bridged channel   | Links a Minecraft account                                |
| `<code>`       | Direct message        | Links a Minecraft account                                |
| `!bind <code>` | The channel to bridge | Binds the channel, the code comes from `/mb discord bind` |

`/linkminecraft <code>` and `/linkmc <code>` work the same as `/link <code>`.
