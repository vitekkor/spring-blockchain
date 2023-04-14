package com.vitekkor.blockchain.model

sealed class HttpIncomingMessage {
    data class NewBlockMessage(val block: Block): HttpIncomingMessage()
}
