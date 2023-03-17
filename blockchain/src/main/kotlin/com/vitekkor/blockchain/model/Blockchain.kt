package com.vitekkor.blockchain.model

data class Blockchain(val index: Long, val previousHash: String, val hash: String, val data: String, val nonce: String)
