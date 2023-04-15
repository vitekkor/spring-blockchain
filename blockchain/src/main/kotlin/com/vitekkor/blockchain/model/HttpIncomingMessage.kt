package com.vitekkor.blockchain.model

import kotlinx.serialization.Serializable

@Serializable
sealed class HttpIncomingMessage {
    @Serializable
    data class NewBlockMessage(val block: Block): HttpIncomingMessage()
}
