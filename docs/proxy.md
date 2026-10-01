# Proxy

[Telegram](telegram.md) · [Discord](discord.md) · [Commands](commands.md) ·
[Troubleshooting](troubleshooting.md) · [Limitations](limitations.md)

Telegram and Discord are blocked in some countries, for example in Russia. Then the server cannot reach them
and the log shows errors like `UnknownHostException`, `connect timed out` or `Connection reset`.
MessageBridge can connect through a proxy. Telegram and Discord have separate proxy settings.

## What works

| Proxy                         | Telegram | Discord |
|-------------------------------|----------|---------|
| HTTP                          | Yes      | Yes     |
| HTTP with login and password  | Yes      | Yes     |
| SOCKS5                        | Yes      | No      |
| SOCKS5 with login and password | No      | No      |
| Own Bot API server (`api_url`) | Yes     | —       |

- Discord needs an HTTP proxy. Its websocket library supports only HTTP proxies (`CONNECT`).
- SOCKS5 works only without a password. Java needs a global authenticator for SOCKS passwords,
  and the plugin does not install one.
- If you have one proxy for both, use its HTTP port.

## Set the proxy

Telegram:

```
mb telegram proxy http 127.0.0.1 10809
mb telegram proxy socks5 127.0.0.1 10808
mb telegram proxy http proxy.example.com 3128 user password
mb telegram proxy off
```

Discord:

```
mb discord proxy http 127.0.0.1 10809
mb discord proxy http proxy.example.com 3128 user password
mb discord proxy off
```

The addresses and ports here are examples, use your own.
In game, a command with a password needs `--unsafe` at the end, because the server writes player commands
into `logs/latest.log`. The console does not need it.

In `config.yml` the proxy looks like this, in `tgConfig` and in `jdaConfig`:

```yaml
  proxy:
    type: "HTTP"        # HTTP or SOCKS5
    host: "127.0.0.1"
    port: 10809
    username: null      # or "user"
    password: null      # or "password"
```

`proxy: null` means a direct connection.

Then run `mb telegram check` and `mb discord check`. They show whether the bot can connect through the proxy.

## Option 1: a proxy client on the same machine

Clients like xray, v2ray, sing-box, Clash or Hiddify run on the server machine and open local ports,
usually one SOCKS5 port and one HTTP port on `127.0.0.1`. Some clients have one "mixed" port for both.
Look up the ports in the client settings.

- Use the HTTP port for Discord.
- Use the HTTP or the SOCKS5 port for Telegram.

If the Minecraft server runs in Docker (many hosting panels, e.g. Pterodactyl, do this),
`127.0.0.1` inside the container is the container itself, not the machine.
Use the address of the machine as the container sees it (often `172.17.0.1`),
and make the proxy client listen on that address, not only on `127.0.0.1`.

## Option 2: a remote HTTP proxy

A proxy on another machine or from a provider. Take the host, the port and, if needed, the login and password:

```
mb telegram proxy http proxy.example.com 3128 user password
mb discord proxy http proxy.example.com 3128 user password
```

If the provider gives only SOCKS5 with a password, run a local client that connects to it
and opens an HTTP port without a password, then use Option 1.

## Option 3: own Bot API server for Telegram

Instead of a proxy, Telegram can go through a server that you run outside the blocked network.
Set its address in `api_url`:

```
mb telegram api-url https://tg.example.com
mb telegram api-url default
```

`default` returns to `https://api.telegram.org`.

**Whoever runs this server sees your bot token and all messages. Only use a server you control.**
Do not use public mirrors.

### Reverse proxy of api.telegram.org

A VPS outside the blocked network with nginx that forwards requests to `api.telegram.org`:

```nginx
server {
    listen 443 ssl;
    server_name tg.example.com;
    # ssl_certificate ... ; ssl_certificate_key ... ;

    location / {
        proxy_pass https://api.telegram.org;
        proxy_set_header Host api.telegram.org;
        proxy_ssl_server_name on;
        proxy_read_timeout 120s;
    }
}
```

The bot waits for new messages with long requests, so keep `proxy_read_timeout` above 100 seconds.

### Self-hosted Bot API server

[telegram-bot-api](https://github.com/tdlib/telegram-bot-api) is the official Bot API server that you can run yourself.
It listens on port 8081 by default, e.g. `mb telegram api-url http://127.0.0.1:8081` if it runs on the same machine.

Before moving a bot to it, log the bot out of the cloud Bot API once, as its documentation says:
open `https://api.telegram.org/bot<token>/logOut` from a network where Telegram is not blocked.

## Still does not connect

See [troubleshooting.md → Proxy](troubleshooting.md#proxy).
