package com.vitekkor.blockchain.model

sealed class UDPIncomingMessage {
    data class NewBlockMessage(val block: Blockchain): UDPIncomingMessage()

    object GetLastBlockMessage : UDPIncomingMessage()

    object WhoIsThereMessage: UDPIncomingMessage()
}
