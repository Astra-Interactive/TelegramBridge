# Telegram setup

[Русская версия](../ru/telegram.md) · [Discord](discord.md) · [Proxy](proxy.md) · [Commands](commands.md) ·
[Troubleshooting](troubleshooting.md) · [Limitations](limitations.md)

MessageBridge relays chat between Minecraft and one Telegram group, or one topic of a group with topics.
It works through a bot that you create.

## Quick start

Type these commands in the server console. In the console, commands have no `/`.
In game, add `/` in front and `--unsafe` after the token (see [Set the token](#2-set-the-token)).

1. In Telegram, open [@BotFather](https://t.me/BotFather), send `/newbot` and copy the token.
2. Run `mb telegram token 123456789:AA...`
3. Add the bot to your group and make it an admin with the **Delete messages** right.
4. Run `mb telegram bind`. Send the `/bind <code>` it shows into the group.
   If the group has topics, send it inside the topic you want to bridge.
5. Run `mb telegram check` and fix anything it marks as an error.

`mb telegram` without arguments shows these steps and the current status.

Telegram is blocked in some countries, for example in Russia. If `mb telegram check` cannot reach Telegram,
set up a proxy first: [proxy.md](proxy.md).

## 1. Create the bot

1. Open [@BotFather](https://t.me/BotFather) in Telegram.
2. Send `/newbot`.
3. Enter a name. Players see it in the chat, e.g. `My Server`.
4. Enter a username. It must end with `bot`, e.g. `my_server_chat_bot`.
5. BotFather replies with the token. It looks like `123456789:AAHd3...` — digits, a colon and about 35 characters.

Keep the token private. Anyone who has it can read the chat and write as your bot.
If it leaked: @BotFather → `/mybots` → your bot → **API Token** → **Revoke current token**, then set the new token.

Create a separate bot for every server. One token works in one place at a time,
see [One bot per server](#one-bot-per-server).

## 2. Set the token

From the console or RCON:

```
mb telegram token 123456789:AAHd3...
```

From the game:

```
/mb telegram token 123456789:AAHd3... --unsafe
```

The server writes every command a player types into `logs/latest.log`, so the token ends up in the log.
`--unsafe` confirms that you accept this. The console does not need it.

You can also put the token into `config.yml` (`tgConfig.token`) and run `mb reload`.

The bot connects right away, there is no need to restart the server.

## 3. Add the bot to the group

1. Open the group → **Add members** → find the bot by its username → add it.
2. Open the group settings → **Administrators** → **Add Admin** → choose the bot.
3. Keep **Delete messages** on. The bot does not need other rights.

Why the bot must be an admin:

- It deletes the messages it does not relay: messages longer than `max_telegram_message_length`
  and messages from names rejected by `display_name_regex`. This needs **Delete messages**.
- An admin bot sees every message in the group, whatever its privacy mode is.

### Privacy mode

New bots have **Group Privacy** turned on. A bot with privacy mode on that is not an admin only receives commands,
the messages that start with `/`. Normal chat messages do not reach Minecraft.

Do one of these:

- Make the bot an admin (recommended).
- Or turn privacy mode off: @BotFather → `/mybots` → your bot → **Bot Settings** → **Group Privacy** → **Turn off**.
  Then remove the bot from the group and add it again. Telegram applies the change only when the bot joins.

`mb telegram check` tells you if the bot cannot see the messages.

## 4. Choose the chat and the topic

### With a bind code (recommended)

1. Run `mb telegram bind`. It shows a code, e.g. `48213907`.
2. Send `/bind 48213907` into the group. If the group has topics, send it inside the topic you want to bridge.
3. The plugin saves `chat_id` and `topic_id` and tells you what it bound.

- The code works once and is valid for 10 minutes.
- Only an admin of the group can bind it.
- If the bot does not react, it cannot see the message. Make it an admin, or send the command with its name:
  `/bind@my_server_chat_bot 48213907`.

### By hand

1. Send `/minfo` into the group, inside the topic if you use one.
   The bot replies with the chat id, the topic id and the chat type. `/minfo` works in any chat.
2. Run `mb telegram chat -1001234567890`
3. Run `mb telegram topic 12`, or `mb telegram topic none` to relay without a topic.

Group ids are negative. Groups with topics and other supergroups have ids that start with `-100`.

Tip: in a private supergroup, the link of a message or a topic looks like `https://t.me/c/1234567890/12`.
The chat id is `-100` followed by the first number: `-1001234567890`.

### What gets relayed

| Group                  | `topic_id`       | Telegram → Minecraft                           | Minecraft → Telegram       |
|------------------------|------------------|------------------------------------------------|----------------------------|
| Without topics         | empty            | Every message in the group                     | Into the group             |
| With topics            | id of a topic    | Messages in this topic                         | Into this topic            |
| With topics            | empty            | Messages in the **General** topic              | Into **General**           |
| Without topics         | id of a message  | Only replies to this message ("reply thread")  | As replies to this message |

The last row is for groups without topics: pick a message, put its id into `topic_id`,
and the conversation lives in the replies to it.
The last number in a message link is its id: `https://t.me/c/1234567890/345` → `345`.

### When the chat id changes

Turning on topics, making the group public and some other settings turn a group into a supergroup.
A supergroup has a new id that starts with `-100`.

When Telegram reports the move, the plugin updates `chat_id` by itself.
If it did not, for example because the server was off, run `mb telegram bind` again.

## 5. Check the setup

`mb telegram check` checks:

- the token and the bot,
- privacy mode,
- the chat: the bot is in it and can see it,
- the rights of the bot,
- the topic,
- and sends a test message.

Every line that failed says what to do. `mb status` shows both bots, the chat, the proxy
and the last error of sending a message.

If something does not work, see [troubleshooting.md](troubleshooting.md).

## Settings

The settings are in `plugins/MessageBridge/config.yml` on Paper and Spigot,
and in `config/MessageBridge/config.yml` on Forge and NeoForge, in the `tgConfig` block.

| Key                           | Default | What it is                                                                 | Command                       |
|-------------------------------|---------|----------------------------------------------------------------------------|-------------------------------|
| `token`                       | `""`    | Bot token. Empty turns Telegram off                                        | `mb telegram token`           |
| `chat_id`                     | `""`    | Id of the group                                                            | `mb telegram chat`, `bind`    |
| `topic_id`                    | `""`    | Id of the topic, see [What gets relayed](#what-gets-relayed)               | `mb telegram topic`, `bind`   |
| `max_telegram_message_length` | `90`    | Longer messages are deleted and not relayed                                |                               |
| `display_name_regex`          | `".*"`  | Names of users without a @username must match it, or the message is deleted |                               |
| `proxy`                       | `null`  | Proxy for Telegram, see [proxy.md](proxy.md)                               | `mb telegram proxy`           |
| `api_url`                     | `""`    | Own Bot API server, empty for `api.telegram.org`, see [proxy.md](proxy.md) | `mb telegram api-url`         |

Example:

```yaml
tgConfig:
  token: "123456789:AAHd3..."
  chat_id: "-1001234567890"
  topic_id: "12"
  max_telegram_message_length: 90
  display_name_regex: ".*"
  proxy: null
  api_url: ""
```

The plugin rewrites `config.yml` on every load. Comments you add are lost, only the built-in comments stay.

## Bot commands in Telegram

| Command        | Where               | What it does                                                    |
|----------------|---------------------|-----------------------------------------------------------------|
| `/vanilla`     | The bridged chat    | Lists the players online                                        |
| `/link <code>` | The bridged chat    | Links a Minecraft account. The player gets the code with `/link` in game |
| `/minfo`       | Any chat            | Replies with the chat id, the topic id and the chat type        |
| `/bind <code>` | The chat to bridge  | Binds the chat. The code comes from `mb telegram bind`          |

Commands also work with the bot's name, e.g. `/vanilla@my_server_chat_bot`.

## Account linking

1. The player runs `/link` in game and gets a code.
2. The player sends `/link <code>` into the bridged chat.

The Telegram account must have a public username (Telegram **Settings** → **Username**).
If the `link` block is set in `config.yml`, the player gets the LuckPerms group from `linkLuckPermsRole`.
See [Account linking in discord.md](discord.md#account-linking) for the `link` block.

## Things to know

### One bot per server

Telegram gives the messages of a bot to one program at a time. If two servers, or a server and some other program,
use the same token, both get `409 Conflict: terminated by other getUpdates request`. Create a bot for every server.

### Other limits

- Bots do not see messages of other bots.
- Telegram lets a bot send about 20 messages per minute into one group.
- A message that is older than 10 seconds when the server receives it is ignored. Keep the server clock correct.
- Messages longer than `max_telegram_message_length` are deleted and not relayed.

More in [limitations.md](limitations.md).
