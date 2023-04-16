package com.vitekkor.blockchain.service

import com.vitekkor.blockchain.configuration.properties.GenerationStrategy
import com.vitekkor.blockchain.configuration.properties.GenerationStrategyProperties
import com.vitekkor.blockchain.exception.NoBlocksInNodeException
import com.vitekkor.blockchain.model.Block
import com.vitekkor.blockchain.model.HttpOutgoingMessage
import com.vitekkor.blockchain.util.fibonacci
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging.logger
import org.springframework.stereotype.Service
import java.util.Collections
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
    private val newBlockGenerationJob
        get() = scope.launch(start = CoroutineStart.LAZY) {
            while (isActive) {
                generateBlock()
            }
        }

    private var job = newBlockGenerationJob

    private val lastIndex
        get() = blocks.lastOrNull()?.index ?: 0L
    private val blocks = Collections.synchronizedList(ArrayList<Block>())

    private var lastNonce = -1L


    fun generateGenesis() = runBlocking {
        generateBlock()
    }

    private suspend fun generateBlock() {
        val data = generateData()
        val previousHash = getPreviousHash()
        do {
            val newBlock = Block(
                index = lastIndex + 1,
                previousHash = previousHash,
                data = data,
                nonce = generateNonce()
            )
            try {
                newBlock.validate()
                if (newBlock.previousHash != previousHash) {
                    continue
                }
                val accepted = sendNewBlock(newBlock)
                if (!accepted) {
                    val lastBlock = nodeClients.random().getLastBlock()
                    if (blocks.lastOrNull()?.hash == lastBlock.previousHash) {
                        blocks.add(lastBlock)
                    } else {
                        val blockChain = nodeClients.random().getBlockChain()
                        blocks.clear()
                        blocks.addAll(blockChain)
                    }
                    continue
                }
                blocks.add(newBlock)
                return
            } catch (e: IllegalArgumentException) {
                log.info { "Invalid block $newBlock. Regenerate..." }
            }
        } while (job.isActive)
    }

    private fun getPreviousHash(): String {
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
            GenerationStrategy.FIBONACCI -> fibonacci(++lastNonce)
        }
    }

    fun sendNewBlock(block: Block): Boolean {
        return nodeClients.map { nodeClient ->
            nodeClient.sendNewBlock(block)
        }.all { it }
    }

    fun start() {
        if (job.isCancelled) {
            job = newBlockGenerationJob
        }
        job.start()
    }

    fun stop() {
        job.cancel()
    }

    fun getLastBlock(): Block = blocks.lastOrNull() ?: throw NoBlocksInNodeException()

    fun getBlockChain(): List<Block> = blocks

    fun validateNewBlockAndSave(newBlock: Block): HttpOutgoingMessage {
        return try {
            newBlock.validate()
            if (blocks.isEmpty() && newBlock.previousHash == "") {
                log.info { "Accept genesys block $newBlock" }
                blocks.add(newBlock)
                return HttpOutgoingMessage.BlockAcceptedMessage(newBlock)
            }
            if (newBlock.previousHash == getLastBlock().hash) {
                blocks.add(newBlock)
                log.info { "New block $newBlock accepted." }
                HttpOutgoingMessage.BlockAcceptedMessage(newBlock)
            } else {
                log.info { "New block $newBlock doesn't accepted - invalid previous hash" }
                HttpOutgoingMessage.BlockValidationError("Invalid previous hash", newBlock)
            }
        } catch (e: IllegalArgumentException) {
            log.info { "Invalid new block $newBlock" }
            HttpOutgoingMessage.BlockValidationError("Invalid block", newBlock)
        }
    }

    @PreDestroy
    private fun destroyBlockGeneratorService() {
        job.cancel()
    }
}
