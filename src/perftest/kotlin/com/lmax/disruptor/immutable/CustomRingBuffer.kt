package com.lmax.disruptor.immutable

import com.lmax.disruptor.BatchEventProcessor
import com.lmax.disruptor.DataProvider
import com.lmax.disruptor.EventHandler
import com.lmax.disruptor.LifecycleAware
import com.lmax.disruptor.Sequencer

class CustomRingBuffer<T>(private val sequencer: Sequencer) : DataProvider<EventAccessor<T>>, EventAccessor<T> {

    private class AccessorEventHandler<T> constructor(private val handler: EventHandler<T>) : EventHandler<EventAccessor<T>>, LifecycleAware {
        private val lifecycle: LifecycleAware? = when (handler) {
            is LifecycleAware -> handler
            else -> null
        }

        @Throws(Exception::class)
        override fun onEvent(accessor: EventAccessor<T>, sequence: Long, endOfBatch: Boolean) {
            this.handler.onEvent(accessor.take(sequence), sequence, endOfBatch)
        }

        override fun onShutdown() {
            lifecycle?.onShutdown()
        }

        override fun onStart() {
            lifecycle?.onStart()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private val buffer: Array<T?> = arrayOfNulls<Any>(sequencer.bufferSize) as Array<T?>
    private val mask: Int = sequencer.bufferSize - 1

    private fun index(sequence: Long): Int {
        return sequence.toInt() and mask
    }

    fun put(e: T) {
        val next = sequencer.next()
        buffer[index(next)] = e
        sequencer.publish(next)
    }

    override fun take(sequence: Long): T {
        val index = index(sequence)

        val t = buffer[index]
        buffer[index] = null

        return t!!
    }

    override fun get(sequence: Long): EventAccessor<T> {
        return this
    }

    fun createHandler(handler: EventHandler<T>): BatchEventProcessor<EventAccessor<T>> {
        val processor = BatchEventProcessor(
                this,
                sequencer.newBarrier(),
                AccessorEventHandler(handler))
        sequencer.addGatingSequences(processor.sequence)

        return processor
    }
}
