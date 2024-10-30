[![kotlin_version](https://img.shields.io/badge/kotlin-1.7.0-blueviolet?style=flat-square)](https://github.com/Astra-Interactive/AstraLibs)
[![minecraft_version](https://img.shields.io/badge/minecraft-1.19-green?style=flat-square)](https://github.com/Astra-Interactive/AstraLibs)
[![platforms](https://img.shields.io/badge/platform-spigot-blue?style=flat-square)](https://github.com/Astra-Interactive/AstraLibs)

# MessageBridge

Send Telegram-Discord-Minecraft messages

# Configuration

```yaml
# Discord bot configuration
jdaConfig:
  token: ""
  activity: "play.EmpireProjekt.ru"
  channelId: ""
  # Your VPS located in countries that block discord? Use this proxy config
  proxy:
    host: "0.0.0.0"
    port: 2222
    username: "user"
    password: "password"
# Telegram bot configuration    
tgConfig:
  token: "TOKEN"
  chat_id: "-11111111"
  topic_id: "1111111"
  max_telegram_message_length: 140
# In-Game events
displayJoinMessage: true
displayLeaveMessage: true
displayDeathMessage: true
# Want to enable linking?
link:
  linkDiscordRole: "123213123123123"
  linkLuckPermsRole: "verified"
```

# Platforms

- [x] Spigot/Paper

Also, checkout [AstraLearner](https://play.google.com/store/apps/details?id=com.makeevrserg.astralearner) - it will help you to learn foreign words easily!