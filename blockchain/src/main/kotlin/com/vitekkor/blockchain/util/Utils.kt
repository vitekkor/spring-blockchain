package com.vitekkor.blockchain.util

import java.security.MessageDigest
import kotlin.random.Random
import kotlin.streams.asSequence

fun String.sha256(): String {
    val bytes = this.toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.fold("") { str, it -> str + "%02x".format(it) }
}

tailrec fun fibonacci(n: Long, a: Long = 0, b: Long = 1): Long = when (n) {
    0L -> a
    1L -> b
    else -> fibonacci(n - 1, b, a + b)
}

fun generateData(): String {
    val source = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    val length = Random.nextLong(0, 256)
    return java.util.Random().ints(length, 0, source.size)
        .asSequence()
        .map(source::get)
        .joinToString("")
}
