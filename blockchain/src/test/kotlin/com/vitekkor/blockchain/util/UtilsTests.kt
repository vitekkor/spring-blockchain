package com.vitekkor.blockchain.util

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class UtilsTests {
    @Test
    fun fibonacciTest() {
        val fibonacciNumbers = mapOf(
            0L to 0L,
            1L to 1L,
            2L to 1L,
            3L to 2L,
            4L to 3L,
            5L to 5L,
            6L to 8L,
            7L to 13L,
            8L to 21L,
            9L to 34L,
            10L to 55L,
            11L to 89L,
            12L to 144L,
            13L to 233L,
            14L to 377L,
            15L to 610L,
            16L to 987L,
            17L to 1597L,
            18L to 2584L,
            19L to 4181L,
            20L to 6765L
        )
        fibonacciNumbers.forEach { (n, expectedResult) ->
            val actualResult = fibonacci(n)
            assertEquals(expectedResult, actualResult)
        }
    }

    @Test
    fun sha256Test() {
        val testString = "съешь ещё этих мягких французских булок, да выпей чаю"
        val sha256 = testString.sha256()
        assertEquals(64, sha256.length)
        assertTrue(sha256.matches("^[0-9a-fA-F]+$".toRegex()))
    }

    @Test
    fun generateDataTest() {
        val random = mockk<Random>()
        every { random.nextLong(0, 256) } returns 5

        every { random.nextInt(0, any()) } returnsMany (0..4).toList()
        assertEquals("ABCDE", generateData(random))

        every { random.nextLong(0, 256) } returns 62
        every { random.nextInt(0, any()) } returnsMany (0..62).toList()
        assertEquals((('A'..'Z') + ('a'..'z') + ('0'..'9')).joinToString(""), generateData(random))
    }
}
