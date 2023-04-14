package com.vitekkor.blockchain.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("blockchain")
data class GenerationStrategyProperties(val generationStrategyName: GenerationStrategy)

enum class GenerationStrategy {
    INCREMENT, RANDOM, FIBONACCI
}
