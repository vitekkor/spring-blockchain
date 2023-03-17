package com.vitekkor.blockchain.configuration

import com.vitekkor.blockchain.configuration.properties.UDPProperties
import com.vitekkor.blockchain.model.UDPIncomingMessage
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.dsl.IntegrationFlow
import org.springframework.integration.dsl.integrationFlow
import org.springframework.integration.ip.dsl.Udp
import org.springframework.integration.ip.dsl.UdpInboundChannelAdapterSpec
import org.springframework.integration.ip.dsl.UdpUnicastOutboundChannelAdapterSpec
import org.springframework.messaging.Message

@Configuration
class BlockchainConfiguration {
    @Bean
    fun udpInboundChannel(udpProperties: UDPProperties): UdpInboundChannelAdapterSpec {
        return Udp.inboundAdapter(udpProperties.port)
    }

    @Bean
    fun udpOutboundChannel(udpProperties: UDPProperties): UdpUnicastOutboundChannelAdapterSpec {
        return Udp.outboundAdapter("localhost", udpProperties.port)
    }

    @Bean
    fun udpIntegrationFlow(udpInboundChannel: UdpInboundChannelAdapterSpec): IntegrationFlow = integrationFlow(udpInboundChannel) {
        log<Message<Any>> { "Incoming udp message: $it" }
        transform<Message<Any>> { it.payload as UDPIncomingMessage }
        handle("UDPController", "handle")
    }
}
