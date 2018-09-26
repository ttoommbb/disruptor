package com.lmax.disruptor.primitive

import com.lmax.disruptor.BatchEventProcessor
import com.lmax.disruptor.DataProvider
import com.lmax.disruptor.EventHandler
import com.lmax.disruptor.Sequencer

class LongRingBuffer(private val sequencer: Sequencer) {
    private val buffer: LongArray = LongArray(sequencer.bufferSize)
    private val mask: Int = sequencer.bufferSize - 1

    private fun index(sequence: Long): Int {
        return sequence.toInt() and mask
    }

    private fun LongArray.putSeq(sequence: Long, value: Long) {
        this[index(sequence)] = value
    }

    fun put(e: Long) {
        val next = sequencer.next()
        buffer.putSeq(next, e)
        sequencer.publish(next)
    }

    inner class LongEvent : DataProvider<LongEvent> {
        private var sequence: Long = 0

        fun get(): Long {
            return buffer[index(sequence)]
        }

        override fun get(sequence: Long): LongEvent {
            this.sequence = sequence
            return this
        }
    }

    fun createProcessor(handler: (value: Long, sequence: Long, endOfBatch: Boolean) -> Unit): BatchEventProcessor<LongEvent> {
        return BatchEventProcessor(
                LongEvent(),
                sequencer.newBarrier(),
                EventHandler { event, sequence, endOfBatch -> handler.invoke(event.get(), sequence, endOfBatch) })
    }
}
