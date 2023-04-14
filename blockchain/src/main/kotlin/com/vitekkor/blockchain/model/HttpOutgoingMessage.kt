package com.vitekkor.blockchain.model

sealed class HttpOutgoingMessage {
    data class BlockValidationError(val message: String, val block: Block): HttpOutgoingMessage()

    data class BlockAcceptedMessage(val block: Block): HttpOutgoingMessage()

    data class LastBlockMessage(val block: Block) : HttpIncomingMessage()
}
