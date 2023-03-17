package com.vitekkor.blockchain.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("udp")
data class UDPProperties(val port: Int)
