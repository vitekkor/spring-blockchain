package com.vitekkor.blockchain.model

import kotlinx.serialization.Serializable

@Serializable
sealed class HttpOutgoingMessage {
    @Serializable
    data class BlockValidationError(val message: String, val block: Block): HttpOutgoingMessage()

    @Serializable
    data class BlockAcceptedMessage(val block: Block): HttpOutgoingMessage()

    @Serializable
    data class LastBlockMessage(val lastBlock: Block) : HttpOutgoingMessage()

    @Serializable
    data class BlockChainMessage(val blocks: List<Block>) : HttpOutgoingMessage()
}
