package com.lmax.disruptor.immutable

import com.lmax.disruptor.EventHandler

class SimpleEventHandler : EventHandler<SimpleEvent> {
    var counter: Long = 0

    @Throws(Exception::class)
    override fun onEvent(arg0: SimpleEvent, arg1: Long, arg2: Boolean) {
        counter += arg0.counter
    }
}
