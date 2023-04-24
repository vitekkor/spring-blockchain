package com.vitekkor.blockchain.service

import com.ninjasquad.springmockk.SpykBean
import com.vitekkor.blockchain.configuration.properties.GenerationStrategy
import com.vitekkor.blockchain.configuration.properties.GenerationStrategyProperties
import com.vitekkor.blockchain.model.HttpOutgoingMessage
import com.vitekkor.blockchain.util.generateData
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.awaitility.Awaitility
import org.awaitility.Durations
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.test.assertTrue

@AutoConfigureMockMvc
@SpringBootTest
internal class GenerationStrategyTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var blockGeneratorService: BlockGeneratorService

    @SpykBean
    private lateinit var generationStrategyProperties: GenerationStrategyProperties

    @Test
    fun fibonacciStrategyTest() {
        every { generationStrategyProperties.generationStrategyName } returns GenerationStrategy.FIBONACCI
        mockkStatic(::generateData)
        every { generateData() } returns "Мимо тещиного дома я без шуток не хожу"
        ReflectionTestUtils.setField(blockGeneratorService, "lastNonce", 14000L)

        mockMvc.perform(MockMvcRequestBuilders.get("/start"))
            .andExpect(MockMvcResultMatchers.status().isOk)

        lateinit var result: HttpOutgoingMessage.BlockChainMessage

        Awaitility.await().atMost(Durations.ONE_MINUTE).untilAsserted {
            mockMvc.perform(MockMvcRequestBuilders.get("/blockChain"))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andDo {
                    result = Json.decodeFromString(it.response.contentAsString)
                    assertTrue(result.blocks.isNotEmpty())
                }
            mockMvc.perform(MockMvcRequestBuilders.get("/stop"))
                .andExpect(MockMvcResultMatchers.status().isOk)
            assertTrue(result.blocks.size == 1)
        }
    }

    @Test
    fun randomStrategyTest() {
        every { generationStrategyProperties.generationStrategyName } returns GenerationStrategy.RANDOM
        mockkStatic(::generateData)
        every { generateData() } returns "Мимо тещиного дома я без шуток не хожу"

        mockMvc.perform(MockMvcRequestBuilders.get("/start"))
            .andExpect(MockMvcResultMatchers.status().isOk)

        lateinit var result: HttpOutgoingMessage.BlockChainMessage

        Awaitility.await().atMost(Durations.ONE_MINUTE).untilAsserted {
            mockMvc.perform(MockMvcRequestBuilders.get("/blockChain"))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andDo {
                    result = Json.decodeFromString(it.response.contentAsString)
                    assertTrue(result.blocks.isNotEmpty())
                }
            mockMvc.perform(MockMvcRequestBuilders.get("/stop"))
                .andExpect(MockMvcResultMatchers.status().isOk)
            assertTrue(result.blocks.size == 1)
        }
    }

    companion object {
        @JvmStatic
        @AfterAll
        fun unmock() {
            unmockkAll()
        }
    }
}
