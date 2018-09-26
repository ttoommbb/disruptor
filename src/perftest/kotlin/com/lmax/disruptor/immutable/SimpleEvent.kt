package com.lmax.disruptor.immutable

data class SimpleEvent(private val id: Long, val counter: Long, private val v2: Long, private val v3: Long) {

    override fun toString(): String {
        return "SimpleEvent [id=$id, v1=$counter, v2=$v2, v3=$v3]"
    }
}