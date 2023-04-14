package com.vitekkor.blockchain.service

import com.vitekkor.blockchain.configuration.properties.GenerationStrategy
import com.vitekkor.blockchain.configuration.properties.GenerationStrategyProperties
import com.vitekkor.blockchain.model.Block
import com.vitekkor.blockchain.util.fibonacci
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging.logger
import org.springframework.stereotype.Service
import javax.annotation.PreDestroy
import kotlin.random.Random
import kotlin.streams.asSequence


@Service
class BlockGeneratorService(
    private val nodeClients: List<NodeClient>,
    private val generationStrategyProperties: GenerationStrategyProperties
) {
    private val log = logger {}

    private val scope = CoroutineScope(Dispatchers.Default)
    private val job = scope.launch(start = CoroutineStart.LAZY) {
        while (true) {
            generateBlock()
        }
    }

    private var lastIndex = -1L
    private val blocks = ArrayList<Block>()

    private var lastNonce = 1L


    fun generateGenesis() {
        generateBlock()
    }

    private fun generateBlock() {
        do {
            val newBlock = Block(
                index = lastIndex,
                previousHash = getPreviousHash(),
                data = generateData(),
                nonce = generateNonce()
            )
            try {
                newBlock.validate()
                val accepted = sendNewBlock(newBlock)
                if (!accepted) {
                    TODO()
                    continue
                }
                blocks.add(newBlock)
                lastIndex++
                return
            } catch (e: IllegalArgumentException) {
                log.info { "Illegal block $newBlock. Regenerate..." }
            }
        } while (true)
    }

    fun getPreviousHash(): String {
        return blocks.lastOrNull()?.hash ?: ""
    }

    private fun generateData(): String {
        val source = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        val length = Random.nextLong(0, 256)
        return java.util.Random().ints(length, 0, source.size)
            .asSequence()
            .map(source::get)
            .joinToString("")
    }

    private fun generateNonce(): Long {
        return when (generationStrategyProperties.generationStrategyName) {
            GenerationStrategy.INCREMENT -> lastNonce++
            GenerationStrategy.RANDOM -> Random.nextLong()
            GenerationStrategy.FIBONACCI -> fibonacci(lastNonce++)
        }
    }

    fun sendNewBlock(block: Block): Boolean {
        return nodeClients.map { nodeClient ->
            nodeClient.sendNewBlock(block)
        }.all { it }
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
