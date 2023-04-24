package com.vitekkor.blockchain.stub

import com.vitekkor.blockchain.model.Block
import com.vitekkor.blockchain.model.HttpOutgoingMessage
import org.springframework.stereotype.Component

@Component
class NodeStubDelegate {
    @Volatile
    var blocks = listOf<Block>()

    @Volatile
    lateinit var blockValidationLambda: (Block) -> HttpOutgoingMessage

    @Volatile
    lateinit var lastBlockLambda: (List<Block>) -> Block

    @Volatile
    var getBlockChainPreHook = {}

    fun getLastBlock(): Block {
        return lastBlockLambda(blocks)
    }

    fun getBlockChain(): List<Block> {
        getBlockChainPreHook()
        return blocks
    }

    fun validateNewBlockAndSave(block: Block): HttpOutgoingMessage {
        return blockValidationLambda(block)
    }
}
