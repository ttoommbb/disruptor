package com.lmax.disruptor.immutable

import com.lmax.disruptor.EventFactory
import com.lmax.disruptor.EventHandler
import com.lmax.disruptor.SingleProducerSequencer
import com.lmax.disruptor.YieldingWaitStrategy
import com.lmax.disruptor.dsl.Disruptor
import com.lmax.disruptor.dsl.ProducerType
import com.lmax.disruptor.primitive.LongRingBuffer
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.locks.LockSupport
import kotlin.system.measureNanoTime

class LongRingBufferTest {

    @Test
    fun `measure 10 times`() {
        measureXTimes(10, ::`test produce and consume LongRingBuffer`)

        measureXTimes(10, ::`use ringBuffer`)
    }



    @Test
    fun `use ringBuffer`() {
        val eventFactory: EventFactory<Long> = EventFactory { 1L }
        val disruptor = Disruptor(eventFactory, Constants.SIZE, Executors.defaultThreadFactory(), ProducerType.SINGLE, YieldingWaitStrategy())

        disruptor.handleEventsWith(EventHandler<Long> { event, sequence, endOfBatch ->
            //nothing
        })
        disruptor.start()

        for (l in 0 until Constants.ITERATIONS) {
            disruptor.publishEvent { _, _ -> /**/l }
        }

        disruptor.halt()

    }

    @Test
    fun `test produce and consume LongRingBuffer`() {
            val ringBuffer = LongRingBuffer(SingleProducerSequencer(Constants.SIZE, YieldingWaitStrategy()))
            val processor = ringBuffer.createProcessor { value, sequence, endOfBatch ->
                //                println("$value:value, sequence:$sequence, eob:$endOfBatch")
            }

            val t = Thread(processor)
            t.start()
            for (l in 0 until Constants.ITERATIONS) {
                ringBuffer.put(l)
            }

            while (processor.sequence.get() != Constants.ITERATIONS - 1) {
                LockSupport.parkNanos(1)
            }

            processor.halt()
            t.join()
        //357885157 nanos!
    }

    companion object {
        fun measureXTimes(times: Int, block: ()->Unit) {
            repeat(times){
                measureNanoTime(block).apply { println("$this nanos! for $block") }
            }
        }
    }
}