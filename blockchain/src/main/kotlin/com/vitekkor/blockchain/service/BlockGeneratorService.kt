package com.vitekkor.blockchain.service

import com.vitekkor.blockchain.model.Block
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging.logger
import org.springframework.stereotype.Service
import javax.annotation.PreDestroy


@Service
class BlockGeneratorService(private val nodeClients: List<NodeClient>) {
    private val log = logger {}

    private val scope = CoroutineScope(Dispatchers.Default)
    private val job = scope.launch(start = CoroutineStart.LAZY) {
        while (true) {
            generateBlocks()
        }
    }

    private fun generateBlocks() {
        TODO("Not yet implemented")
    }

    fun sendNewBlock(block: Block) {
        nodeClients.forEach { nodeClient ->
            nodeClient.sendNewBlock(block)
        }
        TODO("Implement logic")
    }

    fun start() {
        job.start()
    }

    fun stop() {
        job.cancel()
    }

    @PreDestroy
    private fun destroyBlockGeneratorService() {
        job.cancel()
    }
}
