package com.lmax.disruptor.immutable

interface EventAccessor<T> {
    fun take(sequence: Long): T
}
