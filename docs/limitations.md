# Limitations

[Telegram](telegram.md) · [Discord](discord.md) · [Proxy](proxy.md) ·
[Commands](commands.md) · [Troubleshooting](troubleshooting.md)

What MessageBridge, Telegram and Discord do not allow, and what to do about it.

## Telegram

| Limitation | What to do |
|------------|------------|
| One token works in one place at a time. A second server or program with the same token gets `409 Conflict` | Create a bot for every server |
| Bots do not see messages of other bots | Nothing, this is how Telegram works |
| A bot with privacy mode on that is not an admin sees only commands | Make the bot an admin, see [Privacy mode](telegram.md#privacy-mode) |
| About 20 messages per minute into one group. More gets `429 Too Many Requests` | Turn off join and leave messages on a busy server |
| A message older than 10 seconds when the server receives it is ignored | Keep the server clock in sync (NTP) |
| Messages longer than `max_telegram_message_length` (90 by default) are deleted and not relayed | Raise the value in `config.yml` |
| Users without a @username are shown by name. Names that do not match `display_name_regex` get their messages deleted | Keep `display_name_regex: ".*"` to allow every name |
| Only text is relayed. Photos, stickers, voice messages and files are not | — |
| One chat, and at most one topic in it | — |
| Turning on topics or making a group public changes its id | The plugin updates `chat_id` when Telegram reports it, otherwise run `mb telegram bind` again |
| Linking a Telegram account needs a public @username | Set a username in Telegram **Settings** |

## Discord

| Limitation | What to do |
|------------|------------|
| Message Content Intent must be on, and Server Members Intent too when `link` gives roles, otherwise Discord refuses the connection (close code 4014) | Turn them on in the Developer Portal → **Bot** |
| Only an HTTP proxy works, not SOCKS5 | Use the HTTP port of your proxy client |
| One normal text channel. Announcement channels, threads, forums and voice channels do not work | — |
| Messages of bots and webhooks are not relayed | — |
| Chat messages go through a webhook, so the bot needs Manage Webhooks, and a channel has at most 15 webhooks | Delete unused webhooks of the channel |
| The bot can give only roles lower than its own role | Drag the bot's role above the link role |
| Discord limits how often a channel topic can change, so the online count can lag | — |

## Proxy

| Limitation | What to do |
|------------|------------|
| SOCKS5 works only for Telegram | Use HTTP for Discord |
| SOCKS5 with a login and password is not supported | Use HTTP, or a local client that opens an HTTP port without a password |
| Whoever runs the server in `api_url` sees the bot token | Use only a server you control |

See [proxy.md](proxy.md).

## Plugin

| Limitation | What to do |
|------------|------------|
| `config.yml` is rewritten on every load, your own comments are lost | Change settings with the `/mb` commands. The built-in comments stay |
| A `config.yml` with a YAML error is not applied, the previous settings stay | Fix the line from the log or from `mb reload`, see [troubleshooting.md](troubleshooting.md#configyml) |
| In game, tokens and passwords need `--unsafe`, because player commands are written into `logs/latest.log` | Set them from the console |
