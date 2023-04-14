package com.vitekkor.blockchain.service

import com.vitekkor.blockchain.configuration.properties.Node
import com.vitekkor.blockchain.model.Block
import com.vitekkor.blockchain.model.HttpOutgoingMessage
import mu.KotlinLogging.logger
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import org.springframework.web.client.postForObject

class NodeClient(private val restTemplate: RestTemplate, private val node: Node) {

    private val log = logger {}
    fun getLastBlock(): Block {
        return restTemplate.getForObject<HttpOutgoingMessage.LastBlockMessage>("/lastBlock").block
    }

    fun getBlockChain(): List<Block> {
        return restTemplate.getForObject<HttpOutgoingMessage.BlockChainMessage>("/blockChain").blocks
    }

    fun sendNewBlock(block: Block): Boolean {
        return when (val result = restTemplate.postForObject<HttpOutgoingMessage>("/newBlock", block)) {
            is HttpOutgoingMessage.BlockValidationError -> {
                log.warn("Block ${result.block} not accepted by node ${node.address}:${node.port}: ${result.message}")
                false
            }

            is HttpOutgoingMessage.BlockAcceptedMessage -> {
                log.warn("Block ${result.block} accepted by node ${node.address}:${node.port}")
                true
            }
        }
    }
}
