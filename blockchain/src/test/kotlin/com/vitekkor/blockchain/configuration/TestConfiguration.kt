package com.vitekkor.blockchain.configuration

import com.vitekkor.blockchain.service.NodeClient
import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TestConfiguration {
    @Bean
    fun testNodeClients(): List<NodeClient> {
        return listOf(Mockito.mock(NodeClient::class.java))
    }
}
