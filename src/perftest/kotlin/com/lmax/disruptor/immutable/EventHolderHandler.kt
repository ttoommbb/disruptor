package com.lmax.disruptor.immutable

import com.lmax.disruptor.EventHandler

class EventHolderHandler(private val delegate: EventHandler<SimpleEvent>) : EventHandler<EventHolder> {

    @Throws(Exception::class)
    override fun onEvent(holder: EventHolder, sequence: Long, endOfBatch: Boolean) {
        delegate.onEvent(holder.event, sequence, endOfBatch)
        holder.event = null
    }
}
