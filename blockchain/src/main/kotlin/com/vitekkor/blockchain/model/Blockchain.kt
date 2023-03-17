package com.vitekkor.blockchain.model

import com.vitekkor.blockchain.util.sha256

data class Blockchain(
    val index: Long,
    val previousHash: String,
    val hash: String,
    val data: String,
    val nonce: Long
) {
    fun validate() {
        val hashString = "$index$previousHash$data$nonce"
        val sha256String = hashString.sha256()
        require(sha256String.takeLast(4).all { it == '0' })
    }
}
