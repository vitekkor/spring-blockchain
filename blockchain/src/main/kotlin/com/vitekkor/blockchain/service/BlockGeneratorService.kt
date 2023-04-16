package com.vitekkor.blockchain.service

import com.vitekkor.blockchain.configuration.properties.GenerationStrategy
import com.vitekkor.blockchain.configuration.properties.GenerationStrategyProperties
import com.vitekkor.blockchain.exception.NoBlocksInNodeException
import com.vitekkor.blockchain.model.Block
import com.vitekkor.blockchain.model.HttpOutgoingMessage
import com.vitekkor.blockchain.util.fibonacci
import com.vitekkor.blockchain.util.generateData
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
                log.info { "Successfully generate block $newBlock!" }
                blocks.add(newBlock)
                return
            } catch (e: IllegalArgumentException) {
                log.trace { "Invalid block $newBlock. Regenerate..." }
            }
        } while (job.isActive)
    }

    private fun getPreviousHash(): String {
        return blocks.lastOrNull()?.hash ?: ""
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
            if (blocks.isEmpty() && newBlock.previousHash == "" && newBlock.index == 1L) {
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
        } catch (e: NoBlocksInNodeException) {
            log.info { "Invalid new block $newBlock - is not a genesis. Retrive blockchain from another node..." }
            HttpOutgoingMessage.BlockValidationError("Invalid block - is not a genesis", newBlock)
        }
    }

    @PreDestroy
    private fun destroyBlockGeneratorService() {
        job.cancel()
    }
}
