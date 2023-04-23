package com.vitekkor.blockchain.stub

import com.vitekkor.blockchain.model.Block
import com.vitekkor.blockchain.model.HttpOutgoingMessage
import org.springframework.stereotype.Component

@Component
class NodeStubDelegate {
    var blocks = listOf<Block>()

    lateinit var blockValidationLambda: (Block) -> HttpOutgoingMessage

    lateinit var lastBlockLambda: (List<Block>) -> Block

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
