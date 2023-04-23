package com.vitekkor.blockchain.service

import com.vitekkor.blockchain.exception.NoBlocksInNodeException
import com.vitekkor.blockchain.model.Block
import com.vitekkor.blockchain.model.HttpIncomingMessage
import com.vitekkor.blockchain.model.HttpOutgoingMessage
import com.vitekkor.blockchain.stub.NodeStubDelegate
import com.vitekkor.blockchain.util.generateData
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.awaitility.Awaitility
import org.awaitility.Durations
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.getForEntity
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.util.ReflectionTestUtils
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = ["blockchain.nodes=http://localhost:8080/stub"]
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
internal class NodeClientTest {

    @Autowired
    private lateinit var blockGeneratorService: BlockGeneratorService

    @Autowired
    private lateinit var testRestTemplate: TestRestTemplate

    @Autowired
    private lateinit var nodeStubDelegate: NodeStubDelegate

    @OptIn(ExperimentalSerializationApi::class)
    private val blocks = checkNotNull(this::class.java.getResourceAsStream("/blocks.json")?.let {
        Json.decodeFromStream<List<Block>>(it)
    })

    @BeforeEach
    fun cleanUp() {
        ReflectionTestUtils.setField(blockGeneratorService, "blocks", mutableListOf<Block>())
        ReflectionTestUtils.setField(blockGeneratorService, "lastNonce", -1L)
        nodeStubDelegate.blocks = emptyList()
        nodeStubDelegate.getBlockChainPreHook = {}
        nodeStubDelegate.lastBlockLambda = { it.last() }
        nodeStubDelegate.blockValidationLambda = { HttpOutgoingMessage.BlockAcceptedMessage(it) }
    }

    @Test
    fun allBlocksNotAcceptedTest() {
        nodeStubDelegate.blocks = blocks
        nodeStubDelegate.blockValidationLambda = {
            if (it.index == 11L) {
                HttpOutgoingMessage.BlockAcceptedMessage(it)
            } else {
                HttpOutgoingMessage.BlockValidationError("Invalid previous hash", it)
            }
        }
        var i = 0
        nodeStubDelegate.lastBlockLambda = {
            i++
            ReflectionTestUtils.setField(blockGeneratorService, "lastNonce", it[i + 1].nonce - 100)
            it[i]
        }
        mockkStatic(::generateData)
        every { generateData() } returnsMany blocks.map { it.data }

        assertTrue(testRestTemplate.getForEntity<String>("/start").statusCode.is2xxSuccessful)

        val genesis = HttpIncomingMessage.NewBlockMessage(blocks.first())

        testRestTemplate.postForEntity<HttpOutgoingMessage>("/newBlock", genesis).let {
            assertTrue(it.statusCode.is2xxSuccessful)
            assertNotNull(it.body)
            assertEquals(HttpOutgoingMessage.BlockAcceptedMessage(genesis.block), it.body)
        }

        ReflectionTestUtils.setField(blockGeneratorService, "lastNonce", 39400)

        Awaitility.await().atMost(Durations.ONE_MINUTE.multipliedBy(2)).untilAsserted {
            testRestTemplate.getForEntity<HttpOutgoingMessage.BlockChainMessage>("/blockChain").let {
                assertTrue(it.statusCode.is2xxSuccessful)
                assertNotNull(it.body)
                assertTrue(it.body?.blocks?.size == 11)
                assertTrue(testRestTemplate.getForEntity<String>("/stop").statusCode.is2xxSuccessful)
                assertTrue(testRestTemplate.getForEntity<String>("/start").statusCode.is2xxSuccessful)
            }
        }
    }

    @Test
    fun getAllBlocksTest() {
        nodeStubDelegate.blocks = blocks
        nodeStubDelegate.blockValidationLambda = {
            if (it.index == 11L) {
                HttpOutgoingMessage.BlockAcceptedMessage(it)
            } else {
                HttpOutgoingMessage.BlockValidationError("Invalid previous hash", it)
            }
        }
        nodeStubDelegate.lastBlockLambda = {
            it.last()
        }
        mockkStatic(::generateData)
        every { generateData() } returnsMany blocks.map { it.data }

        assertTrue(testRestTemplate.getForEntity<String>("/start").statusCode.is2xxSuccessful)

        val genesis = HttpIncomingMessage.NewBlockMessage(blocks.first())

        testRestTemplate.postForEntity<HttpOutgoingMessage>("/newBlock", genesis).let {
            assertTrue(it.statusCode.is2xxSuccessful)
            assertNotNull(it.body)
            assertEquals(HttpOutgoingMessage.BlockAcceptedMessage(genesis.block), it.body)
        }

        ReflectionTestUtils.setField(blockGeneratorService, "lastNonce", 39400)

        Awaitility.await().atMost(Durations.ONE_MINUTE.multipliedBy(2)).untilAsserted {
            testRestTemplate.getForEntity<HttpOutgoingMessage.BlockChainMessage>("/blockChain").let {
                assertTrue(it.statusCode.is2xxSuccessful)
                assertNotNull(it.body)
                assertTrue(it.body?.blocks?.size == 11)
            }
        }
    }

    @Test
    fun nodeHasNoBlocksTest() {
        nodeStubDelegate.blocks = blocks.dropLast(1)
        nodeStubDelegate.blockValidationLambda = {
            if (it.index == 11L) {
                HttpOutgoingMessage.BlockAcceptedMessage(it)
            } else {
                HttpOutgoingMessage.BlockValidationError("Invalid previous hash", it)
            }
        }
        nodeStubDelegate.lastBlockLambda = {
            throw NoBlocksInNodeException()
        }

        nodeStubDelegate.getBlockChainPreHook = {
            ReflectionTestUtils.setField(blockGeneratorService, "lastNonce", (blocks.last().nonce - 1000L))
        }
        mockkStatic(::generateData)
        every { generateData() } returnsMany (blocks.drop(1).take(1) + blocks.takeLast(1)).map { it.data }

        assertTrue(testRestTemplate.getForEntity<String>("/start").statusCode.is2xxSuccessful)

        val genesis = HttpIncomingMessage.NewBlockMessage(blocks.first())

        testRestTemplate.postForEntity<HttpOutgoingMessage>("/newBlock", genesis).let {
            assertTrue(it.statusCode.is2xxSuccessful)
            assertNotNull(it.body)
            assertEquals(HttpOutgoingMessage.BlockAcceptedMessage(genesis.block), it.body)
        }

        ReflectionTestUtils.setField(blockGeneratorService, "lastNonce", 39400)

        Awaitility.await().atMost(Durations.ONE_MINUTE.multipliedBy(2)).untilAsserted {
            testRestTemplate.getForEntity<HttpOutgoingMessage.BlockChainMessage>("/blockChain").let {
                assertTrue(it.statusCode.is2xxSuccessful)
                assertNotNull(it.body)
                assertTrue(it.body?.blocks?.size == 11)
            }
        }
    }

    @Test
    fun getBlockChainExceptionTest() {
        mockkStatic(::generateData)
        every { generateData() } returnsMany blocks.map { it.data }
        var i = 0

        nodeStubDelegate.blocks = blocks.dropLast(1)
        nodeStubDelegate.blockValidationLambda = {
            if (it.index == 11L || (it.index == 1L && i > 0)) {
                HttpOutgoingMessage.BlockAcceptedMessage(it)
            } else {
                HttpOutgoingMessage.BlockValidationError("Invalid previous hash", it)
            }
        }

        nodeStubDelegate.lastBlockLambda = {
            if (i++ > 0) {
                nodeStubDelegate.getBlockChainPreHook = {}
            } else {
                ReflectionTestUtils.setField(blockGeneratorService, "lastNonce", 25300)
                Thread.sleep(1000)
            }
            it.last()
        }
        nodeStubDelegate.getBlockChainPreHook = {
            throw NoBlocksInNodeException()
        }

        assertTrue(testRestTemplate.getForEntity<String>("/start").statusCode.is2xxSuccessful)

        ReflectionTestUtils.setField(blockGeneratorService, "lastNonce", 25300)

        Awaitility.await().atMost(Durations.ONE_MINUTE.multipliedBy(2)).untilAsserted {
            testRestTemplate.getForEntity<HttpOutgoingMessage.BlockChainMessage>("/blockChain").let {
                assertTrue(it.statusCode.is2xxSuccessful)
                assertNotNull(it.body)
                assertTrue(it.body?.blocks?.size == 11)
            }
        }
    }

    @Test
    fun sendNewNodeExceptionTest() {
        mockkStatic(::generateData)
        every { generateData() } returnsMany blocks.filter { it.index == 1L || it.index == 11L }.map { it.data }

        nodeStubDelegate.blocks = blocks.dropLast(1)
        nodeStubDelegate.blockValidationLambda = {
            throw NoBlocksInNodeException()
        }

        nodeStubDelegate.getBlockChainPreHook = {
            ReflectionTestUtils.setField(blockGeneratorService, "lastNonce", (blocks.last().nonce - 1000L))
        }

        nodeStubDelegate.lastBlockLambda = {
            nodeStubDelegate.blockValidationLambda = { block ->
                HttpOutgoingMessage.BlockAcceptedMessage(block)
            }
            it.last()
        }

        assertTrue(testRestTemplate.getForEntity<String>("/start").statusCode.is2xxSuccessful)

        ReflectionTestUtils.setField(blockGeneratorService, "lastNonce", 25300)

        Awaitility.await().atMost(Durations.ONE_MINUTE.multipliedBy(2)).untilAsserted {
            testRestTemplate.getForEntity<HttpOutgoingMessage.BlockChainMessage>("/blockChain").let {
                assertTrue(it.statusCode.is2xxSuccessful)
                assertNotNull(it.body)
                assertTrue(it.body?.blocks?.size == 11)
            }
        }
    }

    @Test
    fun generateGenesisTest() {
        mockkStatic(::generateData)
        every { generateData() } returns "Не до конца раскрыта тема природы в данном блокчейне..."

        nodeStubDelegate.blockValidationLambda = {
            assertEquals(1L, it.index)
            assertEquals("", it.previousHash)
            HttpOutgoingMessage.BlockAcceptedMessage(it)
        }

        ReflectionTestUtils.setField(blockGeneratorService, "lastNonce", 130000L)

        assertTrue(testRestTemplate.getForEntity<String>("/generateGenesys").statusCode.is2xxSuccessful)

        testRestTemplate.getForEntity<HttpOutgoingMessage.BlockChainMessage>("/blockChain").let {
            assertTrue(it.statusCode.is2xxSuccessful)
            assertNotNull(it.body)
            assertTrue(it.body?.blocks?.size == 1)
        }
    }

}
