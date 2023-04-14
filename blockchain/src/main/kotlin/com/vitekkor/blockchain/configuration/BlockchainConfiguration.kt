package com.vitekkor.blockchain.configuration

import com.vitekkor.blockchain.configuration.properties.NodesProperties
import com.vitekkor.blockchain.service.NodeClient
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BlockchainConfiguration {
    @Bean
    fun nodeClients(nodesProperties: NodesProperties, restTemplateBuilder: RestTemplateBuilder): List<NodeClient> {
        return nodesProperties.nodes.map {
            val restTemplate = restTemplateBuilder.rootUri("${it.address}:${it.port}").build()
            NodeClient(restTemplate, it)
        }
    }
}
