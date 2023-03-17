package com.vitekkor.blockchain.controller

import com.vitekkor.blockchain.model.UDPIncomingMessage
import mu.KotlinLogging.logger
import org.springframework.stereotype.Service

@Suppress("unused")
@Service
class UDPController {
    private val log = logger {}
    fun handle(udpIncomingMessage: UDPIncomingMessage) {
        when (udpIncomingMessage) {
            UDPIncomingMessage.GetLastBlockMessage -> TODO("Get last block")
            is UDPIncomingMessage.NewBlockMessage -> {
                try {
                    udpIncomingMessage.block.validate()
                } catch (e: IllegalArgumentException) {
                    log.error { "Invalid block ${udpIncomingMessage.block}" }
                }
                log.info("New last block accepted: ${udpIncomingMessage.block}")
                TODO("Set as last block")
            }

            UDPIncomingMessage.WhoIsThereMessage -> TODO("Receive node port and host")
        }
    }
}
