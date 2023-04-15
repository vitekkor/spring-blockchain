package com.vitekkor.blockchain.model

import kotlinx.serialization.Serializable

@Serializable
sealed class HttpOutgoingMessage {
    @Serializable
    data class BlockValidationError(val message: String, val block: Block): HttpOutgoingMessage()

    @Serializable
    data class BlockAcceptedMessage(val block: Block): HttpOutgoingMessage()

    @Serializable
    data class LastBlockMessage(val block: Block) : HttpIncomingMessage()

    @Serializable
    data class BlockChainMessage(val blocks: List<Block>) : HttpIncomingMessage()
}
