package com.vitekkor.blockchain.service

import com.vitekkor.blockchain.configuration.properties.Node
import com.vitekkor.blockchain.model.Block
import com.vitekkor.blockchain.model.HttpIncomingMessage
import com.vitekkor.blockchain.model.HttpOutgoingMessage
import mu.KotlinLogging.logger
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import org.springframework.web.client.postForObject
import java.lang.IllegalStateException

class NodeClient(private val restTemplate: RestTemplate, private val node: Node) {

    private val log = logger {}
    fun getLastBlock(): Block {
        return try {
            restTemplate.getForObject<HttpOutgoingMessage.LastBlockMessage>("/lastBlock").lastBlock
        } catch (e: RestClientException) {
            log.error("Unexpected exception occurred while trying to get last block from the node $node", e)
            Block.EMPTY
        }
    }

    fun getBlockChain(): List<Block> {
        return try {
            restTemplate.getForObject<HttpOutgoingMessage.BlockChainMessage>("/blockChain").blocks
        } catch (e: RestClientException) {
            log.error("Unexpected exception occurred while trying to get blockChain from the node $node", e)
            emptyList()
        }
    }

    fun sendNewBlock(block: Block): Boolean {
        try {
            val result =
                restTemplate.postForObject<HttpOutgoingMessage>("/newBlock", HttpIncomingMessage.NewBlockMessage(block))
            return when (result) {
                is HttpOutgoingMessage.BlockValidationError -> {
                    log.warn("Block ${result.block} not accepted by node ${node.uri}: ${result.message}")
                    false
                }

                is HttpOutgoingMessage.BlockAcceptedMessage -> {
                    log.info("Block ${result.block} accepted by node ${node.uri}")
                    true
                }
                else -> throw IllegalStateException()
            }
        } catch (e: RestClientException) {
            log.error("Unexpected exception occurred while trying to send new block to the node $node", e)
            return false
        }
    }
}
