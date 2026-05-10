package com.tankobun.core.network

fun interface TimeSource {
    fun nowMillis(): Long
}

object SystemTimeSource : TimeSource {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
