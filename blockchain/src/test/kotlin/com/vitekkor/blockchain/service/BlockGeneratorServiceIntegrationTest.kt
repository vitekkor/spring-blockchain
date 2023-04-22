package com.vitekkor.blockchain.service

import com.ninjasquad.springmockk.MockkBean
import com.vitekkor.blockchain.model.Block
import com.vitekkor.blockchain.model.HttpIncomingMessage
import com.vitekkor.blockchain.model.HttpOutgoingMessage
import com.vitekkor.blockchain.util.generateData
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.awaitility.Awaitility
import org.awaitility.Durations
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Duration
import java.time.temporal.ChronoUnit
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@AutoConfigureMockMvc
@SpringBootTest
internal class BlockGeneratorServiceIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var blockGeneratorService: BlockGeneratorService

    @MockkBean
    private lateinit var nodeClient: NodeClient

    @BeforeEach
    fun cleanUp() {
        ReflectionTestUtils.setField(blockGeneratorService, "blocks", mutableListOf<Block>())
        ReflectionTestUtils.setField(blockGeneratorService, "lastNonce", -1L)
    }

    @Test
    fun startStopTest() {
        mockkStatic(::generateData)
        every { generateData() } returns "Не до конца раскрыта тема природы в данном блокчейне..."
        every { nodeClient.sendNewBlock(any()) } returns true

        mockMvc.perform(get("/start"))
            .andExpect(status().isOk)

        lateinit var result: HttpOutgoingMessage.BlockChainMessage

        ReflectionTestUtils.setField(blockGeneratorService, "lastNonce", 130000L)

        Awaitility.await().atMost(Duration.of(20, ChronoUnit.SECONDS)).untilAsserted {
            mockMvc.perform(get("/blockChain"))
                .andExpect(status().isOk)
                .andDo {
                    result = Json.decodeFromString(it.response.contentAsString)
                    assertTrue(result.blocks.isNotEmpty())
                }
            mockMvc.perform(get("/stop"))
                .andExpect(status().isOk)
            assertTrue(result.blocks.size == 1)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun allBlocksNotAcceptedTest() {
        val blocks = checkNotNull(this::class.java.getResourceAsStream("/blocks.json")?.let {
            Json.decodeFromStream<List<Block>>(it)
        })
        val blocksIterator = blocks.drop(1).iterator()
        var nextBlock = blocksIterator.next()

        every { nodeClient.sendNewBlock(any()) } returns false
        every { nodeClient.getLastBlock() } answers {
            val answer = nextBlock
            if (blocksIterator.hasNext()) {
                nextBlock = blocksIterator.next()
                    .also { ReflectionTestUtils.setField(blockGeneratorService, "lastNonce", it.nonce - 100L) }
            }
            answer
        }

        mockkStatic(::generateData)
        every { generateData() } returnsMany blocks.map { it.data }

        mockMvc.perform(get("/start"))
            .andExpect(status().isOk)

        val genesis = HttpIncomingMessage.NewBlockMessage(blocks.first())

        mockMvc.perform(post("/newBlock").contentType(MediaType.APPLICATION_JSON).content(Json.encodeToString(genesis)))
            .andExpect(status().isOk)

        ReflectionTestUtils.setField(blockGeneratorService, "lastNonce", 39400)

        lateinit var result: HttpOutgoingMessage.BlockChainMessage

        Awaitility.await().atMost(Durations.ONE_MINUTE).untilAsserted {
            mockMvc.perform(get("/blockChain"))
                .andExpect(status().isOk)
                .andDo {
                    result = Json.decodeFromString(it.response.contentAsString)
                    assertTrue(result.blocks.size == 11)
                }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun nodeValidatesIncomingBlocksTest() {
        val blocks = checkNotNull(this::class.java.getResourceAsStream("/blocks.json")?.let {
            Json.decodeFromStream<List<Block>>(it)
        })

        every { nodeClient.sendNewBlock(any()) } returns Random(42).nextBoolean()
        every { nodeClient.getLastBlock() } returnsMany blocks.drop(1)

        mockkStatic(::generateData)
        every { generateData() } returnsMany blocks.map { it.data }

        mockMvc.perform(get("/start"))
            .andExpect(status().isOk)

        val genesis = HttpIncomingMessage.NewBlockMessage(blocks.first())

        mockMvc.perform(post("/newBlock").contentType(MediaType.APPLICATION_JSON).content(Json.encodeToString(genesis)))
            .andExpect(status().isOk)

        for ((i, block) in blocks.withIndex()) {
            val newBlock = HttpIncomingMessage.NewBlockMessage(block)
            if (i == 0) {
                mockMvc.perform(
                    post("/newBlock").contentType(MediaType.APPLICATION_JSON).content(Json.encodeToString(newBlock))
                ).andExpect(status().isOk).andDo {
                    val actualResponse = Json.decodeFromString<HttpOutgoingMessage>(it.response.contentAsString)
                    val expectedResponse = HttpOutgoingMessage.BlockValidationError("Invalid previous hash", block)
                    assertEquals(expectedResponse, actualResponse)
                }
            } else {
                if (Random.nextBoolean()) {
                    val invalidBlock = block.copy(index = Random.nextLong())
                    val newBlockMessage = HttpIncomingMessage.NewBlockMessage(invalidBlock)
                    mockMvc.perform(
                        post("/newBlock").contentType(MediaType.APPLICATION_JSON)
                            .content(Json.encodeToString(newBlockMessage))
                    ).andExpect(status().isOk).andDo {
                        val actualResponse = Json.decodeFromString<HttpOutgoingMessage>(it.response.contentAsString)
                        val expectedResponse = HttpOutgoingMessage.BlockValidationError("Invalid block", invalidBlock)
                        assertEquals(expectedResponse, actualResponse)
                    }
                }

                mockMvc.perform(
                    post("/newBlock").contentType(MediaType.APPLICATION_JSON).content(Json.encodeToString(newBlock))
                ).andExpect(status().isOk).andDo {
                    val actualResponse = Json.decodeFromString<HttpOutgoingMessage>(it.response.contentAsString)
                    val expectedResponse = HttpOutgoingMessage.BlockAcceptedMessage(block)
                    assertEquals(expectedResponse, actualResponse)
                }
            }
        }

        lateinit var result: HttpOutgoingMessage.BlockChainMessage

        Awaitility.await().atMost(Durations.TEN_SECONDS).untilAsserted {
            mockMvc.perform(get("/blockChain"))
                .andExpect(status().isOk)
                .andDo {
                    result = Json.decodeFromString(it.response.contentAsString)
                    assertTrue(result.blocks.size == 11)
                }
        }
    }

    @Test
    fun notGenesysBlockTest() {
        val genesisBlock1 = Block(
            index = 2,
            previousHash = "dc577c987e7a02bdf926947a626a6997c063b39c3d21d75728b44656056a0000",
            data = "JvquhZKtNmabbGlabOG56tsDzvOR8vEXF4bzh1KkrNpDBSqFPi8F9Esw9cY10yKMmyrcTKlJAMvZGRknWlp2605xeODeXbp6OrZieapvKt01db8A7MEYvSGdThxZk2miwk79XuYAakY8Sf52E236GKFfBPDKJgeTax5sfIC87Ey5SXNG184tAwV77VpNLw0yc85",
            nonce = 39427
        ).let { HttpIncomingMessage.NewBlockMessage(it) }
        val genesisBlock2 = Block(
            index = 0,
            previousHash = "",
            data = "dABWW8CBi",
            nonce = 2057L
        ).let { HttpIncomingMessage.NewBlockMessage(it) }

        mockMvc.perform(
            post("/newBlock").contentType(MediaType.APPLICATION_JSON).content(Json.encodeToString(genesisBlock1))
        ).andExpect(status().isOk).andDo {
            val actualResponse = Json.decodeFromString<HttpOutgoingMessage>(it.response.contentAsString)
            val expectedResponse =
                HttpOutgoingMessage.BlockValidationError("Invalid block - is not a genesis", genesisBlock1.block)
            assertEquals(expectedResponse, actualResponse)
        }

        mockMvc.perform(
            post("/newBlock").contentType(MediaType.APPLICATION_JSON).content(Json.encodeToString(genesisBlock2))
        ).andExpect(status().isOk).andDo {
            val actualResponse = Json.decodeFromString<HttpOutgoingMessage>(it.response.contentAsString)
            val expectedResponse =
                HttpOutgoingMessage.BlockValidationError("Invalid block - is not a genesis", genesisBlock2.block)
            assertEquals(expectedResponse, actualResponse)
        }
    }
}
