package com.lmax.disruptor.immutable

import java.util.concurrent.locks.LockSupport

import com.lmax.disruptor.BatchEventProcessor
import com.lmax.disruptor.EventTranslatorOneArg
import com.lmax.disruptor.RingBuffer
import com.lmax.disruptor.YieldingWaitStrategy
import kotlin.system.measureNanoTime

class SimplePerformanceTest {
    private val ringBuffer: RingBuffer<EventHolder> = RingBuffer.createSingleProducer(EventHolder.FACTORY, Constants.SIZE, YieldingWaitStrategy())
    private val eventHolderHandler: EventHolderHandler = EventHolderHandler(SimpleEventHandler())

    fun run() {
        try {
            doRun()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

    }

    @Throws(InterruptedException::class)
    private fun doRun() {
        val batchEventProcessor = BatchEventProcessor(
                ringBuffer,
                ringBuffer.newBarrier(),
                eventHolderHandler)
        ringBuffer.addGatingSequences(batchEventProcessor.sequence)

        val t = Thread(batchEventProcessor)
        t.start()

        for (l in 0 until Constants.ITERATIONS) {
            val e = SimpleEvent(l, l, l, l)
            ringBuffer.publishEvent(TRANSLATOR, e)
        }

        while (batchEventProcessor.sequence.get() != Constants.ITERATIONS - 1) {
            LockSupport.parkNanos(1)
        }

        batchEventProcessor.halt()
        t.join()
    }

    companion object {

        private val TRANSLATOR = EventTranslatorOneArg<EventHolder, SimpleEvent> { holder, arg1, event -> holder.event = event }

        @JvmStatic
        fun main(args: Array<String>) {
            measureNanoTime {
                SimplePerformanceTest().run()
            }.apply {
                println("$this nanos")
                //4621960362 nanos
                //4632672575 nanos
                //4109025033 nanos
            }
        }
    }
}
