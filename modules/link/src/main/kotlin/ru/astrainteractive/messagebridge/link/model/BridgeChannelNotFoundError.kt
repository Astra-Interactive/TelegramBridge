package ru.astrainteractive.messagebridge.link.model

internal class BridgeChannelNotFoundError(channelId: String) :
    Exception("The bot cannot see the Discord bridge channel $channelId")
