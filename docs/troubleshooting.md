# Troubleshooting

[Telegram](telegram.md) · [Discord](discord.md) · [Proxy](proxy.md) ·
[Commands](commands.md) · [Limitations](limitations.md)

Search this page for the text of the error (Ctrl+F).

## Start here

1. `mb status` shows both bots, the chat and the channel, the proxy and the last error of sending a message.
2. `mb telegram check` and `mb discord check` check every step and say what to fix.
3. The server log is `logs/latest.log`. The lines of the plugin contain `MessageBridge`.

Commands are typed in the console without `/`. See [commands.md](commands.md).

## config.yml

### config.yml changes were ignored: config.yml has an error and is not applied

The log shows something like:

```
config.yml has an error and is not applied, the previous settings are kept: line 12, column 3: ...
```

The file has a YAML error. The plugin keeps working with the previous settings and does not touch the file.
`mb reload` reports the same error.

Fix:

1. Open `config.yml` at the line and column from the message.
2. Look for the usual mistakes: tabs instead of spaces, wrong indentation, a missing quote,
   a missing space after `:`.
3. Put tokens and ids in double quotes: `chat_id: "-1001234567890"`.
4. Run `mb reload`.

To avoid this, change settings with the `/mb` commands instead of editing the file.

### My comments in config.yml disappeared

The plugin rewrites `config.yml` on every load, so your comments are lost. The built-in comments stay.

## Telegram: token and connection

### 401 Unauthorized / 404 Not Found: wrong or revoked token

The token is wrong, has a typo or was revoked in @BotFather.

Fix: copy the token again from @BotFather → `/mybots` → your bot → **API Token**, then run
`mb telegram token <token>`. The token looks like `123456789:AAHd3...` without spaces or quotes around it.

### 409 Conflict: terminated by other getUpdates request

Another program uses the same bot token: a second server, a test server, an old copy of the plugin
or another bot program. Telegram gives the messages of a bot to one program at a time.

Fix: create a separate bot in @BotFather for every server. Or stop the other program.
If you do not know what uses the token, revoke it in @BotFather and set the new one.

### Cannot reach Telegram: UnknownHostException, connect timed out

The server cannot connect to `api.telegram.org`. Usually Telegram is blocked in the country or by the hosting.

Fix: set up a proxy or an own Bot API server, see [proxy.md](proxy.md).
Then run `mb telegram check`.

### 429 Too Many Requests: retry after

Telegram lets a bot send about 20 messages per minute into one group. A busy server chat or many joins and leaves
go over it.

Fix: it goes away by itself. If it happens often, turn off `displayJoinMessage` and `displayLeaveMessage`
in `config.yml`.

## Telegram: chat and topic

### Bad Request: chat not found

The bot does not know this chat.

- `chat_id` has a typo. Group ids are negative, supergroup ids start with `-100`.
- The bot is not in the group.
- The group became a supergroup and has a new id, see the next section.

Fix: add the bot to the group and run `mb telegram bind`, see [telegram.md](telegram.md#4-choose-the-chat-and-the-topic).

### Group chat was upgraded to a supergroup chat (migrate_to_chat_id)

Turning on topics, making the group public and some other settings turn a group into a supergroup with a new id.
When Telegram reports it, the plugin updates `chat_id` by itself.

Fix, if the id did not update: run `mb telegram bind` and send the code into the group again.

### Bad Request: message thread not found / message to be replied not found

The topic from `topic_id` was deleted, or `topic_id` belongs to another group.
In a group without topics, `topic_id` is the id of a message, and that message was deleted.

Fix: run `mb telegram bind` and send the code inside the right topic, or run `mb telegram topic none`.

### Forbidden: bot was kicked from the group chat / bot is not a member of the chat

The bot was removed from the group.

Fix: add it again, make it an admin, and run `mb telegram check`.

### Not enough rights: message can't be deleted / not enough rights to send text messages

The bot is not an admin, or it has no **Delete messages** right, or the group does not let members write.

Fix: group settings → **Administrators** → the bot → turn on **Delete messages**.
See [telegram.md](telegram.md#3-add-the-bot-to-the-group).

### Messages from Telegram don't reach Minecraft

Go through the list:

1. **Privacy mode.** A bot that is not an admin, with Group Privacy on, only sees commands.
   Make it an admin, or turn privacy off in @BotFather and add the bot to the group again.
   See [Privacy mode](telegram.md#privacy-mode).
2. **Wrong topic.** Only the topic from `topic_id` is relayed. With an empty `topic_id` in a group with topics,
   only **General** is relayed. See [What gets relayed](telegram.md#what-gets-relayed).
3. **Wrong chat.** Send `/minfo` in the group and compare the id with `chat_id` in `mb status`.
4. **Server clock.** Messages older than 10 seconds when they arrive are ignored.
   If the clock of the server is behind or ahead, every message looks old. Turn on time sync (NTP) on the machine.
5. **Another bot.** Bots do not see messages of other bots.
6. **Another program with the same token** takes the messages, see [409 Conflict](#409-conflict-terminated-by-other-getupdates-request).
7. **The message is too long** or the name does not match `display_name_regex`: the bot deletes such messages.
   Only text messages are relayed.

### Messages from Minecraft don't reach Telegram

1. `mb status` shows the last error of sending a message. Find it on this page.
2. Check that the bot can write into the group and the topic: `mb telegram check` sends a test message.
3. Join, leave and death messages can be turned off with `displayJoinMessage`, `displayLeaveMessage`
   and `displayDeathMessage` in `config.yml`.

## Discord

### Invalid token: The provided token is invalid

The token is wrong or was reset in the Developer Portal.

Fix: Developer Portal → your application → **Bot** → **Reset Token**, copy it and run `mb discord token <token>`.
Take the token from the **Bot** tab, not the Client Secret from **OAuth2**.

### Disallowed intents (close code 4014): turn on Message Content Intent

The bot asks Discord for an intent that is off in the Developer Portal: always Message Content Intent, and
Server Members Intent when the `link` block gives roles.

Fix: Developer Portal → your application → **Bot** → **Privileged Gateway Intents** →
turn on **Message Content Intent** (and **Server Members Intent** with `link` set) → **Save Changes**.
Then run `mb reload`.

### Cannot reach Discord: UnknownHostException, connect timed out

The server cannot connect to Discord. Usually Discord is blocked in the country or by the hosting.

Fix: set up an HTTP proxy with `mb discord proxy http <host> <port>`, see [proxy.md](proxy.md).
Discord does not work through SOCKS5.

### Channel not found: Could not find channel

- `channelId` has a typo.
- The bot is not on this Discord server. Add it with `mb discord invite`.
- The bot cannot see the channel. Allow **View Channel** for the bot in the channel settings.
- The channel is not a normal text channel: announcement channels, threads, forums and voice channels do not work.

Fix: run `mb discord bind` and send the code into the channel.

### Missing permission: MANAGE_WEBHOOKS, MESSAGE_SEND, VIEW_CHANNEL...

The bot has no permission for what it tries to do. The message names the permission.

Fix: give the bot this permission on the server and in the channel settings, if the channel has its own.
The simplest way is to add the bot again with the link from `mb discord invite`.
The list of permissions is in [discord.md](discord.md#3-add-the-bot-to-your-server).

### Webhook cannot be created (Manage Webhooks)

Chat messages from Minecraft are sent through the webhook `BRIDGE_HOOK_<channelId>`. The bot creates it itself.

- The bot has no **Manage Webhooks** permission in the channel. Give it.
- The channel already has 15 webhooks, the Discord limit. Delete unused ones in the channel settings
  → **Integrations** → **Webhooks**.

The plugin retries every 30 seconds, no restart is needed.

### Link role is above the bot: Can't modify a role with higher or equal highest role

The bot can give only roles that are lower than its own highest role.

Fix: **Server Settings** → **Roles** → drag the bot's role above the role from `linkDiscordRole`.
The bot also needs **Manage Roles**.

### The online count is not shown in the channel topic

The bot needs **Manage Channel** in this channel. Discord also limits how often a topic can change,
so the count can lag behind.

### Messages from Discord don't reach Minecraft

1. `mb discord check`: the bot is connected and sees the channel.
2. `channelId` is the channel you write in.
3. Message Content Intent is on.
4. Messages of other bots and of webhooks are not relayed.

## Proxy

### Proxy authentication failed: 407 Proxy Authentication Required

The proxy did not accept the login or the password, or needs them and none are set.

Fix: set them again, in game with `--unsafe` at the end:

```
mb telegram proxy http <host> <port> <username> <password>
mb discord proxy http <host> <port> <username> <password>
```

### SOCKS5 with a password is not supported

SOCKS5 works only without a password, and only for Telegram.
Java needs a global authenticator for SOCKS passwords, and the plugin does not install one.

Fix: use the HTTP port of the proxy. If there is only SOCKS5 with a password, run a local proxy client
that connects to it and opens an HTTP port without a password. See [proxy.md](proxy.md).

### Discord does not connect through SOCKS5

Discord works only through an HTTP proxy. Its websocket library supports only HTTP proxies.

Fix: `mb discord proxy http <host> <port>`. Local proxy clients usually have an HTTP port next to the SOCKS5 port.

### Invalid api_url

`api_url` must be the address of a Bot API server, e.g. `https://tg.example.com` or `http://127.0.0.1:8081`,
without `/bot<token>` at the end.

Fix: `mb telegram api-url <url>`, or `mb telegram api-url default` to go back to `api.telegram.org`.
See [proxy.md](proxy.md#option-3-own-bot-api-server-for-telegram).
