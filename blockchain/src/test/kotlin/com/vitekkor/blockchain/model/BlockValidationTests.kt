package com.vitekkor.blockchain.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class BlockValidationTests {
    @Test
    fun hashEndsWith4ZerosTest() {
        val correctBlock = Block(
            index = 2,
            previousHash = "2f8660a39f4ef07a7cd784b5f5140197d95addd17183d25837580df586170000",
            hash = "6e176cdeabbcaf2ef2a5c9013e5242180bb790b3b556c9b2b0d13daafe0f0000",
            data = "p4nLxoVZPrj3ZFQv2W8G3LaSHk3uUFOZWi0RmeN9RnnhB3vmy0cD25CqL8CSThagAX",
            nonce = -8988250733424672812
        )
        val invalidBlock = Block(
            index = 2,
            previousHash = "05284ad85c0779093a3325448d3985f944b19d0535c120dac05c900f24c40000",
            hash = "7d44fc1536dafeb3a635c45b90299b0c08d16235f4d01a81d32eed8fb963ddf7",
            data = "r3MGJAD2dPWDUG2PTHeD55ZhqtJyrW05AF8GHEBWF6kQWBpBFKd2UEp",
            nonce = 15227L
        )
        val blockWithAutoCreatedHash = Block(
            index = 2,
            previousHash = "2f8660a39f4ef07a7cd784b5f5140197d95addd17183d25837580df586170000",
            data = "p4nLxoVZPrj3ZFQv2W8G3LaSHk3uUFOZWi0RmeN9RnnhB3vmy0cD25CqL8CSThagAX",
            nonce = -8988250733424672812
        )
        assertDoesNotThrow { correctBlock.validate() }
        assertDoesNotThrow { blockWithAutoCreatedHash.validate() }
        assertThrows<IllegalArgumentException> { invalidBlock.validate() }
    }

    @Test
    fun invalidHashTest() {
        val blockWithInvalidHash = Block(
            index = 2,
            previousHash = "2f8660a39f4ef07a7cd784b5f5140197d95addd17183d25837580df586170000",
            hash = "7d44fc1536dafeb3a635c45b90299b0c08d16235f4d01a81d32eed8fb963ddf7",
            data = "p4nLxoVZPrj3ZFQv2W8G3LaSHk3uUFOZWi0RmeN9RnnhB3vmy0cD25CqL8CSThagAX",
            nonce = -8988250733424672812
        )
        assertThrows<IllegalArgumentException> { blockWithInvalidHash.validate() }
    }
}
