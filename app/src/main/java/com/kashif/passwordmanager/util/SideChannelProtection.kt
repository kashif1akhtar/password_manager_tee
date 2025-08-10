package com.kashif.passwordmanager.util

import kotlin.random.Random

class SideChannelProtection {

    fun secureOperation(operation: () -> ByteArray): ByteArray {
        val startTime = System.nanoTime()

        // Add computational noise
        addComputationalNoise()

        val result = operation()

        // Ensure constant execution time
        val minExecutionTime = 100_000_000L // 100ms in nanoseconds
        val elapsed = System.nanoTime() - startTime
        if (elapsed < minExecutionTime) {
            Thread.sleep((minExecutionTime - elapsed) / 1_000_000L)
        }

        return result
    }

    private fun addComputationalNoise() {
        // Perform dummy operations to mask power consumption patterns
        val random = Random.Default
        repeat(random.nextInt(100, 500)) {
            Math.sin(random.nextDouble()) * Math.cos(random.nextDouble())
        }
    }

    fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false

        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }
}