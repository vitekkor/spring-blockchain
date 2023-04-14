package com.vitekkor.blockchain.service

import com.vitekkor.blockchain.model.Block
import org.springframework.stereotype.Service

@Service
class BlockGeneratorService(private val nodeClients: List<NodeClient>) {

    fun sendNewBlock(block: Block) {
        nodeClients.forEach { nodeClient ->
            nodeClient.sendNewBlock(block)
        }
        TODO("Implement logic")
    }
}
