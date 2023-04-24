package com.vitekkor.blockchain.service

import com.ninjasquad.springmockk.MockkBean
import com.vitekkor.blockchain.model.Block
import com.vitekkor.blockchain.model.HttpOutgoingMessage
import com.vitekkor.blockchain.util.generateData
import io.mockk.every
import io.mockk.mockkStatic
import org.awaitility.Awaitility.await
import org.awaitility.Durations
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.util.ReflectionTestUtils
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
internal class BlockGeneratorServiceTest {

    @Autowired
    private lateinit var blockGeneratorService: BlockGeneratorService

    @MockkBean
    private lateinit var nodeClient: NodeClient

    @BeforeEach
    fun cleanUp() {
        ReflectionTestUtils.setField(blockGeneratorService, "blocks", mutableListOf<Block>())
        every { nodeClient.sendNewBlock(any()) } returns true
    }

    @Test
    fun blocksGenerationTest()  {
        mockkStatic(::generateData)
        every { generateData() } returns "Не до конца раскрыта тема природы в данном блокчейне..."

        ReflectionTestUtils.setField(blockGeneratorService, "lastNonce", 130000L)

        blockGeneratorService.start()
        await().atMost(Durations.ONE_MINUTE).untilAsserted {
            assertTrue(blockGeneratorService.getBlockChain().isNotEmpty())
        }
        ReflectionTestUtils.setField(blockGeneratorService, "lastNonce", -1L)
    }

    @Test
    fun oneBlockGenerationTest()  {
        mockkStatic(::generateData)
        every { generateData() } returns "Не до конца раскрыта тема природы в данном блокчейне..."
        ReflectionTestUtils.setField(blockGeneratorService, "nodeClients", emptyList<NodeClient>())

        blockGeneratorService.start()
        await().atMost(Durations.ONE_MINUTE).untilAsserted {
            assertTrue(blockGeneratorService.getBlockChain().isNotEmpty())
            blockGeneratorService.stop()
            assertTrue(blockGeneratorService.getBlockChain().size == 1)
        }
    }

    @Test
    fun validateNewBlockAndSaveTest() {
        val genesisBlock = Block(
            index = 1,
            previousHash = "",
            hash = "2f8660a39f4ef07a7cd784b5f5140197d95addd17183d25837580df586170000",
            data = "p9PmGmjLj5N0qxaP4MW6yHBsGQxODwhcQdaDFV4CIkOSi3UU0fn6YK4R",
            nonce = 8344120720967856628L
        )
        val correctBlock = Block(
            index = 2,
            previousHash = "2f8660a39f4ef07a7cd784b5f5140197d95addd17183d25837580df586170000",
            hash = "6e176cdeabbcaf2ef2a5c9013e5242180bb790b3b556c9b2b0d13daafe0f0000",
            data = "p4nLxoVZPrj3ZFQv2W8G3LaSHk3uUFOZWi0RmeN9RnnhB3vmy0cD25CqL8CSThagAX",
            nonce = -8988250733424672812
        )
        val invalidBlock1 = Block(
            index = 3,
            previousHash = "6e176cdeabbcaf2ef2a5c9013e5242180bb790b3b556c9b2b0d13daafe0f0000",
            hash = "7d44fc1536dafeb3a635c45b90299b0c08d16235f4d01a81d32eed8fb963ddf7",
            data = "r3MGJAD2dPWDUG2PTHeD55ZhqtJyrW05AF8GHEBWF6kQWBpBFKd2UEp",
            nonce = 15227L
        )
        val invalidBlock2 = Block(
            index = 3,
            previousHash = "2f8660a39f4ef07a7cd784b5f5140197d95addd17183d25837580df586170000",
            data = "r3MGJAD2dPWDUG2PTHe",
            nonce = 78591L
        )

        var result = blockGeneratorService.validateNewBlockAndSave(genesisBlock)
        var expected: HttpOutgoingMessage = HttpOutgoingMessage.BlockAcceptedMessage(genesisBlock)
        assertEquals(expected, result)
        assertTrue(blockGeneratorService.getBlockChain().size == 1)

        result = blockGeneratorService.validateNewBlockAndSave(correctBlock)
        expected = HttpOutgoingMessage.BlockAcceptedMessage(correctBlock)

        assertEquals(expected, result)
        assertTrue(blockGeneratorService.getBlockChain().size == 2)

        result = blockGeneratorService.validateNewBlockAndSave(invalidBlock1)
        expected = HttpOutgoingMessage.BlockValidationError("Invalid block", invalidBlock1)

        assertEquals(expected, result)
        assertTrue(blockGeneratorService.getBlockChain().size == 2)

        result = blockGeneratorService.validateNewBlockAndSave(invalidBlock2)
        expected = HttpOutgoingMessage.BlockValidationError("Invalid previous hash", invalidBlock2)

        assertEquals(expected, result)
        assertEquals(listOf(genesisBlock, correctBlock), blockGeneratorService.getBlockChain())
    }

    @Test
    fun invalidGenesisBlockTest() {
        val genesisBlock1 = Block(
            index = 1,
            previousHash = "notEmpty",
            data = "YWqZKR2UAiQXefb9Evq3ysY7Bhqw1ApGhuYbTysL6AQePYZBKTGAHQJ0CS2lHPUUvVGSiIPNslDoB2Wsx1NIx1lbNZWifFwGiu9k",
            nonce = 50138
        )
        var result = blockGeneratorService.validateNewBlockAndSave(genesisBlock1)
        var expected = HttpOutgoingMessage.BlockValidationError("Invalid block - is not a genesis", genesisBlock1)
        assertEquals(expected, result)
        assertTrue(blockGeneratorService.getBlockChain().isEmpty())

        val genesisBlock2 = Block(
            index = 5,
            previousHash = "",
            data = "YWqZKR2UAiQXefb9Evq3ysY7Bhqw1ApGhuYbT",
            nonce = 206343
        )
        result = blockGeneratorService.validateNewBlockAndSave(genesisBlock2)
        expected = HttpOutgoingMessage.BlockValidationError("Invalid block - is not a genesis", genesisBlock2)
        assertEquals(expected, result)
        assertTrue(blockGeneratorService.getBlockChain().isEmpty())
    }
}
