package com.vitekkor.blockchain.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("blockchain")
data class NodesProperties(val nodes: List<Node>)

data class Node(val uri: String)
