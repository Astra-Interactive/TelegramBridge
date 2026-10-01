# Прокси

[English version](../en/proxy.md) · [Telegram](telegram.md) · [Discord](discord.md) · [Команды](commands.md) ·
[Решение проблем](troubleshooting.md) · [Ограничения](limitations.md)

В России и некоторых других странах Telegram и Discord заблокированы. Сервер не может до них достучаться,
а в логе появляются ошибки вроде `UnknownHostException`, `connect timed out` или `Connection reset`.
MessageBridge умеет подключаться через прокси. У Telegram и Discord прокси настраиваются отдельно.

## Что поддерживается

| Прокси                          | Telegram | Discord |
|---------------------------------|----------|---------|
| HTTP                            | Да       | Да      |
| HTTP с логином и паролем        | Да       | Да      |
| SOCKS5                          | Да       | Нет     |
| SOCKS5 с логином и паролем      | Нет      | Нет     |
| Свой сервер Bot API (`api_url`) | Да       | —       |

- Discord работает только через HTTP-прокси: его библиотека для websocket умеет только HTTP-прокси (`CONNECT`).
- SOCKS5 работает только без пароля. Java для паролей SOCKS требует глобальный аутентификатор,
  а плагин его не ставит.
- Если прокси один на оба бота, используйте его HTTP-порт.

## Как задать прокси

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

Адреса и порты здесь для примера — подставьте свои.
В игре команду с паролем нужно закончить `--unsafe`: сервер записывает команды игроков в `logs/latest.log`.
В консоли это не нужно.

В `config.yml` прокси выглядит так — и в `tgConfig`, и в `jdaConfig`:

```yaml
  proxy:
    type: "HTTP"        # HTTP или SOCKS5
    host: "127.0.0.1"
    port: 10809
    username: null      # или "user"
    password: null      # или "password"
```

`proxy: null` — прямое подключение.

Затем выполните `mb telegram check` и `mb discord check` — они покажут, подключается ли бот через прокси.

## Вариант 1: прокси-клиент на той же машине

Клиенты вроде xray, v2ray, sing-box, Clash или Hiddify запускаются на машине с сервером и открывают локальные порты —
обычно один SOCKS5 и один HTTP на `127.0.0.1`. У некоторых клиентов один «смешанный» (mixed) порт для обоих.
Номера портов смотрите в настройках клиента.

- Для Discord используйте HTTP-порт.
- Для Telegram подойдёт и HTTP, и SOCKS5.

Если сервер Minecraft работает в Docker (так устроены многие хостинг-панели, например Pterodactyl),
`127.0.0.1` внутри контейнера — это сам контейнер, а не машина.
Укажите адрес машины, каким его видит контейнер (часто `172.17.0.1`),
и настройте прокси-клиент слушать этот адрес, а не только `127.0.0.1`.

## Вариант 2: внешний HTTP-прокси

Прокси на другой машине или купленный у провайдера. Нужны адрес, порт и, если требуется, логин и пароль:

```
mb telegram proxy http proxy.example.com 3128 user password
mb discord proxy http proxy.example.com 3128 user password
```

Если провайдер даёт только SOCKS5 с паролем, запустите локальный клиент, который подключится к нему
и откроет HTTP-порт без пароля, — и дальше как в варианте 1.

## Вариант 3: свой сервер Bot API для Telegram

Вместо прокси Telegram может ходить через сервер, который вы держите за пределами блокировок.
Его адрес задаётся в `api_url`:

```
mb telegram api-url https://tg.example.com
mb telegram api-url default
```

`default` возвращает `https://api.telegram.org`.

**Тот, кто держит этот сервер, видит токен бота и все сообщения. Используйте только свой сервер.**
Не пользуйтесь публичными зеркалами.

### Обратный прокси для api.telegram.org

VPS за пределами блокировок с nginx, который пересылает запросы на `api.telegram.org`:

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

Бот ждёт новые сообщения долгими запросами, поэтому `proxy_read_timeout` должен быть больше 100 секунд.

### Свой сервер Bot API

[telegram-bot-api](https://github.com/tdlib/telegram-bot-api) — официальный сервер Bot API, который можно запустить
у себя. По умолчанию он слушает порт 8081, например `mb telegram api-url http://127.0.0.1:8081`,
если он запущен на той же машине.

Перед переносом бота на свой сервер один раз выведите его из облачного Bot API, как сказано в документации:
откройте `https://api.telegram.org/bot<токен>/logOut` из сети, где Telegram не заблокирован.

## Всё равно не подключается

См. [troubleshooting.md → Прокси](troubleshooting.md#прокси).
